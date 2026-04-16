package com.shopflow.product.service;

import com.shopflow.product.exception.FileStorageException;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // ------------------------------------------------------------------
    // UPLOAD
    // ------------------------------------------------------------------

    // Uploads a file to MinIO and returns the object key.
    // The key is stored on ProductImage.imageUrl in the database.
    // Callers build the full URL from the key via getPresignedUrl()
    // or by constructing endpoint + bucket + key directly.
    public String uploadFile(MultipartFile file, String folder) {

        try {
            ensureBucketExists();

            // Build a unique object key: folder/uuid-originalFilename
            // UUID prefix guarantees no collision even if two sellers
            // upload a file with the same name at the same time.
            String originalFilename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "file";

            String objectKey = folder + "/"
                    + UUID.randomUUID()
                    + "-"
                    + sanitiseFilename(originalFilename);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(resolveContentType(file))
                            .build()
            );

            log.info("File uploaded to MinIO — bucket: {} key: {}", bucketName, objectKey);

            return objectKey;

        } catch (Exception ex) {
            log.error("MinIO upload failed — folder: {} filename: {} error: {}",
                    folder, file.getOriginalFilename(), ex.getMessage(), ex);
            throw new FileStorageException(
                    "Failed to upload file: " + ex.getMessage()
            );
        }
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    // Deletes an object from MinIO by its key.
    // Called when a product image is removed or a product is deleted.
    public void deleteFile(String objectKey) {

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );

            log.info("File deleted from MinIO — bucket: {} key: {}", bucketName, objectKey);

        } catch (Exception ex) {
            // Deletion failure is logged but not rethrown.
            // A missing file in MinIO should not block a database delete
            // from completing — the product or image row should still be removed.
            log.error("MinIO delete failed — key: {} error: {}", objectKey, ex.getMessage(), ex);
        }
    }

    // ------------------------------------------------------------------
    // PRESIGNED URL
    // ------------------------------------------------------------------

    // Generates a time-limited presigned GET URL for an object.
    // Used when serving product images to the client — the client
    // calls this URL directly against MinIO without going through
    // the application server, keeping image traffic off the JVM.
    public String getPresignedUrl(String objectKey, int expirySeconds) {

        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );

            log.debug("Presigned URL generated — key: {} expirySeconds: {}",
                    objectKey, expirySeconds);

            return url;

        } catch (Exception ex) {
            log.error("MinIO presigned URL generation failed — key: {} error: {}",
                    objectKey, ex.getMessage(), ex);
            throw new com.shopflow.product.exception.FileStorageException(
                    "Failed to generate presigned URL: " + ex.getMessage()
            );
        }
    }

    // ------------------------------------------------------------------
    // PRIVATE HELPERS
    // ------------------------------------------------------------------

    // Creates the bucket if it does not already exist.
    // Called before every upload — idempotent, cheap after first run
    // because the bucket exists on all subsequent calls.
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );
            log.info("MinIO bucket created: {}", bucketName);
        }
    }

    // Strips path components and special characters from filenames.
    // Prevents directory traversal (../../etc/passwd) and object key
    // conflicts caused by spaces or special characters in filenames.
    private String sanitiseFilename(String filename) {
        // Take only the last segment after any path separator
        String name = filename.replaceAll(".*[/\\\\]", "");
        // Replace anything that is not alphanumeric, dot, or hyphen
        return name.replaceAll("[^a-zA-Z0-9.\\-]", "_");
    }

    // Returns the file's declared content type if present and non-blank.
    // Falls back to application/octet-stream — MinIO requires a content
    // type on every upload; a null or blank value causes the SDK to throw.
    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return (contentType != null && !contentType.isBlank())
                ? contentType
                : "application/octet-stream";
    }
}
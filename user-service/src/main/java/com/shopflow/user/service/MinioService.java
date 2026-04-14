package com.shopflow.user.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
public class MinioService {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private AmazonS3 s3Client;

    // ── Build S3 client pointed at MinIO after bean is created ──
    @PostConstruct
    private void initClient() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);

        s3Client = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(endpoint, "us-east-1")
                )
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)   // required for MinIO
                .build();

        // create bucket if it does not exist yet
        if (!s3Client.doesBucketExistV2(bucketName)) {
            s3Client.createBucket(bucketName);
            log.info("Created MinIO bucket: {}", bucketName);
        }
    }

    // ── Upload ───────────────────────────────────────────────
    // Returns the public URL of the uploaded file.
    // File is stored under avatars/{uuid}-{originalFilename}
    // to avoid name collisions across users.
    public String uploadFile(MultipartFile file) {
        String key = "avatars/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try {
            s3Client.putObject(new PutObjectRequest(bucketName, key, file.getInputStream(), metadata));
        } catch (IOException e) {
            log.error("Failed to upload file to MinIO: {}", e.getMessage());
            throw new RuntimeException("File upload failed", e);
        }

        // returns: http://localhost:9000/shopflow-images/avatars/{uuid}-{filename}
        return endpoint + "/" + bucketName + "/" + key;
    }

    // ── Delete ───────────────────────────────────────────────
    // Extracts the object key from the full URL and deletes it.
    // Safe to call even if the file does not exist — S3 API
    // does not throw on deleting a non-existent key.
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        // strip endpoint + "/" + bucketName + "/" to get the key
        String prefix = endpoint + "/" + bucketName + "/";
        String key = fileUrl.replace(prefix, "");

        s3Client.deleteObject(bucketName, key);
        log.info("Deleted file from MinIO: {}", key);
    }
}
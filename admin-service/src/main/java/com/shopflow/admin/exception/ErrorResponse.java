package com.shopflow.admin.exception;

import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
@Builder
public class ErrorResponse {
    private ZonedDateTime timestamp;
    private int status;
    private String message;
    private String path;
}
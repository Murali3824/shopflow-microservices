package com.shopflow.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ReturnRequestResponse {
    private UUID id;
    private UUID orderId;
    private UUID orderItemId;
    private String reason;
    private String status;
}
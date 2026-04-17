package com.shopflow.order.dto;

import lombok.Data;

@Data
public class ApproveReturnRequest {
    private boolean approved;
    private String note;
}
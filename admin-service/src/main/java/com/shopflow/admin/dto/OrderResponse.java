package com.shopflow.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private String addressSnapshot;
    private String couponCode;
    private String status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private String paymentMethod;
    private LocalDateTime placedAt;     // FIXED: was ZonedDateTime
    private List<OrderItemResponse> items;
    private List<OrderStatusHistoryResponse> statusHistory;
}
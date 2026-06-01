package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

public class Order {
    public enum Status {
        PENDING, CONFIRMED, CANCELLED, COMPLETED, REFUNDED
    }

    private String orderId; // UUID v4
    private String userId;
    private String orderDate;
    private double totalAmount;
    private String statusStr = "pending";
    private String expiresAt;

    public Order() {}

    @PropertyName("order_id")
    public String getOrderId() { return orderId; }
    @PropertyName("order_id")
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @PropertyName("user_id")
    public String getUserId() { return userId; }
    @PropertyName("user_id")
    public void setUserId(String userId) { this.userId = userId; }

    @PropertyName("order_date")
    public String getOrderDate() { return orderDate; }
    @PropertyName("order_date")
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

    @PropertyName("total_amount")
    public double getTotalAmount() { return totalAmount; }
    @PropertyName("total_amount")
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    @PropertyName("status")
    public String getStatusStr() { return statusStr; }
    @PropertyName("status")
    public void setStatusStr(String statusStr) { this.statusStr = statusStr; }

    public Status getStatus() {
        if (statusStr == null) return Status.PENDING;
        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.PENDING;
        }
    }

    public void setStatus(Status status) {
        this.statusStr = status != null ? status.name().toLowerCase() : Status.PENDING.name().toLowerCase();
    }

    @PropertyName("expires_at")
    public String getExpiresAt() { return expiresAt; }
    @PropertyName("expires_at")
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}

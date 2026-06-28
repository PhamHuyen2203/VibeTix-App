package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Order {
    public enum Status {
        PENDING, CONFIRMED, CANCELLED, COMPLETED, REFUNDED
    }

    private String orderId;
    private String userId;
    private com.google.firebase.Timestamp orderDate;
    private long totalAmount;
    private String statusStr = "pending";
    private String paymentMethod;
    private com.google.firebase.Timestamp expiresAt;

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
    public com.google.firebase.Timestamp getOrderDate() { return orderDate; }
    @PropertyName("order_date")
    public void setOrderDate(com.google.firebase.Timestamp orderDate) { this.orderDate = orderDate; }

    @PropertyName("total_amount")
    public long getTotalAmount() { return totalAmount; }
    @PropertyName("total_amount")
    public void setTotalAmount(long totalAmount) { this.totalAmount = totalAmount; }

    @PropertyName("status")
    public String getStatusStr() { return statusStr; }
    @PropertyName("status")
    public void setStatusStr(String statusStr) { this.statusStr = statusStr; }

    @PropertyName("payment_method")
    public String getPaymentMethod() { return paymentMethod; }
    @PropertyName("payment_method")
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    @Exclude
    public Status getStatus() {
        if (statusStr == null) return Status.PENDING;
        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.PENDING;
        }
    }

    @Exclude
    public void setStatus(Status status) {
        this.statusStr = status != null ? status.name().toLowerCase() : Status.PENDING.name().toLowerCase();
    }

    @PropertyName("expires_at")
    public com.google.firebase.Timestamp getExpiresAt() { return expiresAt; }
    @PropertyName("expires_at")
    public void setExpiresAt(com.google.firebase.Timestamp expiresAt) { this.expiresAt = expiresAt; }
}

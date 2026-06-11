package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class OrderDiscount implements Serializable {
    @PropertyName("order_id")
    private String orderId;

    @PropertyName("discount_id")
    private String discountId;

    @PropertyName("applied_amount")
    private double appliedAmount;

    public OrderDiscount() {}

    @PropertyName("order_id")
    public String getOrderId() { return orderId; }
    @PropertyName("order_id")
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @PropertyName("discount_id")
    public String getDiscountId() { return discountId; }
    @PropertyName("discount_id")
    public void setDiscountId(String discountId) { this.discountId = discountId; }

    @PropertyName("applied_amount")
    public double getAppliedAmount() { return appliedAmount; }
    @PropertyName("applied_amount")
    public void setAppliedAmount(double appliedAmount) { this.appliedAmount = appliedAmount; }
}

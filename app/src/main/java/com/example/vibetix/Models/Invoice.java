package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class Invoice implements Serializable {
    public enum Status {
        UNPAID, PAID, REFUNDED, CANCELLED
    }

    @PropertyName("invoice_id")
    private String invoiceId;

    @PropertyName("order_id")
    private String orderId;

    @PropertyName("invoice_number")
    private String invoiceNumber;

    @PropertyName("total_amount")
    private double totalAmount;

    @PropertyName("discount_amount")
    private double discountAmount = 0;

    @PropertyName("tax_amount")
    private double taxAmount = 0;

    @PropertyName("final_amount")
    private double finalAmount;

    @PropertyName("status")
    private String statusStr = "unpaid";

    @PropertyName("issued_at")
    private Object issuedAt;

    public Invoice() {}

    @PropertyName("invoice_id")
    public String getInvoiceId() { return invoiceId; }
    @PropertyName("invoice_id")
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    @PropertyName("order_id")
    public String getOrderId() { return orderId; }
    @PropertyName("order_id")
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @PropertyName("invoice_number")
    public String getInvoiceNumber() { return invoiceNumber; }
    @PropertyName("invoice_number")
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    @PropertyName("total_amount")
    public double getTotalAmount() { return totalAmount; }
    @PropertyName("total_amount")
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    @PropertyName("discount_amount")
    public double getDiscountAmount() { return discountAmount; }
    @PropertyName("discount_amount")
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    @PropertyName("tax_amount")
    public double getTaxAmount() { return taxAmount; }
    @PropertyName("tax_amount")
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    @PropertyName("final_amount")
    public double getFinalAmount() { return finalAmount; }
    @PropertyName("final_amount")
    public void setFinalAmount(double finalAmount) { this.finalAmount = finalAmount; }

    @PropertyName("status")
    public String getStatusStr() { return statusStr; }
    @PropertyName("status")
    public void setStatusStr(String statusStr) { this.statusStr = statusStr; }

    @com.google.firebase.firestore.Exclude
    public Status getStatus() {
        if (statusStr == null) return Status.UNPAID;
        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.UNPAID;
        }
    }

    @com.google.firebase.firestore.Exclude
    public void setStatus(Status status) {
        this.statusStr = status != null ? status.name().toLowerCase() : Status.UNPAID.name().toLowerCase();
    }

    @PropertyName("issued_at")
    public Object getIssuedAt() { return issuedAt; }
    @PropertyName("issued_at")
    public void setIssuedAt(Object issuedAt) { this.issuedAt = issuedAt; }
}

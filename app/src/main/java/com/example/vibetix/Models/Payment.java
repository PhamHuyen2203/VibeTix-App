package com.example.vibetix.Models;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.PropertyName;
import java.io.Serializable;

@IgnoreExtraProperties
public class Payment implements Serializable {
    public enum Status {
        SUCCESS, FAILED, REFUNDED
    }
    
    public enum Method {
        MOMO, ZALOPAY, VNPAY, VISA, MASTERCARD, ATM
    }

    @PropertyName("payment_id")
    private String paymentId;

    @PropertyName("invoice_id")
    private String invoiceId;

    @PropertyName("method")
    private String methodStr;

    @PropertyName("transaction_id")
    private String transactionId;

    @PropertyName("amount")
    private double amount;

    @PropertyName("payment_date")
    private Object paymentDate;

    @PropertyName("status")
    private String statusStr = "success";

    @PropertyName("note")
    private String note;

    public Payment() {}

    @PropertyName("payment_id")
    public String getPaymentId() { return paymentId; }
    @PropertyName("payment_id")
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    @PropertyName("invoice_id")
    public String getInvoiceId() { return invoiceId; }
    @PropertyName("invoice_id")
    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

    @PropertyName("method")
    public String getMethodStr() { return methodStr; }
    @PropertyName("method")
    public void setMethodStr(String methodStr) { this.methodStr = methodStr; }

    @com.google.firebase.firestore.Exclude
    public Method getMethod() {
        if (methodStr == null) return null;
        try {
            return Method.valueOf(methodStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @com.google.firebase.firestore.Exclude
    public void setMethod(Method method) {
        this.methodStr = method != null ? method.name().toLowerCase() : null;
    }

    @PropertyName("transaction_id")
    public String getTransactionId() { return transactionId; }
    @PropertyName("transaction_id")
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    @PropertyName("amount")
    public double getAmount() { return amount; }
    @PropertyName("amount")
    public void setAmount(double amount) { this.amount = amount; }

    @PropertyName("payment_date")
    public Object getPaymentDate() { return paymentDate; }
    @PropertyName("payment_date")
    public void setPaymentDate(Object paymentDate) { this.paymentDate = paymentDate; }

    @PropertyName("status")
    public String getStatusStr() { return statusStr; }
    @PropertyName("status")
    public void setStatusStr(String statusStr) { this.statusStr = statusStr; }

    @com.google.firebase.firestore.Exclude
    public Status getStatus() {
        if (statusStr == null) return Status.SUCCESS;
        try {
            return Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.SUCCESS;
        }
    }

    @com.google.firebase.firestore.Exclude
    public void setStatus(Status status) {
        this.statusStr = status != null ? status.name().toLowerCase() : Status.SUCCESS.name().toLowerCase();
    }

    @PropertyName("note")
    public String getNote() { return note; }
    @PropertyName("note")
    public void setNote(String note) { this.note = note; }
}

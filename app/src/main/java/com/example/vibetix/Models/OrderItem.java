package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

public class OrderItem {
    private String orderItemId; // UUID v4
    private String orderId;
    private String ticketTypeId;
    private int quantity;
    private double pricePerTicket;

    public OrderItem() {}

    @PropertyName("order_item_id")
    public String getOrderItemId() { return orderItemId; }
    @PropertyName("order_item_id")
    public void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }

    @PropertyName("order_id")
    public String getOrderId() { return orderId; }
    @PropertyName("order_id")
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @PropertyName("ticket_type_id")
    public String getTicketTypeId() { return ticketTypeId; }
    @PropertyName("ticket_type_id")
    public void setTicketTypeId(String ticketTypeId) { this.ticketTypeId = ticketTypeId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @PropertyName("price_per_ticket")
    public double getPricePerTicket() { return pricePerTicket; }
    @PropertyName("price_per_ticket")
    public void setPricePerTicket(double pricePerTicket) { this.pricePerTicket = pricePerTicket; }
}

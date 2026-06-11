package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class OrderItem {
    private String orderItemId; // UUID v4
    private String orderId;
    private String eventId;
    private String ticketTypeId;
    private long quantity;
    private long pricePerTicket;

    // Transient UI fields
    @com.google.firebase.firestore.Exclude private Order parentOrder;
    @com.google.firebase.firestore.Exclude private String ticketTypeName;

    public OrderItem() {}

    @PropertyName("order_item_id")
    public String getOrderItemId() { return orderItemId; }
    @PropertyName("order_item_id")
    public void setOrderItemId(String orderItemId) { this.orderItemId = orderItemId; }

    @PropertyName("order_id")
    public String getOrderId() { return orderId; }
    @PropertyName("order_id")
    public void setOrderId(String orderId) { this.orderId = orderId; }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    @PropertyName("ticket_type_id")
    public String getTicketTypeId() { return ticketTypeId; }
    @PropertyName("ticket_type_id")
    public void setTicketTypeId(String ticketTypeId) { this.ticketTypeId = ticketTypeId; }

    public long getQuantity() { return quantity; }
    public void setQuantity(long quantity) { this.quantity = quantity; }

    @PropertyName("price_per_ticket")
    public long getPricePerTicket() { return pricePerTicket; }
    @PropertyName("price_per_ticket")
    public void setPricePerTicket(long pricePerTicket) { this.pricePerTicket = pricePerTicket; }

    @com.google.firebase.firestore.Exclude
    public Order getParentOrder() { return parentOrder; }
    @com.google.firebase.firestore.Exclude
    public void setParentOrder(Order parentOrder) { this.parentOrder = parentOrder; }

    @com.google.firebase.firestore.Exclude
    public String getTicketTypeName() { return ticketTypeName; }
    @com.google.firebase.firestore.Exclude
    public void setTicketTypeName(String ticketTypeName) { this.ticketTypeName = ticketTypeName; }
}

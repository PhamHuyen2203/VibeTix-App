package com.example.vibetix.Models;

import com.google.firebase.firestore.PropertyName;

public class TicketType {
    private String ticketTypeId; // UUID v4
    private String typeId;       // alias field name
    private String eventId;
    private String name;
    private String description;
    private double price;
    private int totalQuantity;
    private int availableQuantity;
    private int soldCount;       // convenience counter
    private String saleStart;
    private String saleEnd;
    private boolean isTransferable = true;
    private boolean isActive = true;
    private int sortOrder = 0;

    public TicketType() {}

    @PropertyName("ticket_type_id")
    public String getTicketTypeId() { return ticketTypeId; }
    @PropertyName("ticket_type_id")
    public void setTicketTypeId(String ticketTypeId) { this.ticketTypeId = ticketTypeId; }

    @PropertyName("event_id")
    public String getEventId() { return eventId; }
    @PropertyName("event_id")
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    @PropertyName("total_quantity")
    public int getTotalQuantity() { return totalQuantity; }
    @PropertyName("total_quantity")
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }

    @PropertyName("available_quantity")
    public int getAvailableQuantity() { return availableQuantity; }
    @PropertyName("available_quantity")
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    @PropertyName("sale_start")
    public String getSaleStart() { return saleStart; }
    @PropertyName("sale_start")
    public void setSaleStart(String saleStart) { this.saleStart = saleStart; }

    @PropertyName("sale_end")
    public String getSaleEnd() { return saleEnd; }
    @PropertyName("sale_end")
    public void setSaleEnd(String saleEnd) { this.saleEnd = saleEnd; }

    @PropertyName("is_transferable")
    public boolean isTransferable() { return isTransferable; }
    @PropertyName("is_transferable")
    public void setTransferable(boolean transferable) { this.isTransferable = transferable; }

    @PropertyName("is_active")
    public boolean isActive() { return isActive; }
    @PropertyName("is_active")
    public void setActive(boolean active) { this.isActive = active; }

    @PropertyName("sort_order")
    public int getSortOrder() { return sortOrder; }
    @PropertyName("sort_order")
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    // Convenience aliases
    public String getTypeId() { return typeId != null ? typeId : ticketTypeId; }
    public void setTypeId(String id) { this.typeId = id; this.ticketTypeId = id; }

    /** Alias for availableQuantity (used in some adapters). */
    public int getQuantity() { return availableQuantity; }

    /** soldCount — how many sold so far. */
    @PropertyName("sold_count")
    public int getSoldCount() { return soldCount; }
    @PropertyName("sold_count")
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }
}

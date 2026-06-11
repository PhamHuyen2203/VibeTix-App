package com.example.vibetix.Models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class TicketType {
    private String ticketTypeId; // UUID v4
    private String typeId;       // alias field name
    private String eventId;
    private String name;
    private String description;
    private long price;
    private long totalQuantity;
    private long availableQuantity;
    private long soldQuantity;
    private com.google.firebase.Timestamp saleStart;
    private com.google.firebase.Timestamp saleEnd;
    private boolean isTransferable = true;
    private boolean isActive = true;
    private long sortOrder = 0;

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

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    @PropertyName("total_quantity")
    public long getTotalQuantity() { return totalQuantity; }
    @PropertyName("total_quantity")
    public void setTotalQuantity(long totalQuantity) { this.totalQuantity = totalQuantity; }

    @PropertyName("available_quantity")
    public long getAvailableQuantity() { return availableQuantity; }
    @PropertyName("available_quantity")
    public void setAvailableQuantity(long availableQuantity) { this.availableQuantity = availableQuantity; }

    @PropertyName("sale_start")
    public com.google.firebase.Timestamp getSaleStart() { return saleStart; }
    @PropertyName("sale_start")
    public void setSaleStart(com.google.firebase.Timestamp saleStart) { this.saleStart = saleStart; }

    @PropertyName("sale_end")
    public com.google.firebase.Timestamp getSaleEnd() { return saleEnd; }
    @PropertyName("sale_end")
    public void setSaleEnd(com.google.firebase.Timestamp saleEnd) { this.saleEnd = saleEnd; }

    @PropertyName("is_transferable")
    public boolean isTransferable() { return isTransferable; }
    @PropertyName("is_transferable")
    public void setTransferable(boolean transferable) { this.isTransferable = transferable; }

    @PropertyName("is_active")
    public boolean isActive() { return isActive; }
    @PropertyName("is_active")
    public void setActive(boolean active) { this.isActive = active; }

    @PropertyName("sort_order")
    public long getSortOrder() { return sortOrder; }
    @PropertyName("sort_order")
    public void setSortOrder(long sortOrder) { this.sortOrder = sortOrder; }

    // Convenience aliases
    @Exclude
    public String getTypeId() { return typeId != null ? typeId : ticketTypeId; }
    @Exclude
    public void setTypeId(String id) { this.typeId = id; this.ticketTypeId = id; }

    /** Alias for availableQuantity (used in some adapters). */
    @Exclude
    public long getQuantity() { return availableQuantity; }
    @Exclude
    public long getRemainingQuantity() { return availableQuantity; }

    @PropertyName("sold_quantity")
    public long getSoldQuantity() { return soldQuantity; }
    @PropertyName("sold_quantity")
    public void setSoldQuantity(long soldQuantity) { this.soldQuantity = soldQuantity; }

    /** soldCount — how many sold so far. */
    @Exclude
    public long getSoldCount() { 
        if (soldQuantity == 0 && totalQuantity > 0 && availableQuantity <= totalQuantity) {
            return totalQuantity - availableQuantity;
        }
        return soldQuantity; 
    }
    @Exclude
    public void setSoldCount(long soldCount) { this.soldQuantity = soldCount; }
}

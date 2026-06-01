package com.example.vibetix.Models;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

/**
 * Local model cho phương thức thanh toán đã lưu.
 * Sau này thay bằng Firestore document.
 */
public class PaymentMethod {
    private String id;
    private String type;        // "momo"/"zalopay"/"vnpay"/"visa"/"atm"
    private String displayName; // "MoMo", "ZaloPay", "Thẻ Visa"...
    private String account;     // Masked: "098****123" or "**** 4242"
    private boolean isDefault;

    public PaymentMethod() {}

    public PaymentMethod(String type, String displayName, String account) {
        this.id          = UUID.randomUUID().toString();
        this.type        = type;
        this.displayName = displayName;
        this.account     = account;
        this.isDefault   = false;
    }

    public String getId()          { return id; }
    public String getType()        { return type; }
    public String getDisplayName() { return displayName; }
    public String getAccount()     { return account; }
    public boolean isDefault()     { return isDefault; }

    public void setId(String id)          { this.id = id; }
    public void setType(String type)      { this.type = type; }
    public void setDisplayName(String v)  { this.displayName = v; }
    public void setAccount(String v)      { this.account = v; }
    public void setDefault(boolean d)     { this.isDefault = d; }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",          id);
            obj.put("type",        type);
            obj.put("displayName", displayName);
            obj.put("account",     account);
            obj.put("isDefault",   isDefault);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static PaymentMethod fromJson(JSONObject obj) {
        if (obj == null) return null;
        PaymentMethod p = new PaymentMethod();
        p.id          = obj.optString("id",          UUID.randomUUID().toString());
        p.type        = obj.optString("type",        "");
        p.displayName = obj.optString("displayName", "");
        p.account     = obj.optString("account",     "");
        p.isDefault   = obj.optBoolean("isDefault",  false);
        return p;
    }
}

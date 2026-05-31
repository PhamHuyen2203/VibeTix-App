package com.example.vibetix.Models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Local model cho thông tin ban tổ chức.
 * Hỗ trợ nhiều BTC profile (list) — mỗi profile có uuid riêng.
 * Sau này thay bằng Firestore document.
 */
public class OrganizerProfile {
    private String id;          // UUID
    private String brandName;
    private String description;
    private String websiteUrl;
    private String status;      // "none" | "pending" | "approved"
    private boolean isDefault;

    public OrganizerProfile() {
        this.id     = UUID.randomUUID().toString();
        this.status = "none";
    }

    public OrganizerProfile(String brandName, String description, String websiteUrl, String status) {
        this.id          = UUID.randomUUID().toString();
        this.brandName   = brandName;
        this.description = description;
        this.websiteUrl  = websiteUrl;
        this.status      = status;
        this.isDefault   = false;
    }

    // ── Getters ────────────────────────────────────────────────────────────────
    public String  getId()          { return id; }
    public void    setId(String v)  { this.id = v; }
    public String  getBrandName()   { return brandName; }
    public String  getDescription() { return description; }
    public String  getWebsiteUrl()  { return websiteUrl; }
    public String  getStatus()      { return status; }
    public boolean isDefault()      { return isDefault; }

    // ── Setters ────────────────────────────────────────────────────────────────
    public void setBrandName(String v)   { this.brandName = v; }
    public void setDescription(String v) { this.description = v; }
    public void setWebsiteUrl(String v)  { this.websiteUrl = v; }
    public void setStatus(String v)      { this.status = v; }
    public void setDefault(boolean v)    { this.isDefault = v; }

    // ── Helpers ────────────────────────────────────────────────────────────────
    public boolean hasProfile() { return !"none".equals(status) && brandName != null && !brandName.isEmpty(); }
    public boolean isPending()  { return "pending".equals(status); }
    public boolean isApproved() { return "approved".equals(status); }

    // ── JSON ───────────────────────────────────────────────────────────────────
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id",          id          != null ? id          : UUID.randomUUID().toString());
            obj.put("brandName",   brandName   != null ? brandName   : "");
            obj.put("description", description != null ? description : "");
            obj.put("websiteUrl",  websiteUrl  != null ? websiteUrl  : "");
            obj.put("status",      status      != null ? status      : "none");
            obj.put("isDefault",   isDefault);
        } catch (JSONException ignored) {}
        return obj;
    }

    public static OrganizerProfile fromJson(JSONObject obj) {
        if (obj == null) return new OrganizerProfile();
        OrganizerProfile p = new OrganizerProfile();
        p.id          = obj.optString("id",          UUID.randomUUID().toString());
        p.brandName   = obj.optString("brandName",   "");
        p.description = obj.optString("description", "");
        p.websiteUrl  = obj.optString("websiteUrl",  "");
        p.status      = obj.optString("status",      "none");
        p.isDefault   = obj.optBoolean("isDefault",  false);
        return p;
    }
}

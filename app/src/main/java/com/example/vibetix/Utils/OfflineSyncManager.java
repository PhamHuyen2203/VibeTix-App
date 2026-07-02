package com.example.vibetix.Utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.vibetix.Models.UserTicket;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OfflineSyncManager {

    private static final String PREF_NAME = "VibeTixOfflineSync";
    private static final String KEY_OFFLINE_TICKETS = "offline_tickets_";
    private static final String KEY_PENDING_SYNC = "pending_sync_";

    private final SharedPreferences prefs;
    private final Gson gson;
    private final String eventId;

    public OfflineSyncManager(Context context, String eventId) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.eventId = eventId;
    }

    // Lấy danh sách toàn bộ vé Offline của event
    public Map<String, UserTicket> getOfflineTickets() {
        String json = prefs.getString(KEY_OFFLINE_TICKETS + eventId, null);
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, UserTicket>>(){}.getType();
        return gson.fromJson(json, type);
    }

    // Lưu đè danh sách vé Offline
    public void saveOfflineTickets(Map<String, UserTicket> ticketsMap) {
        String json = gson.toJson(ticketsMap);
        prefs.edit().putString(KEY_OFFLINE_TICKETS + eventId, json).apply();
    }

    // Lấy danh sách ID vé đang chờ đồng bộ lên Cloud
    public List<String> getPendingSyncTickets() {
        String json = prefs.getString(KEY_PENDING_SYNC + eventId, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<String>>(){}.getType();
        return gson.fromJson(json, type);
    }

    // Thêm một vé vào danh sách chờ đồng bộ
    public void addPendingSyncTicket(String ticketId) {
        List<String> pending = getPendingSyncTickets();
        if (!pending.contains(ticketId)) {
            pending.add(ticketId);
            savePendingSyncTickets(pending);
        }
    }

    // Lưu đè danh sách chờ đồng bộ (Dùng khi đã đồng bộ thành công để clear)
    public void savePendingSyncTickets(List<String> pendingIds) {
        String json = gson.toJson(pendingIds);
        prefs.edit().putString(KEY_PENDING_SYNC + eventId, json).apply();
    }

    // Cập nhật trạng thái một vé trong Offline Cache (key theo ticket_code)
    public boolean markTicketUsedOffline(String ticketCode) {
        Map<String, UserTicket> ticketsMap = getOfflineTickets();
        if (ticketsMap.containsKey(ticketCode)) {
            UserTicket ticket = ticketsMap.get(ticketCode);
            if (ticket != null) {
                ticket.setStatus(UserTicket.Status.USED);
                ticket.setCheckedInAt(new java.util.Date().getTime());
                saveOfflineTickets(ticketsMap);
                // pendingSync dùng userTicketId (document ID) để update DB
                String docId = ticket.getUserTicketId() != null ? ticket.getUserTicketId() : ticketCode;
                addPendingSyncTicket(docId);
                return true;
            }
        }
        return false;
    }

    /**
     * Mớ́i: Đánh dấu vé đã sử dụng bằng cả ticket_code và document ID riêng biệt.
     * @param ticketCode mã QR (key trong offline map)
     * @param docId      Firestore document ID (dùng khi sync lên DB)
     */
    public boolean markTicketUsedOfflineByCode(String ticketCode, String docId) {
        Map<String, UserTicket> ticketsMap = getOfflineTickets();
        if (ticketsMap.containsKey(ticketCode)) {
            UserTicket ticket = ticketsMap.get(ticketCode);
            if (ticket != null) {
                ticket.setStatus(UserTicket.Status.USED);
                ticket.setCheckedInAt(new java.util.Date().getTime());
                saveOfflineTickets(ticketsMap);
                addPendingSyncTicket(docId); // dùng document ID để sync
                return true;
            }
        }
        return false;
    }

    // Xóa toàn bộ dữ liệu offline của event này (nếu cần thiết)
    public void clearEventData() {
        prefs.edit()
                .remove(KEY_OFFLINE_TICKETS + eventId)
                .remove(KEY_PENDING_SYNC + eventId)
                .apply();
    }
}

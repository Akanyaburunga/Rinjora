package org.kazinduzi.rinjora.network;

import android.net.Uri;

import org.kazinduzi.rinjora.BuildConfig;

public class ApiConfig {
    // ------------------------------------------------------------------
    // Legacy logistics backend (MyMartin/Volley) — kept for compatibility
    // ------------------------------------------------------------------
    private static final String DEV_BASE_URL = "http://192.168.100.156:8000/api";
    private static final String PROD_BASE_URL = "https://martin-logistics.nova.bi/api";

    // Automatically select base URL based on build type
    public static final String BASE_URL = BuildConfig.DEBUG ? DEV_BASE_URL : PROD_BASE_URL;

    // ------------------------------------------------------------------
    // Rinjora (Kazinduzi) game backend — new Retrofit layer
    // ------------------------------------------------------------------
    // TODO: point at the real Kazinduzi API host once provisioned.
    // Dev commonly uses a Laravel Valet/Herd `.test` domain or a LAN IP
    // with `php artisan serve`. The path suffix is the API root (`.../api`).
    private static final String KAZINDUZI_DEV_BASE_URL = "http://192.168.100.156:8000/api";
    private static final String KAZINDUZI_PROD_BASE_URL = "https://api.kazinduzi.bi/api";

    /**
     * Base URL for the Rinjora (Kazinduzi) game API, selected by build type.
     * Retrofit requires the URL to end in '/'.
     */
    public static final String KAZINDUZI_BASE_URL = (BuildConfig.DEBUG ? KAZINDUZI_DEV_BASE_URL : KAZINDUZI_PROD_BASE_URL) + "/";

    // Auth endpoints
    public static final String AUTH_REQUEST_WHATSAPP_OTP = BASE_URL + "/auth/request-whatsapp-otp";
    public static final String AUTH_VERIFY_WHATSAPP_OTP = BASE_URL + "/auth/verify-whatsapp-otp";
    public static final String AUTH_VERIFY_FIREBASE_PHONE = BASE_URL + "/auth/verify-firebase-phone";
    public static final String AUTH_LOGOUT = BASE_URL + "/auth/logout";

    // Support endpoints
    public static final String SUPPORT_TICKETS = BASE_URL + "/support/tickets";
    public static final String SUPPORT_CATEGORIES = BASE_URL + "/support/categories";

    public static String supportTicketDetail(int ticketId) {
        return BASE_URL + "/support/tickets/" + ticketId;
    }

    public static String supportTicketMessages(int ticketId) {
        return BASE_URL + "/support/tickets/" + ticketId + "/messages";
    }

    // Driver home (trips) endpoints
    public static final String TRIP_CURRENT = BASE_URL + "/trips/current";

    // Driver profile endpoint
    public static final String PROFILE = BASE_URL + "/profile";

    // Repair request endpoints (driver side)
    public static final String REPAIR_REQUESTS = BASE_URL + "/mobile/repair-requests";
    public static final String REPAIR_VEHICLES = BASE_URL + "/mobile/repair-requests/vehicles";
    public static final String REPAIR_CURRENT = BASE_URL + "/mobile/repair-requests/current";

    public static String repairParts(String query) {
        String q = Uri.encode(query == null ? "" : query);
        return BASE_URL + "/mobile/repair-requests/parts?q=" + q;
    }

    public static String repairRequestDetail(int requestId) {
        return BASE_URL + "/mobile/repair-requests/" + requestId;
    }

    public static String repairRequestSubmit(int requestId) {
        return BASE_URL + "/mobile/repair-requests/" + requestId + "/submit";
    }

    public static String repairRequestCancel(int requestId) {
        return BASE_URL + "/mobile/repair-requests/" + requestId + "/cancel";
    }

    // Workshop (mechanic) endpoints
    public static final String WORKSHOP_MY_TASKS = BASE_URL + "/mobile/workshop/my-tasks";

    public static String workshopTaskDetail(int assignmentId) {
        return BASE_URL + "/mobile/workshop/tasks/" + assignmentId;
    }

    public static String workshopTaskStart(int assignmentId) {
        return BASE_URL + "/mobile/workshop/tasks/" + assignmentId + "/start";
    }

    public static String workshopTaskComplete(int assignmentId) {
        return BASE_URL + "/mobile/workshop/tasks/" + assignmentId + "/complete";
    }

    // Helper method to get current environment
    public static String getCurrentEnvironment() {
        return BuildConfig.DEBUG ? "Development" : "Production";
    }
}

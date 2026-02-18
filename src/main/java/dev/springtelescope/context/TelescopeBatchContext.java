package dev.springtelescope.context;

import java.util.UUID;

public class TelescopeBatchContext {

    private static final ThreadLocal<String> BATCH_ID = new ThreadLocal<>();

    private TelescopeBatchContext() {}

    public static String get() {
        return BATCH_ID.get();
    }

    public static void set(String id) {
        BATCH_ID.set(id);
    }

    public static void clear() {
        BATCH_ID.remove();
    }

    public static String getOrCreate() {
        String id = BATCH_ID.get();
        if (id == null) {
            id = UUID.randomUUID().toString();
            BATCH_ID.set(id);
        }
        return id;
    }
}

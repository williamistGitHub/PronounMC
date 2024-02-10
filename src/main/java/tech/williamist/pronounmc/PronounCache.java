package tech.williamist.pronounmc;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class PronounCache {

    private static final HashMap<UUID, String> CACHE = new HashMap<>();


    static void cacheSet(UUID uuid, String pronoun) {
        CACHE.put(uuid, pronoun);
    }

    private static final Object2IntMap<UUID> REQUEST_TRIES = new Object2IntArrayMap<>();
    private static long nextRequestTime = 0;
    private static PronounRequestThread thread = null;

    public static void clear() {
        CACHE.clear();
    }

    public static HashMap<UUID, String> getPronounsFor(UUID... uuids) {
        HashMap<UUID, String> outMap = new HashMap<>();
        List<UUID> nonCachedUuids = new ArrayList<>();

        for (UUID uuid : uuids) {
            if (CACHE.containsKey(uuid))
                outMap.put(uuid, CACHE.get(uuid));
            else if (REQUEST_TRIES.getOrDefault(uuid, 0) < 3) { // try to request uuid's if they havent been requested 3 times yet
                nonCachedUuids.add(uuid);
                outMap.put(uuid, "Loading...");
            }
            else
                outMap.put(uuid, "⚠");
        }

        // gather non cached uuids
        long now = System.currentTimeMillis();

        if (thread != null && thread.isAlive())
            thread = null;

        if (now >= nextRequestTime && !nonCachedUuids.isEmpty() && thread == null) {
            // pronoundb requests are a max size of 50, we just strip off any extra
            // they can get requested later lool
            if (nonCachedUuids.size() > 50) {
                nonCachedUuids = nonCachedUuids.subList(0, 50);
            }

            nonCachedUuids.forEach(uuid -> REQUEST_TRIES.put(uuid, REQUEST_TRIES.getOrDefault(uuid, 0) + 1));

            thread = new PronounRequestThread(nonCachedUuids.toArray(UUID[]::new));
            thread.start();

            nextRequestTime = now + 1000; // default 1 second request time
        }

        return outMap;
    }

    public static String getPronounsFor(UUID uuid) {
        return getPronounsFor(new UUID[]{uuid}).get(uuid);
    }

}

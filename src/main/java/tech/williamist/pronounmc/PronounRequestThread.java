package tech.williamist.pronounmc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Predicate;

public class PronounRequestThread extends Thread {

    private final UUID[] uuids;

    public PronounRequestThread(UUID[] uuids) {
        this.uuids = uuids;
    }

    @Override
    public void run() {
        Arrays.stream(uuids).forEach(uuid -> PronounCache.cacheSet(uuid, "Loading..."));

        HashMap<UUID, String> pronouns = PronounDB.lookupSimple(uuids);
        if (pronouns == null) {
            Arrays.stream(uuids).forEach(uuid -> PronounCache.cacheSet(uuid, null));
            return;
        }

        pronouns.forEach(PronounCache::cacheSet);
        Arrays.stream(uuids).filter(Predicate.not(pronouns::containsKey)).forEach(uuid -> PronounCache.cacheSet(uuid, ""));
    }

}

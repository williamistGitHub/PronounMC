package tech.williamist.pronounmc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Language;
import org.apache.http.client.utils.URIBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

// interface to pronoundb.org's api
// there are like 3 endpoints and we only use one so it's quite simple
public final class PronounDB {

    // for now pronoundb only supports the "en" locale
    // maybe more will come, i can't be sure :/
    public enum Locale {
        EN("en")

        ;

        private final String jsonKey;

        Locale(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        private static final Map<String, Locale> JSON_TO_LOCALE_MAP;
        static {
            HashMap<String, Locale> map = new HashMap<>();
            for (Locale locale : Locale.values()) {
                map.put(locale.jsonKey, locale);
            }
            JSON_TO_LOCALE_MAP = Collections.unmodifiableMap(map);
        }

        public static Locale fromJsonKey(String jsonKey) {
            return JSON_TO_LOCALE_MAP.get(jsonKey);
        }

        public static Locale fromGameLanguage(String languageCode) {
            // for now pronoundb only supports en, but this may change since the api supports whatever
            return Locale.EN;
        }
    }

    private static final String API_ROOT = "https://pronoundb.org/api/v2/";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    // this return value is hell
    public static HashMap<UUID, HashMap<Locale, String[]>> lookup(UUID[] uuids) {

        // build request URI
        URI uri;
        try {
            uri = new URIBuilder(API_ROOT + "lookup")
                    .addParameter("platform", "minecraft")
                            .addParameter("ids", String.join(",", Arrays.stream(uuids).map(UUID::toString).toArray(String[]::new)))
                                    .build();
        } catch (URISyntaxException e) {
            // this should never happen because its all constant but okay java, you do you.
            throw new RuntimeException(e);
        }

        // make request object
        HttpRequest request = HttpRequest.newBuilder(uri)
                .GET()
                .build();

        // send off request
        try {
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonObject json = JsonParser.parseReader(new InputStreamReader(response.body())).getAsJsonObject();

            HashMap<UUID, HashMap<Locale, String[]>> outMap = new HashMap<>();

            for (UUID uuid : uuids) {
                if (!json.has(uuid.toString()))
                    continue;

                JsonObject sets = json.getAsJsonObject(uuid.toString()).getAsJsonObject("sets");
                HashMap<Locale, String[]> localeMap = new HashMap<>();

                for (String key : sets.keySet()) {
                    String[] pronouns = sets.getAsJsonArray(key)
                            .asList()
                            .stream()
                            .filter(JsonElement::isJsonPrimitive)
                            .map(JsonElement::getAsString)
                            .toArray(String[]::new);

                    localeMap.put(Locale.fromJsonKey(key), pronouns);
                }

                outMap.put(uuid, localeMap);
            }

            return outMap;
        } catch (IOException | InterruptedException ignored) {
            return null;
        }
    }

    public static HashMap<UUID, String> lookupSimple(UUID[] uuids) {
        HashMap<UUID, String> outMap = new HashMap<>();

        // get user locale
        Locale locale = Locale.fromGameLanguage(null);

        HashMap<UUID, HashMap<Locale, String[]>> lookupResult = lookup(uuids);
        if (lookupResult == null)
            return null;

        for (UUID uuid : lookupResult.keySet()) {
            outMap.put(uuid, String.join("/", lookupResult.get(uuid).get(locale)));
        }

        return outMap;
    }

}

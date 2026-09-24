package com.swingcraft4j.animal;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.swingcraft4j.net.Http;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Blocking client for the iNaturalist taxa API (free, no key); call off the EDT.
 */
public final class AnimalApi {

    private static final String BASE = "https://api.inaturalist.org/v1/taxa";
    private static final int MAX_DESCRIPTION = 340;
    private static final Map<String, String> GROUPS = Map.of(
            "Mammalia", "Mammal", "Aves", "Bird", "Reptilia", "Reptile", "Amphibia", "Amphibian",
            "Actinopterygii", "Fish", "Insecta", "Insect", "Arachnida", "Arachnid", "Mollusca", "Mollusk");

    private AnimalApi() {
    }

    /**
     * Searches animal species by common or scientific name.
     */
    public static List<AnimalSummary> search(String query) throws IOException, InterruptedException {
        URI uri = URI.create(BASE + "/autocomplete?taxon_id=1&rank=species&per_page=15&locale=en&q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8));
        JsonArray results = results(uri);
        List<AnimalSummary> animals = new ArrayList<>();
        for (JsonElement e : results) {
            JsonObject taxon = e.getAsJsonObject();
            String scientific = str(taxon, "name");
            String common = str(taxon, "preferred_common_name");
            JsonObject status = obj(taxon, "conservation_status");
            String statusName = status != null ? str(status, "status_name") : null;
            JsonObject photo = obj(taxon, "default_photo");
            animals.add(new AnimalSummary(taxon.get("id").getAsInt(),
                    common != null ? capitalize(common) : scientific, scientific,
                    statusName != null ? capitalize(statusName) : "Not evaluated",
                    photo != null ? str(photo, "square_url") : null));
        }
        return animals;
    }

    /**
     * Fetches an animal's details and downloads its profile photo; the IUCN status comes from the search hit
     * because the detail endpoint only lists regional statuses.
     */
    public static Animal fetch(AnimalSummary hit) throws IOException, InterruptedException {
        int id = hit.id();
        JsonObject taxon = results(URI.create(BASE + "/" + id + "?locale=en")).get(0).getAsJsonObject();

        String scientific = str(taxon, "name");
        String common = str(taxon, "preferred_common_name");
        String iconic = str(taxon, "iconic_taxon_name");
        String summary = str(taxon, "wikipedia_summary");
        JsonObject defaultPhoto = obj(taxon, "default_photo");
        String photoUrl = defaultPhoto != null ? str(defaultPhoto, "medium_url") : null;
        BufferedImage photo = photoUrl != null ? Http.image(photoUrl) : null;

        return new Animal(id,
                common != null ? capitalize(common) : scientific,
                scientific,
                GROUPS.getOrDefault(iconic, "Animal"),
                capitalize(str(taxon, "rank") != null ? str(taxon, "rank") : "taxon"),
                hit.conservation(),
                taxon.has("observations_count") ? taxon.get("observations_count").getAsInt() : 0,
                summary != null ? truncate(plainText(summary)) : "No description available.",
                photo);
    }

    private static JsonArray results(URI uri) throws IOException, InterruptedException {
        return JsonParser.parseString(Http.text(uri)).getAsJsonObject().getAsJsonArray("results");
    }

    private static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e == null || e.isJsonNull() ? null : e.getAsString();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Strips the simple HTML (b/i/a tags, entities) found in Wikipedia summaries.
     */
    private static String plainText(String html) {
        return html.replaceAll("<[^>]+>", "")
                .replace("&nbsp;", " ").replace("&quot;", "\"").replace("&#39;", "'")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .replaceAll("\\s+", " ").trim();
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_DESCRIPTION) {
            return text;
        }
        int cut = text.lastIndexOf(' ', MAX_DESCRIPTION);
        return text.substring(0, cut > 0 ? cut : MAX_DESCRIPTION) + "…";
    }
}

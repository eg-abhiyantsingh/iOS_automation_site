package com.egalvanic.explore;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * The single seam for all AI calls (Anthropic Messages API). It is intentionally
 * optional: {@link #fromEnv()} returns {@code null} when {@code ANTHROPIC_API_KEY} is
 * unset, and every caller treats {@code null} as "fall back to deterministic behaviour".
 * That keeps the crawler runnable — and CI green — with no key and no network.
 *
 * <p>Prompt caching is applied to the (static) system rubric via {@code cache_control}.
 */
public final class AiClient {

    private static final String ENDPOINT = "https://api.anthropic.com/v1/messages";

    private final String apiKey;
    private final String model;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15)).build();

    private AiClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    public static AiClient fromEnv() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null || key.isBlank()) return null;
        String model = System.getenv().getOrDefault("ANTHROPIC_MODEL", "claude-sonnet-4-6");
        return new AiClient(key, model);
    }

    /** Vision: classify a screenshot. Returns a short verdict ("ok" or a defect description). */
    public String classifyScreenshot(String base64Png, String hint) {
        JsonObject img = new JsonObject();
        img.addProperty("type", "image");
        JsonObject src = new JsonObject();
        src.addProperty("type", "base64");
        src.addProperty("media_type", "image/png");
        src.addProperty("data", base64Png);
        img.add("source", src);

        JsonObject txt = new JsonObject();
        txt.addProperty("type", "text");
        txt.addProperty("text", "Screen context: " + hint
                + ". Reply 'ok' if this is a healthy, fully-rendered iOS screen. "
                + "Otherwise reply with the single biggest defect: blank, overlap, clipped, "
                + "error-state, or garbled.");

        JsonArray content = new JsonArray();
        content.add(img);
        content.add(txt);
        return post("You are a strict iOS UI anomaly detector. Be terse.", content);
    }

    /** Suggest the highest-bug-risk node label to interact with next. */
    public String suggestNextActionLabel(List<String> nodeLabels, String screenHint) {
        JsonObject txt = new JsonObject();
        txt.addProperty("type", "text");
        txt.addProperty("text", "Screen: " + screenHint + "\nInteractable elements:\n- "
                + String.join("\n- ", nodeLabels)
                + "\n\nReturn ONLY the exact label of the one element most likely to expose a bug "
                + "(destructive actions, forms, file/PDF, money/counts).");
        JsonArray content = new JsonArray();
        content.add(txt);
        return post("You prioritise exploratory iOS testing for maximum bug yield. Reply with one label only.",
                content);
    }

    private String post(String systemPrompt, JsonArray userContent) {
        try {
            JsonObject sysBlock = new JsonObject();
            sysBlock.addProperty("type", "text");
            sysBlock.addProperty("text", systemPrompt);
            JsonObject cache = new JsonObject();
            cache.addProperty("type", "ephemeral");
            sysBlock.add("cache_control", cache);            // prompt caching on the static rubric
            JsonArray system = new JsonArray();
            system.add(sysBlock);

            JsonObject msg = new JsonObject();
            msg.addProperty("role", "user");
            msg.add("content", userContent);
            JsonArray messages = new JsonArray();
            messages.add(msg);

            JsonObject body = new JsonObject();
            body.addProperty("model", model);
            body.addProperty("max_tokens", 256);
            body.add("system", system);
            body.add("messages", messages);

            HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(40))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                System.out.println("⚠️ AiClient HTTP " + resp.statusCode() + ": " + resp.body());
                return null;
            }
            JsonObject json = JsonParser.parseString(resp.body()).getAsJsonObject();
            return json.getAsJsonArray("content").get(0).getAsJsonObject()
                    .get("text").getAsString().trim();
        } catch (Exception e) {
            System.out.println("⚠️ AiClient call failed (continuing without AI): " + e.getMessage());
            return null;
        }
    }
}

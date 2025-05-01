package com.example.healthchatbotapp;

import com.example.healthchatbotapp.BuildConfig;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GeminiClient {
    // Matches the curl sample from AI Studio
    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
                    + "?key=" + BuildConfig.GEMINI_API_KEY;

    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient http = new OkHttpClient();
    private final Gson gson = new Gson();

    /** Callback interface for async replies */
    public interface Listener {
        void onSuccess(String content);
        void onFailure(Exception e);
    }

    /**
     * Sends `prompt` to Gemini and invokes the appropriate callback.
     */
    public void generateContent(String prompt, Listener listener) {
        // Construct JSON body:
        // {
        //   "contents": [
        //     { "parts": [ { "text": "..." } ] }
        //   ]
        // }
        Map<String, Object> part = Collections.singletonMap("text", prompt);
        Map<String, Object> content = Collections.singletonMap("parts", Collections.singletonList(part));
        Map<String, Object> payload = Collections.singletonMap("contents", Collections.singletonList(content));
        String jsonBody = gson.toJson(payload);

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException ioe) {
                listener.onFailure(ioe);
            }

            @Override
            public void onResponse(Call call, Response resp) throws IOException {
                if (!resp.isSuccessful()) {
                    listener.onFailure(new IOException("HTTP " + resp.code() + ": " + resp.message()));
                    return;
                }

                JsonObject root = JsonParser.parseReader(resp.body().charStream()).getAsJsonObject();
                JsonArray cands = root.getAsJsonArray("candidates");
                if (cands != null && cands.size() > 0) {
                    JsonObject first = cands.get(0).getAsJsonObject();
                    String output = first.has("output")
                            ? first.get("output").getAsString()
                            : "";
                    listener.onSuccess(output);
                } else {
                    listener.onFailure(new IOException("No candidates in response"));
                }
            }
        });
    }
}

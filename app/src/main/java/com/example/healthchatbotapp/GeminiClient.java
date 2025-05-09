package com.example.healthchatbotapp;

import android.util.Log;

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
    // TODO: Check for WiFi & Mobile (networkConnection)
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

    /*
     Sends 'prompt' to Gemini and invokes the appropriate callback.
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
        Log.d("GeminiRequest", jsonBody);
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
                    String errorBody = resp.body() != null ? resp.body().string() : "≪no body≫";
                    listener.onFailure(new IOException(
                            "HTTP " + resp.code() + ": " + resp.message()
                                    + "\n" + errorBody
                    ));
                    return;
                }

                // parse the new schema:
                JsonObject root = JsonParser.parseReader(resp.body().charStream()).getAsJsonObject();
                JsonArray candidates = root.getAsJsonArray("candidates");
                if (candidates != null && candidates.size() > 0) {
                    JsonObject first = candidates.get(0).getAsJsonObject();
                    JsonObject content = first.getAsJsonObject("content");
                    JsonArray parts = content.getAsJsonArray("parts");
                    if (parts != null && parts.size() > 0) {
                        String reply = parts
                                .get(0).getAsJsonObject()
                                .get("text").getAsString();
                        listener.onSuccess(reply);
                        return;
                    }
                }

                listener.onFailure(new IOException("No reply in Gemini response"));
            }
        });
    }
}

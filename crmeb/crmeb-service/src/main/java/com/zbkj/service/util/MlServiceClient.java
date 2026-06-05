package com.zbkj.service.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MlServiceClient {

    private static final String ML_SERVICE_URL = "http://localhost:5001";
    private static final Gson gson = new Gson();

    public static Map<String, Object> classifyImage(byte[] imageBytes) throws IOException {
        String boundary = "----Boundary" + System.currentTimeMillis();
        URL url = new URL(ML_SERVICE_URL + "/classify");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            os.write("Content-Disposition: form-data; name=\"image\"; filename=\"image.jpg\"\r\n".getBytes(StandardCharsets.UTF_8));
            os.write("Content-Type: image/jpeg\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(imageBytes);
            os.write("\r\n".getBytes(StandardCharsets.UTF_8));
            os.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        }

        String response = readResponse(conn);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if (json.get("success").getAsBoolean()) {
            return gson.fromJson(json.get("data"), Map.class);
        }
        log.error("ML classify failed: {}", response);
        return null;
    }

    public static Map<String, Object> predictPrice(String category, String condition, Double originalPrice) throws IOException {
        URL url = new URL(ML_SERVICE_URL + "/predict-price");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        Map<String, Object> params = new HashMap<>();
        params.put("category", category);
        params.put("condition", condition);
        if (originalPrice != null) {
            params.put("original_price", originalPrice);
        }

        try (OutputStream os = conn.getOutputStream()) {
            os.write(gson.toJson(params).getBytes(StandardCharsets.UTF_8));
        }

        String response = readResponse(conn);
        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        if (json.get("success").getAsBoolean()) {
            return gson.fromJson(json.get("data"), Map.class);
        }
        log.error("ML predict price failed: {}", response);
        return null;
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
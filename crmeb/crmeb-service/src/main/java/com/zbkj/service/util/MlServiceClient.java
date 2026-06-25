package com.zbkj.service.util;

import cn.hutool.core.codec.Base64;
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

    private static final Gson gson = new Gson();

    /**
     * 读取 ML 服务地址，支持通过系统属性 ml.service.url 配置
     * 本地开发: http://localhost:3000
     * Docker 环境: http://ai-server:3000
     */
    private static String getMlServiceUrl() {
        return System.getProperty("ml.service.url", "http://localhost:3000");
    }

    public static Map<String, Object> classifyImage(byte[] imageBytes) throws IOException {
        String ML_SERVICE_URL = getMlServiceUrl();
        URL url = new URL(ML_SERVICE_URL + "/api/identify");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        // 将图片字节转为 Base64 数据 URI
        String base64Image = Base64.encode(imageBytes);
        String dataUri = "data:image/jpeg;base64," + base64Image;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image", dataUri);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8));
        }

        String response = readResponse(conn);
        log.info("ML classify response: {}", response);

        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            Map<String, Object> result = new HashMap<>();

            // 新 AI 服务返回: { rawLabel, confidence, name, category, suggestedOriginalPrice, defaultDesc }
            if (json.has("rawLabel")) {
                result.put("rawLabel", json.get("rawLabel").getAsString());
            }
            if (json.has("name")) {
                result.put("name", json.get("name").getAsString());
            }
            if (json.has("category")) {
                result.put("category", json.get("category").getAsString());
            }
            if (json.has("defaultDesc")) {
                result.put("defaultDesc", json.get("defaultDesc").getAsString());
            }
            if (json.has("suggestedOriginalPrice")) {
                result.put("suggestedOriginalPrice", json.get("suggestedOriginalPrice").getAsBigDecimal());
            }
            // confidence 可能为 "98.5%" 或数字，统一转为 Double
            if (json.has("confidence")) {
                String confStr = json.get("confidence").getAsString().replace("%", "");
                result.put("confidence", Double.parseDouble(confStr) / 100.0);
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to parse ML classify response: {}", response, e);
            return null;
        }
    }

    public static Map<String, Object> predictPrice(String category, String condition, Double originalPrice) throws IOException {
        Map<String, Object> result = new HashMap<>();

        Map<String, Double> conditionDiscount = new HashMap<>();
        conditionDiscount.put("几乎全新", 0.75);
        conditionDiscount.put("轻微使用痕迹", 0.60);
        conditionDiscount.put("明显使用痕迹", 0.45);
        conditionDiscount.put("老旧/有瑕疵", 0.30);
        conditionDiscount.put("almost new", 0.75);
        conditionDiscount.put("slight wear", 0.60);
        conditionDiscount.put("visible wear", 0.45);
        conditionDiscount.put("old/defective", 0.30);

        Map<String, Double> categoryFactor = new HashMap<>();
        categoryFactor.put("手机/数码", 0.90);
        categoryFactor.put("电脑/办公", 0.85);
        categoryFactor.put("家用电器", 0.80);
        categoryFactor.put("服装/鞋帽", 0.70);
        categoryFactor.put("箱包/饰品", 0.75);
        categoryFactor.put("美妆/护肤", 0.65);
        categoryFactor.put("母婴/玩具", 0.75);
        categoryFactor.put("图书/文具", 0.60);
        categoryFactor.put("运动/户外", 0.75);
        categoryFactor.put("家居/家具", 0.70);
        categoryFactor.put("数码电子", 0.90);
        categoryFactor.put("图书教材", 0.55);
        categoryFactor.put("服饰鞋帽", 0.70);
        categoryFactor.put("日用百货", 0.65);
        categoryFactor.put("其他闲置", 0.60);

        double discount = conditionDiscount.getOrDefault(condition != null ? condition : "almost new", 0.60);
        double catFactor = categoryFactor.getOrDefault(category != null ? category : "其他", 0.70);

        if (originalPrice != null && originalPrice > 0) {
            double predicted = originalPrice * discount * catFactor;
            // 取整到 10 元
            predicted = Math.ceil(predicted / 10) * 10;
            result.put("predicted_price", predicted);
        } else {
            result.put("predicted_price", null);
        }

        return result;
    }

    /**
     * 接收前端传来的 data URI，转发给 AI 服务识别
     */
    public static Map<String, Object> identifyFromDataUri(String dataUri) throws IOException {
        String ML_SERVICE_URL = getMlServiceUrl();
        URL url = new URL(ML_SERVICE_URL + "/api/identify");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("image", dataUri);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(gson.toJson(requestBody).getBytes(StandardCharsets.UTF_8));
        }

        String response = readResponse(conn);
        log.info("AI identify proxy response: {}", response);

        try {
            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            Map<String, Object> result = new HashMap<>();
            if (json.has("rawLabel")) result.put("rawLabel", json.get("rawLabel").getAsString());
            if (json.has("name")) result.put("name", json.get("name").getAsString());
            if (json.has("category")) result.put("category", json.get("category").getAsString());
            if (json.has("defaultDesc")) result.put("defaultDesc", json.get("defaultDesc").getAsString());
            if (json.has("suggestedOriginalPrice")) result.put("suggestedOriginalPrice", json.get("suggestedOriginalPrice").getAsBigDecimal());
            if (json.has("confidence")) {
                String confStr = json.get("confidence").getAsString().replace("%", "");
                result.put("confidence", confStr + "%");
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse AI identify response: {}", response, e);
            return null;
        }
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            throw new IOException("No response from AI server, HTTP " + code);
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}

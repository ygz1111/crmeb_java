package com.zbkj.service.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Qwen-VL 多模态识别客户端，由 Java 后端直接调用阿里云 DashScope。
 * apiKey / apiUrl / model 由 {@link com.zbkj.service.config.QwenConfigInitializer}
 * 在 Spring 启动时从 application.yml 注入。
 */
@Slf4j
public class QwenClient {

    private static final Gson gson = new Gson();
    private static final Pattern JSON_BLOCK = Pattern.compile("\\{[\\s\\S]*\\}");

    private static volatile String apiKey;
    private static volatile String apiUrl =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation";
    private static volatile String model = "qwen-vl-max";

    private static final String PROMPT =
            "请分析这张图片中的商品，并以JSON格式返回以下信息：\n" +
            "{\n" +
            "  \"rawLabel\": \"商品完整名称和规格\",\n" +
            "  \"confidence\": \"识别置信度（如98.5%）\",\n" +
            "  \"name\": \"商品简短名称\",\n" +
            "  \"category\": \"商品分类（数码电子/图书教材/服饰鞋帽/日用百货/运动户外/其他闲置）\",\n" +
            "  \"suggestedOriginalPrice\": 建议原价（数字）,\n" +
            "  \"defaultDesc\": \"商品描述文案\"\n" +
            "}\n" +
            "只返回JSON，不要其他内容。";

    public static void configure(String key, String url, String mdl) {
        if (key != null && !key.isEmpty()) apiKey = key;
        if (url != null && !url.isEmpty()) apiUrl = url;
        if (mdl != null && !mdl.isEmpty()) model = mdl;
    }

    public static boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 调用 Qwen-VL，输入 data URI 形式的图片，返回结构化结果。
     */
    public static Map<String, Object> identify(String dataUri) throws IOException {
        if (!isConfigured()) {
            throw new IOException("Qwen API key 未配置，请在 application.yml 中设置 qwen.api-key");
        }

        JsonObject imagePart = new JsonObject();
        imagePart.addProperty("image", dataUri);
        JsonObject textPart = new JsonObject();
        textPart.addProperty("text", PROMPT);

        JsonArray content = new JsonArray();
        content.add(imagePart);
        content.add(textPart);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.add("content", content);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject input = new JsonObject();
        input.add("messages", messages);

        JsonObject parameters = new JsonObject();
        parameters.addProperty("result_format", "message");

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("input", input);
        body.add("parameters", parameters);

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        String response = readResponse(conn);
        log.info("Qwen raw response length: {}", response.length());

        return parseQwenResponse(response);
    }

    private static Map<String, Object> parseQwenResponse(String response) throws IOException {
        JsonObject root;
        try {
            root = JsonParser.parseString(response).getAsJsonObject();
        } catch (Exception e) {
            throw new IOException("Qwen 返回非 JSON: " + response, e);
        }

        if (root.has("code") && root.get("code").getAsString().length() > 0) {
            throw new IOException("Qwen API error: " + root.toString());
        }

        String text = null;
        try {
            JsonArray choices = root.getAsJsonObject("output").getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonArray cs = choices.get(0).getAsJsonObject()
                        .getAsJsonObject("message").getAsJsonArray("content");
                for (int i = 0; i < cs.size(); i++) {
                    JsonObject part = cs.get(i).getAsJsonObject();
                    if (part.has("text")) {
                        text = part.get("text").getAsString();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 Qwen output 失败: {}", response, e);
        }

        if (text == null) {
            return defaultResult();
        }

        Matcher m = JSON_BLOCK.matcher(text);
        if (!m.find()) {
            log.warn("Qwen 返回不含 JSON: {}", text);
            return defaultResult();
        }

        JsonObject parsed = JsonParser.parseString(m.group()).getAsJsonObject();
        Map<String, Object> result = new HashMap<>();
        if (parsed.has("rawLabel")) result.put("rawLabel", parsed.get("rawLabel").getAsString());
        if (parsed.has("name")) result.put("name", parsed.get("name").getAsString());
        if (parsed.has("category")) result.put("category", parsed.get("category").getAsString());
        if (parsed.has("defaultDesc")) result.put("defaultDesc", parsed.get("defaultDesc").getAsString());
        if (parsed.has("suggestedOriginalPrice")) {
            result.put("suggestedOriginalPrice", parsed.get("suggestedOriginalPrice").getAsBigDecimal());
        }
        if (parsed.has("confidence")) {
            String conf = parsed.get("confidence").getAsString().replace("%", "");
            result.put("confidence", conf + "%");
        }
        return result;
    }

    private static Map<String, Object> defaultResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("rawLabel", "商品识别中");
        result.put("confidence", "85%");
        result.put("name", "二手商品");
        result.put("category", "其他闲置");
        result.put("suggestedOriginalPrice", 100);
        result.put("defaultDesc", "优质二手商品，成色良好，欢迎选购！");
        return result;
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (is == null) {
            throw new IOException("Qwen 无响应，HTTP " + code);
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }
}

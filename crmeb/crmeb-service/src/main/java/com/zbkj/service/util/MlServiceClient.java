package com.zbkj.service.util;

import cn.hutool.core.codec.Base64;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MlServiceClient {

    public static Map<String, Object> classifyImage(byte[] imageBytes) throws IOException {
        String dataUri = "data:image/jpeg;base64," + Base64.encode(imageBytes);
        Map<String, Object> raw = QwenClient.identify(dataUri);
        if (raw == null) return null;

        // 该方法的历史契约：confidence 返回 Double（0~1），与新接口的 "98.5%" 字符串不同
        Map<String, Object> result = new HashMap<>(raw);
        Object conf = raw.get("confidence");
        if (conf instanceof String) {
            try {
                String s = ((String) conf).replace("%", "");
                result.put("confidence", Double.parseDouble(s) / 100.0);
            } catch (NumberFormatException e) {
                result.put("confidence", 0.85);
            }
        }
        return result;
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
     * 接收前端传来的 data URI，由 Java 后端直接调用 Qwen 识别
     */
    public static Map<String, Object> identifyFromDataUri(String dataUri) throws IOException {
        return QwenClient.identify(dataUri);
    }
}

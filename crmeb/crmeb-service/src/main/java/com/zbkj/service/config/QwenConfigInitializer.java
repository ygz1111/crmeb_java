package com.zbkj.service.config;

import com.zbkj.service.util.QwenClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 在 Spring 启动时把 application.yml 中的 qwen 配置注入到 {@link QwenClient}。
 * 由于 QwenClient 是工具静态类（被多处静态调用），用 @PostConstruct 桥接。
 */
@Slf4j
@Component
public class QwenConfigInitializer {

    @Value("${qwen.api-key:}")
    private String apiKey;

    @Value("${qwen.api-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/multimodal-generation/generation}")
    private String apiUrl;

    @Value("${qwen.model:qwen-vl-max}")
    private String model;

    @PostConstruct
    public void init() {
        QwenClient.configure(apiKey, apiUrl, model);
        if (QwenClient.isConfigured()) {
            log.info("Qwen client configured. model={}, url={}", model, apiUrl);
        } else {
            log.warn("Qwen apiKey 未配置，二手商品识别接口将不可用。请在 application.yml 设置 qwen.api-key");
        }
    }
}

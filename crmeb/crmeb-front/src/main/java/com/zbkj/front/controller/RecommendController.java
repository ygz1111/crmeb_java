package com.zbkj.front.controller;

import com.alibaba.fastjson.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@RestController("RecommendController")
@RequestMapping("api/front")
@Api(tags = "推荐算法")
public class RecommendController {

    @Value("${recommend.server.url:http://localhost:5000}")
    private String recommendServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/recommend")
    @ApiOperation(value = "个性化推荐", notes = "基于协同过滤算法的个性化推荐")
    public Object getRecommendations(
            @RequestParam Integer uid,
            @RequestParam(defaultValue = "10") Integer n) {
        try {
            String url = recommendServerUrl + "/api/recommend?uid=" + uid + "&n=" + n;
            log.info("调用推荐服务: {}", url);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            log.info("推荐服务响应: {}", response.getBody());
            return JSON.parse(response.getBody());
        } catch (Exception e) {
            log.error("推荐服务调用失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "推荐服务暂不可用");
            result.put("data", Collections.emptyList());
            return result;
        }
    }

    @GetMapping("/recommend/similar/{productId}")
    @ApiOperation(value = "相似商品推荐", notes = "获取与指定商品相似的商品")
    public Object getSimilarProducts(
            @PathVariable Integer productId,
            @RequestParam(defaultValue = "6") Integer n) {
        try {
            String url = recommendServerUrl + "/api/recommend/similar/" + productId + "?n=" + n;
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            return JSON.parse(response.getBody());
        } catch (Exception e) {
            log.error("推荐服务调用失败: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("code", 500);
            result.put("msg", "推荐服务暂不可用");
            result.put("data", Collections.emptyList());
            return result;
        }
    }
}

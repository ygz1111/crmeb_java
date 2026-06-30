package com.zbkj.front.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageInfo;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.PageParamRequest;
import com.zbkj.common.request.SecondHandProductRequest;
import com.zbkj.common.response.IndexProductResponse;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.front.service.SecondHandService;
import com.zbkj.service.service.StoreProductService;
import com.zbkj.service.service.SystemAttachmentService;
import com.zbkj.service.service.UserService;
import com.zbkj.service.util.MlServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class SecondHandServiceImpl implements SecondHandService {

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    @Autowired
    private UserService userService;

    @Value("${crmeb.imagePath}")
    private String imagePath;

    

    @Override
    public CommonPage<IndexProductResponse> getMyList(PageParamRequest pageParamRequest) {
        Integer uid = userService.getUserId();
        
        // 使用PageHelper分页
        com.github.pagehelper.PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StoreProduct> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(StoreProduct::getUid, uid);
        wrapper.eq(StoreProduct::getIsSecondHand, true);
        wrapper.eq(StoreProduct::getIsDel, false);
        wrapper.orderByDesc(StoreProduct::getId);

        List<StoreProduct> list = storeProductService.list(wrapper);
        
        // 转换为响应对象并关联分类信息
        List<IndexProductResponse> responseList = new ArrayList<>();
        for (StoreProduct product : list) {
            IndexProductResponse resp = new IndexProductResponse();
            BeanUtils.copyProperties(product, resp);
            // 图片URL不手动前缀，ResponseFilter会自动为/api/front/响应添加CDN前缀
            // 手动前缀会导致ResponseFilter二次前缀，造成图片URL损坏
            
            // 设置分类名称（如果有分类ID）
            if (product.getCateId() != null && !product.getCateId().isEmpty()) {
                try {
                    String cateName = getCategoryNameById(product.getCateId());
                    resp.setCateName(cateName);
                } catch (Exception e) {
                    log.warn("Failed to get category name for id: {}", product.getCateId());
                }
            }
            
            responseList.add(resp);
        }
        
        return CommonPage.restPage(responseList);
    }
    
    // 根据分类ID获取分类名称
    private String getCategoryNameById(String cateId) {
        if (cateId == null || cateId.isEmpty()) {
            return "";
        }
        
        // 分类ID映射表（可以根据实际数据库调整）
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("280", "母婴专区");
        categoryMap.put("281", "户外出行");
        categoryMap.put("282", "美妆个护");
        categoryMap.put("283", "数码家电");
        categoryMap.put("284", "日用文创");
        categoryMap.put("272", "厨房电器");
        
        return categoryMap.getOrDefault(cateId, "其他");
    }

    @Override
    public Map<String, Object> publish(SecondHandProductRequest request) {
        Integer uid = userService.getUserId();
        Map<String, Object> result = new HashMap<>();

        StoreProduct product = new StoreProduct();
        product.setUid(uid);
        product.setIsSecondHand(true);
        product.setStoreName(request.getStoreName());
        product.setKeyword(request.getStoreName());
        product.setStoreInfo(request.getStoreInfo());
        product.setCateId(request.getCateId());
        product.setPrice(request.getPrice());
        product.setOtPrice(request.getOtPrice());
        product.setItemCondition(request.getItemCondition());
        product.setStock(ObjectUtil.isNotNull(request.getStock()) ? request.getStock() : 1);
        product.setUnitName(ObjectUtil.isNotNull(request.getUnitName()) ? request.getUnitName() : "piece");
        // 清除图片URL中的域名前缀，确保数据库只存相对路径
        // （upload响应会被ResponseFilter自动加前缀，这里需去除）
        product.setImage(systemAttachmentService.clearPrefix(request.getImage()));
        product.setSliderImage(systemAttachmentService.clearPrefix(request.getSliderImage()));
        product.setIsShow(true);
        product.setIsDel(false);
        product.setIsHot(false);
        product.setIsBest(false);
        product.setIsNew(true);
        product.setIsGood(false);
        product.setIsBenefit(false);
        product.setAddTime(CrmebDateUtil.getNowTime());
        product.setSales(0);
        product.setFicti(0);
        product.setBrowse(0);
        product.setMerId(0);
        product.setMerUse(false);
        product.setSpecType(false);
        product.setIsPostage(true);
        product.setPostage(BigDecimal.ZERO);
        product.setSort(0);
        product.setCost(BigDecimal.ZERO);
        product.setGiveIntegral(0);
        product.setVipPrice(BigDecimal.ZERO);
        product.setVersion(0);

        // 优先使用前端传递的AI数据
        if (ObjectUtil.isNotNull(request.getAiCategory())) {
            product.setAiCategory(request.getAiCategory());
            result.put("aiCategory", request.getAiCategory());
        }
        if (ObjectUtil.isNotNull(request.getAiPredictedPrice())) {
            product.setAiPredictedPrice(request.getAiPredictedPrice());
            result.put("aiPredictedPrice", request.getAiPredictedPrice());
        }
        if (ObjectUtil.isNotNull(request.getAiConfidence())) {
            try {
                BigDecimal conf = new BigDecimal(request.getAiConfidence().replace("%", ""));
                product.setAiConfidence(conf);
                result.put("aiConfidence", request.getAiConfidence());
            } catch (Exception e) {
                log.warn("Failed to parse aiConfidence: {}", request.getAiConfidence());
            }
        }

        // 如果前端没有传递AI数据，则尝试调用ML服务
        if (product.getAiCategory() == null) {
            try {
                String imagePath = resolveImagePath(request.getImage());
                if (imagePath != null) {
                    File imgFile = new File(imagePath);
                    if (imgFile.exists()) {
                        byte[] imageBytes = readFileBytes(imgFile);
                        Map<String, Object> classifyResult = MlServiceClient.classifyImage(imageBytes);
                        if (classifyResult != null) {
                            String aiCategory = (String) classifyResult.get("category");
                            Double aiConf = ((Number) classifyResult.get("confidence")).doubleValue();
                            product.setAiCategory(aiCategory);
                            product.setAiConfidence(BigDecimal.valueOf(aiConf));
                            result.put("aiCategory", aiCategory);
                            result.put("aiConfidence", aiConf);

                            String condition = request.getItemCondition() != null ? request.getItemCondition() : "almost new";
                            Double origPrice = request.getOtPrice() != null ? request.getOtPrice().doubleValue() : null;
                            Map<String, Object> priceResult = MlServiceClient.predictPrice(aiCategory, condition, origPrice);
                            if (priceResult != null) {
                                Double predicted = ((Number) priceResult.get("predicted_price")).doubleValue();
                                product.setAiPredictedPrice(BigDecimal.valueOf(predicted));
                                result.put("aiPredictedPrice", predicted);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("ML service call failed, using defaults", e);
                result.put("mlError", e.getMessage());
            }

        // 兜底：AI 和 ML 都失败时，设置默认分类
        if (product.getAiCategory() == null) {
            product.setAiCategory("其他闲置");
            product.setAiConfidence(BigDecimal.valueOf(0.5));
        }
        }
        boolean saved = storeProductService.save(product);
        result.put("productId", product.getId());
        result.put("success", saved);

        return result;
    }

    @Override
    public CommonPage<IndexProductResponse> getPublicList(PageParamRequest pageParamRequest) {
        com.github.pagehelper.PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StoreProduct> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(StoreProduct::getIsSecondHand, true);
        wrapper.eq(StoreProduct::getIsDel, false);
        wrapper.eq(StoreProduct::getIsShow, true);
        wrapper.orderByDesc(StoreProduct::getId);

        List<StoreProduct> list = storeProductService.list(wrapper);
        List<IndexProductResponse> responseList = new ArrayList<>();
        for (StoreProduct product : list) {
            IndexProductResponse resp = new IndexProductResponse();
            BeanUtils.copyProperties(product, resp);
            responseList.add(resp);
        }
        return CommonPage.restPage(responseList);
    }

    @Override
    public Boolean update(SecondHandProductRequest request) {
        Integer uid = userService.getUserId();
        if (request.getId() == null) {
            throw new RuntimeException("商品ID不能为空");
        }

        StoreProduct product = storeProductService.getById(request.getId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        if (!product.getUid().equals(uid)) {
            throw new RuntimeException("无权修改该商品");
        }

        // 更新字段
        product.setStoreName(request.getStoreName());
        product.setKeyword(request.getStoreName());
        product.setStoreInfo(request.getStoreInfo());
        product.setCateId(request.getCateId());
        product.setPrice(request.getPrice());
        product.setOtPrice(request.getOtPrice());
        product.setItemCondition(request.getItemCondition());
        if (request.getStock() != null) {
            product.setStock(request.getStock());
        }
        if (request.getImage() != null) {
            product.setImage(systemAttachmentService.clearPrefix(request.getImage()));
        }
        if (request.getSliderImage() != null) {
            product.setSliderImage(systemAttachmentService.clearPrefix(request.getSliderImage()));
        }
        if (request.getAiCategory() != null) {
            product.setAiCategory(request.getAiCategory());
        }
        if (request.getAiPredictedPrice() != null) {
            product.setAiPredictedPrice(request.getAiPredictedPrice());
        }

        return storeProductService.updateById(product);
    }

    @Override
    public Boolean delete(Integer productId) {
        Integer uid = userService.getUserId();
        StoreProduct product = storeProductService.getById(productId);
        if (product == null || !product.getUid().equals(uid)) {
            return false;
        }
        product.setIsDel(true);
        return storeProductService.updateById(product);
    }
    
    @Override
    public Map<String, Integer> getMyPublishStats() {
        Integer uid = userService.getUserId();
        Map<String, Integer> stats = new HashMap<>();
        
        // 查询所有未删除的二手商品
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StoreProduct> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(StoreProduct::getUid, uid);
        wrapper.eq(StoreProduct::getIsSecondHand, true);
        wrapper.eq(StoreProduct::getIsDel, false);
        
        List<StoreProduct> allProducts = storeProductService.list(wrapper);
        
        // 统计总数
        int total = allProducts.size();
        
        // 统计在售数量
        int onSale = (int) allProducts.stream()
            .filter(p -> p.getIsShow() != null && p.getIsShow() && p.getStock() > 0)
            .count();
        
        // 统计已售罄数量
        int soldOut = (int) allProducts.stream()
            .filter(p -> p.getStock() != null && p.getStock() <= 0)
            .count();
        
        // 统计已下架数量
        int offShelf = (int) allProducts.stream()
            .filter(p -> p.getIsShow() == null || !p.getIsShow())
            .count();
        
        stats.put("total", total);
        stats.put("onSale", onSale);
        stats.put("soldOut", soldOut);
        stats.put("offShelf", offShelf);
        
        return stats;
    }

    private String resolveImagePath(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        String relative = imageUrl.replace("http://localhost:20500/crmebimage/", "");
        relative = relative.replace("/crmebimage/", "");
        return imagePath + "crmebimage/" + relative;
    }

    private byte[] readFileBytes(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
    }
}






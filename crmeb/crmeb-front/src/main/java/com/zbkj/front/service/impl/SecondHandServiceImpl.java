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
import com.zbkj.service.service.UserService;
import com.zbkj.service.util.MlServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private UserService userService;

    private static final String IMAGE_BASE_PATH = "D:/RECM/crmeb_java/crmeb/crmebimage/";

    @Override
    public CommonPage<IndexProductResponse> getMyList(PageParamRequest pageParamRequest) {
        Integer uid = userService.getUserId();
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StoreProduct> wrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(StoreProduct::getUid, uid);
        wrapper.eq(StoreProduct::getIsSecondHand, true);
        wrapper.eq(StoreProduct::getIsDel, false);
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
    public Map<String, Object> publish(SecondHandProductRequest request) {
        Integer uid = userService.getUserId();
        Map<String, Object> result = new HashMap<>();

        StoreProduct product = new StoreProduct();
        product.setUid(uid);
        product.setIsSecondHand(true);
        product.setStoreName(request.getStoreName());
        product.setStoreInfo(request.getStoreInfo());
        product.setCateId(request.getCateId());
        product.setPrice(request.getPrice());
        product.setOtPrice(request.getOtPrice());
        product.setItemCondition(request.getItemCondition());
        product.setStock(ObjectUtil.isNotNull(request.getStock()) ? request.getStock() : 1);
        product.setUnitName(ObjectUtil.isNotNull(request.getUnitName()) ? request.getUnitName() : "piece");
        product.setImage(request.getImage());
        product.setSliderImage(request.getSliderImage());
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

        boolean saved = storeProductService.save(product);
        result.put("productId", product.getId());
        result.put("success", saved);

        return result;
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

    private String resolveImagePath(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return null;
        String relative = imageUrl.replace("http://localhost:20500/crmebimage/", "");
        relative = relative.replace("/crmebimage/", "");
        return IMAGE_BASE_PATH + relative;
    }

    private byte[] readFileBytes(File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
    }
}
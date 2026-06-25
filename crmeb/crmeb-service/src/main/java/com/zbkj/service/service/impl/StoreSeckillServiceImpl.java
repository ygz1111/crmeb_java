package com.zbkj.service.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zbkj.common.constants.Constants;
import com.zbkj.common.exception.CrmebException;
import com.zbkj.common.model.product.StoreProduct;
import com.zbkj.common.model.product.StoreProductAttr;
import com.zbkj.common.model.product.StoreProductAttrValue;
import com.zbkj.common.model.product.StoreProductDescription;
import com.zbkj.common.model.record.UserVisitRecord;
import com.zbkj.common.model.seckill.StoreSeckill;
import com.zbkj.common.model.seckill.StoreSeckillManger;
import com.zbkj.common.model.user.User;
import com.zbkj.common.page.CommonPage;
import com.zbkj.common.request.*;
import com.zbkj.common.response.*;
import com.zbkj.common.utils.CrmebUtil;
import com.zbkj.common.utils.CrmebDateUtil;
import com.zbkj.common.utils.RedisUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zbkj.service.dao.StoreSeckillDao;
import com.zbkj.service.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StoreSeckillService 实现类*/
@Service
public class StoreSeckillServiceImpl extends ServiceImpl<StoreSeckillDao, StoreSeckill>
        implements StoreSeckillService {

    private static final Logger logger = LoggerFactory.getLogger(StoreSeckillServiceImpl.class);

    @Resource
    private StoreSeckillDao dao;

    @Autowired
    private SystemAttachmentService systemAttachmentService;

    @Autowired
    private StoreProductDescriptionService storeProductDescriptionService;

    @Autowired
    private StoreSeckillMangerService storeSeckillMangerService;

    @Autowired
    private UserService userService;

    @Autowired
    private StoreProductRelationService storeProductRelationService;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private StoreProductAttrService attrService;

    @Autowired
    private StoreProductAttrValueService storeProductAttrValueService;

    @Autowired
    private StoreProductAttrResultService storeProductAttrResultService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserVisitRecordService userVisitRecordService;

    /**
    * 列表
    * @param request 请求参数
    * @param pageParamRequest 分页类参数
    * @return List<StoreSeckill>
    */
    @Override
    public PageInfo<StoreSeckillResponse> getList(StoreSeckillSearchRequest request, PageParamRequest pageParamRequest) {
        //带 StoreSeckill 类的多条件查询
        Page<StoreSeckill> storeSeckillProductPage = PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());

        LambdaQueryWrapper<StoreSeckill> lambdaQueryWrapper = Wrappers.lambdaQuery();
        StoreSeckill model = new StoreSeckill();
        BeanUtils.copyProperties(request, model);
        if (null != request.getStatus()) {
            lambdaQueryWrapper.eq(StoreSeckill::getStatus,request.getStatus());
        }
        if (null != request.getTimeId()) {
            lambdaQueryWrapper.eq(StoreSeckill::getTimeId,request.getTimeId());
        }
        lambdaQueryWrapper.eq(StoreSeckill::getIsDel,false);
        if (StringUtils.isNotBlank(request.getKeywords())) {
            lambdaQueryWrapper.and(wrapper -> wrapper
                    .like(StoreSeckill::getTitle,request.getKeywords())
                    .or()
                    .like(StoreSeckill::getId,request.getKeywords())
            );
        }
        lambdaQueryWrapper.orderByDesc(StoreSeckill::getSort).orderByDesc(StoreSeckill::getId);
        List<StoreSeckill> storeProducts = dao.selectList(lambdaQueryWrapper);
        List<StoreSeckillResponse> storeProductResponses = new ArrayList<>();

        // 当前正在秒杀的timeId 正确的数据这里应该只会获得一条数据
        List<StoreSeckillManger> currentSeckillManager = storeSeckillMangerService.getCurrentSeckillManager();
        Integer currentSkillTimeId = 0;
//        String currentSkillTime = null;
        if (null != currentSeckillManager && currentSeckillManager.size() > 0) {
            currentSkillTimeId = currentSeckillManager.get(0).getId();
        }

        // 查询所有秒杀配置后根据关系添加到秒杀商品列表中
        List<StoreSeckillManagerResponse> storeSeckillMangerServiceList = storeSeckillMangerService.getAllList();
        for (StoreSeckill product : storeProducts) {
            StoreSeckillResponse storeProductResponse = new StoreSeckillResponse();
            BeanUtils.copyProperties(product, storeProductResponse);
            storeProductResponse.setStatusName(getStatusName(product, currentSkillTimeId));
            storeProductResponse.setImages(CrmebUtil.stringToArrayStr(product.getImages()));

            StoreProductAttr storeProductAttrPram = new StoreProductAttr();
            storeProductAttrPram.setProductId(product.getId()).setType(Constants.PRODUCT_TYPE_SECKILL);
            List<StoreProductAttr> attrs = attrService.getByEntity(storeProductAttrPram);

            if (attrs.size() > 0) {
                storeProductResponse.setAttr(attrs);
            }
            // 处理富文本
            StoreProductDescription sd = storeProductDescriptionService.getOne(
                    new LambdaQueryWrapper<StoreProductDescription>()
                            .eq(StoreProductDescription::getProductId, product.getId())
                                .eq(StoreProductDescription::getType, Constants.PRODUCT_TYPE_SECKILL));
            if (null != sd) {
                storeProductResponse.setContent(null == sd.getDescription()?"":sd.getDescription());
            }
            // 添加秒杀配置关系
            List<StoreSeckillManagerResponse> hasTimeIds = storeSeckillMangerServiceList.stream()
                    .filter(e -> e.getId().equals(storeProductResponse.getTimeId())).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(hasTimeIds)) {
                storeProductResponse.setStoreSeckillManagerResponse(hasTimeIds.get(0));
                storeProductResponse.setCurrentTimeId(currentSkillTimeId);
                storeProductResponse.setCurrentTime(hasTimeIds.get(0).getTime());
            }
            storeProductResponses.add(storeProductResponse);
        }
        // 多条sql查询处理分页正确
        return CommonPage.copyPageInfo(storeSeckillProductPage, storeProductResponses);
    }

    /**
     * 获取秒杀状态描述
     * @param seckill 秒杀商品
     * @param currentSkillTimeId 当前时间秒杀时间端id
     * @return 秒杀状态描述
     */
    private String getStatusName(StoreSeckill seckill, Integer currentSkillTimeId) {
        if (seckill.getStatus().equals(0)) {
            return "已关闭";
        }

        // 获取当前时间
        DateTime dateTime = cn.hutool.core.date.DateUtil.date();
        // 开始、结束时间
        String startTimeStr = cn.hutool.core.date.DateUtil.date(seckill.getStartTime()).toString(Constants.DATE_FORMAT_DATE);
        String stopTimeStr = cn.hutool.core.date.DateUtil.date(seckill.getStopTime()).toString(Constants.DATE_FORMAT_DATE);
        DateTime startDate = cn.hutool.core.date.DateUtil.parseDate(startTimeStr + " 00:00:00");
        DateTime stopDate = cn.hutool.core.date.DateUtil.parse(stopTimeStr + " 23:59:59");

        // 比较开始时间(日期)
        if (dateTime.compareTo(startDate) < 0) {// 当前时间 < 开始时间
            return "未开始";
        }
        if (dateTime.compareTo(startDate) >= 0 && dateTime.compareTo(stopDate) <= 0) {
            // 开始时间 <= 当前时间 <= 结束时间
            if (seckill.getTimeId().equals(currentSkillTimeId)) {
                return "进行中";
            }
            return "未开始";
        }
        // 当前时间 > 结束时间
        return "已结束";
    }

    /**
     * 逻辑删除
     *
     * @param id 秒杀id
     * @return 删除结果
     */
    @Override
    public Boolean deleteById(Integer id) {
        StoreSeckill skill = new StoreSeckill().setId(id).setIsDel(true);
        return dao.updateById(skill) > 0;
    }

    /**
     * 新增秒杀商品
     *
     * @param request 待新增秒杀商品
     * @return 新增结果
     */
    @Override
    public Boolean saveSeckill(StoreSeckillAddRequest request) {
        request.getAttrValue().forEach(e -> {
            if ((ObjectUtil.isNull(e.getQuota()) || e.getQuota() <= 0)) {
                throw new CrmebException("请正确输入限量");
            }
        });

        if (isExistTile(request.getTitle())) {
            throw new CrmebException("活动标题已经存在");
        }

        StoreSeckill storeSeckill = new StoreSeckill();
        BeanUtils.copyProperties(request, storeSeckill);
        // 主图
        storeSeckill.setImage(systemAttachmentService.clearPrefix(storeSeckill.getImage()));
        // 轮播图
        storeSeckill.setImages(systemAttachmentService.clearPrefix(storeSeckill.getImages()));
        // 设置秒杀开始时间和结束时间
        storeSeckill.setStartTime(CrmebDateUtil.strToDate(request.getStartTime(), Constants.DATE_FORMAT_DATE));
        storeSeckill.setStopTime(CrmebDateUtil.strToDate(request.getStopTime(), Constants.DATE_FORMAT_DATE));

        //计算价格
        List<StoreProductAttrValueAddRequest> attrValueAddRequestList = request.getAttrValue();
        //计算价格
        StoreProductAttrValueAddRequest minAttrValue = attrValueAddRequestList.stream().min(Comparator.comparing(StoreProductAttrValueAddRequest::getPrice)).get();
        storeSeckill.setPrice(minAttrValue.getPrice());
        storeSeckill.setOtPrice(minAttrValue.getOtPrice());
        storeSeckill.setCost(minAttrValue.getCost());
        int quota = attrValueAddRequestList.stream().mapToInt(StoreProductAttrValueAddRequest::getQuota).sum();
        storeSeckill.setStock(quota);
        storeSeckill.setQuota(quota);
        storeSeckill.setQuotaShow(quota);
        storeSeckill.setSort(0);
        if (ObjectUtil.isNotNull(request.getSort())) {
            storeSeckill.setSort(request.getSort());
        }

        List<StoreProductAttrAddRequest> addRequestList = request.getAttr();
        List<StoreProductAttr> attrList = addRequestList.stream().map(e -> {
            StoreProductAttr attr = new StoreProductAttr();
            BeanUtils.copyProperties(e, attr);
            attr.setType(Constants.PRODUCT_TYPE_SECKILL);
            return attr;
        }).collect(Collectors.toList());

        List<StoreProductAttrValue> attrValueList = attrValueAddRequestList.stream().map(e -> {
            StoreProductAttrValue attrValue = new StoreProductAttrValue();
            BeanUtils.copyProperties(e, attrValue);
            attrValue.setId(null);
            attrValue.setSuk(e.getSuk());
            attrValue.setQuota(e.getQuota());
            attrValue.setQuotaShow(e.getQuota());
            attrValue.setType(Constants.PRODUCT_TYPE_SECKILL);
            attrValue.setImage(systemAttachmentService.clearPrefix(e.getImage()));
            return attrValue;
        }).collect(Collectors.toList());

        // 处理富文本
        StoreProductDescription spd = new StoreProductDescription();
        spd.setDescription(request.getContent().length() > 0 ? systemAttachmentService.clearPrefix(request.getContent()) : "");
        spd.setType(Constants.PRODUCT_TYPE_SECKILL);

        Boolean execute = transactionTemplate.execute(e -> {
            save(storeSeckill);

            attrList.forEach(attr -> attr.setProductId(storeSeckill.getId()));
            attrValueList.forEach(value -> value.setProductId(storeSeckill.getId()));
            attrService.saveBatch(attrList);
            storeProductAttrValueService.saveBatch(attrValueList);

            spd.setProductId(storeSeckill.getId());
            storeProductDescriptionService.deleteByProductId(storeSeckill.getId(), Constants.PRODUCT_TYPE_SECKILL);
            storeProductDescriptionService.save(spd);
            return Boolean.TRUE;
        });

        return execute;
    }

    // 是否存在秒杀活动标题查找
    private Boolean isExistTile(String title) {
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.select(StoreSeckill::getId);
        lqw.eq(StoreSeckill::getTitle, title);
        lqw.eq(StoreSeckill::getIsDel, false);
        StoreSeckill storeSeckill = dao.selectOne(lqw);
        return ObjectUtil.isNotNull(storeSeckill);
    }

    /**
     * 更新秒杀商品
     *
     * @param request 待更新秒杀商品
     * @return 更新结果
     */
    @Override
    public Boolean updateSeckill(StoreSeckillAddRequest request) {

        StoreSeckill storeSeckill = getById(request.getId());
        if (ObjectUtil.isNull(storeSeckill) || storeSeckill.getIsDel()) {
            throw new CrmebException("秒杀商品不存在");
        }

        if (storeSeckill.getStatus().equals(1)) {
            throw new CrmebException("请先关闭秒杀商品，再修改商品信息");
        }


        StoreSeckill seckill = new StoreSeckill();
        BeanUtils.copyProperties(request, seckill);
        seckill.setStartTime(CrmebDateUtil.strToDate(request.getStartTime(),Constants.DATE_FORMAT_DATE));
        seckill.setStopTime(CrmebDateUtil.strToDate(request.getStopTime(),Constants.DATE_FORMAT_DATE));

        //主图
        seckill.setImage(systemAttachmentService.clearPrefix(seckill.getImage()));

        //轮播图
        seckill.setImages(systemAttachmentService.clearPrefix(seckill.getImages()));

        //计算价格
        List<StoreProductAttrValueAddRequest> attrValueAddRequestList = request.getAttrValue();
        StoreProductAttrValueAddRequest minAttrValue = attrValueAddRequestList.stream().min(Comparator.comparing(StoreProductAttrValueAddRequest::getPrice)).get();
        seckill.setPrice(minAttrValue.getPrice());
        seckill.setOtPrice(minAttrValue.getOtPrice());
        seckill.setCost(minAttrValue.getCost());
        int quota = attrValueAddRequestList.stream().mapToInt(StoreProductAttrValueAddRequest::getQuota).sum();
        seckill.setStock(quota);
        seckill.setQuota(quota);
        seckill.setQuotaShow(quota);

        List<StoreProductAttrAddRequest> addRequestList = request.getAttr();
        List<StoreProductAttr> attrAddList = CollUtil.newArrayList();
        List<StoreProductAttr> attrUpdateList = CollUtil.newArrayList();
        addRequestList.forEach(e -> {
            StoreProductAttr attr = new StoreProductAttr();
            BeanUtils.copyProperties(e, attr);
            if (ObjectUtil.isNull(attr.getId())) {
                attr.setProductId(seckill.getId());
                attr.setType(Constants.PRODUCT_TYPE_SECKILL);
                attrAddList.add(attr);
            } else {
                attr.setProductId(seckill.getId());
                attr.setIsDel(false);
                attrUpdateList.add(attr);
            }
        });

        // attrValue部分
        List<StoreProductAttrValue> attrValueAddList = CollUtil.newArrayList();
        List<StoreProductAttrValue> attrValueUpdateList = CollUtil.newArrayList();
        attrValueAddRequestList.forEach(e -> {
            StoreProductAttrValue attrValue = new StoreProductAttrValue();
            BeanUtils.copyProperties(e, attrValue);
            attrValue.setSuk(e.getSuk());
            attrValue.setImage(systemAttachmentService.clearPrefix(e.getImage()));
            if (ObjectUtil.isNull(attrValue.getId())) {
                attrValue.setProductId(seckill.getId());
                attrValue.setQuota(e.getQuota());
                attrValue.setQuotaShow(e.getQuota());
                attrValue.setType(Constants.PRODUCT_TYPE_SECKILL);
                attrValueAddList.add(attrValue);
            } else {
                attrValue.setProductId(seckill.getId());
                attrValue.setIsDel(false);
                attrValueUpdateList.add(attrValue);
            }
        });

        // 处理富文本
        StoreProductDescription spd = new StoreProductDescription();
        spd.setDescription(request.getContent().length() > 0 ? systemAttachmentService.clearPrefix(request.getContent()) : "");
        spd.setType(Constants.PRODUCT_TYPE_SECKILL);
        spd.setProductId(seckill.getId());

        Boolean execute = transactionTemplate.execute(e -> {
            dao.updateById(seckill);

            // 先删除原用attr+value
            attrService.deleteByProductIdAndType(seckill.getId(), Constants.PRODUCT_TYPE_SECKILL);
            storeProductAttrValueService.deleteByProductIdAndType(seckill.getId(), Constants.PRODUCT_TYPE_SECKILL);

            if (CollUtil.isNotEmpty(attrAddList)) {
                attrService.saveBatch(attrAddList);
            }
            if (CollUtil.isNotEmpty(attrUpdateList)) {
                attrService.saveOrUpdateBatch(attrUpdateList);
            }

            if (CollUtil.isNotEmpty(attrValueAddList)) {
                storeProductAttrValueService.saveBatch(attrValueAddList);
            }
            if (CollUtil.isNotEmpty(attrValueUpdateList)) {
                storeProductAttrValueService.saveOrUpdateBatch(attrValueUpdateList);
            }

            storeProductDescriptionService.deleteByProductId(seckill.getId(), Constants.PRODUCT_TYPE_SECKILL);
            storeProductDescriptionService.save(spd);

            return Boolean.TRUE;
        });
        return execute;
    }

    /**
     * 更新秒杀状态
     *
     * @param secKillId 秒杀id
     * @param status    秒杀状态
     * @return 更新结果
     */
    @Override
    public Boolean updateSecKillStatus(int secKillId, boolean status) {
        StoreSeckill seckill = getById(secKillId);
        if (ObjectUtil.isNull(seckill) || seckill.getIsDel()) {
            throw new CrmebException("秒杀商品不存在");
        }
        if (status) {
            // 判断商品是否存在
            StoreProduct product = storeProductService.getById(seckill.getProductId());
            if (ObjectUtil.isNull(product)) {
                throw new CrmebException("关联的商品已删除，无法开启活动");
            }
        }

        StoreSeckill storeSeckill = new StoreSeckill().setId(secKillId).setStatus(status?1:0);
        return dao.updateById(storeSeckill) > 0;
    }

    /**
     * 秒杀商品详情
     *
     * @param skillId 秒杀商品id
     * @return 详情
     */
    @Override
    public StoreSeckillDetailResponse getDetailH5(Integer skillId) {
        // 获取秒杀商品信息
        StoreSeckill storeSeckill = dao.selectById(skillId);
        if (ObjectUtil.isNull(storeSeckill) || storeSeckill.getIsDel()) {
            throw new CrmebException("未找到对应秒杀商品信息");
        }
        if (storeSeckill.getStatus().equals(0)) {
            throw new CrmebException("秒杀商品已下架");
        }
        StoreSeckillDetailResponse productDetailResponse = new StoreSeckillDetailResponse();

        SecKillDetailH5Response detailH5Response = new SecKillDetailH5Response();
        BeanUtils.copyProperties(storeSeckill, detailH5Response);
        detailH5Response.setStoreName(storeSeckill.getTitle());
        detailH5Response.setSliderImage(storeSeckill.getImages());
//        detailH5Response.setStoreInfo(storeSeckill.getInfo());
        // 详情
        StoreProductDescription sd = storeProductDescriptionService.getOne(
                new LambdaQueryWrapper<StoreProductDescription>()
                        .eq(StoreProductDescription::getProductId, skillId)
                        .eq(StoreProductDescription::getType, Constants.PRODUCT_TYPE_SECKILL));
        if (ObjectUtil.isNotNull(sd)) {
            detailH5Response.setContent(ObjectUtil.isNull(sd.getDescription()) ? "" : sd.getDescription());
        }
        // 获取主商品信息
        StoreProduct storeProduct = storeProductService.getById(storeSeckill.getProductId());
        // 主商品状态
        if (storeProduct.getIsDel()) {
            productDetailResponse.setMasterStatus("delete");
        } else if (!storeProduct.getIsShow()) {
            productDetailResponse.setMasterStatus("soldOut");
        } else if (storeProduct.getStock() <= 0) {
            productDetailResponse.setMasterStatus("sellOut");
        } else {
            productDetailResponse.setMasterStatus("normal");
        }

        // 秒杀销量 = 原商品销量（包含虚拟销量）
        detailH5Response.setSales(storeProduct.getSales());
        detailH5Response.setFicti(storeProduct.getFicti());

        StoreSeckillManger seckillManger = storeSeckillMangerService.getById(storeSeckill.getTimeId());
        if (ObjectUtil.isNotNull(seckillManger)) {
            int secKillEndSecondTimestamp = CrmebDateUtil.getSecondTimestamp(CrmebDateUtil.nowDateTime("yyyy-MM-dd " + seckillManger.getEndTime() + ":00:00"));
            detailH5Response.setTimeSwap(secKillEndSecondTimestamp + "");
        }
        Integer seckillStatus = getSeckillStatus(storeSeckill, seckillManger);
        detailH5Response.setSeckillStatus(seckillStatus);
        productDetailResponse.setStoreSeckill(detailH5Response);

        // 获取秒杀商品规格
        List<StoreProductAttr> attrList = attrService.getListByProductIdAndType(skillId, Constants.PRODUCT_TYPE_SECKILL);
        // 根据制式设置attr属性
        productDetailResponse.setProductAttr(attrList);

        // 根据制式设置sku属性
        HashMap<String, Object> skuMap = CollUtil.newHashMap();
        // 获取主商品sku
        List<StoreProductAttrValue> storeProductAttrValues = storeProductAttrValueService.getListByProductIdAndType(storeSeckill.getProductId(), Constants.PRODUCT_TYPE_NORMAL);
        // 获取秒杀商品sku
        List<StoreProductAttrValue> seckillAttrValues = storeProductAttrValueService.getListByProductIdAndType(storeSeckill.getId(), Constants.PRODUCT_TYPE_SECKILL);

        for (int i = 0; i < storeProductAttrValues.size(); i++) {
            StoreProductAttrValueResponse atr = new StoreProductAttrValueResponse();
            StoreProductAttrValue productAttrValue = storeProductAttrValues.get(i);
            List<StoreProductAttrValue> valueList = seckillAttrValues.stream().filter(e -> productAttrValue.getSuk().equals(e.getSuk())).collect(Collectors.toList());
            if (CollUtil.isEmpty(valueList)) {
                BeanUtils.copyProperties(productAttrValue, atr);
            } else {
                BeanUtils.copyProperties(valueList.get(0), atr);
            }
            skuMap.put(atr.getSuk(), atr);
        }
        productDetailResponse.setProductValue(skuMap);

        // 设置点赞和收藏
        User user = userService.getInfo();
        if (ObjectUtil.isNotNull(user)) {
            productDetailResponse.setUserCollect(storeProductRelationService.getLikeOrCollectByUser(user.getUid(), detailH5Response.getProductId(),false).size() > 0);
        } else {
            productDetailResponse.setUserCollect(false);
        }

        // 保存用户访问记录
        UserVisitRecord visitRecord = new UserVisitRecord();
        visitRecord.setDate(cn.hutool.core.date.DateUtil.date().toString("yyyy-MM-dd"));
        visitRecord.setUid(userService.getUserId());
        visitRecord.setVisitType(3);
        userVisitRecordService.save(visitRecord);
        return productDetailResponse;
    }

    /**
     * 获取秒杀状态
     * @param storeSeckill 秒杀商品
     * @return 秒杀状态
     */
    private Integer getSeckillStatus(StoreSeckill storeSeckill, StoreSeckillManger seckillManger) {
        if (storeSeckill.getStatus() == 0) {
            // 关闭
            return 0;
        }
        if (storeSeckill.getStatus() == 1) {
            String ymdStart = cn.hutool.core.date.DateUtil.date(storeSeckill.getStartTime()).toString(Constants.DATE_FORMAT_DATE);
            String startTimeStr = seckillManger.getStartTime() < 10 ? "0" + seckillManger.getStartTime() : seckillManger.getStartTime().toString();
            DateTime startTime = cn.hutool.core.date.DateUtil.parse(ymdStart + " " + startTimeStr + ":00:00");
            Date nowDateTime = CrmebDateUtil.nowDateTime();
            if (nowDateTime.compareTo(startTime) <= 0) {
                // 即将开始
                return 1;
            }
            String ymdEnd = cn.hutool.core.date.DateUtil.date(storeSeckill.getStopTime()).toString(Constants.DATE_FORMAT_DATE);
            String endTimeStr = seckillManger.getStartTime() < 10 ? "0" + seckillManger.getEndTime() : seckillManger.getEndTime().toString();
            DateTime stopTime = cn.hutool.core.date.DateUtil.parse(ymdEnd + " " + endTimeStr + ":00:00");
            if (nowDateTime.compareTo(stopTime) >= 0) {
                // 已结束
                return -1;
            }
            if (nowDateTime.compareTo(startTime) > 0 && nowDateTime.compareTo(stopTime) < 0) {
                // 进行中
                return 2;
            }
        }

        return -2;
    }

    /**
     * 秒杀商品详情 管理端
     *
     * @param skillId 秒杀id
     * @return 详情数据
     */
    @Override
    public StoreProductInfoResponse getDetailAdmin(Integer skillId) {
        StoreSeckill storeSeckill = dao.selectById(skillId);
        if (ObjectUtil.isNull(storeSeckill) || storeSeckill.getIsDel()) {
            throw new CrmebException("未找到对应商品信息");
        }
        StoreProductInfoResponse infoResponse = new StoreProductInfoResponse();
        BeanUtils.copyProperties(storeSeckill, infoResponse);

        infoResponse.setStoreName(storeSeckill.getTitle());
//        infoResponse.setStoreInfo(storeSeckill.getInfo());
        infoResponse.setSliderImage(String.join(",",storeSeckill.getImages()));
        infoResponse.setStartTimeStr(cn.hutool.core.date.DateUtil.format(storeSeckill.getStartTime(), Constants.DATE_FORMAT_DATE));
        infoResponse.setStopTimeStr(cn.hutool.core.date.DateUtil.format(storeSeckill.getStopTime(), Constants.DATE_FORMAT_DATE));
        infoResponse.setProductId(storeSeckill.getProductId());

        // 查询attr
        List<StoreProductAttr> attrList = attrService.getListByProductIdAndType(skillId, Constants.PRODUCT_TYPE_SECKILL);
        infoResponse.setAttr(attrList);

        // 注意：数据瓶装步骤：分别查询秒杀和商品本山信息组装sku信息之后，再对比sku属性是否相等来赋值是否秒杀sku信息
        List<StoreProductAttrValue> seckillAttrValueList = storeProductAttrValueService.getListByProductIdAndType(skillId, Constants.PRODUCT_TYPE_SECKILL);
        // 查询主商品sku
        List<StoreProductAttrValue> attrValueList = storeProductAttrValueService.getListByProductIdAndType(storeSeckill.getProductId(), Constants.PRODUCT_TYPE_NORMAL);

        List<AttrValueResponse> valueResponseList = attrValueList.stream().map(e -> {
            AttrValueResponse valueResponse = new AttrValueResponse();
            Integer id = 0;
            for (StoreProductAttrValue value : seckillAttrValueList) {
                if (value.getSuk().equals(e.getSuk())) {
                    id = value.getId();
                    BeanUtils.copyProperties(value, valueResponse);
                    break;
                }
            }
            if (id.equals(0)) {
                BeanUtils.copyProperties(e, valueResponse);
                valueResponse.setId(null);
            } else {
                valueResponse.setId(id);
            }
            return valueResponse;
        }).collect(Collectors.toList());
        infoResponse.setAttrValue(valueResponseList);

        StoreProductDescription sd = storeProductDescriptionService.getByProductIdAndType(skillId, Constants.PRODUCT_TYPE_SECKILL);
        if (ObjectUtil.isNotNull(sd)) {
            infoResponse.setContent(ObjectUtil.isNull(sd.getDescription()) ? "" : sd.getDescription());
        }
        return infoResponse;
    }

    /**
     * 移动端 获取秒杀配置
     * 获取当前时间段 + 下边的6个商品
     * @return 秒杀配置
     */
    @Override
    public List<SecKillResponse> getForH5Index() {
        List<SecKillResponse> response = new ArrayList<>();
        int currentHour = CrmebDateUtil.getCurrentHour();
        // 获取所有的秒杀配置
        List<StoreSeckillManagerResponse> skillManagerList = storeSeckillMangerService.getH5List();
        // 根据当前时间过滤 仅处理正在进行和马上开始的秒杀
        skillManagerList.forEach(e->{
            // 根据当前秒杀配置id查询是否有商品正在参与次时间段
            Integer proNum = getCountByTimeId(e.getId());
            if (proNum > 0) {
                int secKillEndSecondTimestamp = CrmebDateUtil.getSecondTimestamp(CrmebDateUtil.nowDateTime("yyyy-MM-dd " + e.getEndTime() + ":00:00"));
                SecKillResponse r = new SecKillResponse(e.getId(),e.getSilderImgs(),e.getStatusName(),
                        e.getTime(),e.getKillStatus(),secKillEndSecondTimestamp+"");
                if (e.getStartTime() <= currentHour && currentHour < e.getEndTime()) {
                    r.setIsCheck(true);
                }
                response.add(r);
            }
        });

        return response;
    }

    /**
     * 获取秒杀时段商品数量
     * @param timeId 秒杀时段id
     * @return Integer
     */
    private Integer getCountByTimeId(Integer timeId) {
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getStatus,1);
        lqw.eq(StoreSeckill::getIsDel,false);
        lqw.eq(StoreSeckill::getIsShow,true);
        lqw.eq(StoreSeckill::getTimeId,timeId);
        String currentDate = CrmebDateUtil.nowDate(Constants.DATE_FORMAT_DATE);
        lqw.le(StoreSeckill::getStartTime, currentDate);
        lqw.ge(StoreSeckill::getStopTime, currentDate);
        lqw.orderByDesc(StoreSeckill::getId);
        return dao.selectCount(lqw);
    }

    /**
     * 根据秒杀时间段查询已配置的秒杀商品
     *
     * @param timeId 秒杀id
     * @return 秒杀中的商品
     */
    @Override
    public List<StoreSecKillH5Response> getKillListByTimeId(String timeId, PageParamRequest pageParamRequest) {
        PageHelper.startPage(pageParamRequest.getPage(), pageParamRequest.getLimit());
        String currentDate = CrmebDateUtil.nowDate(Constants.DATE_FORMAT_DATE);
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getStatus,1);
        lqw.eq(StoreSeckill::getIsDel,false);
        lqw.eq(StoreSeckill::getIsShow,true);
        lqw.eq(StoreSeckill::getTimeId,timeId);
        lqw.le(StoreSeckill::getStartTime, currentDate);
        lqw.ge(StoreSeckill::getStopTime,currentDate);
        lqw.orderByDesc(StoreSeckill::getId);
        List<StoreSeckill> storeSeckills = dao.selectList(lqw);
        if (CollUtil.isEmpty(storeSeckills)) {
            return CollUtil.newArrayList();
        }
        List<StoreSecKillH5Response> responses = new ArrayList<>();
        storeSeckills.forEach(e->{
            StoreSecKillH5Response response = new StoreSecKillH5Response();
            BeanUtils.copyProperties(e, response);
            response.setPercent(CrmebUtil.percentInstanceIntVal(e.getQuotaShow() - e.getQuota(), e.getQuotaShow()));
            responses.add(response);
        });
        return responses;
    }

    /**
     * 根据商品id查询正在秒杀的商品信息
     *
     * @param productId 商品id
     * @return 正在参与的秒杀信息
     */
    @Override
    public List<StoreSeckill> getCurrentSecKillByProductId(Integer productId) {
        List<StoreSeckill> result = new ArrayList<>();
        // 获取当前时间段的秒杀
        PageParamRequest pageParamRequest = new PageParamRequest();
        pageParamRequest.setLimit(20);
        List<StoreSeckillManagerResponse> storeSeckillManagerResponses = storeSeckillMangerService.getAllList();
        List<StoreSeckillManagerResponse> currentSsmr =
                storeSeckillManagerResponses.stream().filter(e -> e.getKillStatus() == 2).collect(Collectors.toList());
        if (currentSsmr.size() == 0) {
            return result;
        }
        List<Integer> skillManagerIds = currentSsmr.stream().map(StoreSeckillManagerResponse::getId).collect(Collectors.toList());
        // 获取正在秒杀的商品信息
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getProductId,productId);
        lqw.eq(StoreSeckill::getIsDel,false);
        lqw.eq(StoreSeckill::getStatus, 1);
        lqw.in(StoreSeckill::getTimeId, skillManagerIds);
        result = dao.selectList(lqw);
        return result;
    }

    /**
     * 后台任务批量操作库存
     */
    @Override
    public void consumeProductStock() {
        String redisKey = Constants.PRODUCT_SECKILL_STOCK_UPDATE;
        Long size = redisUtil.getListSize(redisKey);
        logger.info("StoreProductServiceImpl.doProductStock | size:" + size);
        if (size < 1) {
            return;
        }
        for (int i = 0; i < size; i++) {
            //如果10秒钟拿不到一个数据，那么退出循环
            Object data = redisUtil.getRightPop(redisKey, 10L);
            if (null == data) {
                continue;
            }
            try{
                StoreProductStockRequest storeProductStockRequest =
                        com.alibaba.fastjson.JSONObject.toJavaObject(com.alibaba.fastjson.JSONObject.parseObject(data.toString()), StoreProductStockRequest.class);
                boolean result = doProductStock(storeProductStockRequest);
                if (!result) {
                    redisUtil.lPush(redisKey, data);
                }
            }catch (Exception e) {
                redisUtil.lPush(redisKey, data);
            }
        }
    }

    /**
     * 商品是否存在秒杀活动
     * @param productId 商品编号
     * @return Boolean
     */
    @Override
    public Boolean isExistActivity(Integer productId) {
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getProductId, productId);
        lqw.eq(StoreSeckill::getIsDel, false);
        List<StoreSeckill> seckillList = dao.selectList(lqw);
        if (CollUtil.isEmpty(seckillList)) {
            return false;
        }
        // 判断关联的商品是否处于活动开启状态
        List<StoreSeckill> list = seckillList.stream().filter(i -> i.getStatus().equals(1)).collect(Collectors.toList());
        return CollUtil.isNotEmpty(list);
    }

    /**
     * 查询带异常
     * @param id 秒杀商品id
     * @return StoreSeckill
     */
    @Override
    public StoreSeckill getByIdException(Integer id) {
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.eq(StoreSeckill::getId, id);
        lqw.eq(StoreSeckill::getIsDel, false);
        lqw.eq(StoreSeckill::getIsShow, true);
        StoreSeckill storeSeckill = dao.selectOne(lqw);
        if (ObjectUtil.isNull(storeSeckill) || storeSeckill.getIsDel()) throw new CrmebException("秒杀商品不存在或以删除");
        return storeSeckill;
    }

    /**
     * 添加(退货)/扣减库存
     * @param id 秒杀商品id
     * @param num 数量
     * @param type 类型：add—添加，sub—扣减
     * @return Boolean
     */
    @Override
    public Boolean operationStock(Integer id, Integer num, String type) {
        UpdateWrapper<StoreSeckill> updateWrapper = new UpdateWrapper<>();
        if (type.equals("add")) {
            updateWrapper.setSql(StrUtil.format("stock = stock + {}", num));
            updateWrapper.setSql(StrUtil.format("sales = sales - {}", num));
            updateWrapper.setSql(StrUtil.format("quota = quota + {}", num));
        }
        if (type.equals("sub")) {
            updateWrapper.setSql(StrUtil.format("stock = stock - {}", num));
            updateWrapper.setSql(StrUtil.format("sales = sales + {}", num));
            updateWrapper.setSql(StrUtil.format("quota = quota - {}", num));
            // 扣减时加乐观锁保证库存不为负
            updateWrapper.last(StrUtil.format(" and (quota - {} >= 0)", num));
        }
        updateWrapper.eq("id", id);
        boolean update = update(updateWrapper);
        if (!update) {
            throw new CrmebException("更新商品库存失败！商品id = " + id);
        }
        return update;
    }

    /**
     * 获取秒杀首页信息
     * 当前时段秒杀信息 + 当前时段秒杀商品6条
     * @return SeckillIndexResponse
     */
    @Override
    public SeckillIndexResponse getIndexInfo() {
        StoreSeckillManger storeSeckillManger = new StoreSeckillManger();
        storeSeckillManger.setIsDel(false);
        // 根据当前时间过滤 仅处理正在进行的秒杀
        List<StoreSeckillManger> currentSeckillManagerList = storeSeckillMangerService.getCurrentSeckillManager();
        if (CollUtil.isEmpty(currentSeckillManagerList)) {
            return null;
        }
        StoreSeckillManger seckillManger = currentSeckillManagerList.get(0);
        if (seckillManger.getStatus().equals(0)) {// 秒杀时段关闭直接返回
            return null;
        }

        // 查询当前时段秒杀商品
        String currentDate = CrmebDateUtil.nowDate(Constants.DATE_FORMAT_DATE);
        LambdaQueryWrapper<StoreSeckill> lqw = Wrappers.lambdaQuery();
        lqw.select(StoreSeckill::getId, StoreSeckill::getProductId, StoreSeckill::getImage, StoreSeckill::getTitle, StoreSeckill::getPrice, StoreSeckill::getOtPrice);
        lqw.eq(StoreSeckill::getStatus,1);
        lqw.eq(StoreSeckill::getIsDel,false);
        lqw.eq(StoreSeckill::getIsShow,true);
        lqw.eq(StoreSeckill::getTimeId, seckillManger.getId());
        lqw.le(StoreSeckill::getStartTime, currentDate);
        lqw.ge(StoreSeckill::getStopTime,currentDate);
        lqw.orderByDesc(StoreSeckill::getId);
        lqw.last(" limit 6");
        List<StoreSeckill> seckillList = dao.selectList(lqw);
        if (CollUtil.isEmpty(seckillList)) {
            // 如果没有秒杀商品也不展示
            return null;
        }

        SeckillIndexResponse response = new SeckillIndexResponse();
        // 处理秒杀时段信息
        StoreSeckillManagerResponse managerResponse = new StoreSeckillManagerResponse();
        BeanUtils.copyProperties(seckillManger, managerResponse);
        String pStartTime = seckillManger.getStartTime().toString();
        String pEndTime = seckillManger.getEndTime().toString();
        String startTime = pStartTime.length() == 1 ? "0" + pStartTime:pStartTime;
        String endTime = pEndTime.length() == 1 ? "0" + pEndTime : pEndTime;
        managerResponse.setTime(startTime + ":00," + endTime + ":00");
        int secKillEndSecondTimestamp = CrmebDateUtil.getSecondTimestamp(CrmebDateUtil.nowDateTime("yyyy-MM-dd " + seckillManger.getEndTime() + ":00:00"));
        SecKillResponse secKillResponse = new SecKillResponse(seckillManger.getId(),seckillManger.getSilderImgs(),managerResponse.getStatusName(),
                managerResponse.getTime(),managerResponse.getKillStatus(),secKillEndSecondTimestamp+"");
        response.setSecKillResponse(secKillResponse);
        response.setProductList(seckillList);
        return response;
    }
    ///////////////////////////////////////////////////////////////////  自定义方法

    // 秒杀操作库存
    private boolean doProductStock(StoreProductStockRequest storeProductStockRequest) {
        // 秒杀商品信息回滚
        StoreSeckill existProduct = getById(storeProductStockRequest.getSeckillId());
        List<StoreProductAttrValue> existAttr =
                storeProductAttrValueService.getListByProductIdAndAttrId(
                        storeProductStockRequest.getSeckillId(),
                        storeProductStockRequest.getAttrId().toString(),
                        storeProductStockRequest.getType());
        if (null == existProduct || null == existAttr) { // 未找到商品
            logger.info("库存修改任务未获取到商品信息"+JSON.toJSONString(storeProductStockRequest));
            return true;
        }

        // 回滚商品库存/销量 并更新
        boolean isPlus = storeProductStockRequest.getOperationType().equals("add");
        int productStock = isPlus ? existProduct.getStock() + storeProductStockRequest.getNum() : existProduct.getStock() - storeProductStockRequest.getNum();
        existProduct.setStock(productStock);
        existProduct.setSales(existProduct.getSales() - storeProductStockRequest.getNum());
        existProduct.setQuota(existProduct.getQuota() + storeProductStockRequest.getNum());
        updateById(existProduct);

        // 回滚sku库存
        for (StoreProductAttrValue attrValue : existAttr) {
            int productAttrStock = isPlus ? attrValue.getStock() + storeProductStockRequest.getNum() : attrValue.getStock() - storeProductStockRequest.getNum();
            attrValue.setStock(productAttrStock);
            attrValue.setSales(attrValue.getSales()-storeProductStockRequest.getNum());
            attrValue.setQuota(attrValue.getQuota() + storeProductStockRequest.getNum());
            storeProductAttrValueService.updateById(attrValue);
        }

        // 商品本身库存回滚
        // StoreProductStockRequest 创建次对象调用商品扣减库存实现扣减上本本身库存
        StoreProductResponse existProductLinkedSeckill = storeProductService.getByProductId(storeProductStockRequest.getProductId());
        for (StoreProductAttrValueResponse attrValueResponse : existProductLinkedSeckill.getAttrValue()) {
            if (attrValueResponse.getSuk().equals(storeProductStockRequest.getSuk())) {
                StoreProductStockRequest r = new StoreProductStockRequest()
                        .setAttrId(attrValueResponse.getId())
                        .setNum(storeProductStockRequest.getNum())
                        .setOperationType("add")
                        .setProductId(storeProductStockRequest.getProductId())
                        .setType(Constants.PRODUCT_TYPE_NORMAL)
                        .setSuk(storeProductStockRequest.getSuk());
                storeProductService.doProductStock(r);
            }
        }
        return true;
    }

}


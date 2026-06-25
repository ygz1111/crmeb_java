package com.zbkj.service.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zbkj.common.model.express.ShippingTemplatesRegion;
import com.zbkj.common.response.ShippingTemplatesRegionResponse;

import java.util.List;

/**
 *  Mapper 接口*/
public interface ShippingTemplatesRegionDao extends BaseMapper<ShippingTemplatesRegion> {

    List<ShippingTemplatesRegionResponse> getListGroup(Integer tempId);
}

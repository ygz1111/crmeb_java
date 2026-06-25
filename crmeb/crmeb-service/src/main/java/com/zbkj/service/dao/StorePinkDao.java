package com.zbkj.service.dao;

import com.zbkj.common.model.combination.StorePink;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 拼团表 Mapper 接口*/
public interface StorePinkDao extends BaseMapper<StorePink> {

    List<StorePink> selectSizePink(Integer size);
}

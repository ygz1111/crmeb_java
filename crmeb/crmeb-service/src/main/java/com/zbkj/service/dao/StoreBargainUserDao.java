package com.zbkj.service.dao;

import com.zbkj.common.model.bargain.StoreBargainUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
 * 用户参与砍价表 Mapper 接口*/
public interface StoreBargainUserDao extends BaseMapper<StoreBargainUser> {

    List<StoreBargainUser> selectHeaderList();
}

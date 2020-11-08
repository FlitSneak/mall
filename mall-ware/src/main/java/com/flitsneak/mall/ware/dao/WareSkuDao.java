package com.flitsneak.mall.ware.dao;

import com.flitsneak.mall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 * 
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 09:56:54
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {
	
}

package com.flitsneak.mall.product.dao;

import com.flitsneak.mall.product.entity.BrandEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 品牌
 * 
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 07:25:56
 */
@Mapper
public interface BrandDao extends BaseMapper<BrandEntity> {
	
}
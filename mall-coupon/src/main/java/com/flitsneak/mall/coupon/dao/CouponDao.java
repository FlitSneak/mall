package com.flitsneak.mall.coupon.dao;

import com.flitsneak.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 09:08:49
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}

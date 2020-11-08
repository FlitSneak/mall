package com.flitsneak.mall.order.dao;

import com.flitsneak.mall.order.entity.OmsOrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 09:20:06
 */
@Mapper
public interface OmsOrderDao extends BaseMapper<OmsOrderEntity> {
	
}

package com.flitsneak.mall.order.dao;

import com.flitsneak.mall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 09:27:09
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}

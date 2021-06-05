package com.flitsneak.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flitsneak.common.utils.PageUtils;
import com.flitsneak.mall.coupon.entity.CouponHistoryEntity;

import java.util.Map;

/**
 * 优惠券领取历史记录
 *
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2021-04-25 23:20:21
 */
public interface CouponHistoryService extends IService<CouponHistoryEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


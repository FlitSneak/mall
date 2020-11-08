package com.flitsneak.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flitsneak.common.utils.PageUtils;
import com.flitsneak.mall.product.entity.ProductAttrValueEntity;

import java.util.Map;

/**
 * spu属性值
 *
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 07:25:56
 */
public interface ProductAttrValueService extends IService<ProductAttrValueEntity> {

    PageUtils queryPage(Map<String, Object> params);
}


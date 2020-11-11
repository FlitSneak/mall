package com.flitsneak.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.flitsneak.common.utils.PageUtils;
import com.flitsneak.mall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 07:25:56
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);

}


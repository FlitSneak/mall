package com.flitsneak.mall.order.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flitsneak.mall.order.entity.OmsOrderEntity;
import com.flitsneak.mall.order.service.OmsOrderService;
import com.flitsneak.common.utils.PageUtils;
import com.flitsneak.common.utils.R;



/**
 * 订单
 *
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 09:20:06
 */
@RestController
@RequestMapping("order/omsorder")
public class OmsOrderController {
    @Autowired
    private OmsOrderService omsOrderService;

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = omsOrderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		OmsOrderEntity omsOrder = omsOrderService.getById(id);

        return R.ok().put("omsOrder", omsOrder);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody OmsOrderEntity omsOrder){
		omsOrderService.save(omsOrder);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody OmsOrderEntity omsOrder){
		omsOrderService.updateById(omsOrder);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		omsOrderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}

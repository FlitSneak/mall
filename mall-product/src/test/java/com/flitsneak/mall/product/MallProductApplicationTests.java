package com.flitsneak.mall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flitsneak.mall.product.entity.BrandEntity;
import com.flitsneak.mall.product.service.BrandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.EnableMBeanExport;

import java.sql.Wrapper;

@SpringBootTest
class MallProductApplicationTests {
    @Autowired
    BrandService brandService;

    @Test
    void contextLoads() {
        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("飞飞");
        //brandService.save(brandEntity);
        brandService.remove(new QueryWrapper<BrandEntity>().eq("name","飞飞"));
    }

}

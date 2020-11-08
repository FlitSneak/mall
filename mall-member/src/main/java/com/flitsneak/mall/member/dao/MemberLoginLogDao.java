package com.flitsneak.mall.member.dao;

import com.flitsneak.mall.member.entity.MemberLoginLogEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员登录记录
 * 
 * @author flitsneak
 * @email flitsneak@gmail.com
 * @date 2020-11-08 09:15:00
 */
@Mapper
public interface MemberLoginLogDao extends BaseMapper<MemberLoginLogEntity> {
	
}
# 谷粒商城gulimall

#### 介绍
此为尚硅谷开放的一个微服务项目。总体来说这个项目比较完备，涉及到了分布式事务，集群部署以及运维的流行技术K8S和Jenkins。

总体设计来看，服务注册和配置中心为SpringAlibaba Nacos，从功能和广度还有便捷度来看都要比Eureka要强的多，网关为SpringCloud Gateway zuul已经去世，zuul2还在挂机。Ribbon实现负载均衡，同时Sentinel实现服务的熔断降级这里取代了Hystrix豪猪哥。请求先行到SpringSecurit和OAuth2.0进行认证，认证实现第三方登录和单点登录，涉及到远程服务调用，远程服务调用框架为OpenFeign。集群的日志收集过滤全文搜索引擎为ELK，ElasticSearch+LogStash+Kibana.服务追踪可视化为Sleuth+Zipkin.对象存储采用阿里云OSS，数据库持久层为Mysql，缓存为redis集群。消息队列采用RabbitMQ。后端运维采用K8S+Jenkins实现CI/CD。

总体划分服务为认证服务+商品微服务+用户服务+存储服务+购物车服务+订单服务+支付服务+优惠服务+秒杀服务+全文检索服务。

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request

### 谷粒商城简介

此为尚硅谷开源的一个微服务项目。这个项目比较完备，涉及到了分布式事务，集群部署以及运维的流行技术K8S和Jenkins。

总体设计来看，服务注册和配置中心为SpringAlibaba Nacos，从功能和广度还有便捷度来看都要比Eureka要强的多，网关为SpringCloud Gateway zuul已经去世，spring官方并没有集成zuul2。Ribbon实现负载均衡，同时Sentinel实现服务的熔断降级这里取代了Hystrix豪猪哥。请求先行到SpringSecurity和OAuth2.0进行认证，认证实现第三方登录和单点登录，涉及到远程服务调用，远程服务调用框架为OpenFeign。集群的日志收集过滤全文搜索引擎为ELK，ElasticSearch+LogStash+Kibana.服务追踪可视化为Sleuth+Zipkin.对象存储采用阿里云OSS，数据库持久层为Mysql，缓存为redis集群。消息队列采用RabbitMQ。后端运维采用K8S+Jenkins实现CI/CD。

总体划分服务为认证服务+商品微服务+用户服务+存储服务+购物车服务+订单服务+支付服务+优惠服务+秒杀服务+全文检索服务。

对于本项目，有些地方的处理方法我并不赞同，后续会进行改造。如果有时间我会全部改成基于netty开发的webflux响应式。还有 本项目我并没有按照视频来做，一般来说视频100小时很难看完，而且大部分知识都比较熟悉，所以应该是基于大体需求自己做了实现。

### 环境准备

起步环境需要安装虚拟机并在虚拟机上安装docker并部署mysql、Redis等docker容器。课程推荐使用VirtualBox，其实VMware也一样。但是不建议用阿里云或者腾讯云，因为前期虽然可以运行但是到了后面ES集群还有redis集群等等至少需要6G运行内存，大家普遍购买的是一核2G运存，即使限制了docker的内存分配，再开swap交换内存也满足不了后续需求，当然你有钱买一台8G运存的阿里云当然最好。

#### 1、虚拟机安装

确保win10系统

​            VirtualBox下载：https://www.virtualbox.org/

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201106235929.png)

推荐通过Vagrant安装虚拟机

​            Vagrant下载：https://www.vagrantup.com/downloads.html

​            Vagrant镜像仓库：https://app.vagrantup.com/boxes/search

简单来说先下载Vagrant，再从仓库拉取centOS7镜像。       



安装完成后cmd输入vagrant有提示：     

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107001728.png)

然后可以从官网镜像仓库拉取镜像，本项目拉取镜像名称为centos/7

下图为初始化一个centos7实例虚拟机并且启动该虚拟机：

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107003106.png)

虚拟机启动后会自动创建一个**vagrant**帐户，该账户拥有sudo权限，系统用户为root：

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107005607.png)

#### 2、网络配置

网络配置是为了适配主机和虚拟机之间的通信。也就是调整本地主机的VirtualBox网段和虚拟机的网段一致，VMware下安装的主机需要虚拟机设置静态Ip然后保持和主机的vmnet8的网段一致。

虚拟机的网络修改在用户目录下的Vagrantfile，例如我的主机是C:\Users\FlitSneak\Vagrantfile

配置修改如下：

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107011440.png)

当然你要先知道本地主机的VirtualBox网桥的网段：

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107011700.png)

实际就是做一个网络适配，使得电脑和虚拟机在同一个网段之下。

![image-20210605202337644](https://gitee.com/flitsneak/mall/raw/develop/picture\image-20210605202337644.png)

#### 3、安装docker

必须要用docker安装，docker是云原生的基础 ，在商城的集群篇就涉及到K8S如何编排容器实现服务上云弹性伸缩.

首先下载docker：

参考docker在centos下安装的文档：

​         https://docs.docker.com/engine/install/centos/

```shell
#1、卸载旧版本
sudo yum remove docker \
                  docker-client \
                  docker-client-latest \
                  docker-common \
                  docker-latest \
                  docker-latest-logrotate \
                  docker-logrotate \
                  docker-engine
#2、安装仓库                  
sudo yum install -y yum-utils

sudo yum-config-manager \
  --add-repo \
  https://download.docker.com/linux/centos/docker-ce.repo
#3、安装docker工具
sudo yum install docker-ce docker-ce-cli containerd.io
#4、启动docker
sudo systemctl start docker
#5、设置开机自启动
sudo systemctl enable docker
```

docker安装成功后可以看到版本号：

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107020817.png)



#### 4、docker安装mysql

数据库都是放在虚拟机中的，方便扩展维护。所以采用docker安装mysql并挂载相应配置文件到虚拟机mydata文件夹中方便对mysql配置的修改和维护。没有加sudo说明是在root用户下操作的，可以用su进行切换密码也是vagrant。

```shell
#1、拉取mysql镜像
docker pull mysql:5.7
#2、如果拉取成功可以通过如下指令看到镜像名称
docker images
#3、实例化mysql容器并进行相应的端口映射以及文件挂载
sudo docker run -p 3306:3306 --name mysql \
-v /mydata/mysql/log:/var/log/mysql \
-v /mydata/mysql/data:/var/lib/mysql \
-v /mydata/mysql/conf:/etc/mysql \
-e MYSQL_ROOT_PASSWORD=root \
-d mysql:5.7
#4、查看活跃容器
docker ps
```

安装好后需要对mysql进行相关配置比如修改字符编码

配置文件我们已经挂载到了/mydata/mysql/conf,只需在这个conf文件下新建my.cnf即可完成对容器中mysql的配置。

```bash
[client]
default-character-set=utf8
[mysql]
default-character-set=utf8
[mysqld]
init_connect='SET collation_connection = utf8_unicode_ci'
init_connect='SET NAMES utf8'
character-set-server=utf8
collation-server=utf8_unicode_ci
skip-character-set-client-handshake
skip-name-resolve
```

习惯vim指令的需要安装vim编辑器：

```shell
sudo yum -y install vim*
```

是否挂载成功可以进入容器内部进行验证：

```shell
#交互模式进入容器内部
docker exec -it mysql /bin/bash
```

在/etc/mysql下发现my.cnf

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107031553.png)

设置mysql自启动：

```shell
docker update mysql --restart=always
```

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107023809.png)

Navicat连接测试（关于软件的破解建议打开微信的文章进行搜索）

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201107024948.png)

#### 5、docker安装redis

如法炮制

```shell
#1、拉取，不加版本默认最新版。
docker pull redis
#2、实例化挂载启动之前最好先生成挂载文件避免文件和文件名混乱
mkdir -p /mydata/redis/conf
touch /mydata/redis/conf/redis.conf
#3、redis开启持久化存储，也可在redis.conf中手动添加appendonly yes
echo "appendonly yes"  >> /mydata/redis/conf/redis.conf
#4、实例化映射端口并挂载
docker run -p 6379:6379 --name redis -v /mydata/redis/data:/data \
-v/mydata/redis/conf/redis.conf:/etc/redis/redis.conf \
-d redis redis-server /etc/redis/redis.conf
#5、设置自启动
docker update redis --restart=always
```

启动redis客户端命令：

```shell
docker exec -it redis redis-cli
```

#### 6、前端环境准备

安装nodejs，这是前后端分离的必须，nodejs相当于后端的容器。

官网下载：https://nodejs.org/en/

安装步骤跳过，因为这玩意多年前我就装好了。

### 项目准备

当在gitee创建develop和master双分支仓库后需要设置develop为**默认仓库**，否则idea提交默认仍然是master分支。为了方便我更喜欢**本地仓库一次提交到GitHub和gitee两个仓库**，实现这种方式非常容易只需要打开项目目录隐藏的.git文件夹，修改配置如下：

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201109053408.png)

我们要快速的初始化一个项目，所以要借助人人开源来搭建后台可视化界面以及逆向生成基础的CRUD操作。

现在是20年11月，人人开源的的逆向工程代码生成器有改动，和视频里的内容有很多不同，直接莽就完了，有问题解决问题。

使用非常简单，只需要修改关联的数据库，修改包名等等，直接跳过。

不过要注意的一点逆向生产ware表时只要勾选前四个表生产即可否则会报错。

还有当你没有用脚手架搭建common工程时默认创建的工程会使用1.5的编译环境会导致Language level不兼容，这时候需要改common的pom文件加上：

```pro
 <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
```

然后我发现这种方式非常low，所以整个项目都使用阿里的jdk,dragonwell 11,common工程也直接使用脚手架搭建。

#### 1、整合mybatis-plus

1、公共模块引入依赖：

```properties
<dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.3.1</version>
        </dependency>
<!--    导入mysql驱动    -->
<dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.17</version>
        </dependency>
```

2、配置数据源这是每一个涉及数据库操作的微服务必须。

```yaml
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://192.168.56.10:3306/gulimall_sms
    driver-class-name: com.mysql.cj.jdbc.Driver
```

3、配置MyBatis-Plus的包扫描（mp高版本应该不需要配置了）

@MapperScan("dao文件夹的全路径")

```yaml
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto#主键自增
```

cj是针对mysql驱动6.0以上版本的。

#### 2、配置nacos注册中心与配置中心

nacos下载地址：

​     https://github.com/alibaba/nacos/releases/tag/1.4.0

可以选择linux下安装或者下载.zip在win10上使用。如果要在win10使用需要做一些操作：

1、需要在mysql新建一个nacos数据库

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201109055943.png)

执行该sql脚本：

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201109060132.png)

2、修改配置，数据库帐号密码为你本地帐号密码

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201109060430.png)

3、修改bin目录下的启动脚本改成单例

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201109060624.png)

然后nacos就可以通过startup.cmd启动了



项目中引入nacos：

​         中文文档：https://github.com/alibaba/spring-cloud-alibaba/blob/master/README-zh.md

公共模块引入配置：

```properties
<!--        nacos注册中心     -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
<!--nacos配置中心-->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        
        
<dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>2.2.3.RELEASE</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

各个微服务模块引入注册中心配置：

```yaml
spring:
  datasource:
    username: root
    password: root
    url: jdbc:mysql://192.168.56.10:3306/gulimall_sms
    driver-class-name: com.mysql.cj.jdbc.Driver
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
```

配置中心需要引入bootstrap.properties

```properties
spring.application.name=mall-coupon

spring.cloud.nacos.config.server-addr=127.0.0.1:8848
```

文件的命名规则为：${spring.application.name}-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}

${spring.application.name}：为微服务名

${spring.profiles.active}：指明是哪种环境下的配置，如dev、test或info

${spring.cloud.nacos.config.file-extension}：配置文件的扩展名，可以为properties、yml等

配置规则的玩法很多，可以指定不同的命名空间不同的组，还可以分割成多个配置文件的数组。

#### 3、引入OpenFeign

公共模块引入依赖：

```properties
<dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
```

这玩意脚手架创建时勾选就可以了。

使用远程调用需要开启注解

@EnableFeignClients（basePackages = {"",""}）

使用远程调用需要本服务创建一个接口：

```java
@FeignClient("mall_coupon")
public interface CouponFeignService {
    @RequestMapping("/coupon/coupon/member/list")
    public R memberCoupons();
}
```

例如这个是优惠券服务，member服务需要远程调用coupon服务就需要本地建立一个接口，加上注解，注解内为coupon服务名，该CouponFeignService可以注入到member中用来调用/coupon/coupon/member/list的资源。

#### 4、Gateway网关

新建moudle挂载mall下，新建时勾选上Gateway即可。Gateway一样需要注册到nacos注册中心和配置中心。

Gateway的核心是predict和route，断言是四个函数式接口的一个，Gateway有很多断言规则。

记得网关也引入了common依赖，**common中有mysql驱动要加载数据库但是网关没有配置数据源所以会报错**，因此要加@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})

至此基础准备完毕。

### 前端开发准备

要大概了解ES6语法，vue组件化开发以及整合ElementUI开发。

vue模块化开发要安装webpack，这是一个打包管理工具，就是把前端的各种复杂语法及框架如vue，sass，ts等输出为简单的css+js+html。

首先全局安装webpack：

```shell
npm install webpack -g
```

然后安装vue脚手架可以快速帮助我们搭建vue框架：

```shell
npm install -g @vue/cli-init
```

安装完成之后就可以快速初始化一个vue项目：

vue init webpack appname

想要开发vue项目起码要知道路由和组件。

无论是vscode还是hbuilderX都可以创建vue模版：

```vue
{
    "Print to console": {
        "prefix": "vue",
        "body": [
            "<!-- $1 -->",
            "<template>",
            "<div class='$2'>$5</div>",
            "</template>",
            "",
            "<script>",
            "//这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）",
            "//例如：import 《组件名称》 from '《组件路径》';",
            "",
            "export default {",
            "//import引入的组件需要注入到对象中才能使用",
            "components: {},",
            "data() {",
            "//这里存放数据",
            "return {",
            "",
            "};",
            "},",
            "//监听属性 类似于data概念",
            "computed: {},",
            "//监控data中的数据变化",
            "watch: {},",
            "//方法集合",
            "methods: {",
            "",
            "},",
            "//生命周期 - 创建完成（可以访问当前this实例）",
            "created() {",
            "",
            "},",
            "//生命周期 - 挂载完成（可以访问DOM元素）",
            "mounted() {",
            "",
            "},",
            "beforeCreate() {}, //生命周期 - 创建之前",
            "beforeMount() {}, //生命周期 - 挂载之前",
            "beforeUpdate() {}, //生命周期 - 更新之前",
            "updated() {}, //生命周期 - 更新之后",
            "beforeDestroy() {}, //生命周期 - 销毁之前",
            "destroyed() {}, //生命周期 - 销毁完成",
            "activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发",
            "}",
            "</script>",
            "<style scoped>",
            "//@import url($3); 引入公共css类",
            "$4",
            "</style>"
        ],
        "description": "生成vue模板"
    },
    "http-get请求": {
	"prefix": "httpget",
	"body": [
		"this.\\$http({",
		"url: this.\\$http.adornUrl(''),",
		"method: 'get',",
		"params: this.\\$http.adornParams({})",
		"}).then(({ data }) => {",
		"})"
	],
	"description": "httpGET请求"
    },
    "http-post请求": {
	"prefix": "httppost",
	"body": [
		"this.\\$http({",
		"url: this.\\$http.adornUrl(''),",
		"method: 'post',",
		"data: this.\\$http.adornData(data, false)",
		"}).then(({ data }) => { });" 
	],
	"description": "httpPOST请求"
    }
}
```

输入vue、httpget或者httppost都会有模版提示。

### 前后端联调

#### 1、实现递归查询出三级分类商品信息

查询父分类及子分类以及子分类的子分类。本质上返回的是一个树形结构，当然可以用树来实现，本文封装为链表对象。

首先要在实体类定义其子类链表集合，但此集合并不在数据库中而是一种逻辑标识所以要加上@Table(exist = false)

```java
@TableField(exist = false)
	private List<CategoryEntity> children;
```

对于一对多的查询采用java8新特性stream流：

```java
public List<CategoryEntity> listWithTree() {
        //1、查询所有分类
       List<CategoryEntity> entities = baseMapper.selectList(null);

       //2、组装成父子的树形结构
        //2、1 找到所有的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu)->{//每一个一级分类都映射到map中。
            menu.setChildren(getChildren(menu,entities));//一级分类追加list<Category>
            return menu;
        }).sorted((menu1,menu2)->{
            return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    private List<CategoryEntity> getChildren(CategoryEntity parent,List<CategoryEntity> entities){

        List<CategoryEntity> children = entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid()==parent.getCatId())
                .map((menu)->{
                    menu.setChildren(getChildren(menu,entities));
                    return menu;
                }).sorted((menu1,menu2)->{
                    return (menu1.getSort()==null?0:menu1.getSort()) - (menu2.getSort()==null?0:menu2.getSort());
                        }).collect(Collectors.toList());


                return children;
    }
```

回到前端renren-fast-vue需要对product/category.vue进行组件化开发目的即在当前页展示三级分类的数据。

使用ElementUI的组件很容易开发三级分类页面展示。

#### 2、配置前后端路由

现在需要对请求路径进行相应的改变，现在的请求并不会发到后端。

首先设置baseurl，使得每次请求都定位到后端网关Gateway（加api是为了更好区分请求来源）。

![](https://gitee.com/flitsneak/mall/raw/develop/picture\QQ拼音截图20201111112336.png)

这样每次请求都会跳转到网关，让网关去请求后端renren-fast。因此后端renren-fast 也要注册到nacos，这样Gateway才可以从nacos获取请求地址转发请求到renren-fast。

根据需求，在renren-fast配置文件中配置服务名称，配置nacos注册中心地址，在Application配置@EnableDiscoverClient。

然后需要配置网关Gateway，使得前端请求都路由到renren-fast：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    gateway:
      routes:
        - id: admin_route
          uri: lb://renren-fast
          predicates:
            - Path=/api/**
##前端项目的请求都以api开头。
```

到此为止，发现前端验证码不显示，分析如下：

前端发送请求验证码的地址实际为：http://localhost:88/api/captcha.jpg 经过网关路由后的地址为: http://renren-fast:8080/captcha.jpg,而真正能请求到验证码的网址为:http://localhost:8080/renren-fast/captcha.jpg

为了满足需求，需要在网关的配置文件中加上filters，参考文档6.16

https://docs.spring.io/spring-cloud-gateway/docs/2.2.5.RELEASE/reference/html/#the-rewritepath-gatewayfilter-factory

改写如下：

```yaml
- id: admin_route
          uri: lb://renren-fast
          predicates:
            - Path=/api/**
          filters:
            - RewritePath=/api/(?<segment>/?.*), /renren-fast/$\{segment}
```

这样跳转请求路径的问题就解决了，接下来解决跨域问题。

#### 3、跨域问题

跨域问题采用网关的全局路由统一处理。

新建MallCorsConfiguration配置类：

```java
@Configuration
public class MallCorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.setAllowCredentials(true);

        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }
}
```

要注意包都是reactive下的，这属于webflux核心,即响应式开发。

测试发现多次跨越请求携带多值，是因为renren-fast也配置了跨域请求。**这是个坑**，说明统一跨域配置后不能再单独注解配置会产生冲突，要清理掉renren-fast的跨域配置。

#### 4、商品请求路由

同样处理使前端对商品的请求转发到商品微服务：

```java
- id: mall-product
          uri: lb://mall-product
          predicates:
            - Path=/api/product/**
          filters:
            - RewritePath=/api/(?<segment>/?.*), /$\{segment}
```

注意这个id要放在上个id前面，**路由配置读取有顺序**。

商品服务要检查有没有配置好nacos配置中心和注册中心。

#### 5、逻辑删除

需要对前端商品分类进行删改，删除采用逻辑删除，参考mybatis-plus的文档：https://baomidou.com/guide/logic-delete.html，mybatisplus的版本不同很多地方的细节也不同，注意文档配置信息。

标志位为数据库的show_status。

```yaml
mybatis-plus:
  mapper-locations: classpath:/mapper/**/*.xml
  global-config:
    db-config:
      id-type: auto
      logic-delete-value: 1
      logic-not-delete-value: 0
```

给Bean加上逻辑删除注解@TableLogic

```java
@TableLogic(value = "1",delval = "0")
	private Integer showStatus;
```


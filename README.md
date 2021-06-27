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

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201106235929.png)

推荐通过Vagrant安装虚拟机

​            Vagrant下载：https://www.vagrantup.com/downloads.html

​            Vagrant镜像仓库：https://app.vagrantup.com/boxes/search

简单来说先下载Vagrant，再从仓库拉取centOS7镜像。       



安装完成后cmd输入vagrant有提示：     

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107001728.png)

然后可以从官网镜像仓库拉取镜像，本项目拉取镜像名称为centos/7

下图为初始化一个centos7实例虚拟机并且启动该虚拟机：

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107003106.png)

虚拟机启动后会自动创建一个**vagrant**帐户，该账户拥有sudo权限，系统用户为root：

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107005607.png)

#### 2、网络配置

网络配置是为了适配主机和虚拟机之间的通信。也就是调整本地主机的VirtualBox网段和虚拟机的网段一致，VMware下安装的主机需要虚拟机设置静态Ip然后保持和主机的vmnet8的网段一致。

虚拟机的网络修改在用户目录下的Vagrantfile，例如我的主机是C:\Users\FlitSneak\Vagrantfile

配置修改如下：

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107011440.png)

当然你要先知道本地主机的VirtualBox网桥的网段：

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107011700.png)

实际就是做一个网络适配，使得电脑和虚拟机在同一个网段之下。

![image-20210605202337644](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210605202337644.png)

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

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107020817.png)



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

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107031553.png)

设置mysql自启动：

```shell
docker update mysql --restart=always
```

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107023809.png)

Navicat连接测试（关于软件的破解建议打开微信的文章进行搜索）

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201107024948.png)

接下来就是建库，导表，官网公开的课件里都是没有数据的，有数据的我之后会附带上。

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

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201109053408.png)

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

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201109055943.png)

执行该sql脚本：

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201109060132.png)

2、修改配置，数据库帐号密码为你本地帐号密码

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201109060430.png)

3、修改bin目录下的启动脚本改成单例

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201109060624.png)

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

![](https://gitee.com/flitsneak/mall/raw/develop/picture/QQ拼音截图20201111112336.png)

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

### vue2开发详解

采用renren-fast开发，我们遵循相应的规范即可。

首先运行后台的renren-fast，mall-product,mall-gateway。然后前端运行renren-fast-vue。

前端开发工具打开项目，执行命令：

``` sh
npm run dev
```

浏览器即可看到运行的界面，我们要对接后端的商品系统，所以要在菜单管理界面新建商品分类，这次主要做的是商品分类维护，所以在商品系统下再创建分类维护。

![image-20210621233304560](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210621233304560.png)

对应映射的前端目录结构为：

![image-20210621233435727](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210621233435727.png)

剩下的工作都是基于category.vue这个页面开发。

要说明一点，分类维护的操作还是比较复杂的比如保持展开和拖拽，但是很系统的能帮助学习vue，elementui和别的ui一样主要就是对树和数据表格的操作，其余的也没什么了。

我们要做的无非是增删查改四种功能，其中改最为麻烦，要支持拖拽等。效果图如下：

![image-20210621234051348](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210621234051348.png)

整个category.vue 的代码如下：

``` html
<template>
  <div>
    <el-switch v-model="draggable" active-text="开启拖拽" inactive-text="关闭拖拽"></el-switch>
    <el-button v-if="draggable" @click="batchSave">批量保存</el-button>
    <el-button type="danger" @click="batchDelete">批量删除</el-button>
    <el-tree
      :data="menus"
      :props="defaultProps"
      :expand-on-click-node="false"
      show-checkbox
      node-key="catId"
      :default-expanded-keys="expandedKey"
      :draggable="draggable"
      :allow-drop="allowDrop"
      @node-drop="handleDrop"
      ref="menuTree"
    >
      <span class="custom-tree-node" slot-scope="{ node, data }">
        <span>{{ node.label }}</span>
        <span>
          <el-button
            v-if="node.level <=2"
            type="text"
            size="mini"
            @click="() => append(data)"
          >Append</el-button>
          <el-button type="text" size="mini" @click="edit(data)">edit</el-button>
          <el-button
            v-if="node.childNodes.length==0"
            type="text"
            size="mini"
            @click="() => remove(node, data)"
          >Delete</el-button>
        </span>
      </span>
    </el-tree>

    <el-dialog
      :title="title"
      :visible.sync="dialogVisible"
      width="30%"
      :close-on-click-modal="false"
    >
      <el-form :model="category">
        <el-form-item label="分类名称">
          <el-input v-model="category.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="category.icon" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="category.productUnit" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitData">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
//这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
//例如：import 《组件名称》 from '《组件路径》';

export default {
  //import引入的组件需要注入到对象中才能使用
  components: {},
  props: {},
  data() {
    return {
      pCid: [],
      draggable: false,
      updateNodes: [],
      maxLevel: 0,
      title: "",
      dialogType: "", //edit,add
      category: {
        name: "",
        parentCid: 0,
        catLevel: 0,
        showStatus: 1,
        sort: 0,
        productUnit: "",
        icon: "",
        catId: null
      },
      dialogVisible: false,
      menus: [],
      expandedKey: [],
      defaultProps: {
        children: "children",
        label: "name"
      }
    };
  },

  //计算属性 类似于data概念
  computed: {},
  //监控data中的数据变化
  watch: {},
  //方法集合
  methods: {
    getMenus() {
      this.$http({
        url: this.$http.adornUrl("/product/category/list/tree"),
        method: "get"
      }).then(({data}) => {
        console.log("成功获取到菜单数据...", data.data);
        this.menus = data.data;
      });
    },
    batchDelete() {
      let catIds = [];
      let checkedNodes = this.$refs.menuTree.getCheckedNodes();
      console.log("被选中的元素", checkedNodes);
      for (let i = 0; i < checkedNodes.length; i++) {
        catIds.push(checkedNodes[i].catId);
      }
      this.$confirm(`是否批量删除【${catIds}】菜单?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(catIds, false)
          }).then(({data}) => {
            this.$message({
              message: "菜单批量删除成功",
              type: "success"
            });
            this.getMenus();
          });
        })
        .catch(() => {
        });
    },
    batchSave() {
      this.$http({
        url: this.$http.adornUrl("/product/category/update/sort"),
        method: "post",
        data: this.$http.adornData(this.updateNodes, false)
      }).then(({data}) => {
        this.$message({
          message: "菜单顺序等修改成功",
          type: "success"
        });
        //刷新出新的菜单
        this.getMenus();
        //设置需要默认展开的菜单
        this.expandedKey = this.pCid;
        this.updateNodes = [];
        this.maxLevel = 0;
        // this.pCid = 0;
      });
    },
    handleDrop(draggingNode, dropNode, dropType, ev) {
      console.log("handleDrop: ", draggingNode, dropNode, dropType);
      //1、当前节点最新的父节点id
      let pCid = 0;
      let siblings = null;
      if (dropType == "before" || dropType == "after") {
        pCid =
          dropNode.parent.data.catId == undefined
            ? 0
            : dropNode.parent.data.catId;
        siblings = dropNode.parent.childNodes;
      } else {
        pCid = dropNode.data.catId;
        siblings = dropNode.childNodes;
      }
      this.pCid.push(pCid);

      //2、当前拖拽节点的最新顺序，
      for (let i = 0; i < siblings.length; i++) {
        if (siblings[i].data.catId == draggingNode.data.catId) {
          //如果遍历的是当前正在拖拽的节点
          let catLevel = draggingNode.level;
          if (siblings[i].level != draggingNode.level) {
            //当前节点的层级发生变化
            catLevel = siblings[i].level;
            //修改他子节点的层级
            this.updateChildNodeLevel(siblings[i]);
          }
          this.updateNodes.push({
            catId: siblings[i].data.catId,
            sort: i,
            parentCid: pCid,
            catLevel: catLevel
          });
        } else {
          this.updateNodes.push({catId: siblings[i].data.catId, sort: i});
        }
      }

      //3、当前拖拽节点的最新层级
      console.log("updateNodes", this.updateNodes);
    },
    updateChildNodeLevel(node) {
      if (node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          var cNode = node.childNodes[i].data;
          this.updateNodes.push({
            catId: cNode.catId,
            catLevel: node.childNodes[i].level
          });
          this.updateChildNodeLevel(node.childNodes[i]);
        }
      }
    },
    allowDrop(draggingNode, dropNode, type) {
      //1、被拖动的当前节点以及所在的父节点总层数不能大于3

      //1）、被拖动的当前节点总层数
      console.log("allowDrop:", draggingNode, dropNode, type);
      //
      this.countNodeLevel(draggingNode);
      //当前正在拖动的节点+父节点所在的深度不大于3即可
      let deep = Math.abs(this.maxLevel - draggingNode.level) + 1;
      console.log("深度：", deep);

      //   this.maxLevel
      if (type == "inner") {
        // console.log(
        //   `this.maxLevel：${this.maxLevel}；draggingNode.data.catLevel：${draggingNode.data.catLevel}；dropNode.level：${dropNode.level}`
        // );
        return deep + dropNode.level <= 3;
      } else {
        return deep + dropNode.parent.level <= 3;
      }
    },
    countNodeLevel(node) {
      //找到所有子节点，求出最大深度
      if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          if (node.childNodes[i].level > this.maxLevel) {
            this.maxLevel = node.childNodes[i].level;
          }
          this.countNodeLevel(node.childNodes[i]);
        }
      }
    },
    edit(data) {
      console.log("要修改的数据", data);
      this.dialogType = "edit";
      this.title = "修改分类";
      this.dialogVisible = true;

      //发送请求获取当前节点最新的数据
      this.$http({
        url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
        method: "get"
      }).then(({data}) => {
        //请求成功
        console.log("要回显的数据", data);
        this.category.name = data.data.name;
        this.category.catId = data.data.catId;
        this.category.icon = data.data.icon;
        this.category.productUnit = data.data.productUnit;
        this.category.parentCid = data.data.parentCid;
        this.category.catLevel = data.data.catLevel;
        this.category.sort = data.data.sort;
        this.category.showStatus = data.data.showStatus;
        /**
         *         parentCid: 0,
         catLevel: 0,
         showStatus: 1,
         sort: 0,
         */
      });
    },
    append(data) {
      console.log("append", data);
      this.dialogType = "add";
      this.title = "添加分类";
      this.dialogVisible = true;
      this.category.parentCid = data.catId;
      this.category.catLevel = data.catLevel * 1 + 1;
      this.category.catId = null;
      this.category.name = "";
      this.category.icon = "";
      this.category.productUnit = "";
      this.category.sort = 0;
      this.category.showStatus = 1;
    },

    submitData() {
      if (this.dialogType == "add") {
        this.addCategory();
      }
      if (this.dialogType == "edit") {
        this.editCategory();
      }
    },
    //修改三级分类数据
    editCategory() {
      var {catId, name, icon, productUnit} = this.category;
      this.$http({
        url: this.$http.adornUrl("/product/category/update"),
        method: "post",
        data: this.$http.adornData({catId, name, icon, productUnit}, false)
      }).then(({data}) => {
        this.$message({
          message: "菜单修改成功",
          type: "success"
        });
        //关闭对话框
        this.dialogVisible = false;
        //刷新出新的菜单
        this.getMenus();
        //设置需要默认展开的菜单
        this.expandedKey = [this.category.parentCid];
      });
    },
    //添加三级分类
    addCategory() {
      console.log("提交的三级分类数据", this.category);
      this.$http({
        url: this.$http.adornUrl("/product/category/save"),
        method: "post",
        data: this.$http.adornData(this.category, false)
      }).then(({data}) => {
        this.$message({
          message: "菜单保存成功",
          type: "success"
        });
        //关闭对话框
        this.dialogVisible = false;
        //刷新出新的菜单
        this.getMenus();
        //设置需要默认展开的菜单
        this.expandedKey = [this.category.parentCid];
      });
    },

    remove(node, data) {
      var ids = [data.catId];
      this.$confirm(`是否删除【${data.name}】菜单?`, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      })
        .then(() => {
          this.$http({
            url: this.$http.adornUrl("/product/category/delete"),
            method: "post",
            data: this.$http.adornData(ids, false)
          }).then(({data}) => {
            this.$message({
              message: "菜单删除成功",
              type: "success"
            });
            //刷新出新的菜单
            this.getMenus();
            //设置需要默认展开的菜单
            this.expandedKey = [node.parent.data.catId];
          });
        })
        .catch(() => {
        });

      console.log("remove", node, data);
    }
  },
  //生命周期 - 创建完成（可以访问当前this实例）
  created() {
    this.getMenus();
  },
  //生命周期 - 挂载完成（可以访问DOM元素）
  mounted() {
  },
  beforeCreate() {
  }, //生命周期 - 创建之前
  beforeMount() {
  }, //生命周期 - 挂载之前
  beforeUpdate() {
  }, //生命周期 - 更新之前
  updated() {
  }, //生命周期 - 更新之后
  beforeDestroy() {
  }, //生命周期 - 销毁之前
  destroyed() {
  }, //生命周期 - 销毁完成
  activated() {
  } //如果页面有keep-alive缓存功能，这个函数会触发
};
</script>
<style scoped>
</style>
```



#### 1、查询

首先，在页面导入我们创建好的vue模板：

```html
<!--  -->
<template>
<div class=''></div>
</template>

<script>
//这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
//例如：import 《组件名称》 from '《组件路径》';

export default {
//import引入的组件需要注入到对象中才能使用
components: {},
data() {
//这里存放数据
return {

};
},
//监听属性 类似于data概念
computed: {},
//监控data中的数据变化
watch: {},
//方法集合
methods: {

},
//生命周期 - 创建完成（可以访问当前this实例）
created() {

},
//生命周期 - 挂载完成（可以访问DOM元素）
mounted() {

},
beforeCreate() {}, //生命周期 - 创建之前
beforeMount() {}, //生命周期 - 挂载之前
beforeUpdate() {}, //生命周期 - 更新之前
updated() {}, //生命周期 - 更新之后
beforeDestroy() {}, //生命周期 - 销毁之前
destroyed() {}, //生命周期 - 销毁完成
activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
}
</script>
<style scoped>

</style>
```

要树形展示数据首先要有一棵树，我们去elementui的官网，找到树形组件：

![image-20210622002024335](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622002024335.png)

直接复制作用域插槽的相关代码到template标签即可，可以看到双向绑定了一个:data="data",我们改成menus，并把menus挂载到data()中，还要注意的一点，我们请求后台返回的数据要对应解析，label对应解析为name。

看一下具体改动：

![image-20210622030506136](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622030506136.png)

![image-20210622023903876](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622023903876.png)

![image-20210622030649267](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622030649267.png)

这样查询就没问题了：

![image-20210622030728270](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622030728270.png)

对于作用域插槽，再优化一下，即只有不是子类才能append，只有子类才能delete，对应语句如下：

![image-20210622042920632](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622042920632.png)

#### 2、删除

在以上操作中我们要加入删除操作。删除是件严肃的操作所以要有二次确认和成功通知，删除后要重新刷新菜单，最好要保持删除节点的父节点展开。

二次确认和通知直接复制elementui的messagebox和message组件即可。

做删除操作需要指定数据的唯一id是什么，对应属性为：

```vue
node-key="catId"
```

此外保持展开需要双向绑定属性：

```vue
:default-expanded-keys="expandedKey"
```

挂载到data即可：

![image-20210622043056903](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622043056903.png)

其余即发送post请求，将需要删除的catId数组传到后台。

``` vue
remove(node, data) {
        var ids = [data.catId];
        this.$confirm(`是否删除【${data.name}】菜单?`, "提示", {
            confirmButtonText: "确定",
            cancelButtonText: "取消",
            type: "warning"
          })
          .then(() => {
            this.$http({
              url: this.$http.adornUrl("/product/category/delete"),
              method: "post",
              data: this.$http.adornData(ids, false)
            }).then(({
              data
            }) => {
              this.$message({
                message: "菜单删除成功",
                type: "success"
              });
              //刷新出新的菜单
              this.getMenus();
              //设置需要默认展开的菜单
              this.expandedKey = [node.parent.data.catId];
            });
          })
          .catch(() => {});

        console.log("remove", node, data);

      }
```

到此删除即完成。

#### 3、增加

增加是通过嵌套表单的dialog来实现的，关键是获取当前添加分类的层级，相对简单。

首先引入dialog组件：

``` html
<el-dialog :visible.sync="dialogVisible" width="30%" :close-on-click-modal="false">
      <el-form :model="category">
        <el-form-item label="分类名称">
          <el-input v-model="category.name" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="category.icon" autocomplete="off"></el-input>
        </el-form-item>
        <el-form-item label="计量单位">
          <el-input v-model="category.productUnit" autocomplete="off"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="addCategory">确 定</el-button>
      </span>
    </el-dialog>
```

这里双向绑定的model即form表单所有数据，我们绑定到category上。append涉及到数据库的字段icon等都要对应到表单上。

点击append按钮计算获取所点击的菜单层级信息赋值给category，当点击确认提交表单时触发addCategory方法，发送post请求给后端添加新分类。

![image-20210622052428710](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622052428710.png)

#### 4、修改

修改最好复用写好的dialog，需要回显数据，还要注意每次回显都要避免以前的数据残留所以要清空数据，修改数据要传入数据主键给后台。

首先要加一个修改按钮到作用域插槽。

```html
<el-button type="text" size="mini" @click="edit(data)">edit</el-button>
```

点击edit会触发edit方法，在edit方法中主要做两件事，一是从后台获取数据节点的最新信息进行回显，二是打开修改对话框，为了使修改对话框标题和append对话框标题不一样，我们设置独立的属性title和dialogType。

挂载属性和以前一样，edit方法如下，要注意的是我返回的数据格式是data.category，老师的是data.data格式：

``` js
edit(data) {
            console.log("要修改的数据", data);
            this.dialogType = "edit";
            this.title = "修改分类";
            this.dialogVisible = true;
      
            //发送请求获取当前节点最新的数据
            this.$http({
              url: this.$http.adornUrl(`/product/category/info/${data.catId}`),
              method: "get"
            }).then(({data}) => {
              //请求成功
              console.log("要回显的数据", data);
              this.category.name = data.category.name;
          this.category.catId = data.category.catId;
          this.category.icon = data.category.icon;
          this.category.productUnit = data.category.productUnit;
          this.category.parentCid = data.category.parentCid;
          this.category.catLevel = data.category.catLevel;
          this.category.sort = data.category.sort;
          this.category.showStatus = data.category.showStatus;
              /**
               *         parentCid: 0,
               catLevel: 0,
               showStatus: 1,
               sort: 0,
               */
            });
          },
```

当我们提交dialog的时候要区分是append还是edit，所以要修改确定插槽的按钮绑定方法为submitData(),我们在submitData()方法对全局属性dialogType做个判断然后选择对应的方法执行。

![image-20210622191429085](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210622191429085.png)

``` js
submitData() {
        if (this.dialogType == "add") {
          this.addCategory();
        }
        if (this.dialogType == "edit") {
          this.editCategory();
        }
      },
```

最后是editCategory方法，主要就是根据catId修改name,icon,productUnit属性，修改完成提示成功，关闭dialog重新刷新菜单然后最好保持树展开。

```js
//修改三级分类数据
      editCategory() {
        var {
          catId,
          name,
          icon,
          productUnit
        } = this.category;
        this.$http({
          url: this.$http.adornUrl("/product/category/update"),
          method: "post",
          data: this.$http.adornData({
            catId,
            name,
            icon,
            productUnit
          }, false)
        }).then(({
          data
        }) => {
          this.$message({
            message: "菜单修改成功",
            type: "success"
          });
          //关闭对话框
          this.dialogVisible = false;
          //刷新出新的菜单
          this.getMenus();
          //设置需要默认展开的菜单
          this.expandedKey = [this.category.parentCid];
        });
      },
```

到此edit完毕，视频增加了拖拽功能，这个比较复杂单独说明。

#### 5、拖拽

要实现拖拽效果，要保证最大层级不能超过三层，比如当前节点有两层那么只能挂在一层之下，这里有两种情况，一种是当前节点level是1深度是1，另一种是当前节点level是1深度是2，所以要做对应的处理。

视频里是用当前节点最深子节点的level减去当前节点的层级再加一获得深度。比如第一层子节点最深深度是3，当前节点的level是1，那么3-1+1即是深度3，如果是当前节点是第二层，子节点最大level是3，那么深度为3-2+1为2，深度为2。

理解思路后，我们为树添加可拖拽的属性

``` vue
:draggable="draggable"
:allow-drop="allowDrop"
```

draggable是data属性，allow-drop是函数属性。

判断当前节点最大子节点level的是递归算法：

``` js
 countNodeLevel(node) {
      //找到所有子节点，求出最大深度
      if (node.childNodes != null && node.childNodes.length > 0) {
        for (let i = 0; i < node.childNodes.length; i++) {
          if (node.childNodes[i].level > this.maxLevel) {
            this.maxLevel = node.childNodes[i].level;
          }
          this.countNodeLevel(node.childNodes[i]);
        }
      }
    },
```

allow-drop方法如下：

```js
allowDrop(draggingNode, dropNode, type) {
this.maxLevel=0;
      //1、被拖动的当前节点以及所在的父节点总层数不能大于3

      //1）、被拖动的当前节点总层数
      console.log("allowDrop:", draggingNode, dropNode, type);
      //
      this.countNodeLevel(draggingNode);
      //当前正在拖动的节点+父节点所在的深度不大于3即可
      let deep = this.maxLevel==0 ? 0 : Math.abs(this.maxLevel - draggingNode.level) + 1;
      console.log("深度：", deep);

      //   this.maxLevel
      if (type == "inner") {
        // console.log(
        //   `this.maxLevel：${this.maxLevel}；draggingNode.data.catLevel：${draggingNode.data.catLevel}；dropNode.level：${dropNode.level}`
        // );
        return deep + dropNode.level <= 3;
      } else {
        return deep + dropNode.parent.level <= 3;
      }
    },
```

这里有个bug，举个例子，大家电catlevel是2，子节点最大catlevel是3，如果拖成家用电器的兄弟节点，那么大家电的level是1，子节点的level最大是2（catlevel和level不是一个东西），实际计算采用level，现在大家电的level是1了，我们无法将大家电再回归到家用电器分类之下，因为maxLevel始终记录最大值3，所以要记得清空马修Level值。所以我在allowDrop()函数开头加了

```js
this.maxLevel=0;
```

然后还有一个bug，子节点无法拖动，因为默认子节点maxlevel为0然后level又为3，0减3取绝对值加一总为4，所以我也做了判断：

``` js
let deep = this.maxLevel==0 ? 0 : Math.abs(this.maxLevel - draggingNode.level) + 1;
```



拖拽完成，接下来要思考如何保存拖拽修改的信息。其实要修改的字段不过，只要知道拖拽的分类catId和层级以及子关联信息还有新的父类id，递归修改即可。

elementui对应拖拽信息分为三种两类，一是inner拖拽，这种dropNode就是父节点，第二种和第三种拖拽的情况其父节点是dropNode的parent，这里有一种情形，如果节点拖拽成立一级节点那么父节点会显示为undefined，我们赋予默认值0,否则父节点为拖拽后节点的父节点catId。

考虑之前首先要绑定拖拽事件触发函数，以及指定树的ref：

``` html
@node-drop="handleDrop" 
ref="menuTree"
```

分类的考虑都体现在handleDrop这个函数之中，为了保持拖拽后节点展开，我们写个数组属性pCid[]保存所有的父节点catId信息。

获取到pCid自然获取到所有子信息，遍历子节点就能根据catId定位到所拖拽节点的层级等信息，我们再做递归修改，递归修改就是根据catId修改sort，parentCid和catLevel。

```js
for (let i = 0; i < siblings.length; i++) {
          if (siblings[i].data.catId == draggingNode.data.catId) {
            //如果遍历的是当前正在拖拽的节点
            let catLevel = draggingNode.level;
            if (siblings[i].level != draggingNode.level) {
              //当前节点的层级发生变化
              catLevel = siblings[i].level;
              //修改他子节点的层级
              this.updateChildNodeLevel(siblings[i]);
            }
            this.updateNodes.push({
              catId: siblings[i].data.catId,
              sort: i,
              parentCid: pCid,
              catLevel: catLevel
            });
          } else {
            this.updateNodes.push({
              catId: siblings[i].data.catId,
              sort: i
            });
          }
        }

        //3、当前拖拽节点的最新层级
        console.log("updateNodes", this.updateNodes);
      },
```

我们要把发生改变的节点都存到一个数组之中保存起来交给后台批量修改，所以data中放个updateNodes[]。最后在补充一个递归保存修改数据的方法updateChildNodeLevel：

```js
updateChildNodeLevel(node) {
        if (node.childNodes.length > 0) {
          for (let i = 0; i < node.childNodes.length; i++) {
            var cNode = node.childNodes[i].data;
            this.updateNodes.push({
              catId: cNode.catId,
              catLevel: node.childNodes[i].level
            });
            this.updateChildNodeLevel(node.childNodes[i]);
          }
        }
      },
```

最后的最后，添加批量保存的按钮绑定点击事件即可：

```js
<el-switch v-model="draggable" active-text="开启拖拽" inactive-text="关闭拖拽"></el-switch>
    <el-button v-if="draggable" @click="batchSave">批量保存</el-button>
    <el-button type="danger" @click="batchDelete">批量删除</el-button>
```

开启拖拽，批量删除就不说了。

``` js
batchSave() {
        this.$http({
          url: this.$http.adornUrl("/product/category/update/sort"),
          method: "post",
          data: this.$http.adornData(this.updateNodes, false)
        }).then(({
          data
        }) => {
          this.$message({
            message: "菜单顺序等修改成功",
            type: "success"
          });
          //刷新出新的菜单
          this.getMenus();
          //设置需要默认展开的菜单
          this.expandedKey = this.pCid;
          this.updateNodes = [];
          this.maxLevel = 0;
          // this.pCid = 0;
        });
      },
batchDelete() {
        let catIds = [];
        let checkedNodes = this.$refs.menuTree.getCheckedNodes();
        console.log("被选中的元素", checkedNodes);
        for (let i = 0; i < checkedNodes.length; i++) {
          catIds.push(checkedNodes[i].catId);
        }
        this.$confirm(`是否批量删除【${catIds}】菜单?`, "提示", {
            confirmButtonText: "确定",
            cancelButtonText: "取消",
            type: "warning"
          })
          .then(() => {
            this.$http({
              url: this.$http.adornUrl("/product/category/delete"),
              method: "post",
              data: this.$http.adornData(catIds, false)
            }).then(({
              data
            }) => {
              this.$message({
                message: "菜单批量删除成功",
                type: "success"
              });
              this.getMenus();
            });
          })
          .catch(() => {});
      },
```

至此，vue2树部分详解结束。

#### 6、品牌管理和文件上传

vue表单部分就比较简单了，使用逆向生成的代码然后简单修改即可。代码部分如下：

brand.vue

```html
<template>
  <div class="mod-config">
    <el-form :inline="true" :model="dataForm" @keyup.enter.native="getDataList()">
      <el-form-item>
        <el-input v-model="dataForm.key" placeholder="参数名" clearable></el-input>
      </el-form-item>
      <el-form-item>
        <el-button @click="getDataList()">查询</el-button>
        <el-button
          v-if="isAuth('product:brand:save')"
          type="primary"
          @click="addOrUpdateHandle()"
        >新增
        </el-button>
        <el-button
          v-if="isAuth('product:brand:delete')"
          type="danger"
          @click="deleteHandle()"
          :disabled="dataListSelections.length <= 0"
        >批量删除
        </el-button>
      </el-form-item>
    </el-form>
    <el-table
      :data="dataList"
      border
      v-loading="dataListLoading"
      @selection-change="selectionChangeHandle"
      style="width: 100%;"
    >
      <el-table-column type="selection" header-align="center" align="center" width="50"></el-table-column>
      <el-table-column prop="brandId" header-align="center" align="center" label="品牌id"></el-table-column>
      <el-table-column prop="name" header-align="center" align="center" label="品牌名"></el-table-column>
      <el-table-column prop="logo" header-align="center" align="center" label="品牌logo地址">
        <template slot-scope="scope">
          <!-- <el-image
              style="width: 100px; height: 80px"
              :src="scope.row.logo"
          fit="fill"></el-image>-->
          <img :src="scope.row.logo" style="width: 100px; height: 80px"/>
        </template>
      </el-table-column>
      <el-table-column prop="descript" header-align="center" align="center" label="介绍"></el-table-column>
      <el-table-column prop="showStatus" header-align="center" align="center" label="显示状态">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.showStatus"
            active-color="#13ce66"
            inactive-color="#ff4949"
            :active-value="1"
            :inactive-value="0"
            @change="updateBrandStatus(scope.row)"
          ></el-switch>
        </template>
      </el-table-column>
      <el-table-column prop="firstLetter" header-align="center" align="center" label="检索首字母"></el-table-column>
      <el-table-column prop="sort" header-align="center" align="center" label="排序"></el-table-column>
      <el-table-column fixed="right" header-align="center" align="center" width="250" label="操作">
        <template slot-scope="scope">
          <el-button type="text" size="small" @click="updateCatelogHandle(scope.row.brandId)">关联分类</el-button>
          <el-button type="text" size="small" @click="addOrUpdateHandle(scope.row.brandId)">修改</el-button>
          <el-button type="text" size="small" @click="deleteHandle(scope.row.brandId)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      @size-change="sizeChangeHandle"
      @current-change="currentChangeHandle"
      :current-page="pageIndex"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      :total="totalPage"
      layout="total, sizes, prev, pager, next, jumper"
    ></el-pagination>
    <!-- 弹窗, 新增 / 修改 -->
    <add-or-update v-if="addOrUpdateVisible" ref="addOrUpdate" @refreshDataList="getDataList"></add-or-update>

    <el-dialog title="关联分类" :visible.sync="cateRelationDialogVisible" width="30%">
      <el-popover placement="right-end" v-model="popCatelogSelectVisible">
        <category-cascader :catelogPath.sync="catelogPath"></category-cascader>
        <div style="text-align: right; margin: 0">
          <el-button size="mini" type="text" @click="popCatelogSelectVisible = false">取消</el-button>
          <el-button type="primary" size="mini" @click="addCatelogSelect">确定</el-button>
        </div>
        <el-button slot="reference">新增关联</el-button>
      </el-popover>
      <el-table :data="cateRelationTableData" style="width: 100%">
        <el-table-column prop="id" label="#"></el-table-column>
        <el-table-column prop="brandName" label="品牌名"></el-table-column>
        <el-table-column prop="catelogName" label="分类名"></el-table-column>
        <el-table-column fixed="right" header-align="center" align="center" label="操作">
          <template slot-scope="scope">
            <el-button
              type="text"
              size="small"
              @click="deleteCateRelationHandle(scope.row.id,scope.row.brandId)"
            >移除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer" class="dialog-footer">
        <el-button @click="cateRelationDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="cateRelationDialogVisible = false">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import AddOrUpdate from "./brand-add-or-update";
// import CategoryCascader from "../common/category-cascader";
export default {
  data() {
    return {
      dataForm: {
        key: ""
      },
      brandId: 0,
      catelogPath: [],
      dataList: [],
      cateRelationTableData: [],
      pageIndex: 1,
      pageSize: 10,
      totalPage: 0,
      dataListLoading: false,
      dataListSelections: [],
      addOrUpdateVisible: false,
      cateRelationDialogVisible: false,
      popCatelogSelectVisible: false
    };
  },
  components: {
    AddOrUpdate,
    // CategoryCascader
  },
  activated() {
    this.getDataList();
  },
  methods: {
    addCatelogSelect() {
      //{"brandId":1,"catelogId":2}
      this.popCatelogSelectVisible = false;
      this.$http({
        url: this.$http.adornUrl("/product/categorybrandrelation/save"),
        method: "post",
        data: this.$http.adornData({
          brandId: this.brandId,
          catelogId: this.catelogPath[this.catelogPath.length - 1]
        }, false)
      }).then(({data}) => {
        this.getCateRelation();
      });
    },
    deleteCateRelationHandle(id, brandId) {
      this.$http({
        url: this.$http.adornUrl("/product/categorybrandrelation/delete"),
        method: "post",
        data: this.$http.adornData([id], false)
      }).then(({data}) => {
        this.getCateRelation();
      });
    },
    updateCatelogHandle(brandId) {
      this.cateRelationDialogVisible = true;
      this.brandId = brandId;
      this.getCateRelation();
    },
    getCateRelation() {
      this.$http({
        url: this.$http.adornUrl("/product/categorybrandrelation/catelog/list"),
        method: "get",
        params: this.$http.adornParams({
          brandId: this.brandId
        })
      }).then(({data}) => {
        this.cateRelationTableData = data.data;
      });
    },
    // 获取数据列表
    getDataList() {
      this.dataListLoading = true;
      this.$http({
        url: this.$http.adornUrl("/product/brand/list"),
        method: "get",
        params: this.$http.adornParams({
          page: this.pageIndex,
          limit: this.pageSize,
          key: this.dataForm.key
        })
      }).then(({data}) => {
        if (data && data.code === 0) {
          this.dataList = data.page.list;
          this.totalPage = data.page.totalCount;
        } else {
          this.dataList = [];
          this.totalPage = 0;
        }
        this.dataListLoading = false;
      });
    },
    updateBrandStatus(data) {
      console.log("最新信息", data);
      let {brandId, showStatus} = data;
      //发送请求修改状态
      this.$http({
        url: this.$http.adornUrl("/product/brand/update/status"),
        method: "post",
        data: this.$http.adornData({brandId, showStatus}, false)
      }).then(({data}) => {
        this.$message({
          type: "success",
          message: "状态更新成功"
        });
      });
    },
    // 每页数
    sizeChangeHandle(val) {
      this.pageSize = val;
      this.pageIndex = 1;
      this.getDataList();
    },
    // 当前页
    currentChangeHandle(val) {
      this.pageIndex = val;
      this.getDataList();
    },
    // 多选
    selectionChangeHandle(val) {
      this.dataListSelections = val;
    },
    // 新增 / 修改
    addOrUpdateHandle(id) {
      this.addOrUpdateVisible = true;
      this.$nextTick(() => {
        this.$refs.addOrUpdate.init(id);
      });
    },
    // 删除
    deleteHandle(id) {
      var ids = id
        ? [id]
        : this.dataListSelections.map(item => {
          return item.brandId;
        });
      this.$confirm(
        `确定对[id=${ids.join(",")}]进行[${id ? "删除" : "批量删除"}]操作?`,
        "提示",
        {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning"
        }
      ).then(() => {
        this.$http({
          url: this.$http.adornUrl("/product/brand/delete"),
          method: "post",
          data: this.$http.adornData(ids, false)
        }).then(({data}) => {
          if (data && data.code === 0) {
            this.$message({
              message: "操作成功",
              type: "success",
              duration: 1500,
              onClose: () => {
                this.getDataList();
              }
            });
          } else {
            this.$message.error(data.msg);
          }
        });
      });
    }
  }
};
</script>
<style scoped>
</style>
```

brand-add-or-update.vue

```html
<template>
  <el-dialog
    :title="!dataForm.id ? '新增' : '修改'"
    :close-on-click-modal="false"
    :visible.sync="visible"
  >
    <el-form
      :model="dataForm"
      :rules="dataRule"
      ref="dataForm"
      @keyup.enter.native="dataFormSubmit()"
      label-width="140px"
    >
      <el-form-item label="品牌名" prop="name">
        <el-input v-model="dataForm.name" placeholder="品牌名"></el-input>
      </el-form-item>
      <el-form-item label="品牌logo地址" prop="logo">
        <!-- <el-input v-model="dataForm.logo" placeholder="品牌logo地址"></el-input> -->
        <single-upload v-model="dataForm.logo"></single-upload>
      </el-form-item>
      <el-form-item label="介绍" prop="descript">
        <el-input v-model="dataForm.descript" placeholder="介绍"></el-input>
      </el-form-item>
      <el-form-item label="显示状态" prop="showStatus">
        <el-switch
          v-model="dataForm.showStatus"
          active-color="#13ce66"
          inactive-color="#ff4949"
          :active-value="1"
          :inactive-value="0"
        ></el-switch>
      </el-form-item>
      <el-form-item label="检索首字母" prop="firstLetter">
        <el-input v-model="dataForm.firstLetter" placeholder="检索首字母"></el-input>
      </el-form-item>
      <el-form-item label="排序" prop="sort">
        <el-input v-model.number="dataForm.sort" placeholder="排序"></el-input>
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="dataFormSubmit()">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
import SingleUpload from '@/components/upload/singleUpload';

export default {
  components: {SingleUpload},
  data() {
    return {
      visible: false,
      dataForm: {
        brandId: 0,
        name: "",
        logo: "",
        descript: "",
        showStatus: 1,
        firstLetter: "",
        sort: 0
      },
      dataRule: {
        name: [{required: true, message: "品牌名不能为空", trigger: "blur"}],
        logo: [
          {required: true, message: "品牌logo地址不能为空", trigger: "blur"}
        ],
        descript: [
          {required: true, message: "介绍不能为空", trigger: "blur"}
        ],
        showStatus: [
          {
            required: true,
            message: "显示状态[0-不显示；1-显示]不能为空",
            trigger: "blur"
          }
        ],
        firstLetter: [
          {
            validator: (rule, value, callback) => {
              if (value == "") {
                callback(new Error("首字母必须填写"));
              } else if (!/^[a-zA-Z]$/.test(value)) {
                callback(new Error("首字母必须a-z或者A-Z之间"));
              } else {
                callback();
              }
            },
            trigger: "blur"
          }
        ],
        sort: [
          {
            validator: (rule, value, callback) => {
              if (value == "") {
                callback(new Error("排序字段必须填写"));
              } else if (!Number.isInteger(value) || value < 0) {
                callback(new Error("排序必须是一个大于等于0的整数"));
              } else {
                callback();
              }
            },
            trigger: "blur"
          }
        ]
      }
    };
  },
  methods: {
    init(id) {
      this.dataForm.brandId = id || 0;
      this.visible = true;
      this.$nextTick(() => {
        this.$refs["dataForm"].resetFields();
        if (this.dataForm.brandId) {
          this.$http({
            url: this.$http.adornUrl(
              `/product/brand/info/${this.dataForm.brandId}`
            ),
            method: "get",
            params: this.$http.adornParams()
          }).then(({data}) => {
            if (data && data.code === 0) {
              this.dataForm.name = data.brand.name;
              this.dataForm.logo = data.brand.logo;
              this.dataForm.descript = data.brand.descript;
              this.dataForm.showStatus = data.brand.showStatus;
              this.dataForm.firstLetter = data.brand.firstLetter;
              this.dataForm.sort = data.brand.sort;
            }
          });
        }
      });
    },
    // 表单提交
    dataFormSubmit() {
      this.$refs["dataForm"].validate(valid => {
        if (valid) {
          this.$http({
            url: this.$http.adornUrl(
              `/product/brand/${!this.dataForm.brandId ? "save" : "update"}`
            ),
            method: "post",
            data: this.$http.adornData({
              brandId: this.dataForm.brandId || undefined,
              name: this.dataForm.name,
              logo: this.dataForm.logo,
              descript: this.dataForm.descript,
              showStatus: this.dataForm.showStatus,
              firstLetter: this.dataForm.firstLetter,
              sort: this.dataForm.sort
            })
          }).then(({data}) => {
            if (data && data.code === 0) {
              this.$message({
                message: "操作成功",
                type: "success",
                duration: 1500,
                onClose: () => {
                  this.visible = false;
                  this.$emit("refreshDataList");
                }
              });
            } else {
              this.$message.error(data.msg);
            }
          });
        }
      });
    }
  }
};
</script>
```

可以注意到，brand-add-or-update.vue里导入了一个组件叫singleUpload，这是自定义组件用于前端签名上传文件，详细如下，分为多文件上传，单文件上传和获取签名的promise函数policy.js。

从单文件上传组件分析，其实就是上传图片前调用函数beforeUpload(file)，使用policy从服务器获取签名然后绑定到对应图片上直接上传到阿里云oss。

singleUpload.vue:

``` html
<template>
  <div>
    <el-upload
      action="http://gulimall-hello.oss-cn-beijing.aliyuncs.com"
      :data="dataObj"
      list-type="picture"
      :multiple="false" :show-file-list="showFileList"
      :file-list="fileList"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      :on-success="handleUploadSuccess"
      :on-preview="handlePreview">
      <el-button size="small" type="primary">点击上传</el-button>
      <div slot="tip" class="el-upload__tip">只能上传jpg/png文件，且不超过10MB</div>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible">
      <img width="100%" :src="fileList[0].url" alt="">
    </el-dialog>
  </div>
</template>
<script>
   import {policy} from './policy'
   import { getUUID } from '@/utils'

  export default {
    name: 'singleUpload',
    props: {
      value: String
    },
    computed: {
      imageUrl() {
        return this.value;
      },
      imageName() {
        if (this.value != null && this.value !== '') {
          return this.value.substr(this.value.lastIndexOf("/") + 1);
        } else {
          return null;
        }
      },
      fileList() {
        return [{
          name: this.imageName,
          url: this.imageUrl
        }]
      },
      showFileList: {
        get: function () {
          return this.value !== null && this.value !== ''&& this.value!==undefined;
        },
        set: function (newValue) {
        }
      }
    },
    data() {
      return {
        dataObj: {
          policy: '',
          signature: '',
          key: '',
          ossaccessKeyId: '',
          dir: '',
          host: '',
          // callback:'',
        },
        dialogVisible: false
      };
    },
    methods: {
      emitInput(val) {
        this.$emit('input', val)
      },
      handleRemove(file, fileList) {
        this.emitInput('');
      },
      handlePreview(file) {
        this.dialogVisible = true;
      },
      beforeUpload(file) {
        let _self = this;
        return new Promise((resolve, reject) => {
          policy().then(response => {
            console.log("响应的数据",response);
            _self.dataObj.policy = response.data.policy;
            _self.dataObj.signature = response.data.signature;
            _self.dataObj.ossaccessKeyId = response.data.accessid;
            _self.dataObj.key = response.data.dir +getUUID()+'_${filename}';
            _self.dataObj.dir = response.data.dir;
            _self.dataObj.host = response.data.host;
            console.log("响应的数据222。。。",_self.dataObj);
            resolve(true)
          }).catch(err => {
            reject(false)
          })
        })
      },
      handleUploadSuccess(res, file) {
        console.log("上传成功...")
        this.showFileList = true;
        this.fileList.pop();
        this.fileList.push({name: file.name, url: this.dataObj.host + '/' + this.dataObj.key.replace("${filename}",file.name) });
        this.emitInput(this.fileList[0].url);
      }
    }
  }
</script>
<style>
</style>
```

multiUpload.vue

``` html
<template>
  <div>
    <el-upload
      action="http://gulimall-hello.oss-cn-beijing.aliyuncs.com"
      :data="dataObj"
      :list-type="listType"
      :file-list="fileList"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      :on-success="handleUploadSuccess"
      :on-preview="handlePreview"
      :limit="maxCount"
      :on-exceed="handleExceed"
      :show-file-list="showFile"
    >
      <i class="el-icon-plus"></i>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible">
      <img width="100%" :src="dialogImageUrl" alt />
    </el-dialog>
  </div>
</template>
<script>
import { policy } from "./policy";
import { getUUID } from '@/utils'
export default {
  name: "multiUpload",
  props: {
    //图片属性数组
    value: Array,
    //最大上传图片数量
    maxCount: {
      type: Number,
      default: 30
    },
    listType:{
      type: String,
      default: "picture-card"
    },
    showFile:{
      type: Boolean,
      default: true
    }

  },
  data() {
    return {
      dataObj: {
        policy: "",
        signature: "",
        key: "",
        ossaccessKeyId: "",
        dir: "",
        host: "",
        uuid: ""
      },
      dialogVisible: false,
      dialogImageUrl: null
    };
  },
  computed: {
    fileList() {
      let fileList = [];
      for (let i = 0; i < this.value.length; i++) {
        fileList.push({ url: this.value[i] });
      }

      return fileList;
    }
  },
  mounted() {},
  methods: {
    emitInput(fileList) {
      let value = [];
      for (let i = 0; i < fileList.length; i++) {
        value.push(fileList[i].url);
      }
      this.$emit("input", value);
    },
    handleRemove(file, fileList) {
      this.emitInput(fileList);
    },
    handlePreview(file) {
      this.dialogVisible = true;
      this.dialogImageUrl = file.url;
    },
    beforeUpload(file) {
      let _self = this;
      return new Promise((resolve, reject) => {
        policy()
          .then(response => {
            console.log("这是什么${filename}");
            _self.dataObj.policy = response.data.policy;
            _self.dataObj.signature = response.data.signature;
            _self.dataObj.ossaccessKeyId = response.data.accessid;
            _self.dataObj.key = response.data.dir +getUUID()+"_${filename}";
            _self.dataObj.dir = response.data.dir;
            _self.dataObj.host = response.data.host;
            resolve(true);
          })
          .catch(err => {
            console.log("出错了...",err)
            reject(false);
          });
      });
    },
    handleUploadSuccess(res, file) {
      this.fileList.push({
        name: file.name,
        // url: this.dataObj.host + "/" + this.dataObj.dir + "/" + file.name； 替换${filename}为真正的文件名
        url: this.dataObj.host + "/" + this.dataObj.key.replace("${filename}",file.name)
      });
      this.emitInput(this.fileList);
    },
    handleExceed(files, fileList) {
      this.$message({
        message: "最多只能上传" + this.maxCount + "张图片",
        type: "warning",
        duration: 1000
      });
    }
  }
};
</script>
<style>
</style>
```

policy.js:

```js
import http from '@/utils/httpRequest.js'
export function policy() {
   return  new Promise((resolve,reject)=>{
        http({
            url: http.adornUrl("/third-party/oss/policy"),
            method: "get",
            params: http.adornParams({})
        }).then(({ data }) => {
            resolve(data);
        })
    });
}
```

#### 7、前端校验

主要是elementui组件封装的校验，我们遵循elementui官网的form表单自定义验证规则来看。

![image-20210623213312890](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210623213312890.png)

图片一目了然，validator还可以使用箭头函数写在内部，除了写在data()中return上。

比如对brand-add-or-update.vue的首字母检索和排序自定义校验规则：

``` js
firstLetter: [
          {
            validator: (rule, value, callback) => {
              if (value == "") {
                callback(new Error("首字母必须填写"));
              } else if (!/^[a-zA-Z]$/.test(value)) {
                callback(new Error("首字母必须a-z或者A-Z之间"));
              } else {
                callback();
              }
            },
            trigger: "blur"
          }
        ],
        sort: [
          {
            validator: (rule, value, callback) => {
              if (value == "") {
                callback(new Error("排序字段必须填写"));
              } else if (!Number.isInteger(value) || value < 0) {
                callback(new Error("排序必须是一个大于等于0的整数"));
              } else {
                callback();
              }
            },
            trigger: "blur"
          }
        ]
```

可以总结为两步，一是return内声明form表单绑定校验集合，二是编写validator自定义校验函数。

#### 8、组件抽取

我们想要如下的效果：

![image-20200430215649355](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20200430215649355.png)

左边的树已经写过，考虑复用，左右布局是栅栏结构，共分为24份，左边8份右边16.

首先抽取树组件到common包下，起名catgory.vue。

代码是裁剪后：

``` html
<template>
  <el-tree :data="menus" :props="defaultProps" node-key="catId" ref="menuTree">
  </el-tree>
</template>

<script>
  //这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
  //例如：import 《组件名称》 from '《组件路径》';

  export default {
    //import引入的组件需要注入到对象中才能使用
    components: {},
    data() {
      return {
        menus: [],
        defaultProps: {
          children: "children",
          label: "name"
        }
      };
    },
    //监听属性 类似于data概念
    computed: {},
    //监控data中的数据变化
    watch: {},
    //方法集合
    methods: {
      getMenus() {
        this.$http({
          url: this.$http.adornUrl("/product/category/list/tree"),
          method: "get"
        }).then(({
          data
        }) => {
          console.log("成功获取到菜单数据...", data.data);
          this.menus = data.data;
        });
      },
      nodeclick(data,node,component){
        console.log("子组件category的节点被点击",data,node,component);
        this.$emit("tree-node-click",data,node,component);
      }
    },
    //生命周期 - 创建完成（可以访问当前this实例）
    created() {
this.getMenus();
    },
    //生命周期 - 挂载完成（可以访问DOM元素）
    mounted() {

    },
    beforeCreate() {}, //生命周期 - 创建之前
    beforeMount() {}, //生命周期 - 挂载之前
    beforeUpdate() {}, //生命周期 - 更新之前
    updated() {}, //生命周期 - 更新之后
    beforeDestroy() {}, //生命周期 - 销毁之前
    destroyed() {}, //生命周期 - 销毁完成
    activated() {}, //如果页面有keep-alive缓存功能，这个函数会触发
  }
</script>
<style scoped>
</style>
```

然后写个父组件，将category作为子组件引入，新建attrgroup.vue采用elementui的栅格布局：

``` html
<!--  -->
<template>
  <el-row>
    <el-col :span="8">
      <category></category>
      <div class="grid-content bg-purple"></div>
    </el-col>
    <el-col :span="16">
      <el-form :inline="true" :model="dataForm" @keyup.enter.native="getDataList()">
        <el-form-item>
          <el-input v-model="dataForm.key" placeholder="参数名" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-button @click="getDataList()">查询</el-button>
          <el-button v-if="isAuth('product:attrgroup:save')" type="primary" @click="addOrUpdateHandle()">新增</el-button>
          <el-button v-if="isAuth('product:attrgroup:delete')" type="danger" @click="deleteHandle()" :disabled="dataListSelections.length <= 0">批量删除</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="dataList" border v-loading="dataListLoading" @selection-change="selectionChangeHandle" style="width: 100%;">
        <el-table-column type="selection" header-align="center" align="center" width="50">
        </el-table-column>
        <el-table-column prop="attrGroupId" header-align="center" align="center" label="分组id">
        </el-table-column>
        <el-table-column prop="attrGroupName" header-align="center" align="center" label="组名">
        </el-table-column>
        <el-table-column prop="sort" header-align="center" align="center" label="排序">
        </el-table-column>
        <el-table-column prop="descript" header-align="center" align="center" label="描述">
        </el-table-column>
        <el-table-column prop="icon" header-align="center" align="center" label="组图标">
        </el-table-column>
        <el-table-column prop="catelogId" header-align="center" align="center" label="所属分类id">
        </el-table-column>
        <el-table-column fixed="right" header-align="center" align="center" width="150" label="操作">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="addOrUpdateHandle(scope.row.attrGroupId)">修改</el-button>
            <el-button type="text" size="small" @click="deleteHandle(scope.row.attrGroupId)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="grid-content bg-purple-light"></div>
    </el-col>
  </el-row>
</template>

<script>
  //这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
  //例如：import 《组件名称》 from '《组件路径》';

  import Category from "../../common/category"
  import AddOrUpdate from './attrgroup-add-or-update'
  export default {
    //import引入的组件需要注入到对象中才能使用
    components: {
      Category,
      AddOrUpdate
    },
    data() {
      return {
        dataForm: {
          key: ''
        },
        dataList: [],
        pageIndex: 1,
        pageSize: 10,
        totalPage: 0,
        dataListLoading: false,
        dataListSelections: [],
        addOrUpdateVisible: false
      }
    },
    activated() {
      this.getDataList()
    },
    methods: {
      // 获取数据列表
      getDataList() {
        this.dataListLoading = true
        this.$http({
          url: this.$http.adornUrl('/product/attrgroup/list'),
          method: 'get',
          params: this.$http.adornParams({
            'page': this.pageIndex,
            'limit': this.pageSize,
            'key': this.dataForm.key
          })
        }).then(({
          data
        }) => {
          if (data && data.code === 0) {
            this.dataList = data.page.list
            this.totalPage = data.page.totalCount
          } else {
            this.dataList = []
            this.totalPage = 0
          }
          this.dataListLoading = false
        })
      },
      // 每页数
      sizeChangeHandle(val) {
        this.pageSize = val
        this.pageIndex = 1
        this.getDataList()
      },
      // 当前页
      currentChangeHandle(val) {
        this.pageIndex = val
        this.getDataList()
      },
      // 多选
      selectionChangeHandle(val) {
        this.dataListSelections = val
      },
      // 新增 / 修改
      addOrUpdateHandle(id) {
        this.addOrUpdateVisible = true
        this.$nextTick(() => {
          this.$refs.addOrUpdate.init(id)
        })
      },
      // 删除
      deleteHandle(id) {
        var ids = id ? [id] : this.dataListSelections.map(item => {
          return item.attrGroupId
        })
        this.$confirm(`确定对[id=${ids.join(',')}]进行[${id ? '删除' : '批量删除'}]操作?`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          this.$http({
            url: this.$http.adornUrl('/product/attrgroup/delete'),
            method: 'post',
            data: this.$http.adornData(ids, false)
          }).then(({
            data
          }) => {
            if (data && data.code === 0) {
              this.$message({
                message: '操作成功',
                type: 'success',
                duration: 1500,
                onClose: () => {
                  this.getDataList()
                }
              })
            } else {
              this.$message.error(data.msg)
            }
          })
        })
      }
    }
  }
</script>
<style scoped>

</style>
```

#### 9、父子组件通信

当我们点击子组件category时，要让父组件爱她突然group感知到。使用的是事件机制，子组件发送事件给父组件。

首先在子组件category.vue绑定一个节点点击事件：

![image-20210625180028856](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210625180028856.png)

我们为点击事件编写父子组件通信机制，使用$emit：

![image-20210625180206012](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210625180206012.png)

在父组件attrgroup.vue中绑定该事件：

![image-20210625180404918](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210625180404918.png)

然后在函数中书写相应的处理逻辑：

![image-20210625180512412](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210625180512412.png)

#### 10、级联选择器

参考elementUI的cascade级联选择器，数据是绑定在options上的，是个数组，我们取名categorys[]同样绑定解析对象props指定label和value。

![image-20210627223802653](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210627223802653.png)

这里还是做了组件抽取：

``` html
<template>
<!-- 
使用说明：
1）、引入category-cascader.vue
2）、语法：<category-cascader :catelogPath.sync="catelogPath"></category-cascader>
    解释：
      catelogPath：指定的值是cascader初始化需要显示的值，应该和父组件的catelogPath绑定;
          由于有sync修饰符，所以cascader路径变化以后自动会修改父的catelogPath，这是结合子组件this.$emit("update:catelogPath",v);做的
      -->
  <div>
    <el-cascader
      filterable
      clearable 
      placeholder="试试搜索：手机"
      v-model="paths"
      :options="categorys"
      :props="setting"
    ></el-cascader>
  </div>
</template>

<script>
//这里可以导入其他文件（比如：组件，工具js，第三方插件js，json文件，图片文件等等）
//例如：import 《组件名称》 from '《组件路径》';

export default {
  //import引入的组件需要注入到对象中才能使用
  components: {},
  //接受父组件传来的值
  props: {
    catelogPath: {
      type: Array,
      default(){
        return [];
      }
    }
  },
  data() {
    //这里存放数据
    return {
      setting: {
        value: "catId",
        label: "name",
        children: "children"
      },
      categorys: [],
      paths: this.catelogPath
    };
  },
  watch:{
    catelogPath(v){
      this.paths = this.catelogPath;
    },
    paths(v){
      this.$emit("update:catelogPath",v);
      //还可以使用pubsub-js进行传值
      this.PubSub.publish("catPath",v);
    }
  },
  //方法集合
  methods: {
    getCategorys() {
      this.$http({
        url: this.$http.adornUrl("/product/category/list/tree"),
        method: "get"
      }).then(({ data }) => {
        this.categorys = data.data;
      });
    }
  },
  //生命周期 - 创建完成（可以访问当前this实例）
  created() {
    this.getCategorys();
  }
};
</script>
<style scoped>
</style>
```

attr-add-or-update.vue:

![image-20210627230619942](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210627230619942.png)

级联回显是绑定的一个数组catelogPath[],发送请求给后台发送的是catId对应的属性是catelogId: ""。编写一个接口根据三级catId返回完整路径数组即可。

至此vue部分结束。

### 第三方模块

需要创建独立的第三方模块来集成第三方服务，建立比较简单，要注意聚合到mall主工程pom之中，注册到nacos注册中心和指定nacos命名空间，配置端口号和服务名称，开启注册发现，配置网关路由转发。

至于阿里云开通oss服务，开通子账号授权oss权限获取认证密钥和访问地址等等太简单略过。

pom.xml

``` xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.2.6.RELEASE</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.flitsneak.mall</groupId>
    <artifactId>mall-third-part</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>mall-third-part</name>
    <description>第三方服务</description>
    <properties>
        <java.version>11</java.version>
        <spring-cloud.version>Hoxton.SR3</spring-cloud.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.flitsneak</groupId>
            <artifactId>mall-common</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <exclusions>
                <exclusion>
                    <groupId>com.baomidou</groupId>
                    <artifactId>mybatis-plus-boot-starter</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alicloud-oss</artifactId>
            <version>2.2.0.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

我的yml和bootstrap.properties如下，老师分的细了一点，本质上没差别：

```yaml
spring:
  application:
    name: mall-third-part
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    alicloud:
      access-key:
      secret-key:
      oss:
        endpoint:
        bucket:
server:
  port: 8823
```

```pro
spring.application.name=mall-third-part

spring.cloud.nacos.config.server-addr=127.0.0.1:8848
spring.cloud.nacos.config.namespace=d8a8c975-806f-458c-a3be-e8e087ce7ba3
spring.cloud.nacos.config.file-extension=yml
spring.cloud.nacos.config.group=dev
```

网关配置和之前的没什么区别：

```yml
- id: mall-third-party
            uri: lb://mall-third-party
            predicates:
              - Path=/api/third-party/**
            filters:
              - RewritePath=/api/third-party/(?<segment>.*),/$\{segment}
```

图片上传采用的是前端请求本地服务器获取签名然后直接上传到阿里云oss，所以第三方模块要有颁发签名的接口：

``` java
package com.flitsneak.mall.thirdpart.controller;

import com.aliyun.oss.OSS;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.MatchMode;
import com.aliyun.oss.model.PolicyConditions;
import com.flitsneak.common.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class OssController {

    @Autowired
    OSS ossClient;

    @Value("${spring.cloud.alicloud.oss.endpoint}")
    private String endpoint;
    @Value("${spring.cloud.alicloud.oss.bucket}")
    private String bucket;
    @Value("${spring.cloud.alicloud.access-key}")
    private String accessId;

    /**
     * Oss 获取服务端签名
     * @return
     */
    @RequestMapping("/oss/policy")
    public R policy() {

        // https://gulimall-hello.oss-cn-beijing.aliyuncs.com/hahaha.jpg  host的格式为 bucketname.endpoint
        String host = "https://" + bucket + "." + endpoint;
        // callbackUrl为 上传回调服务器的URL，请将下面的IP和Port配置为您自己的真实信息。
        // String callbackUrl = "http://88.88.88.88:8888";
        String format = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        // 用户上传文件时指定的前缀。
        String dir = format + "/";

        Map<String, String> respMap = null;
        try {
            long expireTime = 30;
            long expireEndTime = System.currentTimeMillis() + expireTime * 1000;
            Date expiration = new Date(expireEndTime);
            PolicyConditions policyConds = new PolicyConditions();
            policyConds.addConditionItem(PolicyConditions.COND_CONTENT_LENGTH_RANGE, 0, 1048576000);
            policyConds.addConditionItem(MatchMode.StartWith, PolicyConditions.COND_KEY, dir);

            String postPolicy = ossClient.generatePostPolicy(expiration, policyConds);
            byte[] binaryData = postPolicy.getBytes(StandardCharsets.UTF_8);
            String encodedPolicy = BinaryUtil.toBase64String(binaryData);
            String postSignature = ossClient.calculatePostSignature(postPolicy);

            respMap = new LinkedHashMap<String, String>();
            respMap.put("accessid", accessId);
            respMap.put("policy", encodedPolicy);
            respMap.put("signature", postSignature);
            respMap.put("dir", dir);
            respMap.put("host", host);
            respMap.put("expire", String.valueOf(expireEndTime / 1000));
            // respMap.put("expire", formatISO8601Date(expiration));

        } catch (Exception e) {
            // Assert.fail(e.getMessage());
            System.out.println(e.getMessage());
        }

        return R.ok().put("data", respMap);
    }
}
```

### JSR 303

#### 1、简介

jsr 是 Java Specification Requests 的缩写，意思是java的请求规范。周志明老师的书上还着重介绍过jsr292(jvm多语言支持包括Kotlin,Clojure,JRuby,Scala等)。

JSR303着重参数校验功能，点开javax.validation.constraints，可以看到已经封装好的注解有这些：

![image-20210623223330676](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210623223330676.png)

使用jsr303规范很简单，第一步在实体类相应字段上标注校验注解，比如@Email或者标注自定义校验注解@Pattern(regexp=”“)自定义正则表达式来处理；第二步是使用校验，只需要在@RequestBody之前加上@Valid注解即表明开启校验。

#### 2、分组校验

更复杂的场景，我们可以分组校验：

​    1)、   @NotBlank(message = "品牌名必须提交",groups = {AddGroup.class,UpdateGroup.class})
​     给校验注解标注什么情况需要进行校验,比如增加不校验修改时校验。这里group里传入的是个接口。
​     2）、开启分组校验要使用spring实现的注解@Validated({AddGroup.class})
​     3)、默认没有指定分组的校验注解比如@NotBlank，在分组校验情况@Validated({AddGroup.class})下不生效，只会在@Validated生效，也就是说Validated后加了分组那么不加分组的校验注解就会失效；

细节如下：

```java
@Data
@TableName("pms_brand")
public class BrandEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 品牌id
     */
    @NotNull(message = "修改必须指定品牌id", groups = {UpdateGroup.class})
    @Null(message = "新增不能指定id", groups = {AddGroup.class})
    @TableId
    private Long brandId;
    /**
     * 品牌名
     */
    @NotBlank(message = "品牌名必须提交", groups = {AddGroup.class, UpdateGroup.class})
    private String name;
    /**
     * 品牌logo地址
     */
    @NotBlank(groups = {AddGroup.class})
    @URL(message = "logo必须是一个合法的url地址", groups = {AddGroup.class, UpdateGroup.class})
    private String logo;
    /**
     * 介绍
     */
    private String descript;
    /**
     * 显示状态[0-不显示；1-显示]
     */
//  @Pattern()
    @NotNull(groups = {AddGroup.class, UpdateStatusGroup.class})
    @ListValue(values = {0, 1}, groups = {AddGroup.class, UpdateStatusGroup.class})
    private Integer showStatus;
    /**
     * 检索首字母
     */
    @NotEmpty(groups = {AddGroup.class})
    @Pattern(regexp = "^[a-zA-Z]$", message = "检索首字母必须是一个字母", groups = {AddGroup.class, UpdateGroup.class})
    private String firstLetter;
    /**
     * 排序
     */
    @NotNull(groups = {AddGroup.class})
    @Min(value = 0, message = "排序必须大于等于0", groups = {AddGroup.class, UpdateGroup.class})
    private Integer sort;
}
```

controller层内容：

```java
public R update(@Validated(UpdateGroup.class) @RequestBody BrandEntity brand)
```

#### 3、自定义校验注解

还有自定义校验，可以编写一个自定义的校验注解，然后编写一个自定义的校验器 ConstraintValidator，然后两者关联。

也就是说@Pattern()正则不能满足校验的情况，可以使用自定义校验注解。

比如对showStatus做自定义校验，规定只能是整数0或1。

需要先按照规范自定义一个注解@ListValue()

``` java
import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = {ListValueConstraintValidator.class})
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
public @interface ListValue {
    String message() default "{com.flitsneak.common.valid.ListValue.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int[] values() default {};
}
```

注解增加属性int[]，提示信息仿照规范自定义common模块resource目录下新建

ValidationMessages.properties文件

``` properties
com.flitsneak.common.valid.ListValue.message=必须提交指定的值
```

validatedBy后传入我们自定义的校验器，注解作为参数通过自定义校验器ListValueConstraintValidator对参数进行校验。

``` java
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

/**
 * @Author FlitSneak
 * @Date 2021/6/24
 */
public class ListValueConstraintValidator implements ConstraintValidator<ListValue,Integer> {
    private final Set<Integer> set = new HashSet<>();
    /**
     * 初始化方法
     * 参数：自定义注解的详细信息
     */
    @Override
    public void initialize(ListValue constraintAnnotation) {
        int[] values = constraintAnnotation.values();
        for (int val : values) {
            set.add(val);
        }

    }

    /**
     * 判断是否校验成功
     *
     * @param value   需要校验的值
     * @param context
     * @return
     */
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return set.contains(value);
    }
}
```

就是将注解的值放大set集合之中，然后对字段之判断是否在set集合之中。

### 统一拦截处理

校验响应的结果可以用BindingResult来接收处理返回，但是相当麻烦，推荐做统一拦截处理。

BingdingResult处理方式：

``` java
public R save(@Valid @RequestBody BrandEntity brand, BindingResult bindingResult){
        if (bindingResult.hasErrors()){
            Map<String,String> map = new HashMap<>();
            //获取校验错误结果
            bindingResult.getFieldErrors().forEach(i->{
                //获取到错误提示
                String message = i.getField();
                //获取出错的字段
                String field = i.getField();
                map.put(field,message);
            });
            return R.error(400,"校验错误").put("data",map);
        }else {

		brandService.save(brand);

        return R.ok();}
    }
```

改为ControllerAdvice统一拦截处理，

``` java
@Slf4j
@RestControllerAdvice(basePackages = "com.flitsneak.mall.product.controller")
public class MallControllerAdvice {
    @ExceptionHandler(value= MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e){
        log.error("数据校验出现问题{}，异常类型：{}",e.getMessage(),e.getClass());
        BindingResult bindingResult = e.getBindingResult();

        Map<String,String> errorMap = new HashMap<>();
        bindingResult.getFieldErrors().forEach((fieldError)->{
            errorMap.put(fieldError.getField(),fieldError.getDefaultMessage());
        });
        return R.error(BizCodeEnum.VALID_EXCEPTION.getCode(),BizCodeEnum.VALID_EXCEPTION.getMsg()).put("data",errorMap);
    }

    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable throwable){

        log.error("错误：",throwable);
        return R.error(BizCodeEnum.UNKNOWN_EXCEPTION.getCode(),BizCodeEnum.UNKNOWN_EXCEPTION.getMsg());
    }
}
```

指定拦截的是MethodArgumentNotValidException异常，异常对象e里面可以获取BindingResult，处理方式一样。

### 数据库设计

商品表来看，做了很多复用属性的提取。category是三级分类表，brand是品牌表，品牌下有很多分类，比如小米下有手机分类也可能有毛巾等日用分类，是多对多关系，所以两张表通过中间表category_brand_relation关联起来。

从attr属性表来看，attr是商品属性，一类商品有很多属性，不同属性可以组合成一组，对应的是attr_group表，他俩也是多对多关系，比如手机主体组可能包含入网型号和上市年份，而牛奶主体可能会包含厂家和生产地，这两者通过中间表attr_attrgroup_relation关联起来。

![image-20210625201356313](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210625201356313.png)

![image-20210625201806183](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210625201806183.png)

![image-20210625201834788](https://gitee.com/flitsneak/mall/raw/develop/picture/image-20210625201834788.png)

spu是指具体食物的公共属性，华为mate40pro来说，华为mate40pro的spu是华为mate40pro的公共信息，包括名字和分类和品牌，也包含重量，发行年费都是spu，这是公有的属性。

sku是指具体信息，比如华为mate40pro的具体又分很多颜色，不同颜色价格就不一样，本人的夏日胡杨就会贵500，配置可选8+256甚至12+512等等。

有些attr的属性一样可以在sku中引入，同一个sku可以有多种attr属性两者通过sku_sale_attr_value做多对多映射。

spu和sku则是一对多的关系，这点在sku_info表中可以看出。

总的来说，想要获取mate40pro 8+256 秋日胡杨的信息，可以从sku_info出发，根据spu_id去spu_info查询mate40pro的基本信息，然后根据catalog_id查询三级分类归属，拿着sku_id,可以获取具体的图片等等信息。

### 接口编写

接口文档地址：https://easydoc.net/doc/75716633/ZUqEdvA4/hKJTcbfd

纯接口的编写，没什么特别的。不再解析。
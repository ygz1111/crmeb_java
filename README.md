# CRMEB 开源商城系统 Java 版（已修改）

基于 CRMEB-Java-KY-v1.4 改造的开源电商解决方案。在原版基础上新增了 **二手交易模块**（含 AI 定价分析）并优化了移动端体验。

---

## 项目结构

```
crmeb_java/
├── crmeb/                          # Java 后端（SpringBoot + Mybatis-Plus）
│   ├── crmeb-common/               #   公共层
│   │   └── src/main/java/com/zbkj/common/
│   │       ├── model/              #     实体类
│   │       │   ├── product/        #       商品（StoreProduct 含二手/AI字段）
│   │       │   ├── order/          #       订单
│   │       │   ├── user/           #       用户
│   │       │   ├── bargain/        #       砍价
│   │       │   ├── combination/    #       拼团
│   │       │   ├── seckill/        #       秒杀
│   │       │   ├── coupon/         #       优惠券
│   │       │   ├── finance/        #       财务
│   │       │   ├── express/        #       快递/运费
│   │       │   ├── system/         #       系统配置
│   │       │   ├── wechat/         #       微信相关
│   │       │   ├── article/        #       文章
│   │       │   ├── category/       #       分类
│   │       │   ├── page/           #       DIY页面
│   │       │   ├── sms/            #       短信
│   │       │   └── record/         #       访问记录
│   │       ├── request/            #     请求参数
│   │       ├── response/           #     响应对象
│   │       ├── constants/          #     常量
│   │       ├── enums/              #     枚举
│   │       ├── exception/          #     异常
│   │       ├── config/             #     公共配置
│   │       ├── interceptor/        #     拦截器
│   │       ├── utils/              #     工具类
│   │       ├── annotation/         #     注解
│   │       ├── token/              #     JWT Token
│   │       └── vo/                 #     值对象
│   │
│   ├── crmeb-service/              #   业务层
│   │   ├── src/main/java/com/zbkj/service/
│   │   │   ├── service/            #     Service 接口
│   │   │   │   └── impl/           #     实现
│   │   │   ├── dao/                #     Mapper 接口
│   │   │   └── util/               #     工具（含易联云打印）
│   │   └── src/main/resources/mapper/  #   MyBatis XML
│   │       ├── store/              #     商品/订单 Mapper
│   │       ├── user/               #     用户 Mapper
│   │       ├── system/             #     系统 Mapper
│   │       ├── wechat/             #     微信 Mapper
│   │       ├── marketing/          #     营销 Mapper
│   │       ├── finance/            #     财务 Mapper
│   │       ├── express/            #     快递 Mapper
│   │       ├── article/            #     文章 Mapper
│   │       └── category/           #     分类 Mapper
│   │
│   ├── crmeb-admin/                #   管理后台 API
│   │   ├── src/main/java/com/zbkj/admin/
│   │   │   ├── controller/         #     后台 Controller
│   │   │   ├── service/            #     后台 Service
│   │   │   ├── config/             #     后台配置
│   │   │   ├── filter/             #     过滤器
│   │   │   ├── manager/            #     权限管理
│   │   │   ├── quartz/             #     定时任务
│   │   │   ├── task/               #     业务任务
│   │   │   └── util/               #     后台工具
│   │   └── src/main/resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-beta.yml
│   │       ├── application-prod.yml
│   │       └── logback-spring.xml
│   │
│   ├── crmeb-front/                #   移动端 API
│   │   ├── src/main/java/com/zbkj/front/
│   │   │   ├── controller/
│   │   │   │   ├── IndexController       - 首页
│   │   │   │   ├── LoginController       - 登录
│   │   │   │   ├── ProductController     - 商品
│   │   │   │   ├── CartController        - 购物车
│   │   │   │   ├── StoreOrderController  - 订单
│   │   │   │   ├── PayController         - 支付
│   │   │   │   ├── BargainController     - 砍价
│   │   │   │   ├── CombinationController - 拼团
│   │   │   │   ├── SecKillController     - 秒杀
│   │   │   │   ├── CouponController      - 优惠券
│   │   │   │   ├── UserController        - 用户
│   │   │   │   ├── UserAddressController - 地址
│   │   │   │   ├── UserSignController    - 签到
│   │   │   │   ├── UserCollectController - 收藏
│   │   │   │   ├── UserRechargeController- 充值
│   │   │   │   ├── SecondHandController  - ⭐ 二手交易
│   │   │   │   ├── ArticleController     - 文章
│   │   │   │   ├── StoreController       - 门店
│   │   │   │   ├── CityController        - 城市
│   │   │   │   ├── WeChatController      - 微信
│   │   │   │   ├── QrCodeController      - 二维码
│   │   │   │   ├── PageDiyController     - 页面装修
│   │   │   │   └── UploadFrontController - 上传
│   │   │   ├── service/
│   │   │   ├── interceptor/
│   │   │   └── filter/
│   │   └── src/main/resources/
│   │
│   ├── sql/
│   ├── shell/
│   ├── crmebimage/
│   └── pom.xml
│
├── admin/                          # 管理后台前端（Vue 2 + ElementUI）
│   ├── src/
│   ├── build/
│   ├── package.json
│   └── README.md
│
├── app/                            # 移动端（uni-app / Vue 2）
│   ├── pages/
│   │   ├── index/                  #     首页
│   │   ├── goods/                  #     商品
│   │   ├── goods_cate/             #     分类
│   │   ├── order_addcart/          #     购物车
│   │   ├── order/                  #     订单
│   │   ├── secondhand/             #     ⭐ 二手交易
│   │   ├── activity/               #     活动
│   │   ├── users/                  #     用户子包
│   │   ├── promoter/               #     分销
│   │   ├── news/                   #     资讯
│   │   ├── infos/                  #     个人资料
│   │   └── user/                   #     个人中心
│   ├── components/
│   ├── api/
│   ├── store/
│   ├── utils/
│   ├── config/
│   ├── pages.json
│   ├── manifest.json
│   └── package.json
│
├── crmeb_admin_log/
├── crmeb_front_log/
└── README.md
```

---

## 主要改动

### 新增：二手交易模块
- **移动端**：`pages/secondhand/` — 商品发布页 + 我的发布列表
- **后端 API**：`SecondHandController` — 发布/列表/删除接口
- **AI 分析**：发布时自动调用 AI 进行商品分类、建议售价预测
- **数据扩展**：`StoreProduct` 实体新增 `uid`、`itemCondition`、`aiPredictedPrice`、`aiCategory`、`aiConfidence`、`isSecondHand` 字段
- **TabBar**：底部导航新增"发布"入口

### 其他调整
- 更新依赖版本：Lombok 1.18.34、Maven 编译插件 3.13.0
- 数据库连接升级至 MySQL 8.0.33 驱动
- 修复部分配置兼容性问题

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | SpringBoot 2.2.6.RELEASE |
| ORM | Mybatis-Plus 3.3.1 |
| 数据库 | MySQL 5.7+（驱动 8.0.33） |
| 缓存 | Redis（Jedis） |
| 权限 | Spring Security |
| 管理后台 | Vue 2.x + ElementUI 2.13 |
| 移动端 | uni-app（Vue 2 + Vuex） |
| 构建工具 | Maven 3.6+ / npm 6+ |
| JDK | 1.8 |

---

## 运行环境要求

- JDK 1.8
- MySQL 5.7+
- Redis
- Maven 3.6+
- Node 14+ / npm 6+

---

## 模块说明

### crmeb — Java 后端
Maven 多模块项目，四个子模块：
- **crmeb-common**：实体类、枚举、工具、配置、异常
- **crmeb-service**：核心业务逻辑、DAO 层
- **crmeb-admin**：后台管理接口（端口 8080），含定时任务
- **crmeb-front**：移动端接口（端口 8081）

### admin — 管理后台前端
Vue CLI 构建，ElementUI 组件库，支持页面 DIY 装修、权限管理。

### app — 移动端（H5/小程序/App）
uni-app 项目，一套代码发布多端（H5 / 微信小程序 / App）。

---

## API 接口

移动端 API 前缀（通过 `app/config/app.js` 配置）：
- 普通接口：`/api/front/`
- 公开接口：`/api/public/`

默认开发地址：`http://localhost:20510`

---

## 项目资料

- CRMEB 原版文档：https://doc.crmeb.com/java/crmeb_java
- CRMEB 官网：https://www.crmeb.com
- 技术社区：https://www.crmeb.com/ask

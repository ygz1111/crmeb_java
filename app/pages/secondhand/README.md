# 智能二手发布功能使用说明

## 功能概述

这是一个基于 Vue 2 Options API 和 uni-app 开发的智能二手商品发布功能，集成了 AI 智能识别和估价功能。

## 主要功能

1. **AI 智能识别**：上传商品图片后，自动识别商品名称、分类、原价
2. **智能估价**：根据商品成色和分类，自动计算建议售价
3. **多图上传**：支持最多 5 张商品图片上传
4. **表单验证**：完整的表单验证和错误提示

## 文件结构

```
pages/secondhand/
├── publish.vue      # 发布页面（Vue 3 Composition API）
├── ai.js            # AI 识别服务
├── mylist.vue       # 我的发布列表
└── README.md        # 本说明文档
```

## 配置说明

### 1. AI 识别模式

在 `ai.js` 中，有两种模式：

- **模拟模式**（默认）：`USE_MOCK = true`
  - 无需后端支持，使用预设的商品数据进行模拟识别
  - 适合测试和演示

- **真实模式**：`USE_MOCK = false`
  - 需要部署 AI 识别后端服务
  - 后端 API：`POST /api/identify`
  - 请求格式：`{ "image": "base64_data_url" }`
  - 响应格式：
    ```json
    {
      "rawLabel": "iPhone 14 Pro 128G 深空黑",
      "confidence": "98.5%",
      "name": "iPhone 14 Pro 128G",
      "category": "数码电子",
      "suggestedOriginalPrice": 7999,
      "defaultDesc": "商品描述..."
    }
    ```

### 2. 商品分类

支持以下分类：
- 数码电子
- 图书教材
- 服饰鞋帽
- 日用百货
- 运动户外
- 其他闲置

### 3. 成色等级

支持以下成色等级：
- 99新（折损系数：0.85）
- 95新（折损系数：0.70）
- 90新（折损系数：0.55）
- 85新（折损系数：0.40）
- 70新（折损系数：0.20）

### 4. 交易地点

默认支持以下地点（可在代码中修改）：
- 宿舍西区
- 宿舍东区
- 教学综合楼
- 图书馆前广场
- 第一运动场
- 同校快递邮寄

## 使用流程

1. 点击底部导航栏的"发布"按钮
2. 上传商品图片（首图会自动触发 AI 识别）
3. 等待 AI 识别完成（模拟模式约 1.5 秒）
4. 查看识别结果，可手动修改商品信息
5. 填写商品标题、分类、成色、描述
6. 设置价格（可采纳 AI 建议售价）
7. 选择交易地点和是否包邮
8. 点击"发布"按钮提交

## 后端 API 要求

### 发布商品 API

- **路径**：`POST /api/front/secondhand/publish`
- **请求参数**：
  ```json
  {
    "storeName": "商品标题",
    "storeInfo": "商品描述",
    "category": "数码电子",
    "condition": "95新",
    "originalPrice": 7999,
    "price": 5599,
    "location": "宿舍西区",
    "isPost": false,
    "image": "图片URL",
    "sliderImage": "图片URL1,图片URL2",
    "itemCondition": "95新",
    "aiCategory": "数码电子",
    "aiPredictedPrice": 5599,
    "aiConfidence": "98.5%"
  }
  ```

### 图片上传 API

- **路径**：`POST /api/front/upload/image`
- **请求方式**：multipart/form-data
- **参数**：
  - `multipart`：图片文件
  - `model`：模型名称（默认 "product"）
  - `pid`：父级 ID（默认 1）

## 注意事项

1. **登录验证**：发布前需要先登录
2. **图片限制**：最多上传 5 张图片，首张图片用于 AI 识别
3. **描述长度**：商品描述至少 10 个字符
4. **价格验证**：售价必须大于 0

## 切换 AI 模式

如需使用真实 AI 识别服务，请修改 `ai.js` 中的配置：

```javascript
// 将此行改为 false
const USE_MOCK = false;
```

然后部署 AI 识别后端服务（参考 INTEGRATION_GUIDE.md）。

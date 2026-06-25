# CRMEB AI 识别服务

基于阿里通义千问 VL 多模态模型的商品图片识别服务。

## 快速开始

### 1. 安装依赖

```bash
npm install
```

### 2. 配置 API Key

在 `.env` 文件中设置你的 Qwen API Key：

```env
QWEN_API_KEY=sk-xxxxxxxxxxxxxxxx
```

**获取 API Key**：
1. 访问 [阿里云 DashScope](https://dashscope.console.aliyun.com/)
2. 开通通义千问服务
3. 创建 API Key

### 3. 启动服务

```bash
npm start
# 或开发模式（自动重启）
npm run dev
```

服务将在 `http://localhost:3000` 启动。

## API 接口

### POST /api/identify

识别商品图片

**请求**：
```json
{
  "image": "data:image/jpeg;base64,/9j/4AAQ..."
}
```

**响应**：
```json
{
  "rawLabel": "iPhone 14 Pro 128G 深空黑",
  "confidence": "98.5%",
  "name": "iPhone 14 Pro 128G",
  "category": "数码电子",
  "suggestedOriginalPrice": 7999,
  "defaultDesc": "自用一手爱机，成色极佳..."
}
```

### GET /health

健康检查

## 前端配置

在前端 `app/config/app.js` 中，确保 API 地址正确：

```javascript
let domain = 'http://localhost:3000'  // AI 服务地址
```

## 注意事项

1. **图片大小**：Base64 图片较大，请求体限制为 20MB
2. **API 限流**：Qwen API 有调用频率限制，请注意控制
3. **费用**：通义千问 VL 模型按调用次数计费，请关注用量

## 故障排除

### 识别失败

1. 检查 API Key 是否正确
2. 检查网络连接
3. 查看服务日志

### 服务无法启动

1. 检查端口 3000 是否被占用
2. 检查 Node.js 版本（需要 >= 14）

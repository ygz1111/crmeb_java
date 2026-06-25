# 协同过滤推荐算法服务

## 算法原理

基于用户行为的协同过滤推荐算法，通过分析用户的历史行为（收藏、点赞、购买、加入购物车），计算用户之间的相似度，为用户推荐其可能感兴趣的商品。

### 算法流程

1. **数据采集**：从数据库读取用户行为数据
   - 收藏（权重 3.0）
   - 点赞（权重 1.0）
   - 购买（权重 5.0）
   - 加入购物车（权重 2.0）

2. **构建用户-商品交互矩阵**：行为类型对应不同权重

3. **计算用户相似度**：使用余弦相似度算法

4. **生成推荐结果**：
   - 找到与目标用户最相似的 10 个用户
   - 计算这些用户喜欢但目标用户未接触的商品
   - 按得分排序返回 Top N

5. **冷启动处理**：当用户无行为数据时，返回热门商品

## API 接口

### 个性化推荐
```
GET /api/recommend?uid={用户ID}&n={数量}
```

### 相似商品推荐
```
GET /api/recommend/similar/{商品ID}?n={数量}
```

### 刷新缓存
```
POST /api/recommend/refresh
```

### 健康检查
```
GET /health
```

## 本地启动

```bash
# 方式一：使用启动脚本
chmod +x start.sh
./start.sh

# 方式二：手动启动
cd recommend-server
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python app.py
```

服务启动后运行在 http://localhost:5000

## Docker 部署

```bash
docker-compose up -d recommend-server
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| DB_HOST | 127.0.0.1 | 数据库主机 |
| DB_PORT | 3306 | 数据库端口 |
| DB_USER | root | 数据库用户 |
| DB_PASSWORD | crmeb123456 | 数据库密码 |
| DB_NAME | single_open | 数据库名 |
| RECOMMEND_PORT | 5000 | 服务端口 |

## 技术栈

- Python 3.9+
- Flask 3.0
- NumPy
- scikit-learn（余弦相似度计算）
- PyMySQL

# Pexels商品图片爬取工具

## 简介

本工具使用Pexels API下载高质量免费图片，用于丰富校园二手交易平台的商品展示。

## 使用步骤

### 1. 注册Pexels账号

1. 访问 https://www.pexels.com/api/
2. 点击 "Your API Key" 注册账号
3. 获取API Key

### 2. 设置API Key

**方式一：环境变量**
```bash
# Windows
set PEXELS_API_KEY=your_api_key_here

# Linux/Mac
export PEXELS_API_KEY=your_api_key_here
```

**方式二：修改脚本**
打开 `scraper.py`，修改第12行：
```python
API_KEY = "your_api_key_here"
```

### 3. 安装依赖

```bash
pip install -r requirements.txt
```

### 4. 运行爬取

```bash
python scraper.py
```

### 5. 查看结果

爬取完成后：
- `images/` 目录包含下载的图片
- `images/manifest.json` 包含图片清单
- `images/update_images.sql` 包含更新SQL

## 图片类别

| 目录 | 类别 | 预计数量 |
|------|------|----------|
| images/clothing/ | 服饰鞋帽 | 20张 |
| images/electronics/ | 数码电子 | 15张 |
| images/bags/ | 箱包饰品 | 10张 |
| images/beauty/ | 美妆个护 | 10张 |
| images/home/ | 家用电器 | 10张 |
| images/sports/ | 运动户外 | 10张 |

## 图片授权

Pexels图片遵循Pexels License：
- ✅ 免费使用
- ✅ 可商用
- ✅ 无需署名（但建议署名）

## 注意事项

- API免费计划每月200次请求
- 图片下载后建议调整为800x800尺寸
- 仅用于学习目的

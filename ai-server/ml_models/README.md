# 二手商品价格预测模型

## 简介

本模块使用机器学习算法（随机森林回归）训练二手商品价格预测模型。

## 文件结构

```
ml_models/
├── train_price_predictor.py    # 训练脚本
├── requirements.txt            # Python依赖
├── data/
│   └── secondhand_data.csv     # 训练数据
├── models/
│   └── price_model.pkl         # 训练好的模型
└── README.md                   # 说明文档
```

## 使用方法

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 运行训练

```bash
python train_price_predictor.py
```

### 3. 查看结果

训练完成后会生成：
- `data/secondhand_data.csv` - 训练数据（500条）
- `models/price_model.pkl` - 训练好的模型文件

## 模型说明

### 算法
- 随机森林回归（RandomForestRegressor）
- 100棵决策树

### 特征
- 物品品类（One-Hot编码）
- 物品状态（数值编码：几乎全新=4，轻微使用=3，明显使用=2，老旧=1）
- 原价

### 评估指标
- MAE（平均绝对误差）
- RMSE（均方根误差）
- R²（决定系数）

## 注意事项

本模型仅供展示使用。实际演示时，系统使用Java后端的公式预测逻辑，确保预测结果的稳定性和可控性。

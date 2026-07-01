"""
二手商品价格预测模型训练脚本
使用随机森林回归算法训练价格预测模型
"""

import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
import pickle
import os

# ========== 1. 配置参数 ==========
RANDOM_SEED = 42
DATA_SIZE = 500
MODEL_DIR = "models"
DATA_DIR = "data"

CATEGORIES = {
    "手机/数码": {"base_discount": 0.75, "price_range": (500, 5000)},
    "电脑/办公": {"base_discount": 0.70, "price_range": (300, 4000)},
    "服装/鞋帽": {"base_discount": 0.45, "price_range": (30, 800)},
    "箱包/饰品": {"base_discount": 0.50, "price_range": (50, 1500)},
    "美妆/护肤": {"base_discount": 0.55, "price_range": (20, 600)},
    "家用电器": {"base_discount": 0.65, "price_range": (100, 3000)},
    "图书/文具": {"base_discount": 0.40, "price_range": (5, 100)},
    "运动/户外": {"base_discount": 0.60, "price_range": (50, 1200)},
}

CONDITIONS = {
    "几乎全新": {"discount": 0.85, "label": 4},
    "轻微使用痕迹": {"discount": 0.70, "label": 3},
    "明显使用痕迹": {"discount": 0.55, "label": 2},
    "老旧/有瑕疵": {"discount": 0.35, "label": 1},
}

np.random.seed(RANDOM_SEED)

# ========== 2. 生成模拟训练数据 ==========
def generate_training_data(n_samples=500):
    print("=" * 60)
    print("第一步：生成模拟训练数据")
    print("=" * 60)
    
    data = []
    
    for _ in range(n_samples):
        category = np.random.choice(list(CATEGORIES.keys()))
        cat_info = CATEGORIES[category]
        
        condition = np.random.choice(list(CONDITIONS.keys()))
        cond_info = CONDITIONS[condition]
        
        original_price = np.random.uniform(*cat_info["price_range"])
        
        base_discount = cat_info["base_discount"]
        condition_discount = cond_info["discount"]
        noise = np.random.normal(0, 0.05)
        
        actual_price = original_price * base_discount * condition_discount * (1 + noise)
        actual_price = max(5, round(actual_price, 2))
        
        data.append({
            "category": category,
            "condition": condition,
            "original_price": round(original_price, 2),
            "actual_price": actual_price,
            "condition_label": cond_info["label"],
        })
    
    df = pd.DataFrame(data)
    
    os.makedirs(DATA_DIR, exist_ok=True)
    data_path = os.path.join(DATA_DIR, "secondhand_data.csv")
    df.to_csv(data_path, index=False, encoding="utf-8-sig")
    
    print(f"生成 {n_samples} 条训练数据")
    print(f"数据已保存到: {data_path}")
    print(f"\n数据统计：")
    print(f"  品类分布：{df['category'].value_counts().to_dict()}")
    print(f"  状态分布：{df['condition'].value_counts().to_dict()}")
    print(f"  原价范围：{df['original_price'].min():.0f} - {df['original_price'].max():.0f} 元")
    print(f"  成交价范围：{df['actual_price'].min():.0f} - {df['actual_price'].max():.0f} 元")
    
    return df

# ========== 3. 特征工程 ==========
def feature_engineering(df):
    print("\n" + "=" * 60)
    print("第二步：特征工程")
    print("=" * 60)
    
    category_dummies = pd.get_dummies(df["category"], prefix="cat")
    
    condition_encoded = df["condition_label"].values.reshape(-1, 1)
    original_price = df["original_price"].values.reshape(-1, 1)
    
    X = np.hstack([category_dummies.values, condition_encoded, original_price])
    feature_names = list(category_dummies.columns) + ["condition_label", "original_price"]
    
    y = df["actual_price"].values
    
    print(f"特征数量：{X.shape[1]}")
    print(f"特征名称：{feature_names}")
    print(f"样本数量：{X.shape[0]}")
    
    return X, y, feature_names

# ========== 4. 模型训练 ==========
def train_model(X, y, feature_names):
    print("\n" + "=" * 60)
    print("第三步：模型训练")
    print("=" * 60)
    
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_SEED
    )
    
    print(f"训练集：{X_train.shape[0]} 条")
    print(f"测试集：{X_test.shape[0]} 条")
    
    print("\n训练 RandomForestRegressor 模型...")
    rf_model = RandomForestRegressor(
        n_estimators=100,
        max_depth=10,
        min_samples_split=5,
        min_samples_leaf=2,
        random_state=RANDOM_SEED,
        n_jobs=-1
    )
    rf_model.fit(X_train, y_train)
    
    y_pred = rf_model.predict(X_test)
    
    mae = mean_absolute_error(y_test, y_pred)
    rmse = np.sqrt(mean_squared_error(y_test, y_pred))
    r2 = r2_score(y_test, y_pred)
    
    print("\n" + "=" * 60)
    print("模型评估结果")
    print("=" * 60)
    print(f"  MAE  (平均绝对误差)：{mae:.2f} 元")
    print(f"  RMSE (均方根误差)：{rmse:.2f} 元")
    print(f"  R2   (决定系数)：{r2:.4f}")
    
    importances = rf_model.feature_importances_
    print("\n特征重要性：")
    for name, importance in sorted(zip(feature_names, importances), key=lambda x: -x[1]):
        print(f"  {name}: {importance:.4f}")
    
    return rf_model, X_test, y_test, y_pred

# ========== 5. 保存模型 ==========
def save_model(model, feature_names):
    print("\n" + "=" * 60)
    print("第四步：保存模型")
    print("=" * 60)
    
    os.makedirs(MODEL_DIR, exist_ok=True)
    model_path = os.path.join(MODEL_DIR, "price_model.pkl")
    
    model_data = {
        "model": model,
        "feature_names": feature_names,
        "categories": list(CATEGORIES.keys()),
        "conditions": list(CONDITIONS.keys()),
    }
    
    with open(model_path, "wb") as f:
        pickle.dump(model_data, f)
    
    print(f"模型已保存到: {model_path}")
    print(f"模型大小: {os.path.getsize(model_path) / 1024:.2f} KB")

# ========== 6. 演示预测 ==========
def demo_prediction(model, feature_names):
    print("\n" + "=" * 60)
    print("演示：使用训练好的模型进行预测")
    print("=" * 60)
    
    test_cases = [
        {"category": "手机/数码", "condition": "几乎全新", "original_price": 3999},
        {"category": "服装/鞋帽", "condition": "明显使用痕迹", "original_price": 299},
        {"category": "电脑/办公", "condition": "轻微使用痕迹", "original_price": 5999},
        {"category": "图书/文具", "condition": "老旧/有瑕疵", "original_price": 45},
    ]
    
    for case in test_cases:
        features = encode_single_sample(case, feature_names)
        predicted_price = model.predict([features])[0]
        predicted_price = max(5, round(predicted_price, 2))
        
        print(f"\n  商品：{case['category']}")
        print(f"  状态：{case['condition']}")
        print(f"  原价：{case['original_price']} 元")
        print(f"  预测价：{predicted_price} 元")
        print(f"  折扣率：{predicted_price/case['original_price']*100:.1f}%")

def encode_single_sample(sample, feature_names):
    features = np.zeros(len(feature_names))
    
    cat_col = f"cat_{sample['category']}"
    if cat_col in feature_names:
        features[feature_names.index(cat_col)] = 1
    
    if "condition_label" in feature_names:
        features[feature_names.index("condition_label")] = CONDITIONS[sample["condition"]]["label"]
    
    if "original_price" in feature_names:
        features[feature_names.index("original_price")] = sample["original_price"]
    
    return features

# ========== 主函数 ==========
def main():
    print("\n" + "=" * 60)
    print("二手商品价格预测模型训练系统")
    print("=" * 60)
    
    df = generate_training_data(DATA_SIZE)
    X, y, feature_names = feature_engineering(df)
    model, X_test, y_test, y_pred = train_model(X, y, feature_names)
    save_model(model, feature_names)
    demo_prediction(model, feature_names)
    
    print("\n" + "=" * 60)
    print("训练完成！")
    print("=" * 60)
    print("\n文件清单：")
    print(f"  {DATA_DIR}/secondhand_data.csv  (训练数据)")
    print(f"  {MODEL_DIR}/price_model.pkl     (训练模型)")
    print("\n注意：实际演示时使用 Java 后端的公式预测，本模型仅供展示。")

if __name__ == "__main__":
    main()

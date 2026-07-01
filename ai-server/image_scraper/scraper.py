"""
Pexels图片爬取脚本
用于下载商品图片，丰富项目展示效果
"""

import os
import sys
import json
import requests
import time
from urllib.parse import quote

# 设置标准输出编码为UTF-8
sys.stdout.reconfigure(encoding='utf-8')

# ========== 配置 ==========
# Pexels API Key（需要注册获取：https://www.pexels.com/api/）
API_KEY = os.getenv("PEXELS_API_KEY", "YOUR_API_KEY_HERE")

# 基础URL
BASE_URL = "https://api.pexels.com/v1/search"

# 图片保存目录
IMAGES_DIR = "images"

# 搜索关键词配置（电商商品风格）
SEARCH_CONFIG = {
    "clothing": {
        "keywords": ["clothing product white background", "fashion dress isolated", "shoes product photo", "jacket product shot", "tshirt flat lay"],
        "count": 20,
        "description": "服饰鞋帽"
    },
    "electronics": {
        "keywords": ["laptop product white", "smartphone product shot", "headphones product photo", "tablet product isolated"],
        "count": 15,
        "description": "数码电子"
    },
    "bags": {
        "keywords": ["handbag product white background", "backpack product shot", "wallet product photo"],
        "count": 10,
        "description": "箱包饰品"
    },
    "beauty": {
        "keywords": ["cosmetics product white", "skincare bottle product", "perfume bottle product shot"],
        "count": 10,
        "description": "美妆个护"
    },
    "home": {
        "keywords": ["kitchen appliance product", "home appliance white background", "blender product shot"],
        "count": 10,
        "description": "家用电器"
    },
    "sports": {
        "keywords": ["sports shoes product", "yoga mat product shot", "fitness equipment product"],
        "count": 10,
        "description": "运动户外"
    }
}


def search_images(keyword, count=10):
    """搜索图片"""
    headers = {
        "Authorization": API_KEY
    }
    params = {
        "query": keyword,
        "per_page": min(count, 80),  # Pexels API限制每次最多80张
        "orientation": "portrait"  # 竖版图片更适合商品展示
    }
    
    try:
        response = requests.get(BASE_URL, headers=headers, params=params)
        response.raise_for_status()
        data = response.json()
        return data.get("photos", [])
    except requests.exceptions.RequestException as e:
        print(f"搜索失败: {e}")
        return []


def download_image(url, save_path):
    """下载图片"""
    try:
        response = requests.get(url, stream=True)
        response.raise_for_status()
        
        with open(save_path, "wb") as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
        
        return True
    except Exception as e:
        print(f"下载失败: {e}")
        return False


def scrape_category(category_name, config):
    """爬取单个类别的图片"""
    print(f"\n{'='*60}")
    print(f"爬取类别: {config['description']} ({category_name})")
    print(f"{'='*60}")
    
    category_dir = os.path.join(IMAGES_DIR, category_name)
    os.makedirs(category_dir, exist_ok=True)
    
    downloaded = []
    total_count = 0
    
    for keyword in config["keywords"]:
        if total_count >= config["count"]:
            break
        
        print(f"\n搜索关键词: {keyword}")
        photos = search_images(keyword, config["count"] - total_count)
        
        for i, photo in enumerate(photos):
            if total_count >= config["count"]:
                break
            
            # 获取图片URL（中等尺寸）
            img_url = photo.get("src", {}).get("medium", "")
            if not img_url:
                continue
            
            # 生成文件名
            filename = f"{category_name}_{total_count + 1:02d}.jpg"
            save_path = os.path.join(category_dir, filename)
            
            # 下载图片
            print(f"  下载: {filename}", end=" ")
            if download_image(img_url, save_path):
                print("OK")
                downloaded.append({
                    "filename": filename,
                    "path": f"crmebimage/public/product/{category_name}/{filename}",
                    "keyword": keyword,
                    "width": photo.get("width"),
                    "height": photo.get("height"),
                    "photographer": photo.get("photographer", "")
                })
                total_count += 1
            else:
                print("FAIL")
            
            # 避免请求过快
            time.sleep(0.5)
    
    print(f"\n类别 {config['description']} 完成: 下载 {len(downloaded)} 张图片")
    return downloaded


def generate_manifest(all_images):
    """生成图片清单"""
    manifest_path = os.path.join(IMAGES_DIR, "manifest.json")
    
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(all_images, f, ensure_ascii=False, indent=2)
    
    print(f"\n图片清单已保存到: {manifest_path}")


def generate_sql(all_images):
    """生成SQL更新语句"""
    print("\n" + "="*60)
    print("生成SQL更新语句")
    print("="*60)
    
    sql_lines = ["-- 商品图片更新SQL", ""]
    
    # 按类别分组
    by_category = {}
    for img in all_images:
        cat = img["filename"].split("_")[0]
        if cat not in by_category:
            by_category[cat] = []
        by_category[cat].append(img)
    
    for cat, images in by_category.items():
        sql_lines.append(f"-- {cat} 类别")
        for img in images:
            sql_lines.append(f"-- UPDATE eb_store_product SET image = '{img['path']}' WHERE id = XXX;")
    
    sql_path = os.path.join(IMAGES_DIR, "update_images.sql")
    with open(sql_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sql_lines))
    
    print(f"SQL文件已保存到: {sql_path}")


def main():
    print("="*60)
    print("Pexels商品图片爬取工具")
    print("="*60)
    
    # 检查API Key
    if API_KEY == "YOUR_API_KEY_HERE":
        print("\n错误: 请设置Pexels API Key")
        print("1. 访问 https://www.pexels.com/api/ 注册账号")
        print("2. 获取API Key")
        print("3. 设置环境变量: set PEXELS_API_KEY=your_api_key")
        print("4. 或者修改脚本中的API_KEY变量")
        return
    
    all_images = []
    
    # 爬取每个类别
    for category_name, config in SEARCH_CONFIG.items():
        images = scrape_category(category_name, config)
        all_images.extend(images)
    
    # 生成清单和SQL
    generate_manifest(all_images)
    generate_sql(all_images)
    
    print("\n" + "="*60)
    print("爬取完成！")
    print("="*60)
    print(f"总计下载: {len(all_images)} 张图片")
    print(f"保存位置: {IMAGES_DIR}/")
    print("\n下一步:")
    print("1. 查看 images/ 目录中的图片")
    print("2. 根据 update_images.sql 更新数据库")


if __name__ == "__main__":
    main()

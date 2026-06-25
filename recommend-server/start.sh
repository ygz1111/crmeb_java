#!/bin/bash
# 协同过滤推荐算法服务 - 本地启动脚本

echo "========================================="
echo " 推荐算法服务启动"
echo "========================================="

cd recommend-server

# 检查 Python 环境
if ! command -v python &> /dev/null && ! command -v python3 &> /dev/null; then
    echo "错误：Python 未安装"
    exit 1
fi

# 检查并安装依赖
if [ ! -d "venv" ]; then
    echo "创建虚拟环境..."
    python -m venv venv 2>/dev/null || python3 -m venv venv
fi

echo "安装依赖..."
source venv/Scripts/activate 2>/dev/null || source venv/bin/activate
pip install -r requirements.txt -q

echo ""
echo "启动推荐服务..."
python app.py

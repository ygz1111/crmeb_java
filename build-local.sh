#!/bin/bash
# 本地构建脚本 - 在 Windows (Git Bash) 上执行
# 构建 Java JAR + Vue 前端 + uni-app H5，产物供 Docker 镜像使用

set -e

echo "========================================="
echo " CRMEB 本地构建脚本"
echo "========================================="

# ---- 1. 构建 Java 后端 ----
echo ""
echo "[1/3] 构建 Java 后端 JAR 包..."
cd crmeb
if command -v mvn &> /dev/null; then
    mvn clean package -DskipTests
else
    echo "Maven 未安装，尝试使用 mvnw..."
    if [ -f mvnw ]; then
        chmod +x mvnw
        ./mvnw clean package -DskipTests
    else
        echo "错误：Maven 未安装且 mvnw 不存在"
        exit 1
    fi
fi

if [ -f crmeb-admin/target/Crmeb-admin.jar ] && [ -f crmeb-front/target/Crmeb-front.jar ]; then
    echo "JAR 包构建成功"
    echo "  crmeb-admin/target/Crmeb-admin.jar ($(du -h crmeb-admin/target/Crmeb-admin.jar | cut -f1))"
    echo "  crmeb-front/target/Crmeb-front.jar ($(du -h crmeb-front/target/Crmeb-front.jar | cut -f1))"
else
    echo "错误：JAR 包构建失败"
    exit 1
fi
cd ..

# ---- 2. 构建管理后台前端 ----
echo ""
echo "[2/3] 构建管理后台前端..."
cd admin
if [ ! -d node_modules ]; then
    echo "安装依赖..."
    npm install
fi
npm run build:prod

if [ -d dist ]; then
    echo "管理后台构建成功 (dist/)"
else
    echo "错误：管理后台构建失败"
    exit 1
fi
cd ..

# ---- 3. 构建 uni-app H5 ----
echo ""
echo "[3/3] 构建 uni-app H5..."
cd app
if [ ! -d node_modules ]; then
    echo "安装依赖..."
    npm install
fi
npm run build:h5

if [ -d unpackage/dist/build/h5 ]; then
    echo "H5 构建成功 (unpackage/dist/build/h5/)"
else
    echo "错误：H5 构建失败"
    exit 1
fi
cd ..

echo ""
echo "========================================="
echo " 所有构建完成！"
echo "========================================="
echo ""
echo "下一步："
echo "  1. 将整个项目上传到服务器："
echo "     scp -r . root@你的服务器IP:/home/crmeb"
echo ""
echo "  2. 在服务器上执行一键部署："
echo "     cd /home/crmeb && chmod +x deploy.sh && ./deploy.sh"

#!/bin/bash
set -e

echo "========================================="
echo " CRMEB Java Docker 一键部署脚本"
echo "========================================="

# ---- 1. 检查并安装 Docker ----
if ! command -v docker &> /dev/null; then
    echo "[1/6] Docker 未安装，正在安装..."
    if command -v yum &> /dev/null; then
        yum install -y yum-utils
        yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
        yum install -y docker-ce docker-ce-cli containerd.io
    elif command -v apt-get &> /dev/null; then
        apt-get update
        apt-get install -y ca-certificates curl gnupg
        install -m 0755 -d /etc/apt/keyrings
        curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
        chmod a+r /etc/apt/keyrings/docker.gpg
        echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
        apt-get update
        apt-get install -y docker-ce docker-ce-cli containerd.io
    else
        curl -fsSL https://get.docker.com | sh
    fi
    systemctl start docker
    systemctl enable docker
    echo "Docker 安装完成"
else
    echo "[1/6] Docker 已安装: $(docker --version)"
fi

# ---- 2. 检查并安装 Docker Compose ----
if ! command -v docker-compose &> /dev/null; then
    echo "[2/6] Docker Compose 未安装，正在安装..."
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
    ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
    echo "Docker Compose 安装完成"
else
    echo "[2/6] Docker Compose 已安装: $(docker-compose --version)"
fi

# ---- 3. 创建 swap（1GB 内存必须）----
SWAP_SIZE=$(free -m | awk '/^Swap:/ {print $2}')
if [ "$SWAP_SIZE" -lt 1024 ]; then
    if [ ! -f /swapfile ]; then
        echo "[3/6] 创建 2GB swap..."
        fallocate -l 2G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=2048
        chmod 600 /swapfile
        mkswap /swapfile
        swapon /swapfile
        if ! grep -q swapfile /etc/fstab; then
            echo '/swapfile none swap sw 0 0' >> /etc/fstab
        fi
        echo "Swap 创建完成"
    else
        swapon /swapfile 2>/dev/null || true
        echo "[3/6] Swap 已存在并启用"
    fi
else
    echo "[3/6] Swap 已有 ${SWAP_SIZE}MB，跳过"
fi

# ---- 4. 配置 Docker 日志限制 ----
if [ ! -f /etc/docker/daemon.json ]; then
    echo "[4/6] 配置 Docker 日志限制..."
    mkdir -p /etc/docker
    cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
EOF
    systemctl restart docker
else
    echo "[4/6] Docker daemon.json 已存在，跳过"
fi

# ---- 5. 停止旧容器 ----
echo "[5/6] 停止旧容器..."
docker-compose down 2>/dev/null || true

# ---- 6. 构建并启动 ----
echo "[6/6] 构建并启动所有服务（首次可能需要几分钟拉取镜像）..."
docker-compose up -d --build

# ---- 等待 MySQL 就绪 ----
echo ""
echo "等待 MySQL 初始化（首次可能需要1-2分钟）..."
RETRY=0
MAX_RETRY=30
until docker exec crmeb-mysql mysqladmin ping -h localhost -u root -pcrmeb123456 --silent 2>/dev/null; do
    RETRY=$((RETRY+1))
    if [ $RETRY -ge $MAX_RETRY ]; then
        echo "MySQL 启动超时，请检查日志: docker logs crmeb-mysql"
        exit 1
    fi
    sleep 3
    echo "  等待中... ($RETRY/$MAX_RETRY)"
done
echo "MySQL 已就绪"

# 重启依赖 MySQL 的服务（确保数据库初始化完成后再连接）
echo "重启应用服务..."
sleep 5
docker-compose restart crmeb-admin crmeb-front

# 等待应用启动
echo "等待应用启动（约30秒）..."
sleep 30

# ---- 显示结果 ----
SERVER_IP=$(hostname -I | awk '{print $1}')
echo ""
echo "========================================="
echo " 部署完成！"
echo "========================================="
echo ""
echo "访问地址："
echo "  管理后台: http://${SERVER_IP}/admin/"
echo "  移动端H5: http://${SERVER_IP}/h5/"
echo "  后台API:  http://${SERVER_IP}/api/admin/"
echo "  移动端API: http://${SERVER_IP}/api/front/"
echo ""
echo "常用命令："
echo "  查看状态: docker-compose ps"
echo "  查看日志: docker-compose logs -f"
echo "  停止服务: docker-compose down"
echo "  重启服务: docker-compose restart"
echo "  查看内存: docker stats --no-stream"
echo ""

# 显示容器状态
docker-compose ps

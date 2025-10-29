#!/bin/bash

# ====================================
# Tool Dashboard - 镜像导出脚本
# ====================================
# 功能: 在开发环境打包所有Docker镜像
# 用途: 离线部署到内网环境
# ====================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# 目标架构配置
TARGET_PLATFORM="linux/amd64"
TARGET_ARCH="x86_64"

# 输出目录
OUTPUT_DIR="./docker-images"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

echo ""
echo "╔═══════════════════════════════════════╗"
echo "║   Tool Dashboard - 镜像导出工具     ║"
echo "╚═══════════════════════════════════════╝"
echo ""

print_info "导出时间: $(date '+%Y-%m-%d %H:%M:%S')"
print_info "输出目录: $OUTPUT_DIR"
print_info "目标架构: $TARGET_PLATFORM ($TARGET_ARCH)"
print_info "当前系统: $(uname -m)"
echo ""

# 检查是否需要跨平台构建
CURRENT_ARCH=$(uname -m)
if [ "$CURRENT_ARCH" != "x86_64" ] && [ "$CURRENT_ARCH" != "amd64" ]; then
    print_warn "当前系统架构为 $CURRENT_ARCH，将构建 x86_64 架构的镜像"
    print_warn "这需要 Docker Buildx 支持，可能需要较长时间"
    echo ""
fi

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

# 检查Docker
if ! command -v docker &> /dev/null; then
    print_error "Docker未安装！"
    exit 1
fi

# 构建镜像
print_step "[1/5] 构建应用镜像..."
echo ""

print_info "构建后端镜像 (平台: $TARGET_PLATFORM)..."
cd tool-dashboard-backend
docker build --platform "$TARGET_PLATFORM" -t tool-dashboard-backend:latest .
cd ..
print_info "✓ 后端镜像构建完成"

# 验证后端镜像架构
BACKEND_ARCH=$(docker image inspect tool-dashboard-backend:latest --format '{{.Architecture}}')
if [ "$BACKEND_ARCH" = "amd64" ]; then
    print_info "  架构验证: $BACKEND_ARCH ✓"
else
    print_warn "  架构验证: $BACKEND_ARCH (预期: amd64)"
fi

print_info "构建前端镜像 (平台: $TARGET_PLATFORM)..."
cd tool-dashboard-frontend
docker build --platform "$TARGET_PLATFORM" -t tool-dashboard-frontend:latest .
cd ..
print_info "✓ 前端镜像构建完成"

# 验证前端镜像架构
FRONTEND_ARCH=$(docker image inspect tool-dashboard-frontend:latest --format '{{.Architecture}}')
if [ "$FRONTEND_ARCH" = "amd64" ]; then
    print_info "  架构验证: $FRONTEND_ARCH ✓"
else
    print_warn "  架构验证: $FRONTEND_ARCH (预期: amd64)"
fi

# 跳过基础镜像拉取（MySQL 等由 docker-compose 在目标服务器自动拉取）
print_step "[2/5] 跳过基础镜像..."
echo ""
print_info "注意：MySQL 等基础镜像将在目标服务器通过 docker-compose 自动拉取"
print_info "      仅导出前后端应用镜像"

# 获取镜像列表
print_step "[3/5] 准备导出镜像..."
echo ""

ALL_IMAGES=(
    "tool-dashboard-backend:latest"
    "tool-dashboard-frontend:latest"
)

# 筛选存在的镜像并验证架构
EXISTING_IMAGES=()
print_info "检查镜像是否存在并验证架构："
for img in "${ALL_IMAGES[@]}"; do
    if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${img}$"; then
        IMG_ARCH=$(docker image inspect "$img" --format '{{.Architecture}}' 2>/dev/null || echo "unknown")
        if [ "$IMG_ARCH" = "amd64" ]; then
            echo "   ✓ $img [架构: $IMG_ARCH]"
            EXISTING_IMAGES+=("$img")
        else
            print_warn "   ⚠ $img [架构: $IMG_ARCH] - 非amd64架构，仍将导出"
            EXISTING_IMAGES+=("$img")
        fi
    else
        echo "   ✗ $img (不存在，将跳过)"
    fi
done

if [ ${#EXISTING_IMAGES[@]} -eq 0 ]; then
    print_error "没有可导出的镜像！"
    exit 1
fi

# 导出镜像
print_step "[4/5] 导出镜像..."
echo ""

EXPORT_FILE="$OUTPUT_DIR/tool-dashboard-images_${TIMESTAMP}.tar"

print_info "正在导出 ${#EXISTING_IMAGES[@]} 个镜像到 $EXPORT_FILE ..."
print_info "这可能需要几分钟，请耐心等待..."
echo ""

# 前后端镜像都是本地构建的，直接使用标签导出即可
print_info "导出镜像列表："
for img in "${EXISTING_IMAGES[@]}"; do
    IMAGE_ID=$(docker image inspect "$img" --format '{{.Id}}' 2>/dev/null | cut -d: -f2 | cut -c1-12)
    print_info "  ✓ $img (ID: $IMAGE_ID)"
done

echo ""
print_info "开始导出..."
docker save -o "$EXPORT_FILE" "${EXISTING_IMAGES[@]}"

if [ -f "$EXPORT_FILE" ]; then
    FILE_SIZE=$(du -h "$EXPORT_FILE" | cut -f1)
    print_info "✓ 镜像导出成功！大小: $FILE_SIZE"
    print_info "  目标架构: $TARGET_ARCH ($TARGET_PLATFORM)"
    print_info "  包含镜像: ${#EXISTING_IMAGES[@]} 个"
else
    print_error "镜像导出失败！"
    exit 1
fi

# 压缩镜像文件
print_step "[5/5] 压缩镜像文件..."
echo ""

print_info "正在压缩镜像文件（可能需要几分钟）..."
gzip "$EXPORT_FILE"

COMPRESSED_FILE="${EXPORT_FILE}.gz"
COMPRESSED_SIZE=$(du -h "$COMPRESSED_FILE" | cut -f1)

print_info "✓ 压缩完成！大小: $COMPRESSED_SIZE"

# 复制部署文件
print_info "复制部署文件..."
cp docker-compose.yml "$OUTPUT_DIR/" 2>/dev/null || true
cp .env.example "$OUTPUT_DIR/" 2>/dev/null || true
cp deploy.sh "$OUTPUT_DIR/" 2>/dev/null || true
cp backup.sh "$OUTPUT_DIR/" 2>/dev/null || true
cp health-check.sh "$OUTPUT_DIR/" 2>/dev/null || true
cp DEPLOYMENT_GUIDE.md "$OUTPUT_DIR/" 2>/dev/null || true

# 创建导入脚本
cat > "$OUTPUT_DIR/import-images.sh" << 'IMPORT_EOF'
#!/bin/bash

# Tool Dashboard - 镜像导入脚本

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo ""
echo "╔═══════════════════════════════════════╗"
echo "║   Tool Dashboard - 镜像导入工具     ║"
echo "╚═══════════════════════════════════════╝"
echo ""

# 查找镜像文件
IMAGE_FILE=$(ls tool-dashboard-images_*.tar.gz 2>/dev/null | head -1)

if [ -z "$IMAGE_FILE" ]; then
    echo -e "${RED}错误: 未找到镜像文件！${NC}"
    exit 1
fi

echo -e "${GREEN}[INFO]${NC} 找到镜像文件: $IMAGE_FILE"
echo -e "${GREEN}[INFO]${NC} 文件大小: $(du -h "$IMAGE_FILE" | cut -f1)"
echo ""

# 解压
echo -e "${GREEN}[INFO]${NC} 解压镜像文件..."
if ! gunzip -k "$IMAGE_FILE"; then
    echo -e "${RED}错误: 解压失败！${NC}"
    exit 1
fi

TAR_FILE="${IMAGE_FILE%.gz}"

# 导入镜像
echo -e "${GREEN}[INFO]${NC} 导入Docker镜像..."
echo -e "${YELLOW}[WARN]${NC} 这可能需要几分钟，请耐心等待..."
echo ""

if ! docker load -i "$TAR_FILE"; then
    echo -e "${RED}错误: 镜像导入失败！${NC}"
    rm -f "$TAR_FILE"
    exit 1
fi

# 清理解压的tar文件
rm -f "$TAR_FILE"

echo ""
echo -e "${GREEN}[INFO]${NC} ✓ 镜像导入完成！"
echo ""
echo -e "${GREEN}[INFO]${NC} 已导入的镜像（架构验证）："
echo -e "${GREEN}[INFO]${NC} ----------------------------------------"
for img in tool-dashboard-backend:latest tool-dashboard-frontend:latest; do
    if docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${img}$"; then
        ARCH=$(docker image inspect "$img" --format '{{.Architecture}}' 2>/dev/null || echo "unknown")
        OS=$(docker image inspect "$img" --format '{{.Os}}' 2>/dev/null || echo "unknown")
        echo -e "  ✓ $img"
        echo -e "    平台: $OS/$ARCH"
    fi
done
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "下一步:"
echo "  1. 配置环境: cp .env.example .env && vi .env"
echo "  2. 初始化: chmod +x *.sh && ./deploy.sh init"
echo "  3. 启动: ./deploy.sh start"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
IMPORT_EOF

chmod +x "$OUTPUT_DIR/import-images.sh"

# 创建离线部署说明
cat > "$OUTPUT_DIR/OFFLINE_DEPLOYMENT.md" << 'README_EOF'
# Tool Dashboard - 离线部署指南

## 📦 部署包内容

- `tool-dashboard-images_*.tar.gz` - Docker镜像包（x86_64/amd64 架构）
- `import-images.sh` - 镜像导入脚本
- `docker-compose.yml` - Docker编排配置
- `deploy.sh` - 部署管理脚本
- `backup.sh` - 数据备份脚本
- `health-check.sh` - 健康检查脚本
- `.env.example` - 环境变量模板
- `DEPLOYMENT_GUIDE.md` - 详细部署文档

**⚠️ 重要**: 此镜像包专为 **x86_64/amd64 架构** 构建，适用于：
- Intel/AMD 处理器的服务器
- x86_64 架构的 Linux 系统
- 不适用于 ARM 架构（如 ARM 服务器、树莓派等）

**📦 打包内容**: 
- 包含前后端应用镜像（本地构建）
- MySQL 等基础镜像需在目标服务器通过 docker-compose 自动拉取
- 确保目标服务器可访问 Docker Hub 或配置国内镜像源

## �� 快速开始

### 1. 传输文件

将整个 `docker-images` 目录传输到内网服务器：

```bash
# 方式1: SCP
scp -r docker-images/ user@server:/opt/tool-dashboard/

# 方式2: U盘/移动硬盘
# 直接复制 docker-images 文件夹

# 方式3: 内网文件服务器
# 上传后在目标服务器下载
```

### 2. 导入镜像

```bash
cd /opt/tool-dashboard/docker-images

# 赋予执行权限
chmod +x *.sh

# 导入Docker镜像（需要几分钟）
./import-images.sh
```

### 3. 配置环境

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置（必须修改密码和密钥！）
vi .env
```

**⚠️ 必须修改的配置：**
```bash
MYSQL_ROOT_PASSWORD=修改为强密码
MYSQL_PASSWORD=修改为强密码
JWT_SECRET=修改为至少256位的随机字符串
```

**📌 配置 Docker 镜像源（可选，国内服务器建议配置）：**

如果目标服务器在国内，建议配置 Docker 镜像加速器以提高 MySQL 等基础镜像的拉取速度：

```bash
# 编辑 Docker daemon 配置
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://mirror.ccs.tencentyun.com"
  ]
}
EOF

# 重启 Docker 服务
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 4. 初始化并启动

```bash
# 初始化项目
./deploy.sh init

# 启动所有服务
./deploy.sh start
```

### 5. 访问系统

```
前端: http://服务器IP
后端API: http://服务器IP:8080/api

默认管理员账号:
- 用户名: admin  
- 密码: admin123

⚠️ 首次登录后请立即修改密码！
```

## 📋 系统要求

### 硬件要求
- CPU: 2核心+
- 内存: 4GB+
- 磁盘: 20GB+

### 软件要求
- 操作系统: Linux (CentOS 7+/Ubuntu 18+)
- Docker: 20.10+
- Docker Compose: 2.0+

### 端口要求
- 80: 前端服务
- 8080: 后端API
- 3306: MySQL（仅容器内部）

## 🔧 常用命令

```bash
# 查看服务状态
./deploy.sh status

# 查看实时日志
./deploy.sh logs

# 健康检查
./deploy.sh health

# 重启服务
./deploy.sh restart

# 备份数据
./deploy.sh backup
```

## ⚠️ 注意事项

1. **防火墙配置**
   ```bash
   # CentOS/RHEL
   firewall-cmd --permanent --add-port=80/tcp
   firewall-cmd --permanent --add-port=8080/tcp
   firewall-cmd --reload
   
   # Ubuntu
   ufw allow 80/tcp
   ufw allow 8080/tcp
   ```

2. **SELinux配置（CentOS）**
   ```bash
   # 临时关闭
   setenforce 0
   
   # 永久关闭（重启后生效）
   sed -i 's/SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config
   ```

3. **时间同步**
   ```bash
   # 安装ntp
   yum install -y ntp
   systemctl start ntpd
   systemctl enable ntpd
   ```

## 🔍 故障排查

### 问题1: 端口被占用

```bash
# 查看端口占用
netstat -tulnp | grep -E '80|8080|3306'

# 修改端口（编辑 .env）
FRONTEND_PORT=8000
BACKEND_PORT=9090
```

### 问题2: Docker服务未启动

```bash
# 启动Docker
systemctl start docker
systemctl enable docker

# 检查状态
systemctl status docker
```

### 问题3: 镜像导入失败

```bash
# 检查磁盘空间
df -h

# 检查Docker存储
docker system df

# 清理无用镜像
docker system prune -a
```

### 问题4: 服务无法启动

```bash
# 查看详细日志
./deploy.sh logs

# 检查容器状态
docker ps -a

# 重新部署
./deploy.sh stop
./deploy.sh start
```

## 📊 监控与维护

### 定期备份

```bash
# 手动备份
./deploy.sh backup

# 配置定时任务
crontab -e
# 添加：每天凌晨3点备份
0 3 * * * cd /opt/tool-dashboard/docker-images && ./deploy.sh backup
```

### 健康检查

```bash
# 定期检查系统健康
./deploy.sh health

# 查看资源使用
docker stats
```

### 日志管理

```bash
# 查看日志
./deploy.sh logs

# 清理旧日志
docker system prune -f
```

## 🆙 升级部署

当有新版本时：

1. 在开发环境重新执行 `./export-images.sh`
2. 传输新的镜像包到服务器
3. 备份数据：`./deploy.sh backup`
4. 导入新镜像：`./import-images.sh`
5. 重启服务：`./deploy.sh restart`

## �� 技术支持

如遇到问题：

1. 查看日志：`./deploy.sh logs`
2. 健康检查：`./deploy.sh health`
3. 查看文档：`DEPLOYMENT_GUIDE.md`
4. 检查配置：`cat .env`

---

**部署包版本**: 1.0  
**生成时间**: 请查看文件时间戳  
**适用环境**: 内网离线部署
README_EOF

# 生成文件清单
cat > "$OUTPUT_DIR/manifest.txt" << EOF
Tool Dashboard 离线部署包
生成时间: $(date '+%Y-%m-%d %H:%M:%S')
=====================================

文件列表:
EOF

ls -lh "$OUTPUT_DIR" >> "$OUTPUT_DIR/manifest.txt"

# 完成
echo ""
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_info "✓ 导出完成！"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📦 离线部署包信息:"
echo "   目录: $OUTPUT_DIR"
echo "   镜像: $(basename "$COMPRESSED_FILE") ($COMPRESSED_SIZE)"
echo "   架构: $TARGET_ARCH ($TARGET_PLATFORM)"
echo "   镜像数: ${#EXISTING_IMAGES[@]} 个"
echo ""
echo "📋 包含文件:"
ls -lh "$OUTPUT_DIR" | tail -n +2 | awk '{printf "   %s (%s)\n", $9, $5}'
echo ""
echo "🚀 传输到内网服务器:"
echo "   scp -r $OUTPUT_DIR user@server:/opt/tool-dashboard/"
echo ""
echo "🔧 在内网服务器执行:"
echo "   cd /opt/tool-dashboard/$OUTPUT_DIR"
echo "   ./import-images.sh"
echo "   cp .env.example .env && vi .env"
echo "   ./deploy.sh init && ./deploy.sh start"
echo ""
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

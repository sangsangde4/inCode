#!/bin/bash

# ====================================
# Tool Dashboard - 镜像重建脚本
# ====================================
# 用于清理旧镜像并重新构建 x86 架构镜像

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

print_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

echo ""
echo "╔═══════════════════════════════════════╗"
echo "║   Tool Dashboard - 镜像重建工具     ║"
echo "╚═══════════════════════════════════════╝"
echo ""

print_info "当前系统: $(uname -m)"
print_info "目标架构: linux/amd64 (x86_64)"
echo ""

print_warn "此操作将执行以下步骤："
echo "  1. 停止所有运行的容器"
echo "  2. 删除旧的应用镜像"
echo "  3. 重新构建 x86 架构镜像"
echo ""

read -p "是否继续? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warn "已取消"
    exit 0
fi

# 停止容器
print_info "[1/3] 停止运行的容器..."
docker-compose down 2>/dev/null || print_warn "没有运行的容器"

# 删除旧镜像
print_info "[2/3] 删除旧的应用镜像..."
docker rmi tool-dashboard-backend:latest 2>/dev/null && print_info "✓ 已删除后端镜像" || print_warn "后端镜像不存在"
docker rmi tool-dashboard-frontend:latest 2>/dev/null && print_info "✓ 已删除前端镜像" || print_warn "前端镜像不存在"

# 重新构建
print_info "[3/3] 重新构建 x86 架构镜像..."
echo ""

print_info "构建后端镜像 (linux/amd64)..."
cd tool-dashboard-backend
docker build --platform linux/amd64 -t tool-dashboard-backend:latest .
cd ..
print_info "✓ 后端镜像构建完成"

print_info "构建前端镜像 (linux/amd64)..."
cd tool-dashboard-frontend
docker build --platform linux/amd64 -t tool-dashboard-frontend:latest .
cd ..
print_info "✓ 前端镜像构建完成"

# 验证镜像
echo ""
print_info "验证构建的镜像："
docker images | grep -E "tool-dashboard|REPOSITORY"

echo ""
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_info "✓ 镜像重建完成！"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
print_info "下一步操作："
echo "   启动服务: ./deploy.sh start"
echo "   导出镜像: ./export-images.sh"
echo ""

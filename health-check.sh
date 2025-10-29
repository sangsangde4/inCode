#!/bin/bash

# ====================================
# Tool Dashboard - 健康检查脚本 v2.0
# ====================================
# 功能: 全面检查系统健康状态
# 作者: Tool Dashboard Team
# ====================================

set -o pipefail

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 打印函数
print_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_ok() { echo -e "${GREEN}✓${NC} $1"; }
print_fail() { echo -e "${RED}✗${NC} $1"; }

# 加载配置
if [ -f .env ]; then
    source .env
fi

# 初始化状态
ALL_HEALTHY=true

echo ""
echo "╔═══════════════════════════════════════╗"
echo "║   Tool Dashboard - 健康检查         ║"
echo "╚═══════════════════════════════════════╝"
echo ""
print_info "检查时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 1. 检查Docker服务
echo "🐳 [1/6] 检查Docker服务..."
if docker info &> /dev/null; then
  DOCKER_VERSION=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo "unknown")
  print_ok "Docker服务运行正常 (v$DOCKER_VERSION)"
else
  print_fail "Docker服务未运行"
  ALL_HEALTHY=false
fi

# 2. 检查容器状态
echo ""
echo "📦 [2/6] 检查容器状态..."
CONTAINERS=("tool-dashboard-mysql" "tool-dashboard-backend" "tool-dashboard-frontend")

for container in "${CONTAINERS[@]}"; do
  if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
    # 获取容器健康状态
    HEALTH=$(docker inspect -f '{{.State.Health.Status}}' "$container" 2>/dev/null || echo "none")
    STATUS=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null)
    UPTIME=$(docker inspect -f '{{.State.StartedAt}}' "$container" 2>/dev/null | xargs -I {} date -d {} +%s 2>/dev/null || echo 0)
    NOW=$(date +%s)
    DURATION=$((NOW - UPTIME))
    
    if [ "$HEALTH" = "healthy" ] || [ "$HEALTH" = "none" ] && [ "$STATUS" = "running" ]; then
      print_ok "$container: $STATUS $([ "$HEALTH" != "none" ] && echo "($HEALTH)") - 运行 $(($DURATION / 3600))h $(($DURATION % 3600 / 60))m"
    else
      print_fail "$container: $STATUS $([ "$HEALTH" != "none" ] && echo "($HEALTH)")"
      ALL_HEALTHY=false
    fi
  else
    print_fail "$container: 未运行"
    ALL_HEALTHY=false
  fi
done

# 3. 检查MySQL连接
echo ""
echo "💾 [3/6] 检查MySQL数据库..."
if docker exec tool-dashboard-mysql mysqladmin ping -h localhost -uroot -p"${MYSQL_ROOT_PASSWORD:-root123456}" --silent 2>/dev/null; then
  # 获取MySQL版本和数据库大小
  MYSQL_VERSION=$(docker exec tool-dashboard-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root123456}" -e "SELECT VERSION();" 2>/dev/null | tail -1)
  DB_SIZE=$(docker exec tool-dashboard-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root123456}" -e "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 2) as Size_MB FROM information_schema.TABLES WHERE table_schema = '${MYSQL_DATABASE:-tool_dashboard}';" 2>/dev/null | tail -1)
  print_ok "MySQL连接正常 (v$MYSQL_VERSION, 数据库: ${DB_SIZE}MB)"
else
  print_fail "MySQL连接失败"
  ALL_HEALTHY=false
fi

# 4. 检查后端API
echo ""
echo "🔌 [4/6] 检查后端API..."
BACKEND_URL="http://localhost:${BACKEND_PORT:-8080}/api/actuator/health"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL" 2>/dev/null)
if [ "$RESPONSE" = "200" ]; then
  # 获取API响应时间
  RESPONSE_TIME=$(curl -s -o /dev/null -w "%{time_total}" "$BACKEND_URL" 2>/dev/null)
  print_ok "后端API响应正常 (${RESPONSE_TIME}s)"
else
  print_fail "后端API响应异常 (HTTP $RESPONSE)"
  ALL_HEALTHY=false
fi

# 5. 检查前端服务
echo ""
echo "🌐 [5/6] 检查前端服务..."
FRONTEND_URL="http://localhost:${FRONTEND_PORT:-80}"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$FRONTEND_URL" 2>/dev/null)
if [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "304" ]; then
  print_ok "前端服务响应正常 (HTTP $RESPONSE)"
else
  print_fail "前端服务响应异常 (HTTP $RESPONSE)"
  ALL_HEALTHY=false
fi

# 6. 资源使用情况
echo ""
echo "📊 [6/6] 资源使用检查..."
echo ""
printf "%-30s %-15s %-20s %-15s\n" "容器名称" "CPU使用率" "内存使用" "网络 I/O"
echo "$(printf '%.0s-' {1..80})"
docker stats --no-stream --format "{{.Name}}	{{.CPUPerc}}	{{.MemUsage}}	{{.NetIO}}" | while read name cpu mem net; do
  printf "%-30s %-15s %-20s %-15s\n" "$name" "$cpu" "$mem" "$net"
done

# 磁盘使用
echo ""
echo "💾 磁盘使用情况:"
if [ -d "./data" ]; then
  echo "   数据目录: $(du -sh ./data 2>/dev/null | cut -f1)"
  du -sh ./data/* 2>/dev/null | awk '{printf "   - %s: %s\n", $2, $1}'
fi
if [ -d "./backups" ]; then
  BACKUP_COUNT=$(find ./backups -type f 2>/dev/null | wc -l)
  echo "   备份目录: $(du -sh ./backups 2>/dev/null | cut -f1) ($BACKUP_COUNT 个文件)"
fi

# 总结
echo ""
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
if [ "$ALL_HEALTHY" = true ]; then
  print_info "🎉 系统状态: 健康"
  print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  exit 0
else
  print_error "⚠️  系统状态: 异常"
  print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo "🔧 建议操作:"
  echo "   1. 查看日志: ./deploy.sh logs"
  echo "   2. 重启服务: ./deploy.sh restart"
  echo "   3. 检查配置: cat .env"
  echo "   4. 重新部署: ./deploy.sh start"
  echo ""
  exit 1
fi

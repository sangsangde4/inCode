#!/bin/bash

# ====================================
# Tool Dashboard - 备份脚本 v2.0
# ====================================
# 功能: 自动备份MySQL数据和文件
# 作者: Tool Dashboard Team
# ====================================

set -e  # 遇到错误立即退出
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
print_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

# 加载配置
if [ -f .env ]; then
    source .env
fi

# 设置变量
BACKUP_DIR="${BACKUP_DIR:-./backups}"
DATE=$(date +%Y%m%d_%H%M%S)
MYSQL_CONTAINER="tool-dashboard-mysql"
MYSQL_DB="${MYSQL_DATABASE:-tool_dashboard}"
MYSQL_PASSWORD="${MYSQL_ROOT_PASSWORD:-root123456}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"

# 创建备份目录
mkdir -p "$BACKUP_DIR"

# 检查容器是否运行
if ! docker ps | grep -q "$MYSQL_CONTAINER"; then
    print_error "MySQL容器未运行！"
    exit 1
fi

echo ""
echo "╔═══════════════════════════════════════╗"
echo "║   Tool Dashboard - 数据备份         ║"
echo "╚═══════════════════════════════════════╝"
echo ""
print_info "备份开始时间: $(date '+%Y-%m-%d %H:%M:%S')"
print_info "备份目录: $BACKUP_DIR"
echo ""

# 1. 备份MySQL数据库
print_step "[1/3] 备份MySQL数据库..."

MYSQL_BACKUP="$BACKUP_DIR/mysql_${DATE}.sql"

if docker exec "$MYSQL_CONTAINER" mysqldump \
  -uroot -p"$MYSQL_PASSWORD" \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  --set-gtid-purged=OFF \
  "$MYSQL_DB" > "$MYSQL_BACKUP" 2>/dev/null; then
  
  print_info "✓ 数据库导出成功"
  
  # 压缩SQL文件
  if gzip "$MYSQL_BACKUP"; then
    MYSQL_SIZE=$(du -h "${MYSQL_BACKUP}.gz" | cut -f1)
    print_info "✓ 已压缩: mysql_${DATE}.sql.gz ($MYSQL_SIZE)"
  else
    print_warn "压缩失败，保留原文件"
  fi
else
  print_error "MySQL备份失败！"
  exit 1
fi

# 2. 备份上传文件
print_step "[2/3] 备份上传文件..."

UPLOADS_BACKUP="$BACKUP_DIR/uploads_${DATE}.tar.gz"

# 尝试从容器中备份
if docker exec tool-dashboard-backend test -d /app/uploads 2>/dev/null; then
  if docker run --rm \
    -v tool-dashboard_backend_uploads:/data:ro \
    -v "$(pwd)/$BACKUP_DIR":/backup \
    alpine \
    tar -czf "/backup/uploads_${DATE}.tar.gz" -C /data . 2>/dev/null; then
    
    UPLOADS_SIZE=$(du -h "$UPLOADS_BACKUP" | cut -f1)
    print_info "✓ 文件备份成功: uploads_${DATE}.tar.gz ($UPLOADS_SIZE)"
  else
    print_warn "文件备份失败"
  fi
else
  print_warn "上传目录不存在，跳过文件备份"
fi

# 3. 清理旧备份
print_step "[3/3] 清理旧备份..."

OLD_BACKUPS=$(find "$BACKUP_DIR" -name "*.sql.gz" -mtime +"$RETENTION_DAYS" -o -name "*.tar.gz" -mtime +"$RETENTION_DAYS")

if [ -n "$OLD_BACKUPS" ]; then
  DELETED_COUNT=$(echo "$OLD_BACKUPS" | wc -l)
  find "$BACKUP_DIR" -name "*.sql.gz" -mtime +"$RETENTION_DAYS" -delete
  find "$BACKUP_DIR" -name "*.tar.gz" -mtime +"$RETENTION_DAYS" -delete
  print_info "✓ 已清理 $DELETED_COUNT 个 $RETENTION_DAYS 天前的旧备份"
else
  print_info "✓ 无需清理旧备份"
fi

# 备份完成总结
echo ""
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
print_info "✓ 备份完成！"
print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "📊 备份统计："
echo "   备份目录: $BACKUP_DIR"
echo "   总大小: $(du -sh "$BACKUP_DIR" | cut -f1)"
echo "   保留天数: $RETENTION_DAYS 天"
echo ""
echo "📁 最新备份："
ls -lht "$BACKUP_DIR" | head -6 | tail -5 | awk '{printf "   %s %s %s\n", $9, $5, $6" "$7" "$8}'

echo ""
echo "🔒 恢复命令："
echo "   MySQL: docker exec -i $MYSQL_CONTAINER mysql -uroot -p密码 $MYSQL_DB < $BACKUP_DIR/mysql_${DATE}.sql.gz"
echo "   文件: tar -xzf $BACKUP_DIR/uploads_${DATE}.tar.gz -C /path/to/restore"

print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

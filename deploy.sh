#!/bin/bash

# ====================================
# Tool Dashboard - 部署管理脚本
# ====================================
# 功能: 一键部署、管理和维护
# 作者: Tool Dashboard Team
# 版本: 2.0

# ====================================

set -e  # 遇到错误立即退出
set -o pipefail  # 管道命令错误也退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的信息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查系统要求
check_requirements() {
    print_info "检查系统要求..."
    
    # 检查Docker
    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装"
        print_info "请访问: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    # 检查Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose 未安装"
        print_info "请访问: https://docs.docker.com/compose/install/"
        exit 1
    fi
    
    # 检查Docker服务状态
    if ! docker info &> /dev/null; then
        print_error "Docker 服务未运行"
        print_info "请启动Docker服务: sudo systemctl start docker"
        exit 1
    fi
    
    # 检查磁盘空间
    local available=$(df -BG . | tail -1 | awk '{print $4}' | sed 's/G//')
    if [ "$available" -lt 5 ]; then
        print_warn "磁盘可用空间不足 5GB，当前: ${available}GB"
    fi
    
    # 检查内存
    local total_mem=$(free -g | awk '/^Mem:/{print $2}')
    if [ "$total_mem" -lt 2 ]; then
        print_warn "系统内存不足 2GB，当前: ${total_mem}GB"
    fi
    
    print_info "✓ Docker: $(docker --version | awk '{print $3}')"
    print_info "✓ Docker Compose: $(docker-compose --version 2>/dev/null | awk '{print $3}' || echo 'v2')"
    print_info "✓ 磁盘空间: ${available}GB"
    print_info "✓ 内存: ${total_mem}GB"
}

# 检查 .env 文件
check_env_file() {
    if [ ! -f .env ]; then
        print_warn ".env 文件不存在，正在创建..."
        if [ -f .env.example ]; then
            cp .env.example .env
            print_info "已从 .env.example 创建 .env 文件"
            print_warn "请编辑 .env 文件，修改数据库密码和 JWT 密钥"
            print_warn "修改完成后，请重新运行此脚本"
            exit 0
        else
            print_error ".env.example 文件不存在"
            exit 1
        fi
    fi
}

# 安全加载.env（支持中文注释、去除BOM、防止set -e退出）
load_env () {
    if [ ! -f ".env" ]; then
        print_warn ".env 文件不存在"
        return
    fi

    print_info "加载环境变量..."
    # 去掉 utf-8 bom 并只读取 key=value 格式的行
    while IFS='=' read -r key value; do
        # 跳过注释行和空行
        [[ "$key" =~ ^#.*$ || -z "$key" ]] && continue
        # 去掉可能的引号
        value="${value%\"}"
        value="${value#\"}"
        value="${value%\'}"
        value="${value#\'}"
        # 导出环境变量
        export "$key"="$value"
    done < <(sed 's/^\xEF\xBB\xBF//' .env)
    
    print_info "环境变量加载完成"
}

# 启动服务
start() {
    print_info "启动 Tool Dashboard 服务..."
    check_requirements
    check_env_file
    
    # 加载环境变量
    load_env
    
    # 检查镜像是否存在
    print_info "检查镜像..."
    local missing_images=()
    
    if ! docker images | grep -q "tool-dashboard-backend"; then
        missing_images+=("tool-dashboard-backend:latest")
    fi
    
    if ! docker images | grep -q "tool-dashboard-frontend"; then
        missing_images+=("tool-dashboard-frontend:latest")
    fi
    
    if [ ${#missing_images[@]} -gt 0 ]; then
        print_warn "缺少以下镜像:"
        for img in "${missing_images[@]}"; do
            echo "   - $img"
        done
        echo ""
        print_info "请选择:"
        echo "   1. 运行 ./export-images.sh 构建并导出镜像"
        echo "   2. 如果已有镜像包，运行 ./import-images.sh 导入镜像"
        echo "   3. 手动构建: docker build -t tool-dashboard-backend:latest ./tool-dashboard-backend"
        exit 1
    fi
    
    print_info "✓ 所有镜像已就绪"
    
    print_info "启动容器..."
    docker-compose up -d --remove-orphans
    
    print_info "等待服务启动..."
    local max_wait=60
    local wait_time=0
    
    while [ $wait_time -lt $max_wait ]; do
        if docker-compose ps | grep -q "Up"; then
            break
        fi
        sleep 2
        wait_time=$((wait_time + 2))
        echo -n "."
    done
    echo ""
    
    # 健康检查
    print_info "执行健康检查..."
    sleep 5
    ./health-check.sh || print_warn "健康检查发现问题，请查看日志"
    
    echo ""
    print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    print_info "✓ 服务启动完成！"
    print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    print_info "📱 访问地址："
    echo "   前端: http://localhost:${FRONTEND_PORT:-80}"
    echo "   后端: http://localhost:${BACKEND_PORT:-8080}/api"
    echo ""
    print_info "📊 常用命令："
    echo "   查看日志: ./deploy.sh logs"
    echo "   查看状态: ./deploy.sh status"
    echo "   健康检查: ./deploy.sh health"
    print_info "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# 停止服务
stop() {
    print_info "停止 Tool Dashboard 服务..."
    docker-compose down
    print_info "✓ 服务已停止"
}

# 重启服务
restart() {
    print_info "重启 Tool Dashboard 服务..."
    docker-compose restart
    print_info "✓ 服务已重启"
}

# 查看日志
logs() {
    docker-compose logs -f
}

# 查看状态
status() {
    print_info "Tool Dashboard 服务状态："
    docker-compose ps
    echo ""
    print_info "资源使用情况："
    docker stats --no-stream $(docker-compose ps -q)
}

# 更新服务
update() {
    print_info "更新 Tool Dashboard 服务..."
    print_warn "更新功能需要重新构建镜像"
    print_info "请执行以下步骤:"
    echo "   1. 停止服务: ./deploy.sh stop"
    echo "   2. 重新构建: ./export-images.sh"
    echo "   3. 启动服务: ./deploy.sh start"
    exit 0
}

# 备份数据
backup() {
    print_info "备份数据..."
    BACKUP_DIR="./backups"
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    
    mkdir -p $BACKUP_DIR
    
    print_info "备份 MySQL 数据库..."
    docker-compose exec -T mysql mysqldump -uroot -p${MYSQL_ROOT_PASSWORD:-root123456} tool_dashboard > "$BACKUP_DIR/db_backup_$TIMESTAMP.sql"
    
    print_info "备份上传文件..."
    docker run --rm -v tool-dashboard_backend_uploads:/data -v $(pwd)/$BACKUP_DIR:/backup alpine tar czf /backup/uploads_backup_$TIMESTAMP.tar.gz -C /data .
    
    print_info "✓ 备份完成，保存在 $BACKUP_DIR 目录"
}

# 清理资源
clean() {
    print_warn "此操作将删除所有容器、镜像和数据卷"
    read -p "确定要继续吗？(yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        print_info "操作已取消"
        exit 0
    fi
    
    print_info "清理资源..."
    docker-compose down -v
    docker system prune -a -f
    print_info "✓ 清理完成"
}

# 健康检查
health() {
    if [ -x ./health-check.sh ]; then
        ./health-check.sh
    else
        print_error "health-check.sh 不存在或无执行权限"
        exit 1
    fi
}

# 初始化
init() {
    print_info "初始化 Tool Dashboard..."
    
    # 创建必要目录
    print_info "创建必要目录..."
    mkdir -p data/{mysql,uploads,logs} backups
    
    # 设置权限
    chmod 755 data backups
    chmod 755 *.sh 2>/dev/null || true
    
    # 创建.env文件
    if [ ! -f .env ]; then
        if [ -f .env.example ]; then
            cp .env.example .env
            print_info "✓ 已创建 .env 文件"
            print_warn "请修改 .env 中的密码和密钥！"
        fi
    fi
    
    print_info "✓ 初始化完成"
}

# 显示帮助信息
show_help() {
    cat << EOF
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tool Dashboard - 部署管理脚本 v2.0
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

使用方法: ./deploy.sh [命令]

📦 基础命令：
  init     - 初始化项目（首次部署必须）
  start    - 启动所有服务
  stop     - 停止所有服务
  restart  - 重启所有服务
  status   - 查看服务状态

📊 运维命令：
  logs     - 查看实时日志
  health   - 健康检查
  backup   - 备份数据
  update   - 更新服务
  clean    - 清理资源（危险）

📝 示例：
  ./deploy.sh init      # 首次部署初始化
  ./deploy.sh start     # 启动服务
  ./deploy.sh logs      # 查看日志
  ./deploy.sh health    # 健康检查

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
EOF
}

# 主函数
main() {
    # 显示banner
    if [ "${1:-}" != "logs" ]; then
        echo ""
        echo "╔═══════════════════════════════════════╗"
        echo "║   Tool Dashboard - 部署管理脚本 v2.0  ║"
        echo "╚═══════════════════════════════════════╝"
        echo ""
    fi
    
    case "${1:-}" in
        init)
            init
            ;;
        start)
            start
            ;;
        stop)
            stop
            ;;
        restart)
            restart
            ;;
        logs)
            logs
            ;;
        status)
            status
            ;;
        health)
            health
            ;;
        update)
            update
            ;;
        backup)
            backup
            ;;
        clean)
            clean
            ;;
        help|--help|-h|"")
            show_help
            ;;
        *)
            print_error "未知命令: $1"
            echo ""
            show_help
            exit 1
            ;;
    esac
}

main "$@"

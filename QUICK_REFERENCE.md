# 离线部署快速参考

## 🚀 一句话总结

**开发环境打包 → 传输文件 → 内网导入 → 启动服务**

---

## 📦 开发环境（有网络）

```bash
# 一键打包
./export-images.sh

# 输出: docker-images/ 目录
```

---

## 📤 传输文件

```bash
# U盘
cp -r docker-images /media/usb/

# 或 SCP
scp -r docker-images/ user@server:/opt/tool-dashboard/
```

---

## 🌐 内网环境（无网络）

```bash
cd /opt/tool-dashboard/docker-images

# 1. 导入镜像
./import-images.sh

# 2. 配置环境
cp .env.example .env
vi .env  # 修改密码

# 3. 启动
./deploy.sh init
./deploy.sh start
```

---

## ✅ 验证

```
访问: http://服务器IP
账号: admin / admin123
```

---

## 🔧 常用命令

| 操作 | 命令 |
|------|------|
| 查看状态 | `./deploy.sh status` |
| 查看日志 | `./deploy.sh logs` |
| 健康检查 | `./deploy.sh health` |
| 重启服务 | `./deploy.sh restart` |
| 备份数据 | `./deploy.sh backup` |

---

## ⚠️ 必改配置 (.env)

```bash
MYSQL_ROOT_PASSWORD=改成强密码
MYSQL_PASSWORD=改成强密码  
JWT_SECRET=改成256位随机字符串
```

---

## 🆘 问题解决

| 问题 | 解决方法 |
|------|---------|
| 端口占用 | 修改 `.env` 中的端口 |
| 空间不足 | `docker system prune -a` |
| 防火墙 | `firewall-cmd --add-port=80/tcp` |
| 权限问题 | `chmod +x *.sh` |

---

详细文档: `OFFLINE_DEPLOYMENT_GUIDE.md`

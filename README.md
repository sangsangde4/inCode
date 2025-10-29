# 工具看板系统

统一的可视化看板系统，用于展示和管理部门内多个工具与平台的信息，支持文件下载、历史版本查看、变更日志等功能。

## 项目结构

```
inCode/
├── tool-dashboard-backend/    # 后端项目（Spring Boot）
└── tool-dashboard-frontend/   # 前端项目（Vue 3 + TypeScript）
```

## 技术栈

### 后端
- **框架**: Spring Boot 3.1.5
- **持久层**: MyBatis-Plus 3.5.4
- **数据库**: MySQL 8.0+
- **认证**: JWT
- **构建工具**: Maven

### 前端
- **框架**: Vue 3.3+
- **语言**: TypeScript
- **UI库**: Element Plus 2.4+
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **构建工具**: Vite 4

## 功能特性

### 普通用户功能（无需登录）
- ✅ 浏览工具看板，查看所有工具/平台信息
- ✅ 搜索和筛选工具
- ✅ 查看工具详情（版本、负责人、状态等）
- ✅ 下载工具相关文件
- ✅ 查看历史版本列表
- ✅ 查看变更日志

### 管理员功能（需要登录）
- ✅ 管理员登录认证（JWT Token）
- ✅ 工具/平台的增删改查
- ✅ 上传和管理工具文件
- ✅ 维护变更日志
- ✅ 查看下载统计
- 🆕 **完善的权限控制系统**
- 🆕 **基于注解的权限验证**

## 快速开始

### 环境要求

#### 后端
- JDK 17+
- Maven 3.6+
- MySQL 8.0+

#### 前端
- Node.js 16+
- npm 或 yarn

### 后端启动步骤

1. **创建数据库**
```bash
cd tool-dashboard-backend
# 执行数据库初始化脚本
mysql -u root -p < src/main/resources/db/schema.sql
```

2. **修改配置文件**
编辑 `src/main/resources/application.yml`，修改数据库连接信息：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tool_dashboard
    username: your_username
    password: your_password
```

3. **启动后端服务**
```bash
# 使用Maven启动
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/tool-dashboard-1.0.0.jar
```

后端服务将在 `http://localhost:8080/api` 启动

### 前端启动步骤

1. **安装依赖**
```bash
cd tool-dashboard-frontend
npm install
```

2. **启动开发服务器**
```bash
npm run dev
```

前端服务将在 `http://localhost:3000` 启动

3. **构建生产版本**
```bash
npm run build
```

## 默认管理员账号

- **用户名**: admin
- **密码**: admin123

⚠️ **重要**: 首次部署后请立即修改默认管理员密码

## 数据库表结构

### tb_tool - 工具表
存储工具/平台的基本信息

### tb_tool_file - 工具文件表
存储工具相关的文件信息和下载记录

### tb_change_log - 变更日志表
记录工具的版本变更历史

### tb_admin - 管理员表
管理员账号信息

## 📋 文档

- **[API 接口文档](./API_DOCUMENTATION.md)** - 完整的API接口说明
- **[优化报告](./OPTIMIZATION_REPORT.md)** - 项目优化详细说明
- **[变更日志](./CHANGELOG.md)** - 版本更新记录

## API文档

> 详细的API文档请参考 [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)

### 公开接口（无需认证）

#### 工具相关
- `GET /api/tools/list` - 获取所有工具列表
- `GET /api/tools/page` - 分页查询工具列表
- `GET /api/tools/{id}` - 获取工具详情

#### 文件相关
- `GET /api/files/tool/{toolId}` - 获取工具的文件列表
- `GET /api/files/download/{id}` - 下载文件

#### 日志相关
- `GET /api/changelogs/tool/{toolId}` - 获取工具的变更日志

### 管理员接口（需要认证）

#### 认证
- `POST /api/auth/login` - 管理员登录

#### 工具管理
- `POST /api/tools` - 新增工具
- `PUT /api/tools/{id}` - 更新工具
- `DELETE /api/tools/{id}` - 删除工具

#### 文件管理
- `POST /api/files/upload` - 上传文件
- `DELETE /api/files/{id}` - 删除文件

#### 日志管理
- `POST /api/changelogs` - 新增变更日志
- `PUT /api/changelogs/{id}` - 更新变更日志
- `DELETE /api/changelogs/{id}` - 删除变更日志

## 配置说明

### 文件上传配置

后端配置文件中可以修改文件上传路径：
```yaml
file:
  upload-path: /data/tool-dashboard/uploads  # 文件存储路径
  access-url: /files                          # 文件访问URL前缀
```

### JWT配置

```yaml
jwt:
  secret: your-secret-key-change-this-in-production  # JWT密钥（生产环境必须修改）
  expiration: 86400000  # Token过期时间（毫秒）
```

### 日志配置

```yaml
logging:
  level:
    root: INFO
    com.company.tooldashboard: DEBUG  # 开发环境使用DEBUG，生产环境建议使用INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

## 部署建议

### 后端部署
1. 使用 Maven 打包生成 jar 文件
2. 配置 systemd 或使用 Docker 部署
3. 配置 Nginx 反向代理
4. 确保文件上传目录有正确的读写权限

### 前端部署
1. 执行 `npm run build` 生成生产版本
2. 将 `dist` 目录部署到 Nginx 或其他 Web 服务器
3. 配置代理转发 API 请求到后端服务

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态文件
    location / {
        root /path/to/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 开发说明

### 添加新功能
1. 后端：在相应的 Controller、Service、Mapper 中添加代码
2. 前端：在 `src/api` 中添加 API 调用，在 `src/views` 中添加页面组件

### 添加需要权限的接口
后端只需在Controller方法上添加 `@RequireAdmin` 注解：
```java
@RequireAdmin
@PostMapping("/your-endpoint")
public Result<Void> yourMethod() {
    // 只有登录的管理员才能访问
}
```

### 代码规范
- 后端遵循阿里巴巴Java开发规范
- 前端使用 TypeScript 严格模式，遵循 Vue 3 Composition API 风格
- 使用 SLF4J 进行日志记录，避免使用 System.out.println

## 常见问题

### Q: 前端出现跨域错误？
A: 确保后端已配置 CORS，或者在开发环境使用 Vite 的代理配置。

### Q: 文件上传失败？
A: 检查文件上传目录权限，确保应用有读写权限。

### Q: 无法登录？
A: 检查数据库中是否有管理员账号，密码是否正确（默认密码的MD5值）。

### Q: 接口返回401未授权？
A: 检查Token是否存在且有效，是否已正确登录。管理接口需要登录后访问。

### Q: TypeScript 报错？
A: 运行 `npm install` 安装依赖后，TypeScript 错误会自动解决。

## 🎉 最新更新 (v1.1.0)

- ✨ 新增完善的权限控制系统
- 🔒 所有管理操作接口现已受到保护
- 🧹 清理冗余代码和Mock数据
- 📝 优化日志输出，使用标准SLF4J
- 📚 完善项目文档

详细变更请查看 [CHANGELOG.md](./CHANGELOG.md)

## 许可证

MIT License

## 联系方式

如有问题，请联系项目维护团队。

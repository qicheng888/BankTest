# Bank Transaction Management System

## 1. 项目背景

本项目是一个银行交易管理系统，用于记录、查看和管理金融交易。系统提供 RESTful API 接口和 Web 界面，支持交易的增删改查操作。

### 核心功能

| 功能 | 描述 |
|------|------|
| **交易管理** | 创建、查询、更新、删除交易记录 |
| **分页查询** | 支持高效的分页查询，按时间倒序排列 |
| **重复检测** | 自动检测重复交易（相同金额、类型、类别、描述） |
| **缓存优化** | 使用 Caffeine 高性能缓存减少数据库访问 |
| **Web 界面** | 提供响应式 Web 界面管理交易 |

---

## 2. 架构设计

### 2.1 技术栈

| 组件 | 技术 | 说明 |
|------|------|------|
| 框架 | Spring Boot 3.2.2 | Java 21 |
| 数据库 | H2 (内存模式) | 通过 MyBatis 操作 |
| 缓存 | Caffeine | 高性能本地缓存 |
| API 文档 | SpringDoc OpenAPI | Swagger UI |
| 模板引擎 | Thymeleaf | Web 界面 |
| 容器化 | Docker | 支持 docker-compose |

### 2.2 分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Controller Layer                        │
│              REST API + Web Page Controllers                 │
├─────────────────────────────────────────────────────────────┤
│                       Service Layer                          │
│           业务逻辑 + 缓存管理 + 事务控制                        │
├─────────────────────────────────────────────────────────────┤
│                     Repository Layer                         │
│                   数据访问抽象层                              │
├─────────────────────────────────────────────────────────────┤
│                      MyBatis Mapper                          │
│                    SQL 映射与执行                             │
├─────────────────────────────────────────────────────────────┤
│                       H2 Database                            │
│                      内存数据库                               │
└─────────────────────────────────────────────────────────────┘
```

### 2.3 目录结构

```
src/main/java/com/bank/transaction/
├── controller/         # REST API 控制器
├── service/            # 业务逻辑层（含缓存和事务）
├── repository/         # 数据访问层
├── mapper/             # MyBatis Mapper 接口
├── entity/             # 实体类
├── dto/                # 请求/响应 DTO (Record)
├── enums/              # 枚举类型
├── exception/          # 异常处理
└── config/             # 配置类

src/main/resources/
├── schema.sql          # 数据库表结构
├── application.yml     # 应用配置
├── templates/          # Thymeleaf 模板
└── static/             # 静态资源
```

---

## 3. 核心组件设计

### 3.1 缓存设计

使用 **Caffeine** 实现两级缓存，确保高性能与数据一致性：

| 缓存名称 | 用途 | 最大容量 | 过期时间 | 说明 |
|----------|------|----------|----------|------|
| `transactions` | 单条交易缓存 | 1000 | 300秒 | 按 ID 缓存，命中率高 |
| `transactionList` | 分页列表缓存 | 100 | 60秒 | 短 TTL 保证数据新鲜度 |

**缓存 Key 设计：**

| 缓存名称 | Key 格式 | 示例 | 说明 |
|----------|----------|------|------|
| `transactions` | `#id` (交易 UUID) | `a1b2c3d4-e5f6-...` | 每条交易独立缓存 |
| `transactionList` | `'page_' + #page + '_size_' + #size` | `page_0_size_10` | 按分页参数缓存 |

```java
// 单条交易缓存 - Key 为交易 ID
@Cacheable(value = "transactions", key = "#id")
public TransactionResponse getTransaction(String id) { ... }

// 分页列表缓存 - Key 为 "page_{页码}_size_{每页条数}"
@Cacheable(value = "transactionList", key = "'page_' + #page + '_size_' + #size")
public PageResponse<TransactionResponse> getAllTransactions(int page, int size) { ... }
```

> **说明：** 分页列表缓存使用 `page` 和 `size` 组合作为 Key，这意味着：
> - `GET /api/transactions?page=0&size=10` → 缓存 Key: `page_0_size_10`
> - `GET /api/transactions?page=1&size=10` → 缓存 Key: `page_1_size_10`
> - `GET /api/transactions?page=0&size=20` → 缓存 Key: `page_0_size_20`
> 
> 当发生任何写操作（创建/更新/删除）时，所有 `transactionList` 缓存会被全部清空（`allEntries = true`），确保分页查询返回最新数据。

**缓存一致性策略：**

| 操作 | 缓存行为 |
|------|----------|
| 创建 | `@CachePut` 写入单条缓存 + `@CacheEvict` 清空列表缓存 |
| 查询 | `@Cacheable` 优先读缓存，未命中则查库 |
| 更新 | `@CachePut` 更新单条缓存 + `@CacheEvict` 清空列表缓存 |
| 删除 | `@CacheEvict` 清空单条缓存 + 清空列表缓存 |


### 3.2 事务设计

使用 `@Transactional` 注解确保数据库操作的原子性，并保证缓存操作在事务提交后执行：

```java
@Transactional                    // 写操作包装在事务中
@Transactional(readOnly = true)   // 读操作使用只读事务优化
```

### 3.3 缓存与数据库更新顺序

本项目采用 **Cache Aside（旁路缓存）** 模式，这是业界最常用的缓存一致性方案：

**写操作（创建/更新/删除）执行顺序：**

```
┌─────────────────────────────────────────────────────────────┐
│  1. Spring AOP 拦截方法调用                                   │
│                    ↓                                         │
│  2. 开启数据库事务 (@Transactional)                           │
│                    ↓                                         │
│  3. 执行业务逻辑（参数校验、重复检测等）                         │
│                    ↓                                         │
│  4. 执行数据库操作（MyBatis INSERT/UPDATE/DELETE）            │
│                    ↓                                         │
│  5. 事务提交成功                                              │
│                    ↓                                         │
│  6. 执行缓存操作 (@CachePut / @CacheEvict)                    │
│                    ↓                                         │
│  7. 返回结果                                                  │
└─────────────────────────────────────────────────────────────┘
```

**读操作（查询）执行顺序：**

```
┌─────────────────────────────────────────────────────────────┐
│  1. 检查缓存是否命中 (@Cacheable)                             │
│                    ↓                                         │
│  ┌─────────────┬─────────────────────────────────────────┐  │
│  │  缓存命中    │  → 直接返回缓存数据                       │  │
│  ├─────────────┼─────────────────────────────────────────┤  │
│  │  缓存未命中  │  → 查询数据库 → 写入缓存 → 返回数据        │  │
│  └─────────────┴─────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

**为什么选择「先更新数据库，再更新缓存」？**

| 方案 | 问题 | 本项目选择 |
|------|------|----------|
| 先更新缓存，再更新数据库 | ❌ 数据库失败时缓存已脏，导致不一致 | 否 |
| 先更新数据库，再更新缓存 | ✅ 数据库失败则缓存不更新，保证一致性 | **是** |
| 先删除缓存，再更新数据库 | ❌ 并发读可能读到旧数据并回填缓存 | 否 |
| 先更新数据库，再删除缓存 | ✅ 经典 Cache Aside 模式 | 部分采用 |

**本项目的一致性保证机制：**

1. **事务回滚保护**：`@Transactional` 确保数据库操作失败时事务回滚，此时缓存操作不会执行
2. **短 TTL 兜底**：即使极端情况下出现不一致，缓存最多 60-300 秒后自动过期
3. **列表缓存全量失效**：任何写操作都会清空 `transactionList` 缓存，确保列表查询始终从数据库获取最新数据

**并发场景分析：**

| 场景 | 处理方式 |
|------|----------|
| 并发读 | 多个请求可能同时查库并写缓存，但数据一致，无影响 |
| 读写并发 | 读操作可能读到旧缓存，但 TTL 过期后会更新 |
| 并发写 | 数据库层面保证唯一性约束，最后一次写入为准 |

---

## 4. API 接口设计

**Base URL:** `/api/transactions`

**Swagger 文档:** http://localhost:8080/swagger-ui.html

### 4.1 创建交易

```
POST /api/transactions
```

| 项目 | 说明 |
|------|------|
| **请求体** | `{ "amount": 100.00, "type": "DEPOSIT", "category": "SALARY", "description": "工资" }` |
| **成功响应** | `201 Created` 返回创建的交易对象 |
| **缓存** | 写入 `transactions` 缓存，清空 `transactionList` 缓存 |
| **事务** | `@Transactional` 保证原子性 |

**异常情况：**

| HTTP 状态码 | 异常类型 | 触发条件 |
|-------------|----------|----------|
| `400 Bad Request` | 参数校验失败 | amount 为空/负数，type/category 无效 |
| `409 Conflict` | `DuplicateTransactionException` | 存在相同 amount+type+category+description 的交易 |

---

### 4.2 查询单条交易

```
GET /api/transactions/{id}
```

| 项目 | 说明 |
|------|------|
| **路径参数** | `id` - 交易 UUID |
| **成功响应** | `200 OK` 返回交易对象 |
| **缓存** | `@Cacheable` 优先从 `transactions` 缓存读取 |
| **事务** | `@Transactional(readOnly = true)` 只读事务 |

**异常情况：**

| HTTP 状态码 | 异常类型 | 触发条件 |
|-------------|----------|----------|
| `404 Not Found` | `TransactionNotFoundException` | 指定 ID 的交易不存在 |

---

### 4.3 分页查询交易列表

```
GET /api/transactions?page=0&size=10
```

| 项目 | 说明 |
|------|------|
| **查询参数** | `page` (默认 0), `size` (默认 10, 最大 100) |
| **成功响应** | `200 OK` 返回分页对象 `{ content, page, size, totalElements, totalPages, first, last }` |
| **缓存** | `@Cacheable` 按 `page_size` 缓存到 `transactionList` |
| **事务** | `@Transactional(readOnly = true)` 只读事务 |
| **排序** | 按 `timestamp` 降序排列 |

---

### 4.4 更新交易

```
PUT /api/transactions/{id}
```

| 项目 | 说明 |
|------|------|
| **路径参数** | `id` - 交易 UUID |
| **请求体** | 同创建交易 |
| **成功响应** | `200 OK` 返回更新后的交易对象 |
| **缓存** | 更新 `transactions` 缓存，清空 `transactionList` 缓存 |
| **事务** | `@Transactional` 保证原子性 |

**异常情况：**

| HTTP 状态码 | 异常类型 | 触发条件 |
|-------------|----------|----------|
| `404 Not Found` | `TransactionNotFoundException` | 指定 ID 的交易不存在 |
| `409 Conflict` | `DuplicateTransactionException` | 更新后与其他交易重复 |
| `400 Bad Request` | 参数校验失败 | 请求体参数无效 |

---

### 4.5 删除交易

```
DELETE /api/transactions/{id}
```

| 项目 | 说明 |
|------|------|
| **路径参数** | `id` - 交易 UUID |
| **成功响应** | `204 No Content` |
| **缓存** | 清空该 ID 的 `transactions` 缓存，清空 `transactionList` 缓存 |
| **事务** | `@Transactional` 保证原子性 |

**异常情况：**

| HTTP 状态码 | 异常类型 | 触发条件 |
|-------------|----------|----------|
| `404 Not Found` | `TransactionNotFoundException` | 指定 ID 的交易不存在 |

---

## 5. 数据模型

### 5.1 交易类型 (TransactionType)

| 枚举值 | 描述 |
|--------|------|
| `DEPOSIT` | 存款 |
| `WITHDRAWAL` | 取款 |
| `TRANSFER` | 转账 |

### 5.2 交易类别 (TransactionCategory)

| 枚举值 | 描述 |
|--------|------|
| `SALARY` | 工资 |
| `SHOPPING` | 购物 |
| `FOOD` | 餐饮 |
| `ENTERTAINMENT` | 娱乐 |
| `UTILITIES` | 账单/水电费 |
| `HEALTHCARE` | 医疗 |
| `TRANSPORTATION` | 交通 |
| `OTHER` | 其他 |

---

## 6. 测试

### 6.1 运行测试

```bash
# 运行所有测试
mvn test

# 仅运行单元测试
mvn test -Dtest=TransactionServiceTest,TransactionControllerTest

# 仅运行压力测试
mvn test -Dtest=TransactionStressTest
```

### 6.2 单元测试

| 测试类 | 测试内容 |
|--------|----------|
| `TransactionServiceTest` | Service 层业务逻辑测试：CRUD 操作、重复检测、分页查询 |
| `TransactionControllerTest` | Controller 层 API 测试：HTTP 状态码、请求验证、响应格式 |

**测试覆盖场景：**
- ✅ 正常创建/查询/更新/删除
- ✅ 重复交易检测与异常处理
- ✅ 不存在的交易处理
- ✅ 参数校验（空值、负数、无效枚举）
- ✅ 分页查询边界情况

### 6.3 压力测试

| 测试场景 | 配置 | 预期结果 |
|----------|------|----------|
| 并发创建 | 100 虚拟线程同时创建 | 全部成功，无冲突 |
| 高负载读取 | 1000 次并发读取 | 全部成功 |
| 批量写入 | 连续写入 1000 条 | < 5 秒完成 |
| 混合操作 | CRUD 并发执行 | 数据一致性保证 |

**性能指标：**

| 指标 | 结果 |
|------|------|
| 批量写入 (1000条) | ~524ms (~1908 ops/sec) |
| 并发创建 (100线程) | 100% 成功率 |

---

## 7. 启动指南

### 7.1 使用 Maven 启动（推荐）

```bash
cd /path/to/project

# 编译并启动
mvn spring-boot:run

# 或者先打包再运行
mvn clean package -DskipTests
java -jar target/transaction-management-1.0.0.jar
```

### 7.2 使用 Docker 启动

```bash
# 构建并启动
docker-compose up --build

# 后台运行
docker-compose up -d --build

# 查看日志
docker-compose logs -f

# 停止
docker-compose down
```

### 7.3 访问地址

| 服务 | URL |
|------|-----|
| Web 界面 | http://localhost:8080 |
| Swagger API 文档 | http://localhost:8080/swagger-ui.html |
| H2 数据库控制台 | http://localhost:8080/h2-console |

> **H2 Console 连接信息：**
> - JDBC URL: `jdbc:h2:mem:transactiondb`
> - Username: `sa`
> - Password: (空)
> - 配置文件: [`src/main/resources/application.yml`](file:///Users/chengqiming/hsbc_test/src/main/resources/application.yml)

---

## 8. 依赖说明

| 依赖 | 用途 |
|------|------|
| `spring-boot-starter-web` | REST API 支持 |
| `spring-boot-starter-validation` | 请求参数校验 |
| `spring-boot-starter-cache` | 缓存抽象层 |
| `caffeine` | 高性能本地缓存实现 |
| `mybatis-spring-boot-starter` | MyBatis ORM 集成 |
| `h2` | H2 内存数据库 |
| `spring-boot-starter-thymeleaf` | Web 页面模板 |
| `springdoc-openapi-starter-webmvc-ui` | Swagger API 文档 |
| `lombok` | 简化代码（Builder模式等） |
| `spring-boot-starter-test` | 测试框架 |

---

## License

MIT License

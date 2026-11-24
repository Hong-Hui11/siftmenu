# 更新日志

## 1.22.1 — 2026-11-24

### 变更

- **默认配置外置**：新增 `config/siftmenu/config.json`，跨服地址（主服 / Bingo / 起床战争）与赞助链接全部留空，不再硬编码到代码中
- 未配置地址时，对应入口显示「未配置」并给出提示，不会连接任何默认服务器
- **全球市场默认目录精简**：只保留原版物品示例（种子、作物、畜牧产品、建筑方块、矿物、基础资源）
  - 移除服务器专属条目：FarmersDelight、Kaleidoscope、CrateDelight、Ultramarine、Yuushya、Spell Engine、家具命名空间等
  - 移除服务器专属价格重平衡、法术页清理与收购项恢复逻辑
- 移除 Yuushya 饰品渲染数据与帽子标签（`assets/siftmenu/trinkets`、`data/trinkets`）
- 文档新增「服务器配置」章节并更新市场说明

## 1.22.0 — 2026-11-23

### 新增

- **JSON 自定义菜单系统**（`CustomMenus`）
  - `config/siftmenu/menus.json` 定义独立页面，或在内置页面的指定槽位追加 / 覆盖按钮
  - 支持左键 / 右键动作：打开页面、执行命令（玩家 / 控制台）、发送消息、跨服转移、关闭、返回主菜单
  - 支持页面级与按钮级管理员权限、分页条件、颜色代码、附魔光效
  - `/siftmenu menu reload` 热重载，`/siftmenu menu list` / `open <id>` 辅助排查
  - 配置错误（缺 id、槽位越界、物品 id 无效、重复 id）会被跳过并写入日志，解析失败保留上一份可用配置
- 新增 `Page.CUSTOM` 与容器 `customId`，自定义页面复用原版 9×6 容器
- 服务器启动时自动加载自定义菜单配置

### 工程

- 整理为可公开的开源工程：`src/main/java` + `src/main/resources` 标准目录
- 新增 `build.ps1`：自动解包 Fabric API、收集 classpath、JDK 25 编译、打包 jar
- 新增静态开发文档站（`docs/index.html`）与示例配置（`docs/examples/menus.example.json`）
- 新增 README、LICENSE（MIT）、CONTRIBUTING、CHANGELOG

## 1.21.x

- 1.21.57：菜单、经济、悬赏、头衔、邮箱、Bingo 联动等功能迭代（历史版本，源码合并入本仓库）

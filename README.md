# Sift Menu

面向 **Minecraft 26.3 / Fabric** 的服务器菜单模组。把传送、经济、悬赏、队伍、邮箱、头衔等系统集中到一个统一的 6×9 箱子菜单，并支持用 **JSON 配置自定义菜单页面**——管理员无需改代码即可调整菜单布局与按钮行为。

> 文档站：[`docs/index.html`](docs/index.html)

## 功能

- **菜单与传送**：家、死亡地点、随机传送、玩家互传、主城大厅、公共传送点、精品传送点
- **经济**：金币账户（分单位）、全球市场（服务器买卖 + 动态价格）、自由市场（玩家挂单、挂单费与成交税）
- **玩法**：每日悬赏板、头衔与成就解锁、邮箱公告 / 物品补偿、Bingo 联动奖励
- **服务器**：大厅建造 / 生物 / 时间保护、区块与掉落物清理、睡眠跳过、若干兼容修复 Mixin
- **自定义菜单**：`config/siftmenu/menus.json` 定义页面与按钮，支持 `/siftmenu menu reload` 热重载

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.3 |
| Fabric Loader | ≥ 0.19.5 |
| Fabric API | 26.3 |
| Java | 25 |

## 安装

1. 把 `siftmenu-1.22.1.jar` 放进服务端与客户端的 `mods/`。
2. 启动服务器，生成：
   - `config/siftmenu-data.json`：玩家数据（家、金币、挂单、邮件等）
   - `config/siftmenu/menus.json`：自定义菜单配置
   - `config/siftmenu/config.json`：跨服地址与赞助链接（默认留空）
3. 游戏内按 <kbd>G</kbd> 或执行 `/siftmenu` 打开菜单。

### 服务器配置

跨服传送与赞助链接全部从 `config/siftmenu/config.json` 读取，默认留空，不会连接任何服务器：

```json
{
  "schema": 1,
  "servers": {
    "main": { "host": "", "port": 0 },
    "bingo": { "host": "", "port": 0 },
    "bedwars": { "host": "", "port": 0 }
  },
  "sponsorUrl": ""
}
```

留空时对应入口提示「未配置」；修改后重启服务器生效。

## 自定义菜单（示例）

```json
{
  "schema": 1,
  "pages": [
    {
      "id": "guide",
      "title": "&6服务器指南",
      "base": "CUSTOM",
      "items": [
        {
          "slot": 13,
          "item": "minecraft:knowledge_book",
          "name": "&a规则",
          "lore": ["&7点击查看服务器规则"],
          "left": { "type": "open", "page": "rules" }
        }
      ]
    },
    {
      "id": "main-entry",
      "base": "MAIN",
      "items": [
        {
          "slot": 15,
          "item": "minecraft:compass",
          "name": "&a服务器指南",
          "left": { "type": "open", "page": "guide" }
        }
      ]
    }
  ]
}
```

完整字段、动作类型与示例见 [文档站 · 自定义菜单](docs/index.html#custom-menu) 和 [`docs/examples/menus.example.json`](docs/examples/menus.example.json)。

常用命令：

```
/siftmenu menu reload        # 重载 menus.json（管理员）
/siftmenu menu list          # 查看已加载页面
/siftmenu menu open <id>     # 直接打开某个自定义页面
```

## 构建

项目不使用 Gradle，`build.ps1` 直接调用 JDK 25 编译并打包。

```powershell
# 依赖 jar 可放在 lib/mc-26.3-aw.jar，或用 -McJar 指定
.\build.ps1 -McJar "C:\path\to\mc-26.3-aw.jar" -ModsDir "D:\...\mods"
```

构建参数见 [文档站 · 构建](docs/index.html#build)，安装步骤见 [文档站 · 部署](docs/index.html#deploy)。

## 目录结构

```
src/main/java/com/echoes/siftmenu/   Java 源码（61 个文件）
src/main/resources/                  fabric.mod.json、mixin 配置、语言与数据
docs/                                静态开发文档站与示例配置
build.ps1                            构建脚本
```

## 文档

- [项目概览](docs/index.html#overview)
- [架构与数据流](docs/index.html#architecture)
- [功能实现详解](docs/index.html#features)
- [自定义菜单配置](docs/index.html#custom-menu)
- [二次开发指南](docs/index.html#develop)
- [构建与部署](docs/index.html#build)

## 许可

[MIT](LICENSE)

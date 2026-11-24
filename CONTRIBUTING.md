# 贡献指南

感谢你愿意改进 Sift Menu。提交代码前请阅读以下约定。

## 开发环境

- JDK 25
- Minecraft 26.3 依赖（服务端 / 客户端均可）
- `build.ps1` 需要：
  - access-widener 处理后的 `mc-26.3-aw.jar`
  - `%APPDATA%\.minecraft\libraries`
  - 含 `fabric-api` 与联动模组的 `mods` 目录（Bingo、FTB、Trinkets、Ultramarine 等为编译期依赖）

## 构建与自测

```powershell
.\build.ps1 -McJar "C:\path\to\mc-26.3-aw.jar" -ModsDir "D:\...\mods"
```

- 提交前必须能完整编译并产出 jar；
- 涉及菜单的改动，请在测试服实际打开页面、点击每个新增按钮验证；
- 涉及自定义菜单的改动，请同时验证 `/siftmenu menu reload` 与错误配置的容错。

## 代码约定

- 包名：`com.echoes.siftmenu`；客户端代码放 `client` 子包。
- 玩家可见文本使用中文；日志使用 `SiftMenu.LOGGER`，前缀 `[siftmenu]`。
- 不要硬引用可选模组的类。使用 `FabricLoader.isModLoaded` 判断并通过反射调用（参考 `BingoHook`）。
- 经济数值内部一律使用「分」（`Money.fromCoins`），显示用 `Economy.format`。
- 修改 `MenuData` 字段时，必须在 `normalize()` 中补默认值，保证旧存档可读。
- 新增或修改自定义菜单字段时，同步更新：
  - `docs/index.html` 的字段表与动作表
  - `docs/examples/menus.example.json`

## 提交与 PR

1. 一个 PR 只做一件事，附带变更说明与测试方式；
2. 不要在 PR 中包含 `build/`、日志、真实玩家数据或服务器密钥；
3. 如修改数据结构，请在说明中写清迁移方式与回滚方式；
4. 保持向后兼容：旧配置文件、旧存档在升级后应能继续加载。

## 文档

文档是仓库的一部分：`docs/index.html` 为可部署的静态站。改动玩家可见行为时请一并更新对应章节。

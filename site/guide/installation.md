# 安装部署

## 从发布页下载

1. 前往 [GitHub Releases](https://github.com/HeCUana/CRPI-FakePlayer/releases) 下载最新版本
2. 将 `crpi-fakeplayer-x.x.x.jar` 放入服务端 `mods/` 目录
3. 确保以下依赖也在 `mods/` 中：
   - [Fabric API](https://modrinth.com/mod/fabric-api)
   - [Carpet Mod](https://modrinth.com/mod/carpet)

## 从源码构建

```bash
# 克隆仓库
git clone https://github.com/HeCUana/CRPI-FakePlayer.git
cd CRPI-FakePlayer

# 构建（需要 JDK 21）
gradlew.bat build

# 构建产物位于
# build/libs/crpi-fakeplayer-x.x.x.jar
```

## 服务端部署

1. 安装 [Fabric Loader 0.19.3+](https://fabricmc.net/use/installer/) 到服务端
2. 将所有 jar 文件放入 `mods/` 目录
3. 首次启动服务端以生成配置文件
4. 编辑 `config/carpet.conf` 调整规则（可选）

## 验证安装

启动服务端后，执行以下命令验证：

```
/crpi fp list
```

如果返回假人列表（或空列表），说明安装成功。

## 目录结构

```
server/
├── mods/
│   ├── crpi-fakeplayer-x.x.x.jar    # 本模组
│   ├── fabric-api-x.x.x.jar         # Fabric API
│   └── carpet-x.x.x.jar             # Carpet
├── config/
│   └── carpet.conf                   # Carpet 规则配置
└── world/
    └── ...
```

## 常见问题

### Q: 启动时报错 `Missing mod dependencies`

确保 Fabric Loader、Fabric API 和 Carpet 版本符合要求。检查 `logs/latest.log` 中的具体错误信息。

### Q: `/crpi fp` 命令不存在

确认 Carpet 模组已正确加载。CRPI-FakePlayer 的命令注册依赖 Carpet 的扩展机制。

### Q: 假人生成后无法执行行为

检查 Carpet 规则 `fakePlayerActions` 是否为 `true`（默认值）。执行：

```
/crpi-fakeplayer fakePlayerActions true
```

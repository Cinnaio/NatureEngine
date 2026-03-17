### NatureEngine

**语言**: 中文 | [English](README.md)

NatureEngine 是一个面向 Paper/Folia（含 Luminol 等分支）的“自然系统引擎”插件，提供 **季节（Season）**、**天气（Weather）** 与 **作物生长接管（Crop/Plant Growth Control）**。  
插件支持 **原版作物**与 **CraftEngine 自定义作物**的统一生长模型，并提供调试/模拟命令用于快速验证配置效果。

---

### 主要功能

- **季节系统**
  - 世界季节循环、进度、下一季节与持续天数查询
  - 季节切换广播（ActionBar/Chat 可配置）
- **天气系统**
  - 基于季节权重的天气选择与持续时间控制
  - 天气对温度/湿度/土壤等环境的增量影响
- **作物生长接管（原版 + CraftEngine）**
  - 原版作物：拦截原版自然生长与骨粉推进，由插件统一计算是否推进/枯萎
  - CraftEngine 作物：读取/写入自定义方块属性（如 `age`），由插件驱动推进
  - **插件内部 randomTickSpeed**：独立于 `/gamerule randomTickSpeed`，统一控制原版与 CraftEngine 作物的“生长尝试频率”
- **调试与模拟**
  - `/ne debug crop`：查看目标作物是否被接管、当前阶段、环境值、各因子、阈值与最终判定
  - `/ne sim crop`：模拟四季 × 天气对目标作物的 total 与判定（不修改方块）

---

### 快速开始

1) 将 `NatureEngine.jar` 放入服务端 `plugins/`  
2) 启动服务器生成默认配置  
3) 根据需要编辑：
   - `seasons.yml`：季节长度与环境增量
   - `weather.yml`：天气权重/持续时间/环境配置
   - `crops.yml`：原版作物与 CraftEngine 作物参数
   - `growth.yml`：阈值与插件内部 `random-tick-speed`
4) 游戏内（OP）执行 `/ne reload all` 重载配置

> 对 CraftEngine 作物：建议在 CraftEngine 侧 **关闭其自身 random tick 生长**（例如 `is-randomly-ticking: false` 或 `grow-speed: 0`），再由 NatureEngine 完整接管生长推进。

---

### 命令

> 当前默认策略：`/ne` 全部子命令 **仅 OP 可用**，并支持 TAB 补全。

- **调试**
  - `/ne debug`
  - `/ne debug crop`
- **模拟**
  - `/ne sim crop`
- **季节**
  - `/ne season info`
  - `/ne season next`
  - `/ne season set <SPRING|SUMMER|AUTUMN|WINTER>`
  - `/ne season clear`
  - `/ne season apply`
  - `/ne season restore`
- **配置**
  - `/ne reload <all|seasons|growth|debug|weather|visual|crops>`
- **作物生长速度（插件内部 randomTickSpeed）**
  - `/ne crop randomTickSpeed`（查看）
  - `/ne crop randomTickSpeed <值>`（设置并写入 `growth.yml`）

---

### 配置说明（节选）

- **`growth.yml`**
  - `growth.advance-threshold`：推进阈值
  - `growth.wither-threshold`：枯萎阈值
  - `growth.random-tick-speed`：插件内部 randomTickSpeed（独立于 `/gamerule`）

- **`crops.yml`**
  - `crops.vanilla.<MATERIAL>`：原版作物（仅对 `Ageable` 方块生效）
  - `crops.craftengine."<namespace:value>"`：CraftEngine 作物（注意包含冒号的 key 需要加引号）
  - `base-ticks-per-stage`：阶段推进的“基础速度”（越小越快）

---

### API（给开发者）

NatureEngine 提供面向其它插件的静态 API（示例以 `World` 为输入）：

- **Season API**：`com.github.cinnaio.natureEngine.api.SeasonAPI`
  - 获取当前季节、进度、下一季节、剩余天数、持续天数等
- **Weather API**：`com.github.cinnaio.natureEngine.api.WeatherAPI`
  - 获取当前天气类型等
- **Crop API**：`com.github.cinnaio.natureEngine.api.CropAPI`
  - 查询某位置/材质是否为受控作物（返回 `Optional<CropType>`）

如果你使用 PlaceholderAPI，可通过 NatureEngine 的 PAPI expansion 获取季节/天气等占位符。

---

### 如何构建

#### 环境要求

- **JDK 21**
- Gradle Wrapper（仓库已包含）

#### 构建命令

在项目根目录执行：

```bash
./gradlew build
```

如果你在 Windows PowerShell：

```powershell
.\gradlew.bat build
```

---

### 如何贡献

欢迎提交 Issue / PR。建议流程：

- **提交前**
  - 保持改动聚焦、可回滚
  - 避免引入破坏 Folia/Luminol 线程模型的跨区域读写
- **PR 规范**
  - 清晰描述变更动机（why）与影响范围
  - 若涉及配置/文档，请同步更新 `docs/` 与 `lang/`

---

### 使用到的项目 / 致谢

感谢以下项目与生态：

- **Paper / Folia API**：服务端与调度模型基础
- **ProtocolLib**：用于季节视觉相关的数据包处理
- **PlaceholderAPI**：占位符扩展能力
- **CraftEngine**：自定义方块/作物生态与 API
- **Kyori Adventure / MiniMessage**：现代文本组件与格式化

（以及所有上游依赖与社区贡献者）

---

### License

本项目的许可协议以仓库中的 `LICENSE` 为准。


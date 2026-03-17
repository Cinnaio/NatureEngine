# 日历与生长系统（Calendar & Plant Growth）

本文档只描述 **NatureEngine 当前已实现** 的能力与配置，结构参考 AdvancedSeasons 的文档风格：[Gregorian Calendar](https://seasons.advancedplugins.net/features/calendar/gregorian-calendar)。

## 日历类型

### CUSTOM（当前实现）

NatureEngine 的季节推进基于世界时间计算，属于 **CUSTOM 日历**：

- **时间来源**：`World#getFullTime()`
- **世界天数**：`worldDay = floor(fullTime / 24000)`
- **一年长度**：四季 `length-days` 总和
- **季节顺序**：`SPRING -> SUMMER -> AUTUMN -> WINTER -> ...`
- **季节进度**：0.0~1.0（季节内已过天数 / 当前季节天数）

> 当前没有实现 365 天的公历（GREGORIAN）日历模式。

## seasons.yml（季节）

路径：`plugins/NatureEngine/seasons.yml`

### 季节长度（推进用）

```yaml
seasons:
  spring:
    length-days: 10
  summer:
    length-days: 10
  autumn:
    length-days: 10
  winter:
    length-days: 10
```

### 季节对环境的增量（vanilla 标尺）

> 这些是对环境温湿度的“增量”，会叠加到原版气候采样上。\n+> - 温度：vanilla 标尺（约 0~2）\n+> - 湿度：0~1

```yaml
seasons:
  spring:
    temperature-delta: 0.05
    humidity-delta: 0.05
  summer:
    temperature-delta: 0.20
    humidity-delta: -0.05
  autumn:
    temperature-delta: 0.00
    humidity-delta: 0.00
  winter:
    temperature-delta: -0.20
    humidity-delta: -0.05
```

## weather.yml（天气）

路径：`plugins/NatureEngine/weather.yml`

### 天气如何被季节控制

- `WeatherManager` 按 `tick-interval-seconds` 定时 tick
- 每次 tick 对每个世界：按“当前季节”的 `season-weights` 做加权随机选出下一天气
- 选中天气会持续 `duration-seconds.<TYPE>` 秒

### 天气 profile（对环境与生长的影响）

```yaml
weather:
  profiles:
    SUNNY:
      temperature-delta: 0.05
      humidity-delta: -0.02
      soil-moisture-delta: -0.02
      growth-multiplier: 1.0
    RAIN:
      temperature-delta: -0.03
      humidity-delta: 0.05
      soil-moisture-delta: 0.20
      growth-multiplier: 1.1
```

- `temperature-delta / humidity-delta / soil-moisture-delta` 会叠加到 Environment（并对湿度/土壤做 0..1 clamp）
- `growth-multiplier` 会作为生长因子里的 **天气因子 W**

## crops.yml（作物定义）

路径：`plugins/NatureEngine/crops.yml`

### 温度标尺

作物的 `optimal-temperature` / `temperature-tolerance` 使用 **vanilla 标尺**（来自 `World#getTemperature(x,z)`）。

### 示例：小麦

```yaml
crops:
  enabled: true
  vanilla:
    WHEAT:
      enabled: true
      stages: 8
      base-ticks-per-stage: 2400
      optimal-temperature: 0.90
      temperature-tolerance: 0.70
      optimal-humidity: 0.70
      humidity-tolerance: 0.50
      min-light: 9
      preferred-seasons: [SPRING, SUMMER]
```

## 环境（Environment）如何计算

EnvironmentContext 的字段：

- `temperature`：原版气候采样 + season delta + weather delta
- `humidity`：原版气候采样 + season delta + weather delta（0..1 clamp）
- `soilMoisture`：优先读取耕地 `Farmland.moisture/maxMoisture`，再叠加 weather soil delta（0..1 clamp）
- `lightLevel`：`block.getLightLevel()`

## 生长模型（Plant Growth Model）

每次原版触发生长尝试（自然 randomTick / 骨粉）时，NatureEngine 会计算：

### 总公式

`total = T * H * L * S * W`

### 决策阈值

来自 `growth.yml`：

```yaml
growth:
  advance-threshold: 0.22
  wither-threshold: 0.02
```

- `total <= wither-threshold` → WITHER（当前实现：年龄归 0）
- `total >= advance-threshold` → ADVANCE（年龄 +1）
- 否则 → BLOCK（不推进）

### 因子定义

- **T（温度因子）**：环境温度与最适温度差距越小越接近 1；超过容差直接 0\n+  `T = 1 - abs(envT - optT) / tolT`（当 abs < tol）\n+- **H（湿度因子）**：超过容差降到 0.2，否则按差距衰减\n+- **L（光照因子）**：光照 >= min-light → 1，否则 → 0.3\n+- **S（季节因子）**：偏好季节 → 1.2，否则 → 0.8\n+- **W（天气因子）**：`weather.yml profiles.<TYPE>.growth-multiplier`\n+
## 调试与模拟

### /ne debug crop（输出目标作物的详细信息）

- 对准目标方块，显示：概览 / 因子 / 环境 / 阈值 / 参数（分行）
- 不带 `crop` 的 `/ne debug` 只会输出概览行（避免刷屏）

### /ne sim crop（只输出计算结果，不修改方块）

对准目标作物后，命令会：\n+1) 反推出当前坐标的“气候基准”（去掉当前季节/天气增量）\n+2) 枚举 **四季 × 天气类型**，输出每种组合的 `total` 与判定（A/B/W）\n+

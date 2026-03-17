# CustomNameplates 集成

NatureEngine 通过 PlaceholderAPI 扩展提供占位符，可在 [CustomNameplates](https://momi.gtemc.cn/customnameplates) 中显示季节与天气信息。

## 前置条件

- PlaceholderAPI
- CustomNameplates
- NatureEngine

## 可用占位符

| 占位符 | 说明 |
|--------|------|
| `%natureengine_season%` | 季节枚举名 |
| `%natureengine_season_display%` | 本地化季节名（春/夏/秋/冬） |
| `%natureengine_next_season%` | 下一季节枚举名 |
| `%natureengine_next_season_display%` | 下一季节本地化名 |
| `%natureengine_season_progress%` | 季节进度 0~100 |
| `%natureengine_season_duration%` | 当前季节持续天数 |
| `%natureengine_days_until_next_season%` | 距下一季节天数 |
| `%natureengine_weather%` | 天气枚举名 |
| `%natureengine_weather_display%` | 本地化天气名 |
| `%natureengine_temperature%` | 玩家位置温度 |
| `%natureengine_humidity%` | 湿度 |
| `%natureengine_light%` | 光照等级 |

## 配置示例

### 动作栏 (actionbar.yml)

```yaml
actionbar:
  text: '<gray>季节 <gold>%natureengine_season_display%</gray> · <gold>%natureengine_days_until_next_season%</gold> 天后 <gray>%natureengine_next_season_display%</gray>'
```

### 静态文本 (static-text)

```yaml
static-text:
  season_hud:
    position: right
    text: '%natureengine_season_display% · %natureengine_weather_display%'
    value: 120
```

### 背景/铭牌文本 (background-text)

```yaml
background-text:
  season_info:
    background: bedrock_1
    text: '<gold>%natureengine_season_display%</gold> <gray>|</gray> <aqua>%natureengine_weather_display%</aqua> <gray>|</gray> <yellow>%natureengine_days_until_next_season%</yellow> 天后入 <gold>%natureengine_next_season_display%</gold>'
    shadow: false
    left-margin: 1
    right-margin: 1
```

占位符会根据玩家所在世界解析，支持 MiniMessage 格式。

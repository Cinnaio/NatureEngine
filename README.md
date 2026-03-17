### NatureEngine

NatureEngine 是一个面向 Paper/Folia（含 Luminol 等分支）的自然系统插件，提供 **季节**、**天气** 与 **作物生长接管**（原版 + CraftEngine）。

### API（给开发者）

- `SeasonAPI`：`com.github.cinnaio.natureEngine.api.SeasonAPI`
- `WeatherAPI`：`com.github.cinnaio.natureEngine.api.WeatherAPI`
- `CropAPI`：`com.github.cinnaio.natureEngine.api.CropAPI`

### 构建

需要 **JDK 21**：

```bash
./gradlew build
```

Windows PowerShell：

```powershell
.\gradlew.bat build
```

### 贡献与致谢

欢迎 Issue / PR。涉及配置/文档/i18n 的改动请同步更新 `docs/` 与 `lang/`。  
致谢：Paper/Folia、ProtocolLib、PlaceholderAPI、CraftEngine、Kyori Adventure/MiniMessage。

### License

见 `LICENSE`。


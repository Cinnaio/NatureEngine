### NatureEngine

**Language**: [中文](README.zh_cn) | English

NatureEngine is a “nature system engine” plugin for Paper/Folia (including forks like Luminol). It provides **Seasons**, **Weather**, and **Crop/Plant Growth Control**.  
It supports a unified growth model for both **vanilla crops** and **CraftEngine custom crops**, plus debug/simulation commands for fast validation.

---

### Features

- **Seasons**
  - Season cycle, progress, next season, and remaining days
  - Season change broadcasts (ActionBar/Chat configurable)
- **Weather**
  - Weather selection and duration control based on season weights
  - Weather profiles that affect temperature/humidity/soil (deltas)
- **Crop Growth Control (Vanilla + CraftEngine)**
  - Vanilla: intercepts natural growth and bonemeal, then decides advance/block/wither via plugin model
  - CraftEngine: reads/writes custom block properties (e.g. `age`) to drive growth
  - **Plugin randomTickSpeed**: independent from `/gamerule randomTickSpeed`, controls growth attempt frequency for both vanilla and CraftEngine
- **Debug & Simulation**
  - `/ne debug crop`: shows whether the target crop is controlled, stage, env, factors, thresholds, decision
  - `/ne sim crop`: simulates seasons × weathers totals/decisions (no block changes)

---

### Quick Start

1) Put `NatureEngine.jar` into your server `plugins/` folder  
2) Start the server to generate default configs  
3) Edit as needed:
   - `seasons.yml`: season length and environment deltas
   - `weather.yml`: weights/durations/profiles
   - `crops.yml`: vanilla & CraftEngine crop parameters
   - `growth.yml`: thresholds and plugin `random-tick-speed`
4) In-game (OP): run `/ne reload all`

> For CraftEngine crops: it’s recommended to **disable CraftEngine’s own random-tick growth** (e.g. `is-randomly-ticking: false` or `grow-speed: 0`) and let NatureEngine fully control growth.

---

### Commands

> Current default policy: all `/ne` commands are **OP-only**, with TAB completion.

- **Debug**
  - `/ne debug`
  - `/ne debug crop`
- **Simulation**
  - `/ne sim crop`
- **Season**
  - `/ne season info`
  - `/ne season next`
  - `/ne season set <SPRING|SUMMER|AUTUMN|WINTER>`
  - `/ne season clear`
  - `/ne season apply`
  - `/ne season restore`
- **Config**
  - `/ne reload <all|seasons|growth|debug|weather|visual|crops>`
- **Crop speed (plugin randomTickSpeed)**
  - `/ne crop randomTickSpeed` (get)
  - `/ne crop randomTickSpeed <value>` (set; persisted to `growth.yml`)

---

### Config Notes (excerpt)

- **`growth.yml`**
  - `growth.advance-threshold`: advance threshold
  - `growth.wither-threshold`: wither threshold
  - `growth.random-tick-speed`: plugin internal randomTickSpeed (independent from `/gamerule`)

- **`crops.yml`**
  - `crops.vanilla.<MATERIAL>`: vanilla crops (only `Ageable` blocks)
  - `crops.craftengine."<namespace:value>"`: CraftEngine crops (keys containing `:` must be quoted)
  - `base-ticks-per-stage`: baseline speed per stage (smaller = faster)

---

### API (for developers)

NatureEngine provides static APIs for other plugins:

- **Season API**: `com.github.cinnaio.natureEngine.api.SeasonAPI`
  - current season, progress, next season, days until next, season length, etc.
- **Weather API**: `com.github.cinnaio.natureEngine.api.WeatherAPI`
  - current weather type, etc.
- **Crop API**: `com.github.cinnaio.natureEngine.api.CropAPI`
  - query whether a location/material is a controlled crop (`Optional<CropType>`)

If you use PlaceholderAPI, NatureEngine also ships a PAPI expansion for season/weather placeholders.

---

### Build

#### Requirements

- **JDK 21**
- Gradle Wrapper (included)

#### Commands

```bash
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

---

### Contributing

Issues and PRs are welcome.

- Keep changes focused and reversible
- Be careful with Folia/Luminol threading constraints (avoid cross-region world access)
- If you change configs/docs/i18n, please update `docs/` and `lang/` together

---

### Credits / Thanks

Thanks to:

- **Paper / Folia API** for the server platform and scheduling model
- **ProtocolLib** for packet-level visual features
- **PlaceholderAPI** for placeholders
- **CraftEngine** for the custom-block/crop ecosystem and API
- **Kyori Adventure / MiniMessage** for modern text components & formatting

---

### License

See `LICENSE` in this repository.


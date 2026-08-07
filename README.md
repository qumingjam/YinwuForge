# YinwuForge — Yinwu锻造
# YinwuForge — Forge System

**最新版本：v1.2.2** | [下载 Release](https://github.com/qumingjam/YinwuForge/releases/tag/v1.2.2)

A Minecraft forge plugin for enhancing equipment through altars, materials, and potions.

一个为 **Folia / Paper 1.21+** 设计的 Minecraft 锻造插件，提供药水锻造与属性强化双系统。

> ⚡ 完全兼容 Folia 区域线程调度，无 NMS、无 unsafe 反射。

---

## Features | 功能概览

- **Attribute Enhancement** — 6 attributes (damage, attack speed, armor, toughness, mining speed, durability) all ADD_NUMBER
- **Effective Value Display** — 锻造 lore 直接显示攻速/伤害实际值（原版 tooltip 显示修饰符原始值属客户端行为，已用 lore 补齐有效值）
- **Potion Forging** — Attach potion effects to equipment
- **Forge Altar** — Custom multi-layer 5x5 structure with second-layer bonus blocks
- **Concentrated Materials** — 30+ material types (strength/adjuster/potion categories)
- **Diminishing Returns** — Forge count affects yield (×1.5 for 1-5 times, ×0.2 for 41+)
- **YinwuEnchant Integration** — 15%/30% chance to apply custom enchantments on forge success

| 模块 | 说明 |
|------|------|
| 🧪 **药水锻造** | 为装备附加药水效果（速度、力量、急迫、抗火等） |
| ⚔️ **属性强化** | 强化装备属性（耐久、伤害、护甲、攻速等） |
| 🗿 **锻造祭坛** | 自定义多层结构祭坛，搭建后右键打开GUI锻造 |
| 🎛️ **GUI锻造** | 3槽位GUI：装备 + 强化材料 + 概率调整材料 |
| 📦 **材料体系** | 30种浓缩材料 + 6大分类，MythicMobs可掉落 |
| 🎯 **概率调整** | 炼狱/末地/挑战核心，动态调整成功/损坏率 |
| 🌟 **光柱效果** | 祭坛激活后持续显示粒子光柱 |

---

## Quick Start | 快速开始

1. 将 `YinwuForge-1.2.1.jar` 放入 `plugins/` 目录
2. 重启服务器
3. 搭建**锻造祭坛**结构（见下方用法说明）
4. 右键祭坛中心（锻造台）→ 打开**锻造GUI**
5. 放入装备 + 对应核心材料 → 锻造！

---

## Commands | 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/yinwu` | 查看帮助 | `yinwu.forge.use` |
| `/yinwu reload` | 重载所有配置 | `yinwu.forge.admin` |
| `/yinwu give potion` | 获取药水锻造材料（锻造奇点） | `yinwu.forge.admin` |
| `/yinwu give concentrated <id>` | 获取浓缩材料 | `yinwu.forge.admin` |

## Permissions | 权限

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `yinwu.forge.use` | 允许使用锻造功能 | 所有人 |
| `yinwu.forge.admin` | 管理员权限（重载/获取材料） | OP |

---

## Architecture | 架构

```
YinwuForge
├── manager/
│   ├── ForgeManager           # 锻造核心逻辑
│   ├── AltarManager           # 祭坛结构检测与效果
│   ├── ForgeGUI               # 3槽位GUI锻造界面
│   ├── MaterialConfig         # 材料定义加载(material.yml)
│   ├── ConfigManager          # 配置管理器
│   ├── PotionEffectManager    # 药水效果管理
│   ├── PotionForgeConfig      # 药水锻造配置
│   ├── AlloyForgeConfig       # 强化锻造配置
│   ├── EventListener          # 事件监听
│   └── CommandHandler         # 命令处理
├── model/
│   ├── EquipmentData          # 装备锻造数据
│   ├── EquipmentAttributes    # 装备属性数据
│   ├── PotionEffectData       # 药水效果数据
│   └── ForgeResult            # 锻造结果枚举
├── YinwuForgePlugin           # 主类
└── BaseWeaponStats            # 基础属性数据
```

---

## Build | 构建

依赖：**Java 21+**、**Maven 3.8+**

```bash
git clone https://github.com/qumingjam/YinwuForge.git
cd YinwuForge
mvn clean package
```

产出：`target/YinwuForge-1.2.1.jar`

---

## Dependencies | 依赖

- **[YinwuPluginLib](https://github.com/qumingjam/YinwuPluginLib)**（必需）
- **[Paper API 1.21+](https://papermc.io/)**（provided）
- **MythicMobs**（可选）

---

## Configuration | 配置

```
plugins/YinwuForge/
├── config.yml          # 主配置文件（概率、祭坛、GUI、属性）
└── material.yml        # 材料定义文件（物品ID、名称、分类）
```

使用 `/yinwu reload` 热重载所有配置。

---

## Design Principles | 设计原则

- **Folia First** — 所有调度使用 `RegionScheduler` / `GlobalRegionScheduler` / `EntityScheduler`
- **零 NMS** — 仅依赖 Paper/Folia 公共 API
- **线程安全** — 共享状态使用 `ConcurrentHashMap`
- **Java 21** — 使用 records、switch expressions、pattern matching

---

## Links | 链接

- 仓库：[github.com/qumingjam/YinwuForge](https://github.com/qumingjam/YinwuForge)
- 前置：[YinwuPluginLib](https://github.com/qumingjam/YinwuPluginLib)
- 关联：[YinwuRaid](https://github.com/qumingjam/YinwuRaid) | [YinwuEnchant](https://github.com/qumingjam/YinwuEnchant)
- 作者：Qumingjam

---

## Changelog | 更新日志

### v1.2.1
- 版本号更新
- README 完善

### v1.2.0
- Folia 调度全面适配
- onDisable() 新增任务取消和监听器解注册
- Java 21, Paper API 1.21+, Folia 兼容

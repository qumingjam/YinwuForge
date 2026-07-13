# YinwuForge — Yinwu锻造
一个为 **Folia / Paper 1.21+** 设计的 Minecraft 锻造插件，提供药水锻造与属性强化双系统。
> ⚡ 完全兼容 Folia 区域线程调度，无 NMS、无 unsafe 反射。

---

## 功能概览

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

## 快速开始

1. 将 `YinwuForge-1.1.0.jar` 放入 `plugins/` 目录
2. 重启服务器
3. 搭建**锻造祭坛**结构（见下方用法说明）
4. 右键祭坛中心（锻造台）→ 打开**锻造GUI**
5. 放入装备 + 对应核心材料 → 锻造！

---

## 命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/yinwu` | 查看帮助 | `yinwu.forge.use` |
| `/yinwu reload` | 重载所有配置 | `yinwu.forge.admin` |
| `/yinwu give potion` | 获取药水锻造材料（锻造奇点） | `yinwu.forge.admin` |
| `/yinwu give concentrated <id>` | 获取浓缩材料 | `yinwu.forge.admin` |

## 权限

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `yinwu.forge.use` | 允许使用锻造功能 | 所有人 |
| `yinwu.forge.admin` | 管理员权限（重载/获取材料） | OP |

---

## 锻造材料体系

### 强化材料（槽2 - 必填）

| 分类 | 浓缩材料 | 原版底材 | 用途 |
|:----:|:--------|:---------|:----|
| **矿物** | 浓缩铜、金、铁、钻石、下界合金 | 铜锭/金锭/铁锭/钻石/下界合金锭 | 强化**盔甲** |
| **亡灵** | 浓缩腐肉、骨头、火药、蛛眼、幻翼膜 | 腐肉/骨头/火药/蜘蛛眼/幻翼膜 | 强化**武器** |
| **农牧** | 浓缩蜜脾、龟甲、毒土豆、犰狳甲、疣 | 蜜脾/乌龟鳞甲/毒土豆/犰狳鳞甲/下界疣 | 强化**工具** |

### 概率调整材料（槽3 - 可选）

| 分类 | 浓缩材料 | 效果 |
|:----:|:--------|:----|
| **炼狱** | 浓缩泪滴、岩浆球、烈焰棒、石英、荧光粉 | 成功率+15%，损坏率+5% |
| **末地** | 浓缩潜影壳、珍珠、紫菘果、龙息、末地石 | 损坏率-8%，成功率-10% |
| **挑战** | 浓缩龙蛋、下界之星、海洋之心、回响碎片、海绵 | 锻造次数≥5时损坏率-15% |

> 💡 所有材料通过 `material.yml` 配置，MythicMobs 生成的物品只要 **物品类型(material) + 自定义名称(name)** 一致即可被自动识别。

---

## 锻造概率

基础概率表（可在 `config.yml` 中配置）：

| 结果 | 概率 | 说明 |
|:----:|:----:|:----|
| 无惩罚 | 25% | 消耗材料，属性不变 |
| 装备摧毁 | 10% | 装备消失 |
| 降级 | 15% | 随机属性减少 |
| 成功 | 35% | 随机属性增加 |
| 极品 | 15% | 两个属性大幅增加 |

> 概率自动钳制 0%~100%，调整核心/祭坛加成导致溢出时按比例归一化。

---

## 锻造祭坛

### 结构（5×5×2）

```
第二层（加成层，可选）：
BBBBB
BAAAB
BACAB
BAAAB
BBBBB

第一层（必须完整）：
BBBBB
BAAAB
BACAB
BAAAB
BBBBB
```

- **C** = 锻造台（中心方块）
- **B** = 下界合金块（底座方块，支持多种）
- **A** = 任意方块（占位符，不检测）

### 祭坛加成

第二层每放置1个底座方块：**成功率+2%，失败率-1%**（最多16个方块生效）

---

## 使用流程

```
1. 搭建 5×5×2 锻造祭坛
2. 右键祭坛中心（锻造台）
       ↓
3. 打开锻造GUI
   ┌──────────────────────┐
   │ [装备] [强化材料] [调整材料] │
   │         [⛏ 锻造]        │
   └──────────────────────┘
       ↓
4. 放入装备（武器/工具/盔甲）
5. 放入对应强化材料 → 自动匹配装备类型
6. （可选）放入概率调整材料
7. 点击锻造按钮
       ↓
8. 成功/极品 → 属性提升 + 锻造次数+1
   失败降级 → 属性减少
   装备摧毁 → 装备消失
```

---

## 架构

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
└── YinwuForgePlugin           # 主类
```

---

## 构建

依赖：**Java 21+**、**Maven 3.8+**

```bash
git clone https://github.com/qumingjam/YinwuForge.git
cd YinwuForge
mvn clean package
```

产出：`target/YinwuForge-1.1.0.jar`

---

## 依赖

- **[Paper API 1.21.4](https://papermc.io/)**（provided，必选）
- **MythicMobs**（可选，用于掉落材料，本插件仅匹配物品识别）

---

## 配置

配置文件位于 `plugins/YinwuForge/`：

```
config.yml          # 主配置文件（概率、祭坛、GUI、锻造属性）
material.yml        # 材料定义文件（物品ID、名称、分类、功能说明）
```

使用 `/yinwu reload` 热重载所有配置。

---

## 设计原则

- **Folia First** — 所有调度使用 `RegionScheduler` / `GlobalRegionScheduler` / `EntityScheduler`
- **零 NMS** — 仅依赖 Paper/Folia 公共 API，不碰 Mojang 内部类
- **线程安全** — 共享状态使用 `ConcurrentHashMap`，实体/方块操作在对应区域线程执行
- **Java 21** — 使用最新语言特性（records、switch expressions、pattern matching）

---

## 链接

- 仓库：[github.com/qumingjam/YinwuForge](https://github.com/qumingjam/YinwuForge)
- 关联项目：[YinwuRaid - 灾厄袭击](https://github.com/qumingjam/YinwuRaid)
- 作者：Qumingjam

---

## 优化记录

- `isFolia()` 运行时检测简化为常量 `true`（插件已要求 Folia）
- `onDisable()` 新增 `Bukkit.getGlobalRegionScheduler().cancelTasks(this)` 和 `HandlerList.unregisterAll(this)`

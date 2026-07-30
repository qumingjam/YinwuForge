# YinwuForge — 锻造系统插件

## 项目信息
- **技术栈**: Java 21, Maven, Paper API 1.21.4
- **打包**: `mvn clean package` → `target/YinwuForge-<version>.jar`
- **Folia 兼容**: 是
- **GitHub**: https://github.com/qumingjam/YinwuForge

## 功能
- 药水锻造（为装备附加药水效果）
- 属性强化（耐久、伤害、护甲、攻速等）
- 锻造祭坛（自定义多层结构，GUI 锻造）
- 材料体系（30 种浓缩材料，支持 MythicMobs 掉落）

## 共享规则（适用于所有 Yinwu 插件）
本文件继承 `agents.md` 的全部规则，包括：

### 调度规范（Folia）
- ✅ 使用 `RegionizedTask` / `RegionScheduler` / `GlobalRegionScheduler` / `EntityScheduler`
- ❌ 禁止 `Bukkit.getScheduler()`、`runTask`、`runTaskAsynchronously`
- ❌ 初始延迟禁止为 `0L`（必须 ≥ `1L`）

### 代码风格
- 仅输出核心代码，注释极简
- 简洁高效，无冗余逻辑
- 仅使用 Paper / Folia API
- 禁止 NMS 或不安全反射

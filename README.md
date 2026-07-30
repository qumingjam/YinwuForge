# YinwuForge — 锻造系统

Version: **1.2.1**

通过祭坛、材料和药水对装备进行属性强化和药水附加。

## 前置插件

- [YinwuPluginLib](https://github.com/qumingjam/YinwuPluginLib)（必需）

## 功能

- **属性强化** — 6 项属性（基础伤害、攻击速度、护甲值、护甲韧性、挖掘速度、最大耐久），全部 ADD_NUMBER 模式
- **药水锻造** — 为装备附加药水效果（普通/特殊/负面三类）
- **合金锻造** — 按装备类型映射不同的可强化属性，支持自定义属性名称
- **锻造祭坛** — 可配置的多层 5x5 结构，第二层放置额外方块提供成功率加成
- **浓缩材料体系** — 30+ 种材料（material.yml），分为强化/调整/药水三大类，各有不同概率修正
- **收益递减** — 锻造次数越多收益越低（1-5次×1.5, 41+次×0.2）
- **材料品质倍率** — 下界合金×1.25, 钻石×1.0, 铁/铜×0.85 等
- **YinwuEnchant 联动** — 锻造成功时 15%/30% 概率附加随机自定义附魔
- **灾厄强化限制** — 经 YinwuRaid 灾厄强化后的物品不可锻造

## 下载

[YinwuForge-1.2.1.jar](https://github.com/qumingjam/YinwuForge/releases/download/v1.2.1/YinwuForge-1.2.1.jar)

## 技术栈

Java 21, Paper API 1.21+, Folia 兼容
PDC 序列化装备数据
ServicesManager 注册 ForgeAPI

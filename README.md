# YinwuForge — Forge System

Forge system for enhancing equipment through altars, materials, and potions.

## Features

- **Attribute Enhancement** — 6 attributes (damage, attack speed, armor, toughness, mining speed, durability) all ADD_NUMBER
- **Potion Forging** — Attach potion effects to equipment
- **Forge Altar** — Custom multi-layer 5x5 structure with second-layer bonus blocks
- **Concentrated Materials** — 30+ material types (strength/adjuster/potion categories)
- **Diminishing Returns** — Forge count affects yield (×1.5 for 1-5 times, ×0.2 for 41+)
- **YinwuEnchant Integration** — 15%/30% chance to apply custom enchantments on forge success

## Tech Stack

- Java 21, Paper API 1.21+, Folia compatible
- PDC serialization for equipment data
- ServicesManager for ForgeAPI

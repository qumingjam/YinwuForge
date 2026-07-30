package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import com.yinwu.model.ForgeResult;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class AltarManager {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;
    private final ForgeManager forgeManager;
    private ForgeGUI forgeGUI;

    private List<List<String>> structureLayers;
    private Material centerBlock;
    private List<Material> baseBlocks;

    // 第二层加成配置
    private boolean layerBonusEnabled;
    private int maxBonusBlocks;
    private double successBonusPerBlock;
    private double failReductionPerBlock;

    // 活跃祭坛位置集合（用于光柱效果）
    // 使用 ConcurrentHashMap.newKeySet() 确保 updateAltarBeams() 在全局调度器
    // 和区域调度器之间并发迭代/修改时的线程安全
    private final Set<Location> activeAltars = ConcurrentHashMap.newKeySet();
    // 玩家最后使用的祭坛位置
    private final Map<UUID, Location> playerAltars = new ConcurrentHashMap<>();

    public AltarManager(YinwuForgePlugin plugin, ConfigManager configManager, ForgeManager forgeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.forgeManager = forgeManager;
        loadConfig();
    }

    public void setForgeGUI(ForgeGUI forgeGUI) {
        this.forgeGUI = forgeGUI;
    }

    private void loadConfig() {
        FileConfiguration config = configManager.getRawConfig();

        // 加载多层结构配置
        structureLayers = new ArrayList<>();
        List<?> layers = config.getList("altar.structure-layers");
        if (layers != null) {
            for (Object layerObj : layers) {
                if (layerObj instanceof List<?>) {
                    List<String> layer = new ArrayList<>();
                    for (Object rowObj : (List<?>) layerObj) {
                        if (rowObj instanceof String) {
                            layer.add((String) rowObj);
                        }
                    }
                    if (!layer.isEmpty()) {
                        structureLayers.add(layer);
                    }
                }
            }
        }

        // 如果没有配置多层结构，使用默认单层结构
        if (structureLayers.isEmpty()) {
            structureLayers.add(Arrays.asList(
                "BBBBB",
                "BAAAB",
                "BAACAB",
                "BAAAB",
                "BBBBB"
            ));
        }

        // 加载中心方块
        String centerBlockName = config.getString("altar.center-block", "SMITHING_TABLE");
        try {
            centerBlock = Material.valueOf(centerBlockName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的祭坛中心方块配置: " + centerBlockName + "，使用默认值 SMITHING_TABLE");
            centerBlock = Material.SMITHING_TABLE;
        }

        // 加载底座方块（支持多种）
        List<String> baseBlockNames = config.getStringList("altar.base-blocks");
        if (baseBlockNames.isEmpty()) {
            baseBlockNames = Arrays.asList("NETHERITE_BLOCK");
        }
        baseBlocks = new ArrayList<>();
        for (String name : baseBlockNames) {
            try {
                baseBlocks.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的祭坛底座方块配置: " + name);
            }
        }

        // 加载第二层加成配置
        layerBonusEnabled = config.getBoolean("altar.layer-bonus.enabled", true);
        maxBonusBlocks = config.getInt("altar.layer-bonus.max-bonus-blocks", 12);
        successBonusPerBlock = config.getDouble("altar.layer-bonus.success-bonus", 2.0);
        failReductionPerBlock = config.getDouble("altar.layer-bonus.fail-reduction", 1.0);
    }

    public void reloadConfig() {
        loadConfig();
    }

    public boolean handleAltarInteraction(Player player, Block block, Action action) {
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return false;
        }

        if (!isAltarCenter(block)) {
            return false;
        }

        if (!isValidAltarStructure(block)) {
            // 不是完整的祭坛，不发送提示消息，允许打开原版锻造台界面
            return false;
        }

        performAltarForge(player, block);
        return true;
    }

    private boolean isAltarCenter(Block block) {
        return block.getType() == centerBlock;
    }

    private boolean isValidAltarStructure(Block centerBlock) {
        // 检查结构配置是否有效
        if (structureLayers == null || structureLayers.isEmpty()) {
            plugin.getLogger().warning("祭坛结构配置无效");
            return false;
        }

        // 检查中心方块上方是否为空
        Block above = centerBlock.getRelative(BlockFace.UP);
        if (above.getType() != Material.AIR) {
            return false;
        }

        // 只检查第一层（底层）是否完整 - 底层在同一层（Y偏移0）
        List<String> baseLayer = structureLayers.get(structureLayers.size() - 1);
        if (baseLayer.size() != 5) {
            plugin.getLogger().warning("祭坛第一层配置无效，需要 5 行");
            return false;
        }

        // 底层的 Y 偏移是 0（和中心方块同一层）
        int baseYOffset = 0;

        for (int row = 0; row < 5; row++) {
            String rowConfig = baseLayer.get(row);

            if (rowConfig.length() != 5) {
                plugin.getLogger().warning("祭坛第一层第 " + row + " 行配置无效，需要 5 个字符");
                return false;
            }

            for (int col = 0; col < 5; col++) {
                char cell = rowConfig.charAt(col);
                int dx = col - 2;
                int dz = row - 2;

                Block targetBlock = centerBlock.getRelative(dx, baseYOffset, dz);
                Material targetType = targetBlock.getType();

                if (!checkCell(String.valueOf(cell), targetType)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 计算第二层的 B 方块数量（用于加成计算）
     * 根据第二层配置中的 B 位置来检测实际方块，跳过 A 和 C 的位置
     */
    public int countBonusBlocks(Block centerBlock) {
        if (!layerBonusEnabled || structureLayers.size() < 2) {
            return 0;
        }

        List<String> bonusLayer = structureLayers.get(0); // 顶层（第二层）
        int count = 0;
        int yOffset = 1; // 第二层在中心方块上方一格

        for (int row = 0; row < 5 && row < bonusLayer.size(); row++) {
            String rowConfig = bonusLayer.get(row);

            if (rowConfig.length() != 5) {
                continue;
            }

            for (int col = 0; col < 5; col++) {
                char cell = rowConfig.charAt(col);

                // 只检测 B 位置，跳过 A 和 C 的位置
                if (cell != 'B') {
                    continue;
                }

                int dx = col - 2;
                int dz = row - 2;

                Block targetBlock = centerBlock.getRelative(dx, yOffset, dz);
                if (baseBlocks.contains(targetBlock.getType())) {
                    count++;
                }
            }
        }

        return Math.min(count, maxBonusBlocks);
    }

    public int[] getBonusValues(Block centerBlock) {
        int count = countBonusBlocks(centerBlock);
        return new int[]{count, (int)(count * successBonusPerBlock), (int)(count * failReductionPerBlock)};
    }

    public double getSuccessBonus(Block centerBlock) {
        return getBonusValues(centerBlock)[1];
    }

    public double getFailReduction(Block centerBlock) {
        return getBonusValues(centerBlock)[2];
    }

    /**
     * 检查单个格子是否符合配置
     */
    private boolean checkCell(String configCell, Material actualType) {
        return switch (configCell.toUpperCase()) {
            case "C" -> actualType == centerBlock;
            case "B" -> baseBlocks.contains(actualType);
            case "A" -> true;  // A 方块不检测，只作为占位符
            case "X" -> true;
            default -> {
                try {
                    Material requiredType = Material.valueOf(configCell.toUpperCase());
                    yield actualType == requiredType;
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的方块配置: " + configCell);
                    yield false;
                }
            }
        };
    }

    private void performAltarForge(Player player, Block altarBlock) {
        if (forgeGUI == null) {
            player.sendMessage(ChatColor.RED + "锻造系统尚未就绪！");
            return;
        }

        activateAltar(altarBlock);
        playerAltars.put(player.getUniqueId(), altarBlock.getLocation());
        forgeGUI.openForgeGUI(player);
    }

    public double getPlayerSuccessBonus(Player player) {
        Location loc = playerAltars.get(player.getUniqueId());
        if (loc == null) return 0;
        Block block = loc.getBlock();
        if (block.getType() != centerBlock) return 0;
        int[] values = getBonusValues(block);
        return values[1];
    }

    public double getPlayerFailReduction(Player player) {
        Location loc = playerAltars.get(player.getUniqueId());
        if (loc == null) return 0;
        Block block = loc.getBlock();
        if (block.getType() != centerBlock) return 0;
        int[] values = getBonusValues(block);
        return values[2];
    }

    /**
     * 播放玩家当前祭坛的锻造特效
     */
    public void playForgeEffects(Player player, ForgeResult result) {
        Location loc = playerAltars.get(player.getUniqueId());
        if (loc == null) return;
        playForgeEffects(player, loc, result);
    }

    private void playForgeEffects(Player player, Location location, ForgeResult result) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Location baseLoc = location.clone().add(0.5, 1, 0.5);
        world.spawnParticle(Particle.FLAME, baseLoc, 30, 0.5, 1, 0.5, 0.1);
        world.spawnParticle(Particle.SMOKE, baseLoc, 20, 0.3, 0.5, 0.3, 0.05);

        Location effectLoc = location.clone().add(0.5, 1.5, 0.5);

        switch (result) {
            case FAIL_NO_PENALTY:
                world.spawnParticle(Particle.CLOUD, effectLoc, 15, 0.4, 0.3, 0.4);
                world.playSound(location, Sound.BLOCK_STONE_PLACE, 1.0f, 0.8f);
                break;

            case EQUIPMENT_DESTROYED:
                world.spawnParticle(Particle.LAVA, effectLoc, 25, 0.5, 0.5, 0.5, 0.1);
                world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
                break;

            case DOWNGRADE:
                world.spawnParticle(Particle.SOUL, effectLoc, 20, 0.4, 0.4, 0.4);
                world.playSound(location, Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.8f);
                break;

            case SUCCESS:
                world.spawnParticle(Particle.HAPPY_VILLAGER, effectLoc, 20, 0.4, 0.4, 0.4);
                world.spawnParticle(Particle.HEART, effectLoc, 10, 0.3, 0.4, 0.3);
                world.playSound(location, Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
                break;

            case PERFECT:
                world.spawnParticle(Particle.FIREWORK, location.clone().add(0.5, 2, 0.5), 30, 0.5, 1, 0.5);
                world.spawnParticle(Particle.HEART, effectLoc, 20, 0.5, 0.5, 0.5);
                world.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                break;
        }
    }

    public void activateAltar(Block centerBlock) {
        Location loc = centerBlock.getLocation();
        activeAltars.add(loc);
    }

    public void deactivateAltar(Block centerBlock) {
        Location loc = centerBlock.getLocation();
        activeAltars.remove(loc);
    }

    public boolean isAltarActive(Block centerBlock) {
        return activeAltars.contains(centerBlock.getLocation());
    }

    public void updateAltarBeams() {
        for (Location altarLoc : activeAltars) {
            plugin.getServer().getRegionScheduler().run(plugin, altarLoc, (task) -> {
                World world = altarLoc.getWorld();
                if (world == null) {
                    activeAltars.remove(altarLoc);
                    return;
                }

                Block centerBlock = world.getBlockAt(altarLoc);
                if (!isValidAltarStructure(centerBlock)) {
                    activeAltars.remove(altarLoc);
                    return;
                }

                spawnAltarBeam(world, altarLoc);
            });
        }
    }

    private void spawnAltarBeam(World world, Location centerLoc) {
        Location center = centerLoc.clone().add(0.5, 1.0, 0.5);
        Location topCenter = center.clone().add(0, 5, 0);

        Location[] corners = {
            center.clone().add(-2.5, 0, -2.5),
            center.clone().add(2.5, 0, -2.5),
            center.clone().add(-2.5, 0, 2.5),
            center.clone().add(2.5, 0, 2.5)
        };

        for (Location corner : corners) {
            double dx = topCenter.getX() - corner.getX();
            double dy = topCenter.getY() - corner.getY();
            double dz = topCenter.getZ() - corner.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double steps = distance * 2;

            for (int i = 0; i < steps; i++) {
                double progress = i / steps;
                double x = corner.getX() + dx * progress;
                double y = corner.getY() + dy * progress;
                double z = corner.getZ() + dz * progress;

                Location particleLoc = new Location(world, x, y, z);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
        }

        world.spawnParticle(Particle.END_ROD, topCenter, 10, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticle(Particle.SOUL, center, 5, 0.5, 0.5, 0.5, 0.2);
    }

    public Set<Location> getActiveAltars() {
        return new HashSet<>(activeAltars);
    }

    public List<String> getAltarStructureGuide() {
        List<String> guide = new ArrayList<>();
        guide.add(ChatColor.GOLD + "=== 锻造祭坛搭建指南 ===");
        guide.add(ChatColor.WHITE + "中心: " + ChatColor.RED + centerBlock.name().replace("_", " ").toLowerCase());
        guide.add(ChatColor.WHITE + "底座: " + ChatColor.GRAY + baseBlocks.stream()
            .map(m -> m.name().replace("_", " ").toLowerCase())
            .reduce((a, b) -> a + ", " + b)
            .orElse("无"));
        guide.add(ChatColor.WHITE + "要求: 第一层必须完整（5x5）");
        guide.add(ChatColor.GRAY + "  - 中心: 锻造台");
        guide.add(ChatColor.GRAY + "  - 底座: " + baseBlocks.stream()
            .map(m -> m.name().replace("_", " ").toLowerCase())
            .reduce((a, b) -> a + ", " + b)
            .orElse("无"));
        if (layerBonusEnabled) {
            guide.add(ChatColor.WHITE + "第二层加成: 在中心上方一格放置更多底座方块");
            guide.add(ChatColor.GRAY + "  - 每个增加: " + (int)successBonusPerBlock + "% 成功率, -" + (int)failReductionPerBlock + "% 失败率");
            guide.add(ChatColor.GRAY + "  - 最多: " + maxBonusBlocks + " 个");
        }
        guide.add("");
        guide.add(ChatColor.YELLOW + "使用方法:");
        guide.add(ChatColor.GRAY + "1. 将装备放在主手");
        guide.add(ChatColor.GRAY + "2. 将锻造材料放在副手");
        guide.add(ChatColor.GRAY + "3. 右键点击祭坛中心");
        return guide;
    }

    public boolean isNearAltar(Player player) {
        Location loc = player.getLocation();
        int radius = 5;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = loc.getWorld().getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                    if (block.getType() == centerBlock && isValidAltarStructure(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

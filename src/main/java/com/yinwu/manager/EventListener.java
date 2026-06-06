package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import com.yinwu.model.EquipmentAttributes;
import com.yinwu.model.EquipmentData;
import com.yinwu.model.PotionEffectData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventListener implements Listener {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;
    private final ForgeManager forgeManager;
    private final PotionEffectManager potionEffectManager;
    private final AltarManager altarManager;
    private ForgeGUI forgeGUI;
    
    // 存储玩家当前穿戴的装备及其药水效果
    // Key: Player UUID, Value: Map<EquipmentSlot, List<PotionEffectData>>
    // 使用 ConcurrentHashMap 确保跨区域线程（全局调度器 + 玩家区域线程 + 主线程）的并发安全
    private final Map<UUID, Map<EquipmentSlot, List<PotionEffectData>>> playerActiveEffects = new ConcurrentHashMap<>();
    
    private static final String EQUIPMENT_DATA_KEY = "yinwu_equipment_data";
    
    private static final EquipmentSlot[] ALL_EQUIPMENT_SLOTS = {
        EquipmentSlot.HEAD,
        EquipmentSlot.CHEST,
        EquipmentSlot.LEGS,
        EquipmentSlot.FEET,
        EquipmentSlot.HAND,
        EquipmentSlot.OFF_HAND
    };
    
    private static final NamespacedKey ATTR_MINING_SPEED = new NamespacedKey("yinwu", "yinwu_mining_speed");
    private static final NamespacedKey ATTR_DAMAGE = new NamespacedKey("yinwu", "yinwu_damage");
    private static final NamespacedKey ATTR_ATTACK_SPEED = new NamespacedKey("yinwu", "yinwu_attack_speed");
    private static final NamespacedKey ATTR_ARMOR = new NamespacedKey("yinwu", "yinwu_armor");
    private static final NamespacedKey ATTR_ARMOR_TOUGHNESS = new NamespacedKey("yinwu", "yinwu_armor_toughness");
    private static final NamespacedKey ATTR_KNOCKBACK = new NamespacedKey("yinwu", "yinwu_knockback_resistance");
    
    public EventListener(YinwuForgePlugin plugin, ConfigManager configManager, ForgeManager forgeManager, PotionEffectManager potionEffectManager, AltarManager altarManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.forgeManager = forgeManager;
        this.potionEffectManager = potionEffectManager;
        this.altarManager = altarManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // 启动循环任务，每14秒（280 ticks）刷新一次所有在线玩家的药水效果
        startPotionEffectRefreshTask();
        // 启动祭坛光柱效果任务，每20 ticks（1秒）更新一次
        startAltarBeamTask();
    }

    public void setForgeGUI(ForgeGUI forgeGUI) {
        this.forgeGUI = forgeGUI;
    }
    
    /**
     * 启动药水效果刷新任务
     */
    private void startPotionEffectRefreshTask() {
        if (!potionEffectManager.isPotionEffectsEnabled()) {
            return;
        }
        
        // 使用全局区域调度器，每14秒（280 ticks）执行一次
        // 初始延迟1 tick，周期280 ticks
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            refreshAllPlayersPotionEffects();
        }, 1L, 280L);
    }
    
    /**
     * 启动祭坛光柱效果任务
     */
    private void startAltarBeamTask() {
        // 使用全局区域调度器，每20 ticks（1秒）更新一次光柱效果
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, (task) -> {
            updateAltarBeams();
        }, 1L, 20L);
    }
    
    /**
     * 更新所有活跃祭坛的光柱效果
     */
    private void updateAltarBeams() {
        altarManager.updateAltarBeams();
    }
    
    /**
     * 刷新所有在线玩家的药水效果
     * 先同步装备缓存，再重新应用，避免手上没装备时效果仍残留
     */
    private void refreshAllPlayersPotionEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            
            // 在玩家所在区域线程中同步装备缓存
            player.getScheduler().run(plugin, (task) -> {
                checkAndUpdateEquipment(player);
                
                // 从更新后的缓存重新应用效果
                Map<EquipmentSlot, List<PotionEffectData>> playerSlots = playerActiveEffects.get(playerId);
                if (playerSlots == null || playerSlots.isEmpty()) {
                    return;
                }
                
                for (List<PotionEffectData> effects : playerSlots.values()) {
                    if (!effects.isEmpty()) {
                        applyEffectsToPlayer(player, effects);
                    }
                }
            }, null);
        }
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (configManager.getBoolean("debug")) {
            event.getPlayer().sendMessage(ChatColor.GREEN + "欢迎使用 YinwuForge 锻造系统！");
            event.getPlayer().sendMessage(ChatColor.YELLOW + "输入 /yinwu help 查看帮助");
        }
        // 玩家加入时，为其当前穿戴的所有装备应用效果
        applyAllEquipmentEffects(event.getPlayer());
    }
    
    /**
     * 玩家退出时清理数据
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        // 关闭锻造GUI并返回物品
        if (forgeGUI != null) {
            forgeGUI.removePlayer(player);
        }
        // 移除所有药水效果
        removePlayerAllEffects(playerId);
        // 清除缓存数据
        playerActiveEffects.remove(playerId);
    }
    
    /**
     * 监听库存点击事件（装备穿戴/脱下 + GUI锻造点击）
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        // 优先处理锻造GUI点击
        if (forgeGUI != null) {
            forgeGUI.handleClick(event);
            if (event.isCancelled()) return;
        }
        
        // 延迟1 tick处理，等待物品实际装备/脱下
        // Folia 兼容：使用 player.getScheduler()
        player.getScheduler().runDelayed(plugin, (task) -> {
            checkAndUpdateEquipment(player);
        }, null, 1L);  // 初始延迟至少为 1 tick
    }
    
    /**
     * 监听物品交互事件（处理祭坛交互和主副手切换）
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        
        // 处理祭坛交互
        if (block != null && action == Action.RIGHT_CLICK_BLOCK && block.getType() == Material.SMITHING_TABLE) {
            // 只有当祭坛结构完整时才处理锻造并取消事件
            if (altarManager.handleAltarInteraction(player, block, action)) {
                event.setCancelled(true);
                return;
            }
            // 如果不是完整的祭坛，不取消事件，允许打开原版锻造台界面
        }
    }
    
    /**
     * 监听物品损坏事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemBreak(PlayerItemBreakEvent event) {
        ItemStack brokenItem = event.getBrokenItem();
        // 物品损坏时，如果有药水效果需要移除
        // 注意：这里只是事件通知，实际效果移除会在装备槽变化时处理
    }
    
    /**
     * 监听热键栏切换事件（按数字键1-9或滚轮切换）
     * 主手物品变化时更新装备效果缓存
     */
    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(plugin, (task) -> {
            checkAndUpdateEquipment(player);
        }, null, 1L);
    }
    
    /**
     * 检查并更新玩家装备效果
     */
    private void checkAndUpdateEquipment(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 获取之前记录的该玩家的效果
        Map<EquipmentSlot, List<PotionEffectData>> playerSlots = playerActiveEffects.getOrDefault(playerId, new HashMap<>());
        
        for (EquipmentSlot slot : ALL_EQUIPMENT_SLOTS) {
            ItemStack currentItem = player.getInventory().getItem(slot);
            List<PotionEffectData> previousEffects = playerSlots.getOrDefault(slot, new ArrayList<>());
            
            // 如果槽位没有物品，移除旧效果
            if (currentItem == null || currentItem.getType().isAir()) {
                removeEffectsFromPlayer(player, previousEffects);
                playerSlots.remove(slot);
                continue;
            }
            
            // 检查物品是否有锻造数据
            EquipmentData equipmentData = getEquipmentDataFromItem(currentItem);
            
            // 检查物品类型是否匹配当前槽位（头盔在手持槽位不应触发药水效果）
            boolean correctSlot = isItemInCorrectSlot(currentItem, slot);
            List<PotionEffectData> currentEffects = correctSlot ? equipmentData.getPotionEffects() : new ArrayList<>();
            
            // 如果物品没有药水效果（或槽位不匹配），但之前有效果，则移除
            if (currentEffects.isEmpty()) {
                removeEffectsFromPlayer(player, previousEffects);
                playerSlots.put(slot, new ArrayList<>());
            } else if (!isEffectListSame(previousEffects, currentEffects)) {
                // 效果变化了，移除旧效果，添加新效果
                removeEffectsFromPlayer(player, previousEffects);
                applyEffectsToPlayer(player, currentEffects);
                playerSlots.put(slot, new ArrayList<>(currentEffects));
            }
            
            // 更新属性修饰符（无论是否有药水效果都需要应用）
            applyAttributeModifiers(player, slot, currentItem, equipmentData.getAttributes());
            player.getInventory().setItem(slot, currentItem);
        }
        
        playerActiveEffects.put(playerId, playerSlots);
    }
    
    /**
     * 为玩家当前所有装备应用效果
     */
    private void applyAllEquipmentEffects(Player player) {
        UUID playerId = player.getUniqueId();
        Map<EquipmentSlot, List<PotionEffectData>> playerSlots = new HashMap<>();
        
        for (EquipmentSlot slot : ALL_EQUIPMENT_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            
            EquipmentData equipmentData = getEquipmentDataFromItem(item);
            
            // 检查物品类型是否匹配当前槽位（头盔在手持槽位不应触发药水效果）
            if (isItemInCorrectSlot(item, slot)) {
                List<PotionEffectData> effects = equipmentData.getPotionEffects();
                if (!effects.isEmpty()) {
                    applyEffectsToPlayer(player, effects);
                    playerSlots.put(slot, new ArrayList<>(effects));
                }
            }
            
            // 应用属性修饰符
            applyAttributeModifiers(player, slot, item, equipmentData.getAttributes());
            // 将修改后的物品重新设置回玩家背包
            player.getInventory().setItem(slot, item);
        }
        
        playerActiveEffects.put(playerId, playerSlots);
    }
    
    /**
     * 移除玩家所有效果（退出时调用）
     */
    private void removePlayerAllEffects(UUID playerId) {
        Map<EquipmentSlot, List<PotionEffectData>> playerSlots = playerActiveEffects.get(playerId);
        if (playerSlots == null) {
            return;
        }
        
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        
        for (List<PotionEffectData> effects : playerSlots.values()) {
            removeEffectsFromPlayer(player, effects);
        }
    }
    
    /**
     * 给玩家添加药水效果
     */
    private void applyEffectsToPlayer(Player player, List<PotionEffectData> effects) {
        if (!potionEffectManager.isPotionEffectsEnabled()) {
            return;
        }
        
        for (PotionEffectData effectData : effects) {
            PotionEffectType type = PotionEffectType.getByName(effectData.getEffectName());
            if (type != null) {
                // 持续时间设置为14秒（280 ticks），通过循环任务每14秒刷新一次
                // 放大倍数为 level - 1（Minecraft 中 0=I级, 1=II级）
                int amplifier = Math.max(0, effectData.getLevel() - 1);
                PotionEffect effect = new PotionEffect(type, 280, amplifier, true, false, true);
                player.addPotionEffect(effect);
            }
        }
    }
    
    /**
     * 从玩家移除药水效果
     */
    private void removeEffectsFromPlayer(Player player, List<PotionEffectData> effects) {
        for (PotionEffectData effectData : effects) {
            PotionEffectType type = PotionEffectType.getByName(effectData.getEffectName());
            if (type != null && player.hasPotionEffect(type)) {
                // 检查是否有其他装备也提供同样的效果
                if (!hasOtherEquipmentWithEffect(player, effectData)) {
                    player.removePotionEffect(type);
                }
            }
        }
    }
    
    /**
     * 检查是否有其他装备也提供同样的效果
     */
    private boolean hasOtherEquipmentWithEffect(Player player, PotionEffectData effectData) {
        for (EquipmentSlot slot : ALL_EQUIPMENT_SLOTS) {
            ItemStack item = player.getInventory().getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            
            EquipmentData equipmentData = getEquipmentDataFromItem(item);
            for (PotionEffectData effect : equipmentData.getPotionEffects()) {
                if (effect.getEffectName().equals(effectData.getEffectName())) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 应用属性修饰符到装备
     * 使用硬编码基础值 + 锻造加成一次性设置，兼容 Paper 1.21.4 Data Component 系统
     */
    private void applyAttributeModifiers(Player player, EquipmentSlot slot, ItemStack item, EquipmentAttributes attributes) {
        if (attributes == null || !attributes.hasAnyAttribute()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        Material material = item.getType();
        
        // 获取当前已有修饰符
        Multimap<Attribute, AttributeModifier> allModifiers = meta.getAttributeModifiers();
        if (allModifiers == null) {
            allModifiers = ArrayListMultimap.create();
        } else {
            allModifiers = ArrayListMultimap.create(allModifiers);
        }
        
        // 移除所有旧的 yinwu 自定义修饰符
        List<Map.Entry<Attribute, AttributeModifier>> toRemove = new ArrayList<>();
        for (Map.Entry<Attribute, AttributeModifier> entry : allModifiers.entries()) {
            if ("yinwu".equals(entry.getValue().getKey().getNamespace())) {
                toRemove.add(entry);
            }
        }
        for (Map.Entry<Attribute, AttributeModifier> entry : toRemove) {
            allModifiers.remove(entry.getKey(), entry.getValue());
        }
        
        // ===== 武器伤害/攻速：硬编码基础值 + 锻造加成 =====
        if (attributes.getBaseDamage() != null && attributes.getBaseDamage() != 0) {
            Double baseDamage = ForgeManager.WEAPON_BASE_DAMAGE.get(material);
            if (baseDamage != null) {
                allModifiers.put(Attribute.ATTACK_DAMAGE, new AttributeModifier(
                    ATTR_DAMAGE,
                    baseDamage + attributes.getBaseDamage(),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
                ));
            }
            if (configManager.getBoolean("debug")) {
                plugin.getLogger().info("[属性应用] 综合伤害修饰符: 基础=" + baseDamage + ", 锻造=" + attributes.getBaseDamage());
            }
        }
        
        if (attributes.getAttackSpeed() != null && attributes.getAttackSpeed() != 0) {
            Double baseSpeed = ForgeManager.WEAPON_BASE_SPEED.get(material);
            if (baseSpeed != null) {
                allModifiers.put(Attribute.ATTACK_SPEED, new AttributeModifier(
                    ATTR_ATTACK_SPEED,
                    (baseSpeed - ForgeManager.BASE_PLAYER_ATTACK_SPEED) + (attributes.getAttackSpeed() * 0.2),
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
                ));
            }
        }
        
        // 挖掘速度（工具专用）
        if (attributes.getMiningSpeed() != null && attributes.getMiningSpeed() != 0) {
            allModifiers.put(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(
                ATTR_MINING_SPEED,
                attributes.getMiningSpeed().doubleValue() * 0.2,
                AttributeModifier.Operation.MULTIPLY_SCALAR_1,
                EquipmentSlotGroup.MAINHAND
            ));
        }
        
        // ===== 盔甲：始终设置总盔甲值/韧性（基础值 + 锻造加成）=====
        if (isArmorType(material)) {
            EquipmentSlotGroup slotGroup = ForgeManager.getArmorSlotGroup(material);
            Double baseArmor = ForgeManager.ARMOR_BASE_VALUES.get(material);
            if (baseArmor == null) baseArmor = 0.0;
            double forgeArmor = (attributes.getArmorValue() != null ? attributes.getArmorValue() : 0);
            allModifiers.put(Attribute.ARMOR, new AttributeModifier(
                ATTR_ARMOR,
                baseArmor + forgeArmor,
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            ));
            
            Double baseToughness = ForgeManager.ARMOR_TOUGHNESS_BASE_VALUES.get(material);
            if (baseToughness == null) baseToughness = 0.0;
            double forgeToughness = (attributes.getArmorToughness() != null ? attributes.getArmorToughness() : 0);
            allModifiers.put(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(
                ATTR_ARMOR_TOUGHNESS,
                baseToughness + forgeToughness,
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            ));
            
            // 击退抗性（仅下界合金系列有基础值）
            Double baseKB = ForgeManager.KNOCKBACK_RESISTANCE_BASE_VALUES.get(material);
            if (baseKB != null && baseKB > 0) {
                allModifiers.put(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    ATTR_KNOCKBACK,
                    baseKB,
                    AttributeModifier.Operation.ADD_NUMBER,
                    slotGroup
                ));
            }
        }
        
        meta.setAttributeModifiers(allModifiers);
        
        // 处理最大耐久度
        if (attributes.getMaxDurability() != null && attributes.getMaxDurability() != 0) {
            try {
                if (meta instanceof Damageable damageable) {
                    applyMaxDurability(item, damageable, attributes.getMaxDurability());
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[属性应用] 无法应用耐久度修改: " + e.getMessage());
            }
        }
        
        item.setItemMeta(meta);
    }
    
    /**
     * 添加属性修饰符 (兼容 1.21+ API)
     */
    private void addAttributeModifier(ItemMeta meta, EquipmentSlot slot, Attribute attribute, NamespacedKey key, Integer value) {
        if (value == null || value == 0) {
            plugin.getLogger().fine("属性值为空或0，跳过: " + key);
            return;
        }
        
        try {
            // 先移除可能存在的旧修饰符
            Collection<AttributeModifier> existingModifiers = meta.getAttributeModifiers(attribute);
            if (existingModifiers != null) {
                for (AttributeModifier modifier : existingModifiers) {
                    if (modifier.getKey().equals(key)) {
                        meta.removeAttributeModifier(attribute, modifier);
                        plugin.getLogger().fine("已移除旧的属性修饰符: " + key);
                        break;
                    }
                }
            }
            
            // 添加新的修饰符 - 1.21+ 使用 NamespacedKey 和 EquipmentSlotGroup
            EquipmentSlotGroup slotGroup = convertToSlotGroup(slot);
            AttributeModifier modifier = new AttributeModifier(
                key,
                value.doubleValue(),
                AttributeModifier.Operation.ADD_NUMBER,
                slotGroup
            );
            meta.addAttributeModifier(attribute, modifier);
            plugin.getLogger().fine("成功添加属性修饰符: " + key + " = " + value + " (槽位: " + slot + ")");
        } catch (Exception e) {
            plugin.getLogger().warning("添加属性修饰符失败: " + key + " - " + e.getMessage());
        }
    }
    
    /**
     * 将 EquipmentSlot 转换为 EquipmentSlotGroup
     */
    private EquipmentSlotGroup convertToSlotGroup(EquipmentSlot slot) {
        return switch (slot) {
            case HAND -> EquipmentSlotGroup.HAND;
            case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            default -> EquipmentSlotGroup.ANY;
        };
    }
    
    /**
     * 应用最大耐久度修改
     * @param item 物品栈
     * @param damageable 可损坏的物品元数据
     * @param durabilityBonus 耐久加成值（每+1增加150点耐久）
     */
    private void applyMaxDurability(ItemStack item, Damageable damageable, int durabilityBonus) {
        // 获取物品类型的原始基础耐久值，而不是当前物品的耐久值
        int baseMaxDurability = getDefaultMaxDurability(item);
        
        if (baseMaxDurability <= 0) {
            plugin.getLogger().warning("[属性应用] 无法获取物品的默认耐久值，跳过耐久修改");
            return;
        }
        
        // 检查物品是否有最大耐久属性（避免不可损坏物品的异常）
        if (!damageable.hasMaxDamage()) {
            plugin.getLogger().warning("[属性应用] 物品没有最大耐久属性，跳过耐久修改");
            return;
        }
        
        // 计算新的最大耐久：基础耐久 + (加成值 * 150)
        int newMaxDurability = baseMaxDurability + (durabilityBonus * 150);
        
        // 获取当前已损失的耐久
        int currentDamage = damageable.getDamage();
        
        if (configManager.getBoolean("debug")) {
            plugin.getLogger().info("[属性应用] 应用耐久修改: 基础=" + baseMaxDurability + ", 加成=" + durabilityBonus 
                + ", 新最大耐久=" + newMaxDurability + ", 当前损失=" + currentDamage);
        }
        
        // 使用 setMaxDamage 直接修改（必须在 setItemMeta 之前调用）
        damageable.setMaxDamage(newMaxDurability);
        // 重置为满耐久
        damageable.setDamage(0);
    }
    
    /**
     * 获取物品类型的默认最大耐久值
     * @param item 物品栈
     * @return 默认最大耐久值
     */
    private int getDefaultMaxDurability(ItemStack item) {
        // 创建一个新的相同类型的物品来获取默认耐久值
        Material material = item.getType();
        ItemStack defaultItem = new ItemStack(material);
        ItemMeta defaultMeta = defaultItem.getItemMeta();
        
        if (defaultMeta instanceof Damageable defaultDamageable && defaultDamageable.hasMaxDamage()) {
            return defaultDamageable.getMaxDamage();
        }
        
        // 根据物品类型返回默认值作为后备
        return switch (material) {
            case NETHERITE_PICKAXE, NETHERITE_AXE, NETHERITE_SWORD, NETHERITE_SHOVEL, NETHERITE_HOE,
                 NETHERITE_HELMET, NETHERITE_CHESTPLATE, NETHERITE_LEGGINGS, NETHERITE_BOOTS -> 2031;
            case DIAMOND_PICKAXE, DIAMOND_AXE, DIAMOND_SWORD, DIAMOND_SHOVEL, DIAMOND_HOE,
                 DIAMOND_HELMET, DIAMOND_CHESTPLATE, DIAMOND_LEGGINGS, DIAMOND_BOOTS -> 1561;
            case IRON_PICKAXE, IRON_AXE, IRON_SWORD, IRON_SHOVEL, IRON_HOE,
                 IRON_HELMET, IRON_CHESTPLATE, IRON_LEGGINGS, IRON_BOOTS -> 250;
            case STONE_PICKAXE, STONE_AXE, STONE_SWORD, STONE_SHOVEL, STONE_HOE -> 131;
            case WOODEN_PICKAXE, WOODEN_AXE, WOODEN_SWORD, WOODEN_SHOVEL, WOODEN_HOE,
                 LEATHER_HELMET, LEATHER_CHESTPLATE, LEATHER_LEGGINGS, LEATHER_BOOTS -> 60;
            case GOLDEN_PICKAXE, GOLDEN_AXE, GOLDEN_SWORD, GOLDEN_SHOVEL, GOLDEN_HOE,
                 GOLDEN_HELMET, GOLDEN_CHESTPLATE, GOLDEN_LEGGINGS, GOLDEN_BOOTS -> 33;
            default -> 1561;
        };
    }
    
    /**
     * 从物品获取锻造数据
     */
    private EquipmentData getEquipmentDataFromItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return new EquipmentData();
        }
        
        NamespacedKey key = new NamespacedKey(plugin, EQUIPMENT_DATA_KEY);
        String data = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        
        if (data == null || data.isEmpty()) {
            return new EquipmentData();
        }
        
        // 使用 ForgeManager 的公开方法解析
        return forgeManager.getEquipmentData(item);
    }
    
    /**
     * 比较两个效果列表是否相同
     */
    private boolean isEffectListSame(List<PotionEffectData> list1, List<PotionEffectData> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        
        for (PotionEffectData effect1 : list1) {
            boolean found = false;
            for (PotionEffectData effect2 : list2) {
                if (effect1.getEffectName().equals(effect2.getEffectName()) &&
                    effect1.getLevel() == effect2.getLevel() &&
                    effect1.isSpecial() == effect2.isSpecial()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 判断是否为盔甲类型
     */
    private boolean isArmorType(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || material == Material.ELYTRA;
    }
    
    /**
     * 判断物品是否在正确的装备槽位中（盔甲在手持槽位不应触发效果）
     */
    private boolean isItemInCorrectSlot(ItemStack item, EquipmentSlot slot) {
        if (item == null) return false;
        Material material = item.getType();
        String name = material.name();
        
        // 非盔甲类物品（武器、工具等）在任何槽位都有效
        if (!isArmorType(material)) return true;
        
        // 盔甲类需要匹配正确槽位
        if (name.endsWith("_HELMET") || material == Material.TURTLE_HELMET) return slot == EquipmentSlot.HEAD;
        if (name.endsWith("_CHESTPLATE")) return slot == EquipmentSlot.CHEST;
        if (name.endsWith("_LEGGINGS")) return slot == EquipmentSlot.LEGS;
        if (name.endsWith("_BOOTS")) return slot == EquipmentSlot.FEET;
        if (material == Material.ELYTRA) return slot == EquipmentSlot.CHEST;
        return true;
    }
    
    /**
     * 从全新同类型物品读取默认基础属性值
     */
    private double getBaseAttributeValue(Material material, Attribute attribute) {
        ItemStack defaultItem = new ItemStack(material);
        ItemMeta defaultMeta = defaultItem.getItemMeta();
        if (defaultMeta != null) {
            Collection<AttributeModifier> mods = defaultMeta.getAttributeModifiers(attribute);
            if (mods != null) {
                double total = 0;
                for (AttributeModifier mod : mods) {
                    total += mod.getAmount();
                }
                return total;
            }
        }
        return 0;
    }
}
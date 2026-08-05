package com.yinwu.manager;

import com.yinwu.YinwuForgePlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandler implements TabExecutor {
    private final YinwuForgePlugin plugin;
    private final ConfigManager configManager;
    private final ForgeManager forgeManager;
    private final PotionEffectManager potionEffectManager;
    private final AltarManager altarManager;
    private final MaterialConfig materialConfig;

    private static final List<String> GIVE_TYPES = Arrays.asList("potion", "concentrated");

    public CommandHandler(YinwuForgePlugin plugin, ConfigManager configManager,
                          ForgeManager forgeManager, PotionEffectManager potionEffectManager,
                          AltarManager altarManager, MaterialConfig materialConfig) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.forgeManager = forgeManager;
        this.potionEffectManager = potionEffectManager;
        this.altarManager = altarManager;
        this.materialConfig = materialConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家使用！");
            return true;
        }

        if (!player.hasPermission("yinwu.forge.use")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return true;
        }

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reload" -> handleReload(player);
                case "give" -> handleGive(player, args);
                default -> sendHelp(player);
            }
        } else {
            sendHelp(player);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            commands.add("reload");
            commands.add("give");
            StringUtil.copyPartialMatches(args[0], commands, completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (sender.hasPermission("yinwu.forge.admin")) {
                StringUtil.copyPartialMatches(args[1], GIVE_TYPES, completions);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")
            && args[1].equalsIgnoreCase("concentrated")) {
            if (sender.hasPermission("yinwu.forge.admin")) {
                List<String> ids = new ArrayList<>();
                for (MaterialConfig.ConcentratedMat cm : materialConfig.getAllConcentrated()) {
                    ids.add(cm.id);
                }
                StringUtil.copyPartialMatches(args[2], ids, completions);
            }
        }

        return completions;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== YinwuForge 帮助 ===");
        player.sendMessage(ChatColor.YELLOW + "/yf reload" + ChatColor.GRAY + " - 重载配置");
        if (player.hasPermission("yinwu.forge.admin")) {
            player.sendMessage(ChatColor.YELLOW + "/yf give potion" + ChatColor.GRAY + " - 获取药水锻造材料");
            player.sendMessage(ChatColor.YELLOW + "/yf give concentrated <id>" + ChatColor.GRAY + " - 获取浓缩材料");
        }
        player.sendMessage(ChatColor.GRAY + "在锻造祭坛右键打开锻造GUI");
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("yinwu.forge.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限重载配置。");
            return;
        }

        configManager.reload();
        potionEffectManager.reload();
        plugin.getPotionForgeConfig().reload();
        plugin.getAlloyForgeConfig().reload();
        materialConfig.reload();
        altarManager.reloadConfig();
        forgeManager.reload();
        player.sendMessage(ChatColor.GREEN + "配置已成功重载！");

        if (configManager.getBoolean("debug")) {
            plugin.getLogger().info("配置已被 " + player.getName() + " 重载");
        }
    }

    private void handleGive(Player player, String[] args) {
        if (!player.hasPermission("yinwu.forge.admin")) {
            player.sendMessage(ChatColor.RED + "你没有权限使用此命令。");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /yf give <potion|concentrated> [id]");
            player.sendMessage(ChatColor.GRAY + "  potion - 获取药水锻造材料");
            player.sendMessage(ChatColor.GRAY + "  concentrated <id> - 获取浓缩材料");
            return;
        }

        String type = args[1].toLowerCase();
        int amount = 64;

        switch (type) {
            case "potion" -> givePotionForgeMaterial(player, amount);
            case "concentrated" -> {
                if (args.length < 3) {
                    player.sendMessage(ChatColor.RED + "请指定浓缩材料ID！");
                    return;
                }
                giveConcentratedMaterial(player, args[2], amount);
            }
            default -> player.sendMessage(ChatColor.RED + "无效的类型！使用 potion 或 concentrated");
        }
    }

    private void givePotionForgeMaterial(Player player, int amount) {
        String customName = plugin.getPotionForgeConfig().getCustomName();
        String materialName = plugin.getPotionForgeConfig().getMaterialType();

        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的药水锻造材料: " + materialName);
            player.sendMessage(ChatColor.RED + "配置错误：无效的药水锻造材料类型");
            return;
        }

        ItemStack item = new ItemStack(material, amount);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        if (customName != null && !customName.isEmpty()) {
            meta.setDisplayName(customName);
        } else {
            meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "锻造奇点");
        }

        item.setItemMeta(meta);
        player.getInventory().addItem(item);

        String displayName = customName != null && !customName.isEmpty() ?
            ChatColor.stripColor(customName) : "药水锻造材料";
        player.sendMessage(ChatColor.GREEN + "已给予 " + amount + " 个" + displayName);
    }

    private void giveConcentratedMaterial(Player player, String id, int amount) {
        ItemStack item = materialConfig.createItem(id, amount);
        if (item == null) {
            player.sendMessage(ChatColor.RED + "无效的浓缩材料ID: " + id);
            return;
        }
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "已给予 " + amount + " 个 " + id);
    }
}

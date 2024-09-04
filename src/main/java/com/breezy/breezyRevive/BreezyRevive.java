package com.breezy.breezyRevive;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BreezyRevive extends JavaPlugin implements Listener {

    // 用于跟踪每个玩家的复活任务，防止重复任务
    private Map<UUID, BukkitRunnable> reviveTasks;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        reviveTasks = new HashMap<>();
        getLogger().info("BreezyRevive 插件已启用!");
    }

    @Override
    public void onDisable() {
        // 取消所有正在运行的复活任务
        for (BukkitRunnable task : reviveTasks.values()) {
            task.cancel();
        }
        reviveTasks.clear();
        getLogger().info("BreezyRevive 插件已禁用!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();
        Location respawnLocation = player.getBedSpawnLocation(); // 获取玩家的重生点
        int reviveCountdown = 10; // 倒计时10秒

        // 切换到旁观者模式
        player.setGameMode(GameMode.SPECTATOR);


        // 启动复活倒计时
        startReviveCountdown(player, deathLocation, respawnLocation, reviveCountdown);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 检查玩家当前的游戏模式是否不是生存模式 不是的话启动复活程序
        if (player.getGameMode() != GameMode.SURVIVAL) {
            Location currentLocation = player.getLocation();
            Location respawnLocation = player.getBedSpawnLocation(); // 获取玩家的重生点
            int reviveCountdown = 10; // 倒计时10秒

            // 启动复活倒计时
            startReviveCountdown(player, currentLocation, respawnLocation, reviveCountdown);
        }
    }


    private void startReviveCountdown(Player player, Location deathLocation, Location respawnLocation, int countdownSeconds) {
        UUID playerId = player.getUniqueId();

        // 如果玩家已经有一个复活任务在运行，则不启动新的任务
        if (reviveTasks.containsKey(playerId)) {
            return;
        }

        BukkitRunnable task = new BukkitRunnable() {
            int timeLeft = countdownSeconds;

            @Override
            public void run() {
                if (timeLeft > 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 3, false, true));
                    player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "❤" + ChatColor.DARK_GRAY + "]"
                            + ChatColor.GRAY + " 您将在 " + ChatColor.GOLD + timeLeft + ChatColor.GRAY + " 秒后复活!");
                    timeLeft--;
                } else {
                    // 复活玩家
                    player.spigot().respawn();

                    if (respawnLocation != null) {
                        // 玩家有重生点，传送到重生点
                        player.teleport(respawnLocation);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.sendTitle("§3☠", "§3小心行事!", 20, 30, 20);
                        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "❤" + ChatColor.DARK_GRAY + "]"
                                + ChatColor.GRAY + " 您已复活, 请小心行事!");
                        // 添加缓慢效果，持续0.5秒
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 10, 1, false, true));
                    } else {
                        // 玩家没有重生点，随机传送
                        Location randomLocation = getRandomLocation(player.getWorld(), deathLocation, 300);
                        player.teleport(randomLocation);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.sendTitle("§c☠", "§3小心行事!", 20, 30, 20);
                        player.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "❤" + ChatColor.DARK_GRAY + "]"
                                + ChatColor.GRAY + " 您已复活, 请小心行事!");
                        // 添加缓慢效果，持续30秒
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 600, 1, false, true));
                    }

                    // 取消任务并从任务列表中移除
                    this.cancel();
                    reviveTasks.remove(playerId);
                }
            }
        };

        // 将任务添加到任务列表
        reviveTasks.put(playerId, task);
        // 启动任务，每秒执行一次
        task.runTaskTimer(this, 0, 20);
    }


    private Location getRandomLocation(World world, Location origin, int radius) {
        Random random = new Random();
        double x = origin.getX() + (random.nextDouble() * 2 - 1) * radius;
        double z = origin.getZ() + (random.nextDouble() * 2 - 1) * radius;
        // 确保坐标在世界边界内
        x = Math.max(-30000000, Math.min(30000000, x));
        z = Math.max(-30000000, Math.min(30000000, z));
        // 获取地面高度
        int y = world.getHighestBlockYAt((int) x, (int) z) + 1;
        return new Location(world, x, y, z);
    }
}

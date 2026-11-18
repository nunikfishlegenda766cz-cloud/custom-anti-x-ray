package com.example.antixray;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AntiXrayPlugin extends JavaPlugin implements Listener, CommandExecutor {

    // Sleduje čas posledního vytěžení diamantu a počítá rychlost
    private final Map<UUID, Long> lastDiamondTime = new HashMap<>();
    private final Map<UUID, Integer> consecutiveDiamonds = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("antixray").setExecutor(this);
        getLogger().info("AntiXrayPlugin byl úspěšně spuštěn!");
    }

    @Override
    public void onDisable() {
        lastDiamondTime.clear();
        consecutiveDiamonds.clear();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Admini s právem mají výjimku (aby mohli zkoušet bedny/těžit bez omezení)
        if (player.hasPermission("antixray.bypass")) {
            return;
        }

        // Sledujeme diamantové rudy
        if (block.getType() == Material.DIAMOND_ORE || block.getType() == Material.DEEPSLATE_DIAMOND_ORE) {
            UUID uuid = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            
            long lastTime = lastDiamondTime.getOrDefault(uuid, 0L);
            long diff = currentTime - lastTime;

            // Pokud hráč vykope další diamant za méně než 1.5 sekundy (900 ms), je to podezřele rychlé (X-ray)
            if (diff < 1500) {
                int count = consecutiveDiamonds.getOrDefault(uuid, 0) + 1;
                consecutiveDiamonds.put(uuid, count);

                // Pokud to udělá 3x za sebou moc rychle, zablokujeme mu těžbu
                if (count >= 3) {
                    event.setCancelled(true); // Zruší vytěžení bloku!
                    player.sendActionBar(ChatColor.RED + "⚠ Detekován X-ray! Těžíš příliš rychle.");
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);

                    // Upozorníme adminy v konzoli a ve hře
                    String alert = ChatColor.RED + "[AntiXray] Hráč " + player.getName() + " byl zastaven při podezřelé těžbě diamantů!";
                    getLogger().warning(alert);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.hasPermission("antixray.admin")) {
                            p.sendMessage(alert);
                        }
                    }
                    return;
                }
            } else {
                // Resetujeme počítadlo, pokud těží normální rychlostí
                consecutiveDiamonds.put(uuid, 1);
            }

            lastDiamondTime.put(uuid, currentTime);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antixray.admin")) {
            sender.sendMessage(ChatColor.RED + "Na tento příkaz nemáš práva!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "--- AntiXrayPlugin ---");
        sender.sendMessage(ChatColor.GREEN + "Plugin aktivně chrání server před rychlým X-ray těžením.");
        return true;
    }
}

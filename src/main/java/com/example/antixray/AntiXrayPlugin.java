package com.example.antixray;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

    // Sleduje počet vytěžených diamantů za poslední dobu pro detekci X-ray
    private final Map<UUID, Integer> diamondCounts = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("antixray").setExecutor(this);

        // Každou minutu resetujeme počitadla, abychom sledovali rychlost těžby
        Bukkit.getScheduler().runTaskTimer(this, diamondCounts::clear, 0L, 1200L);

        getLogger().info("AntiXrayPlugin byl úspěšně spuštěn!");
    }

    @Override
    public void onDisable() {
        diamondCounts.clear();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Sledujeme, když hráč vyteží diamantovou rudu
        if (block.getType() == Material.DIAMOND_ORE || block.getType() == Material.DEEPSLATE_DIAMOND_ORE) {
            UUID uuid = player.getUniqueId();
            int count = diamondCounts.getOrDefault(uuid, 0) + 1;
            diamondCounts.put(uuid, count);

            // Pokud hráč vyteží podezřele hodně diamantů v krátkém čase, upozorní to adminy
            if (count >= 5) {
                String alertMessage = ChatColor.RED + "[AntiXray] Hráč " + player.getName() + " těží diamanty podezřele rychle! (" + count + "x)";
                
                // Pošleme upozornění všem adminům online
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("antixray.admin")) {
                        p.sendMessage(alertMessage);
                    }
                }
                getLogger().warning(alertMessage);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antixray.admin")) {
            sender.sendMessage(ChatColor.RED + "Na tento příkaz nemáš práva!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "--- AntiXrayPlugin ---");
        sender.sendMessage(ChatColor.GREEN + "Plugin je aktivní a sleduje podezřelou těžbu diamantů.");
        return true;
    }
}

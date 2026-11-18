package com.example.antixray;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AntiXrayPlugin extends JavaPlugin implements Listener, CommandExecutor {

    // Seznam bloků, které chceme před X-ray schovat (změnit na kámen)
    private final Set<Material> targetOres = new HashSet<>();
    // Pamatuje si pozice skrytých rud, které byly změněny na kámen
    private final Set<String> disguisedBlocks = new HashSet<>();

    @Override
    public void onEnable() {
        // Definujeme rudy, které budeme maskovat
        targetOres.add(Material.DIAMOND_ORE);
        targetOres.add(Material.DEEPSLATE_DIAMOND_ORE);
        targetOres.add(Material.GOLD_ORE);
        targetOres.add(Material.DEEPSLATE_GOLD_ORE);

        getServer().getPluginManager().registerEvents(this, this);
        getCommand("antixray").setExecutor(this);

        // Každou sekundu projdeme okolí hráčů a zamaskujeme rudy v dosahu X-ray
        Bukkit.getScheduler().runTaskTimer(this, this::hideOresAroundPlayers, 0L, 20L);

        getLogger().info("AntiXrayPlugin (Block Disguise) byl úspěšně spuštěn!");
    }

    @Override
    public void onDisable() {
        disguisedBlocks.clear();
    }

    // Metoda pro skrytí rud v okolí hráče
    private void hideOresAroundPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("antixray.bypass")) continue;

            Location loc = player.getLocation();
            int radius = 5; // Okruh bloků kolem hráče, kde se rudy schovají

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block block = loc.clone().add(x, y, z).getBlock();
                        
                        // Pokud je to ruda, pošleme hráči falešnou informaci / změníme ji na kámen,
                        // pokud není odkrytá (má vedle sebe vzduch)
                        if (targetOres.contains(block.getType())) {
                            // Pro zjednodušení a výkon v jednom souboru: pokud je blok obklopen kamenem, zamaskujeme ho
                            if (isSurroundedBySolid(block)) {
                                String coordKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
                                
                                // Změníme blok vizuálně pro hráče (nebo fyzicky na kámen, dokud se k němu nepřiblíží)
                                // Tady pro ukázku pošleme blokchange přímo danému hráči, aby viděl kámen místo rudy!
                                if (!disguisedBlocks.contains(coordKey)) {
                                    player.sendBlockChange(block.getLocation(), Material.STONE.createBlockData());
                                    disguisedBlocks.add(coordKey);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Pomocná metoda – zjistí, jestli je blok schovaný v zemi (nemá vedle sebe vzduch)
    private boolean isSurroundedBySolid(Block block) {
        return block.getRelative(0, 1, 0).getType().isOccluding() &&
               block.getRelative(0, -1, 0).getType().isOccluding() &&
               block.getRelative(1, 0, 0).getType().isOccluding() &&
               block.getRelative(-1, 0, 0).getType().isOccluding();
    }

    // Když hráč kope, zkontrolujeme, jestli nekope do skryté rudy
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        String coordKey = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();

        // Pokud to byla ruda, kterou jsme maskovali, vrátíme jí správný drop
        if (targetOres.contains(block.getType())) {
            disguisedBlocks.remove(coordKey);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antixray.admin")) {
            sender.sendMessage(ChatColor.RED + "Na tento příkaz nemáš práva!");
            return true;
        }

        sender.sendMessage(ChatColor.YELLOW + "--- AntiXrayPlugin ---");
        sender.sendMessage(ChatColor.GREEN + "Maska rud (X-ray obfuscation) je aktivní!");
        return true;
    }
}

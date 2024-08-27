package me.crunchycars.rotatingMapSystem;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;

import java.util.*;

public final class RotatingMapSystem extends JavaPlugin implements Listener {

    private int currentMapIndex = 0;
    private final String[] maps = {"Sahara", "Kuroko", "Icy Caverns"};
    private final Set<Player> playersInMenu = new HashSet<>();
    private int timeRemaining = 30 * 60; // 30 minutes countdown
    private final Set<UUID> restrictedWorldUUIDs = new HashSet<>(Arrays.asList(
            UUID.fromString("ab30c279-7fdd-4ba9-83b8-8cf4644502b6"), // sahara
            UUID.fromString("a84b8efb-bb68-49f1-9074-6d3d3384f561"), // caverns
            UUID.fromString("f6294f42-24f7-4f6c-984f-f37b75c0254e")  // kuroko
    ));

    @Override
    public void onEnable() {
        // Register the commands and event listener
        this.getCommand("adventure").setExecutor(this);
        this.getCommand("setmaptime").setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Start the shared map rotation task
        startMapRotation();

        getLogger().info("Rotator plugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            try {
                if (label.equalsIgnoreCase("adventure")) {
                    // Check if the player's current world is in the restricted worlds set
                    if (restrictedWorldUUIDs.contains(player.getWorld().getUID())) {
                        player.sendMessage(ChatColor.RED + ""+ ChatColor.BOLD + "(!)" + ChatColor.RED +  "You cannot use the /adventure command while in this world.");
                        return true;
                    }
                    openAdventureMenu(player);
                    return true;
                } else if (label.equalsIgnoreCase("timer")) {
                    String formattedTime = formatTime(timeRemaining);
                    String timerMessage = getConfig().getString("messages.timer_message")
                            .replace("%map_name%", maps[currentMapIndex])
                            .replace("%time_remaining%", formattedTime);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', timerMessage));
                    return true;
                } else if (label.equalsIgnoreCase("setmaptime")) {
                    // Ensure the command sender is an admin
                    if (player.hasPermission("rotatingmapsystem.admin")) {
                        if (args.length == 1) {
                            try {
                                int newTime = Integer.parseInt(args[0]);
                                timeRemaining = newTime;
                                player.sendMessage(ChatColor.GREEN + "Time remaining for the current map has been set to " + newTime + " seconds.");
                                return true;
                            } catch (NumberFormatException e) {
                                player.sendMessage(ChatColor.RED + "Invalid time value. Please enter a number.");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Usage: /setmaptime [value]");
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                        return true;
                    }
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "An error occurred: " + e.getMessage());
                e.printStackTrace(); // Print the stack trace to the console for debugging
            }
        } else {
            sender.sendMessage("Only players can use this command.");
        }
        return false;
    }

    public ItemStack createIcyCavernsItem() {
        ItemStack icyCavernsItem = new ItemStack(Material.ICE);  // Using ICE for Icy Caverns
        ItemMeta meta = icyCavernsItem.getItemMeta();

        if (meta != null) {
            String worldName = "icycaverns";  // The world name without spaces
            int playerCount = Bukkit.getWorld(worldName) != null ? Bukkit.getWorld(worldName).getPlayers().size() : 0;

            meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "ICY CAVERNS" + ChatColor.YELLOW + " (Medium)");

            meta.setLore(Arrays.asList(
                    ChatColor.WHITE + "Players: " + ChatColor.AQUA + playerCount,
                    "",
                    ChatColor.GRAY + "With frosty temperatures,",
                    ChatColor.GRAY + "the icy caverns are a barren",
                    ChatColor.GRAY + "battleground for adapted",
                    ChatColor.GRAY + "monsters and enemies.",
                    "",
                    ChatColor.GRAY + "Status: " + getMapStatus(),
                    "",
                    ChatColor.GRAY + "" + ChatColor.ITALIC + "(( Click to queue. ))"
            ));

            icyCavernsItem.setItemMeta(meta);
        }

        return icyCavernsItem;
    }

    public ItemStack createKurokoItem() {
        ItemStack kurokoItem = new ItemStack(Material.LILAC);  // Using LILAC for Kuroko
        ItemMeta meta = kurokoItem.getItemMeta();

        if (meta != null) {
            String worldName = "kuroko";  // The world name without spaces
            int playerCount = Bukkit.getWorld(worldName) != null ? Bukkit.getWorld(worldName).getPlayers().size() : 0;

            meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "KUROKO" + ChatColor.GREEN + " (Easy)");

            meta.setLore(Arrays.asList(
                    ChatColor.WHITE + "Players: " + ChatColor.RED + playerCount,
                    "",
                    ChatColor.GRAY + "Wander through ancient lands",
                    ChatColor.GRAY + "where spirits linger and shadows",
                    ChatColor.GRAY + "tell tales of a forgotten era.",
                    "",
                    ChatColor.GRAY + "Status: " + getMapStatus(),
                    "",
                    ChatColor.GRAY + "" + ChatColor.ITALIC + "(( Click to queue. ))"
            ));

            kurokoItem.setItemMeta(meta);
        }

        return kurokoItem;
    }

    public ItemStack createSaharaItem() {
        ItemStack saharaItem = new ItemStack(Material.RED_SAND);  // Using RED_SAND for Sahara
        ItemMeta meta = saharaItem.getItemMeta();

        if (meta != null) {
            String worldName = "sahara";  // The world name without spaces
            int playerCount = Bukkit.getWorld(worldName) != null ? Bukkit.getWorld(worldName).getPlayers().size() : 0;

            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "SAHARA" + ChatColor.RED + " (Hard)");

            meta.setLore(Arrays.asList(
                    ChatColor.WHITE + "Players: " + ChatColor.YELLOW + playerCount,
                    "",
                    ChatColor.GRAY + "A vast desert with scorching heat,",
                    ChatColor.GRAY + "where only the strongest survive.",
                    "",
                    ChatColor.GRAY + "Status: " + getMapStatus(),
                    "",
                    ChatColor.GRAY + "" + ChatColor.ITALIC + "(( Click to queue. ))"
            ));

            saharaItem.setItemMeta(meta);
        }

        return saharaItem;
    }

    private String getMapStatus() {
        if (timeRemaining <= 10 * 60) { // 10 minutes or less
            return ChatColor.RED + "" + ChatColor.BOLD + "CLOSING SOON";
        } else {
            return ChatColor.GREEN + "" + ChatColor.BOLD + "OPEN";
        }
    }

    private void startMapRotation() {
        // Schedule a repeating task that runs every second to update the countdown
        new BukkitRunnable() {
            @Override
            public void run() {
                timeRemaining--;

                // Schedule 10-minute warning
                if (timeRemaining == 10 * 60) {
                    Bukkit.getScheduler().runTask(RotatingMapSystem.this, () -> {
                        String currentMapName = maps[currentMapIndex];
                        String warningMessage = "§c§l(!) §cThe " + currentMapName + " Map is closing in ten minutes!";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "at sendMessage all " + warningMessage);
                    });
                }

                // Schedule map closing notification
                if (timeRemaining == 0) {
                    Bukkit.getScheduler().runTask(RotatingMapSystem.this, () -> {
                        // Correctly calculate the previous map index
                        int previousMapIndex = (currentMapIndex - 1 + maps.length) % maps.length;
                        String closingMapName = maps[previousMapIndex];
                        String closingMessage = "§c§l(!) §cThe " + closingMapName + " Map has closed!";
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "at sendMessage all " + closingMessage);

                        // Play the dragon roar sound for all players in the closing map
                        World closingWorld = Bukkit.getWorld(closingMapName.replace(" ", "").toLowerCase());
                        if (closingWorld != null) {
                            for (Player player : closingWorld.getPlayers()) {
                                player.playSound(player.getLocation(), "entity.ender_dragon.growl", 1.0f, 1.0f);
                            }
                        }
                    });

                    rotateMap();
                    timeRemaining = 30 * 60; // Reset the countdown to 30 minutes (in seconds)
                }

                refreshOpenMenus();
            }
        }.runTaskTimer(this, 0, 20); // 20 ticks = 1 second
    }


    private void rotateMap() {
        // Get the current world name without spaces
        String currentWorldName = maps[currentMapIndex].replace(" ", "").toLowerCase();

        // Check if the current world exists and get the players in it
        if (Bukkit.getWorld(currentWorldName) != null) {
            for (Player player : Bukkit.getWorld(currentWorldName).getPlayers()) {
                // Apply the Wither effect to the player for 20 seconds at level 1
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 10000, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 10000, 3));
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 10000, 1));

                player.sendTitle(
                        ChatColor.RED + "" + ChatColor.BOLD + "(!)", // Title
                        ChatColor.RED + "The map has ended and you have failed to extract.", // Subtitle
                        10, // Fade in (ticks)
                        100, // Stay (ticks)
                        10 // Fade out (ticks)
                );            }
        }

        // Update the current map index to the next one
        currentMapIndex = (currentMapIndex + 1) % maps.length;
    }

    private void refreshOpenMenus() {
        // Refresh the menu for all players currently viewing it
        for (Player player : playersInMenu) {
            updateAdventureMenu(player);
        }
    }

    public void openAdventureMenu(Player player) {
        // Add the player to the set of players viewing the menu
        playersInMenu.add(player);
        Inventory menu = createMenuForPlayer(player);
        player.openInventory(menu);
    }

    private void updateAdventureMenu(Player player) {
        // Update the player's current menu with the new map and countdown info
        if (player.getOpenInventory().getTitle().equals("Adventure Menu")) {
            Inventory menu = player.getOpenInventory().getTopInventory();

            // Update the current map item
            ItemStack teleportItem;
            switch (maps[currentMapIndex].toLowerCase()) {
                case "icycaverns":
                    teleportItem = createIcyCavernsItem();
                    break;
                case "kuroko":
                    teleportItem = createKurokoItem();
                    break;
                case "sahara":
                    teleportItem = createSaharaItem();
                    break;
                default:
                    teleportItem = createIcyCavernsItem(); // Default to Icy Caverns if something goes wrong
                    break;
            }

            // Place the teleport item in the center slot (slot 13 in a 9x3 inventory)
            menu.setItem(13, teleportItem);

            // Update the countdown and next map item on the bottom middle slot (slot 22 in a 9x3 inventory)
            int nextMapIndex = (currentMapIndex + 1) % maps.length;
            ItemStack nextMapItem = new ItemStack(Material.CLOCK);
            ItemMeta nextMeta = nextMapItem.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Next Map: " + ChatColor.GREEN + maps[nextMapIndex]);
                nextMeta.setLore(Arrays.asList(ChatColor.WHITE + "Time remaining: " + formatTime(timeRemaining)));
                nextMapItem.setItemMeta(nextMeta);
            }
            menu.setItem(22, nextMapItem);

            player.updateInventory();  // Refresh the player's inventory view
        }
    }

    private Inventory createMenuForPlayer(Player player) {
        // Create a 9x3 (27 slots) inventory
        Inventory menu = Bukkit.createInventory(null, 27, "Adventure Menu");

        // Choose the appropriate item for the current map
        ItemStack teleportItem;
        switch (maps[currentMapIndex].toLowerCase()) {
            case "icycaverns":
                teleportItem = createIcyCavernsItem();
                break;
            case "kuroko":
                teleportItem = createKurokoItem();
                break;
            case "sahara":
                teleportItem = createSaharaItem();
                break;
            default:
                teleportItem = createIcyCavernsItem(); // Default to Icy Caverns if something goes wrong
                break;
        }

        // Place the teleport item in the center slot (slot 13 in a 9x3 inventory)
        menu.setItem(13, teleportItem);

        // Add the next map item with countdown to the bottom middle slot (slot 22 in a 9x3 inventory)
        int nextMapIndex = (currentMapIndex + 1) % maps.length;
        ItemStack nextMapItem = new ItemStack(Material.CLOCK);
        ItemMeta nextMeta = nextMapItem.getItemMeta();
        if (nextMeta != null) {
            nextMeta.setDisplayName(ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Next Map: " + ChatColor.GREEN + maps[nextMapIndex]);
            nextMeta.setLore(Arrays.asList(ChatColor.WHITE + "Time remaining: " + formatTime(timeRemaining)));
            nextMapItem.setItemMeta(nextMeta);
        }
        menu.setItem(22, nextMapItem);

        return menu;
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds); // Formats the time as MM:SS
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Adventure Menu")) {
            event.setCancelled(true); // Prevent item from being taken

            // Check if the clicked item is our teleport item
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() != Material.CLOCK) {
                Player player = (Player) event.getWhoClicked();

                // Determine the correct world name without spaces
                String worldName = maps[currentMapIndex].replace(" ", "").toLowerCase();

                // Construct the command to teleport the player using /rtp
                String command = "rtp player " + player.getName() + " " + worldName;
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);

                // Close the menu
                player.closeInventory();

                // Format the remaining time as minutes and seconds
                String formattedTime = formatTime(timeRemaining);

                // Send a message to the player with the remaining time warning
                player.sendMessage(ChatColor.RED + "Warning: This map will remain open for " + ChatColor.GREEN + formattedTime + ChatColor.RED + ".");
                player.sendMessage(ChatColor.RED + "You must exit before time runs out, or you will die!");

                // Start a task to update the action bar for all players in the world
                startActionBarCountdown(Bukkit.getWorld(worldName));
            }
        }
    }

    private void startActionBarCountdown(World world) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (timeRemaining <= 0) {
                    // Stop the task if time runs out
                    this.cancel();
                    return;
                }

                // Format the remaining time as minutes and seconds
                String formattedTime = formatTime(timeRemaining);

                // Send the action bar message to all players in the world
                for (Player player : world.getPlayers()) {
                    if (player.isOnline() && player.getWorld().equals(world)) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Map closes in: " + ChatColor.GREEN + formattedTime));
                    }
                }
            }
        }.runTaskTimer(this, 0, 20); // Run every second (20 ticks)
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("Adventure Menu")) {
            playersInMenu.remove(event.getPlayer());
        }
    }
}

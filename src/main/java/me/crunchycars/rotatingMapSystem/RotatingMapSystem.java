package me.crunchycars.rotatingMapSystem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.scheduler.BukkitRunnable;

import org.bukkit.ChatColor;

import java.util.*;

import java.util.Set;
import java.util.HashSet;


public final class RotatingMapSystem extends JavaPlugin implements Listener {

    private int currentMapIndex = 0;
    private final String[] maps = {"Sahara", "Kuroko", "Icy Caverns"};
    private final Set<Player> playersInMenu = new HashSet<>();
    private int timeRemaining = 20; // 20 seconds countdown
    private final Set<UUID> restrictedWorldUUIDs = new HashSet<>(Arrays.asList(
            UUID.fromString("ab30c279-7fdd-4ba9-83b8-8cf4644502b6"), // sahara
            UUID.fromString("a84b8efb-bb68-49f1-9074-6d3d3384f561"), // caverns
            UUID.fromString("f6294f42-24f7-4f6c-984f-f37b75c0254e")  // kuroko
    ));

    @Override
    public void onEnable() {
        this.getCommand("adventure").setExecutor(this);
        this.getCommand("giveIcyCavernsItem").setExecutor(this);
        Bukkit.getServer().getPluginManager().registerEvents(this, this);

        // Start the shared map rotation task
        startMapRotation();

        getLogger().info("Rotator plugin has been enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Check if the player's current world is in the restricted worlds set
            if (restrictedWorldUUIDs.contains(player.getWorld().getUID())) {
                player.sendMessage(ChatColor.RED + ""+ ChatColor.BOLD + "(!)" + ChatColor.RED +  " You cannot use the /adventure command while in this world.");
                return true;
            }

            // Check if the command is to open the adventure menu or to give the Icy Caverns item
            if (label.equalsIgnoreCase("adventure")) {
                openAdventureMenu(player);
                return true;
            } else if (label.equalsIgnoreCase("giveIcyCavernsItem")) {
                // Give the player the Icy Caverns item directly
                player.getInventory().addItem(createIcyCavernsItem());
                player.sendMessage(ChatColor.GREEN + "Icy Caverns item has been added to your inventory.");
                return true;
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
                    ChatColor.GRAY + "Status: " + ChatColor.GREEN + "" + ChatColor.BOLD + "OPEN",
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
                    ChatColor.GRAY + "Status: " + ChatColor.GREEN + "" + ChatColor.BOLD + "OPEN",
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
                    ChatColor.GRAY + "Status: " + ChatColor.GREEN + "" + ChatColor.BOLD + "OPEN",
                    "",
                    ChatColor.GRAY + "" + ChatColor.ITALIC + "(( Click to queue. ))"
            ));

            saharaItem.setItemMeta(meta);
        }

        return saharaItem;
    }


    private void startMapRotation() {
        // Schedule a repeating task that runs every second to update the countdown
        new BukkitRunnable() {
            @Override
            public void run() {
                timeRemaining--;
                if (timeRemaining <= 0) {
                    rotateMap();
                    timeRemaining = 30 * 60; // Reset the countdown to 30 minutes (in seconds)
                }
                refreshOpenMenus();
            }
        }.runTaskTimer(this, 0, 20); // 20 ticks = 1 second
    }


    private void rotateMap() {
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

                player.closeInventory(); // Close the menu
            }
        }
    }



    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().equals("Adventure Menu")) {
            playersInMenu.remove(event.getPlayer());
        }
    }
}

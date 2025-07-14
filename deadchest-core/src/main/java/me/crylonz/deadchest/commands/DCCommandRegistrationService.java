package me.crylonz.deadchest.commands;

import me.crylonz.deadchest.ChestData;
import me.crylonz.deadchest.DeadChest;
import me.crylonz.deadchest.Permission;
import me.crylonz.deadchest.utils.ConfigKey;
import me.crylonz.deadchest.DeadChestManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static me.crylonz.deadchest.DeadChest.*;
import static me.crylonz.deadchest.DeadChestManager.cleanAllDeadChests;

public class DCCommandRegistrationService extends DCCommandRegistration {

    public DCCommandRegistrationService(DeadChest plugin) {
        super(plugin);
    }

    public void registerReload() {
        registerCommand("dc reload", Permission.ADMIN.label, () -> {
            fileManager.reloadChestDataConfig();
            fileManager.reloadLocalizationConfig();
            plugin.reloadConfig();
            plugin.registerConfig();

            @SuppressWarnings("unchecked")
            ArrayList<ChestData> tmp = (ArrayList<ChestData>) fileManager.getChestDataConfig().get("chestData");
            if (tmp != null) {
                chestData = (List<ChestData>) fileManager.getChestDataConfig().get("chestData");
            }
            local.set(fileManager.getLocalizationConfig().getConfigurationSection("localisation").getValues(true));

            sender.sendMessage(ChatColor.GREEN + local.get("loc_prefix") + " Plugin reload successfully");
        });
    }

    public void registerRepairForce() {
        registerCommand("dc repair force", Permission.ADMIN.label, () -> {
            repair(true);
        });
    }

    public void registerRepair() {
        registerCommand("dc repair", Permission.ADMIN.label, () -> {
            repair(false);
        });
    }

    private void repair(Boolean forced) {
        if (player != null) {
            Collection<Entity> entities = player.getWorld().getNearbyEntities(
                    player.getLocation(), 100.0D, 25.0D, 100.0D);

            int holoRemoved = 0;
            for (Entity entity : entities) {
                if (entity.getType() == EntityType.ARMOR_STAND) {
                    ArmorStand as = (ArmorStand) entity;

                    if (as.hasMetadata("deadchest") || forced) {
                        holoRemoved++;
                        entity.remove();
                    }

                    // Deprecated (support for deadchest 3.X and lower)
                    else if (as.getCustomName() != null && as.getCustomName().contains("×")) {
                        holoRemoved++;
                        entity.remove();
                    }

                }
            }
            player.sendMessage(local.get("loc_prefix") + ChatColor.GOLD + "Operation complete. [" + holoRemoved + "] hologram(s) removed");
        } else {
            sender.sendMessage(local.get("loc_prefix") + ChatColor.RED + "Command must be called by a player");
        }
    }

    public void registerRemoveInfinite() {
        registerCommand("dc removeinfinite", Permission.ADMIN.label, () -> {

            int cpt = 0;
            if (chestData != null && !chestData.isEmpty()) {
                Iterator<ChestData> chestDataIt = chestData.iterator();
                while (chestDataIt.hasNext()) {
                    ChestData chestData = chestDataIt.next();

                    if (chestData.getChestLocation().getWorld() != null) {
                        if (chestData.isInfinity() || config.getInt(ConfigKey.DEADCHEST_DURATION) == 0) {

                            // remove chest
                            Location loc = chestData.getChestLocation();
                            loc.getWorld().getBlockAt(loc).setType(Material.AIR);

                            // remove holographic
                            chestData.removeArmorStand();

                            // remove in memory
                            chestDataIt.remove();

                            cpt++;
                        }
                    }
                }
                fileManager.saveModification();
            }
            sender.sendMessage(local.get("loc_prefix") + ChatColor.GOLD + "Operation complete. [" +
                    ChatColor.GREEN + cpt + ChatColor.GOLD + "] deadchest(s) removed");
        });
    }

    public void registerRemoveAll() {
        registerCommand("dc removeall", Permission.ADMIN.label, () -> {
            int cpt = cleanAllDeadChests();
            sender.sendMessage(local.get("loc_prefix") + ChatColor.GOLD + "Operation complete. [" +
                    ChatColor.GREEN + cpt + ChatColor.GOLD + "] deadchest(s) removed");
        });
    }

    public void registerRemoveOwn() {
        registerCommand("dc remove", Permission.REMOVE_OWN.label, () -> {
            if (player != null) {
                removeAllDeadChestOfPlayer(player.getName());
            } else {
                sender.sendMessage(local.get("loc_prefix") + ChatColor.RED + "Command must be called by a player");
            }
        });
    }

    public void registerRemoveOther() {
        registerCommand("dc remove {0}", Permission.REMOVE_OTHER.label, () -> {
            removeAllDeadChestOfPlayer(args[1]);
        });
    }

    private void removeAllDeadChestOfPlayer(String playerName) {
        int cpt = 0;
        if (chestData != null && !chestData.isEmpty()) {

            Iterator<ChestData> chestDataIt = chestData.iterator();
            while (chestDataIt.hasNext()) {

                ChestData cd = chestDataIt.next();

                if (cd.getChestLocation().getWorld() != null) {

                    if (cd.getPlayerName().equalsIgnoreCase(playerName)) {
                        // remove chest
                        Location loc = cd.getChestLocation();
                        loc.getWorld().getBlockAt(loc).setType(Material.AIR);

                        // remove holographics
                        cd.removeArmorStand();

                        // remove in memory
                        chestDataIt.remove();

                        cpt++;
                    }
                }
            }
            fileManager.saveModification();
        }
        sender.sendMessage(local.get("loc_prefix") + ChatColor.GOLD + "Operation complete. [" +
                ChatColor.GREEN + cpt + ChatColor.GOLD + "] deadchest(s) removed of player " + playerName);
    }

    public void registerListOwn() {
        registerCommand("dc list", null, () -> {
            if (player != null) {
                if (player.hasPermission(Permission.LIST_OWN.label) || !config.getBoolean(ConfigKey.REQUIRE_PERMISSION_TO_LIST_OWN)) {
                    Date now = new Date();
                    if (!chestData.isEmpty()) {
                        sender.sendMessage(local.get("loc_prefix") + local.get("loc_dclistown") + " :");
                        for (ChestData data : chestData) {
                            if (data.getPlayerUUID().equalsIgnoreCase(player.getUniqueId().toString())) {
                                displayChestData(now, data);
                            }
                        }
                    } else {
                        player.sendMessage(local.get("loc_prefix") + local.get("loc_nodc"));
                    }
                }
            } else {
                sender.sendMessage(local.get("loc_prefix") + ChatColor.RED + "Command must be called by a player");
            }
        });
    }


    public void registerListOther() {
        registerCommand("dc list {0}", Permission.LIST_OTHER.label, () -> {

            Date now = new Date();
            if (args[1].equalsIgnoreCase("all")) {
                if (!chestData.isEmpty()) {
                    sender.sendMessage(local.get("loc_prefix") + local.get("loc_dclistall") + ":");
                    for (ChestData data : chestData) {
                        displayChestData(now, data);
                    }
                } else {
                    sender.sendMessage(local.get("loc_prefix") + local.get("loc_nodcs"));
                }

            } else {
                if (!chestData.isEmpty()) {
                    sender.sendMessage(local.get("loc_prefix") + ChatColor.GREEN + args[1] + " deadchests :");
                    for (ChestData data : chestData) {
                        if (data.getPlayerName().equalsIgnoreCase(args[1])) {
                            displayChestData(now, data);
                        }
                    }
                } else {
                    sender.sendMessage(local.get("loc_prefix") + local.get("loc_nodcs"));
                }
            }
        });
    }

    private void displayChestData(Date now, ChestData chestData) {
        String worldName = chestData.getChestLocation().getWorld() != null ?
                chestData.getChestLocation().getWorld().getName() : "???";

        if (chestData.isInfinity() || config.getInt(ConfigKey.DEADCHEST_DURATION) == 0) {
            sender.sendMessage("-" + ChatColor.AQUA + " World: " + ChatColor.WHITE + worldName + " |"
                    + ChatColor.AQUA + " X: " + ChatColor.WHITE + chestData.getChestLocation().getX()
                    + ChatColor.AQUA + " Y: " + ChatColor.WHITE + chestData.getChestLocation().getY()
                    + ChatColor.AQUA + " Z: " + ChatColor.WHITE + chestData.getChestLocation().getZ()
                    + " | "
                    + "∞ " + local.get("loc_endtimer"));
        } else {
            long diff = now.getTime() - (chestData.getChestDate().getTime() + config.getInt(ConfigKey.DEADCHEST_DURATION) * 1000L);
            long diffSeconds = Math.abs(diff / 1000 % 60);
            long diffMinutes = Math.abs(diff / (60 * 1000) % 60);
            long diffHours = Math.abs(diff / (60 * 60 * 1000));
            player.sendMessage("-" + ChatColor.AQUA + " X: " + ChatColor.WHITE + chestData.getChestLocation().getX()
                    + ChatColor.AQUA + " Y: " + ChatColor.WHITE + chestData.getChestLocation().getY()
                    + ChatColor.AQUA + " Z: " + ChatColor.WHITE + chestData.getChestLocation().getZ()
                    + " | " +
                    +diffHours + "h "
                    + diffMinutes + "m "
                    + diffSeconds + "s " + local.get("loc_endtimer"));
        }
    }

    public void registerGiveBack() {
        registerCommand("dc giveback {0}", Permission.GIVEBACK.label, () -> {
            Player targetPlayer = Bukkit.getPlayer(args[1]);

            // 1. First, check if the player is online
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage(local.get("loc_prefix") + "§cThat player is not online.");
                return;
            }

            // 2. Now, find the oldest chest for that player
            ChestData chestToGive = null;
            for (ChestData data : chestData) {
                if (data.getPlayerUUID().equals(targetPlayer.getUniqueId().toString())) {
                    chestToGive = data; // Found the oldest chest, stop looking
                    break;
                }
            }

            // 3. Check if a chest was actually found
            if (chestToGive != null) {
                // Use the new helper method to give items
                DeadChestManager.giveItemsToPlayer(targetPlayer, chestToGive);

                // Remove chest and hologram
                targetPlayer.getWorld().getBlockAt(chestToGive.getChestLocation()).setType(Material.AIR);
                chestToGive.removeArmorStand();
                chestData.remove(chestToGive); // Remove it from the list

                // Send success messages
                sender.sendMessage(local.get("loc_prefix") + local.get("loc_dcgbsuccess"));
                targetPlayer.sendMessage(local.get("loc_prefix") + local.get("loc_gbplayer"));

                // Save the change to file to prevent duping
                fileManager.saveModification();
            } else {
                // If we get here, the player was online but no chest was found
                sender.sendMessage(local.get("loc_prefix") + "§cThat player has no active deadchests.");
            }
        });
    }
}

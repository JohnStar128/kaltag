package net.cubekrowd.kaltag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class KalTag extends JavaPlugin implements Listener {

    boolean gameState;
    String tag = "tagged";
    String quit = "quit";
    String prevTag = "previouslyTagged";
    Map<String, Player> tagged = new HashMap<String, Player>();
    ArrayList<Player> onlinePlayers = new ArrayList<Player>();
    static Random random = new Random();
    static String KT_PREFIX = ChatColor.DARK_GRAY + "[" + ChatColor.DARK_PURPLE + "Kal" + ChatColor.GOLD + "Tag" + ChatColor.DARK_GRAY + "] ";


    @Override
    public void onEnable() {
        // They should totally make JavaPlugin implement Listener by default
        getServer().getPluginManager().registerEvents(this, this);
        // Add the tagged field to the tagged HashMap
        tagged.put(tag, null);
        // Set game state to false, to be turned on via command
        gameState = false;
        // Add all currently online players to onlinePlayers ArrayList
        onlinePlayers.addAll(Bukkit.getOnlinePlayers());
        getConfig().options().copyDefaults();
        saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Check if game is currently running, if so, turn off
        if (gameState()) {
            stopGame();
        }
    }

    public boolean gameState() {
        return gameState;
    }

    public void initializeGame() {
        // Grab a random online player to tag, put them in the tagged HashMap
        // and make them glow
        if (onlinePlayers.size() < 2) {
            Bukkit.broadcastMessage(KT_PREFIX + ChatColor.RED + "Not enough players online to tag someone!");
        } else {
            Player p = (Player) onlinePlayers.toArray()[random.nextInt(onlinePlayers.size())];
            if (tagged.get(prevTag) != p) {
                tagged.put(tag, p);
                p.setGlowing(true);
                Bukkit.broadcastMessage(KT_PREFIX + ChatColor.GOLD + p.getName() + ChatColor.RED + " is it!");
            }
        }
    }

    public void stopGame() {
        // Clear the tagged HashMap, remove glowing status, send confirmation console messages, and set game state to false
        if (tagged.get(tag) != null) {
            Player currentlyTagged = tagged.get(tag);
            currentlyTagged.setGlowing(false);
            tagged.remove(tag);
            System.out.println(KT_PREFIX + ChatColor.RED + "Removed tagged player.");
        }
        // prevTag keeps track of the previously tagged player, used in the cooldown
        if (tagged.get(prevTag) != null) {
            tagged.remove(prevTag);
            System.out.println(KT_PREFIX + ChatColor.RED + "Removed previously tagged player.");
        }
        gameState = false;
        Bukkit.broadcastMessage(KT_PREFIX + ChatColor.GOLD + "KalTag is over! Thanks for playing!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        // Add new players to onlinePlayers ArrayList
        Player playerJoined = e.getPlayer();
        onlinePlayers.add(playerJoined);
        // Tell new players who is currently tagged
        if (tagged.get(tag) != playerJoined) {
            playerJoined.sendMessage(KT_PREFIX + ChatColor.GOLD + tagged.get(tag).getName() + ChatColor.RED + " is it!");
        }

        // Unset glowing for people who come back while game isn't running
        if (gameState() && playerJoined.isGlowing()) {
            e.getPlayer().setGlowing(false);
        }
        // If less than 2 players are online but game state is true, don't initialize game
        if (gameState() && Bukkit.getOnlinePlayers().size() == 2 && tagged.get(tag) == null) {
            Bukkit.broadcastMessage(KT_PREFIX + ChatColor.GOLD + "Enough players online! KalTag can begin!");
            initializeGame();
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        // Remove the player from the onlinePlayers HashMap, clear the current tagged player,
        // and call the initializing method to tag a new player
        onlinePlayers.remove(e.getPlayer());
        if (gameState()) {
            Player leftPlayer = e.getPlayer();
            if (tagged.get(tag) == leftPlayer) {
                Bukkit.broadcastMessage(KT_PREFIX + ChatColor.GOLD + leftPlayer.getName() + ChatColor.RED + " has logged out! Picking a new player to be it...");
                leftPlayer.setGlowing(false);
                tagged.remove(tag);
                tagged.put(quit, leftPlayer);
                tagged.put(prevTag, leftPlayer);
                initializeGame();
            }
        }
    }

    @EventHandler
    public void onRightClickPlayer(PlayerInteractAtEntityEvent e) {
        // If the tagged player right clicks a non-tagged player who wasn't the previous
        // tagged player, make them the new tagged player
        if (gameState() && e.getHand().equals(EquipmentSlot.HAND)) {
            Player currentlyTagged = e.getPlayer();
            var tagDelay = getConfig().getInt("tag-cooldown") * 20;
            if (tagged.get(prevTag) == e.getRightClicked()) {
                currentlyTagged.sendMessage(KT_PREFIX + ChatColor.RED + "You can't tag this player yet!");
                return;
            }
            if (e.getRightClicked() instanceof Player && e.getRightClicked() != tagged.get(prevTag) && e.getRightClicked() != tagged.get(tag)) {
                Player notTagged = (Player) e.getRightClicked();
                tagged.remove(tag, currentlyTagged);
                currentlyTagged.setGlowing(false);
                tagged.put(tag, notTagged);
                notTagged.setGlowing(true);
                tagged.put(prevTag, currentlyTagged);
                Bukkit.broadcastMessage(KT_PREFIX + ChatColor.GOLD + tagged.get(tag).getName() + ChatColor.RED + " is it!");
                getServer().getScheduler().scheduleSyncDelayedTask(this, () -> tagged.remove(prevTag), tagDelay);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("kaltag.toggle")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command!");
            return true;
        }

        if (args.length > 0) {
            sender.sendMessage(KT_PREFIX + ChatColor.RED + "Usage: /kaltag");
            return true;
        }

        if (!gameState()) {
            Bukkit.broadcastMessage(KT_PREFIX + ChatColor.GOLD + "KalTag has begun!");
            initializeGame();
            tagged.put(prevTag, tagged.get(tag));
            gameState = true;
        } else {
            stopGame();
        }
        return true;
    }
}

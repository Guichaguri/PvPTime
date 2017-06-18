package com.guichaguri.pvptime.bukkit;


import com.guichaguri.pvptime.api.IPvPTimeAPI;
import com.guichaguri.pvptime.common.PvPTime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Guilherme Chaguri
 */
public class PvPTimeCommand implements CommandExecutor {

    private final PvPTimeBukkit plugin;

    public PvPTimeCommand(PvPTimeBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length > 0 && args[0].equalsIgnoreCase("info")) {
            info(sender, false);
        } else if(args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reload(sender);
        } else if(sender.hasPermission("pvptime.info")) {
            if(!sender.hasPermission("pvptime.reload")) {
                info(sender, true);
            } else {
                help(sender);
            }
        } else {
            sender.sendMessage(ChatColor.AQUA + "PvPTime " + PvPTime.VERSION + " by Guichaguri");
        }
        return true;
    }

    private void info(CommandSender sender, boolean onlyCurrent) {
        if(!sender.hasPermission("pvptime.info")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to run this command");
            return;
        }

        sender.sendMessage(ChatColor.GREEN + "------------ PvPTime ------------");

        if(onlyCurrent && sender instanceof Player) {
            infoWorld(sender, ((Player)sender).getWorld(), "Current World");
        } else {
            for(World w : Bukkit.getWorlds()) {
                infoWorld(sender, w, w.getName());
            }
        }

        sender.sendMessage(ChatColor.GREEN + "--------------------------------");
    }

    private void infoWorld(CommandSender sender, World world, String name) {
        IPvPTimeAPI<String> engine = plugin.getAPI();
        Boolean isPvPTime = engine.isPvPTime(world.getName());

        String pvp = isPvPTime == null ? ChatColor.RED + "Disabled" : (isPvPTime ? "PvP On" : "PvP Off");
        sender.sendMessage(ChatColor.GOLD + name + " " + ChatColor.YELLOW + pvp);
    }

    private void reload(CommandSender sender) {
        if(!sender.hasPermission("pvptime.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to run this command");
            return;
        }
        plugin.reloadConfig();
        plugin.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "The configuration file was reloaded");
    }

    private void help(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "------------ PvPTime ------------");
        sender.sendMessage(ChatColor.GOLD + "/pvptime info " + ChatColor.YELLOW + "Shows information about the worlds");
        sender.sendMessage(ChatColor.GOLD + "/pvptime reload " + ChatColor.YELLOW + "Reloads the configuration file");
        sender.sendMessage(ChatColor.GREEN + "--------------------------------");
    }
}

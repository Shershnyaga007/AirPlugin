package me.shershnyaga.air.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TabPluginHelp implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender.hasPermission("air.admin")) {
            if (args.length == 1) {
                return Arrays.asList("reload", "infinite", "inf");
            }
            if (args[0].equalsIgnoreCase("reload")) return new ArrayList<>();
            if (args.length == 2) {

                Collection<? extends Player> onlinePlayers = Bukkit.getServer().getOnlinePlayers();
                ArrayList<String> onlinePlayerName = new ArrayList<>();
                for (Player onlinePlayer : onlinePlayers) {
                    String name = onlinePlayer.getName();
                    onlinePlayerName.add(name);
                }
                return onlinePlayerName;
            }
            if (args.length == 3) {
                return Arrays.asList("off", "on");
            }
            return new ArrayList<>();
        }
        return null;
    }
}

package me.shershnyaga.air.commands;

import me.shershnyaga.air.AirPlugin;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.MessageCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;


public class AirInfinite implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.hasPermission("air.admin")) {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    AirPlugin.getPlugin().reloadConfig();
                    sender.sendMessage("Reload Complite");
                    return true;
                }
            } else if (args.length == 3 && !(args[0].equalsIgnoreCase("reload"))) {
                String Player1 = args[1];
                String arg =args[2];
                Player purpose = sender.getServer().getPlayer(Player1);
                PersistentDataContainer data = purpose.getPersistentDataContainer();

                if (arg.equalsIgnoreCase("on")) {
                    if (!data.has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)){
                        data.set(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER, 1);
                        sender.sendMessage(ChatColor.GOLD + "Вы включили игроку " + Player1 + " режим бесконечного воздуха!");
                        purpose.sendMessage(ChatColor.GREEN + "У вас включен режим бесконечного воздуха!");
                    } else sender.sendMessage(ChatColor.RED + "У игрока уже включен режим бесконечного воздуха!");
                } else if (arg.equalsIgnoreCase("off")) {
                    if (data.has(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"), PersistentDataType.INTEGER)){
                        data.remove(new NamespacedKey(AirPlugin.getPlugin(), "AirInf"));
                        sender.sendMessage(ChatColor.GOLD + "Вы выключили игроку " + Player1 + " режим бесконечного воздуха!");
                        purpose.sendMessage(ChatColor.RED + "У вас выключен режим бесконечного воздуха!");
                    } else sender.sendMessage(ChatColor.RED + "У игрока уже выключен режим бесконечного воздуха!");
                }

                if (AirPlugin.getPlugin().shouldConsumeAir(purpose)) {
                    AirPlugin.getPlugin().updateBossbar(purpose);
                }

                return true;
            }
            //sender.sendMessage(String.valueOf(args[0].equalsIgnoreCase("reload")));
            return false;
        }
        return false;
    }
}


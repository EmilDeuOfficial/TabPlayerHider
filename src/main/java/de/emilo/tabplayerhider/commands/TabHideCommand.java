package de.emilo.tabplayerhider.commands;

import de.emilo.tabplayerhider.TabPlayerHider;
import de.emilo.tabplayerhider.managers.HideManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class TabHideCommand implements CommandExecutor, TabCompleter {

    private final TabPlayerHider plugin;
    private final HideManager hideManager;

    public TabHideCommand(TabPlayerHider plugin, HideManager hideManager) {
        this.plugin = plugin;
        this.hideManager = hideManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tabplayerhider.admin")) {
            sender.sendMessage("§cNo permission.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "on" -> {
                if (hideManager.isEnabled()) {
                    sender.sendMessage("§eTabPlayerHider is already enabled.");
                } else {
                    hideManager.enable();
                    sender.sendMessage("§aTabPlayerHider enabled. §7Hidden players are now invisible in the tab list.");
                }
            }
            case "off" -> {
                if (!hideManager.isEnabled()) {
                    sender.sendMessage("§eTabPlayerHider is already disabled.");
                } else {
                    hideManager.disable();
                    sender.sendMessage("§cTabPlayerHider disabled. §7All players are now visible in the tab list.");
                }
            }
            case "status" -> {
                String state = hideManager.isEnabled() ? "§aON" : "§cOFF";
                List<String> hidden = hideManager.getHiddenNames();
                long onlineHidden = hidden.stream()
                    .filter(n -> Bukkit.getPlayerExact(n) != null)
                    .count();
                sender.sendMessage("§6§lTabPlayerHider Status");
                sender.sendMessage("§7State: " + state);
                sender.sendMessage("§7Hidden players: §f" + hidden.size() +
                    " §7(" + onlineHidden + " online)");
                if (!hidden.isEmpty()) {
                    sender.sendMessage("§7Names: §f" + String.join("§7, §f", hidden));
                }
            }
            case "hide" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /tph hide <player>"); return true; }
                String name = args[1];
                if (hideManager.addHidden(name)) {
                    sender.sendMessage("§a" + name + " §fis now hidden from the tab list.");
                } else {
                    sender.sendMessage("§e" + name + " is already hidden.");
                }
            }
            case "show" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /tph show <player>"); return true; }
                String name = args[1];
                if (hideManager.removeHidden(name)) {
                    sender.sendMessage("§a" + name + " §fis now visible in the tab list.");
                } else {
                    sender.sendMessage("§e" + name + " was not hidden.");
                }
            }
            case "list" -> {
                List<String> hidden = hideManager.getHiddenNames();
                if (hidden.isEmpty()) {
                    sender.sendMessage("§eNo players are currently hidden.");
                } else {
                    sender.sendMessage("§6Hidden players §7(" + hidden.size() + ")§6: §f" + String.join("§7, §f", hidden));
                }
            }
            case "reload" -> {
                plugin.reloadConfig();
                hideManager.reload();
                sender.sendMessage("§aConfig reloaded.");
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("tabplayerhider.admin")) return List.of();

        if (args.length == 1) {
            return List.of("on", "off", "status", "hide", "show", "list", "reload").stream()
                .filter(s -> s.startsWith(args[0].toLowerCase()))
                .toList();
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "hide" -> Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
                case "show" -> hideManager.getHiddenNames().stream()
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
                default -> List.of();
            };
        }

        return List.of();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§lTabPlayerHider");
        sender.sendMessage("§f/tph on §7– Enable hiding");
        sender.sendMessage("§f/tph off §7– Disable hiding (shows all)");
        sender.sendMessage("§f/tph status §7– Show current state");
        sender.sendMessage("§f/tph hide <player> §7– Hide from tab list");
        sender.sendMessage("§f/tph show <player> §7– Show in tab list");
        sender.sendMessage("§f/tph list §7– List hidden players");
        sender.sendMessage("§f/tph reload §7– Reload config");
    }
}

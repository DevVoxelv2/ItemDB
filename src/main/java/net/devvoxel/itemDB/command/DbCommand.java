package net.devvoxel.itemDB.command;

import net.devvoxel.itemDB.ItemDB;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DbCommand implements CommandExecutor, TabCompleter {

    private final ItemDB plugin;

    public DbCommand(ItemDB plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        var msg = plugin.messages();

        // /db → Hilfe
        if (args.length == 0) {
            sender.sendMessage(msg.get("usage"));
            return true;
        }

        // /db show → GUI öffnen
        if (args.length == 1 && args[0].equalsIgnoreCase("show")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg.get("only-players"));
                return true;
            }
            if (!sender.hasPermission("net.devvoxel.itemdb.show")) {
                sender.sendMessage(msg.get("no-permission"));
                return true;
            }
            plugin.gui().open(p);
            return true;
        }

        // /db <name> → Item holen
        if (args.length == 1) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg.get("only-players"));
                return true;
            }
            if (!sender.hasPermission("net.devvoxel.itemdb.use")) {
                sender.sendMessage(msg.get("no-permission"));
                return true;
            }
            String name = args[0];
            ItemStack item = plugin.items().get(name);
            if (item == null) {
                p.sendMessage(msg.get("item-not-found").replace("{name}", name));
                return true;
            }
            p.getInventory().addItem(item.clone());
            p.sendMessage(msg.get("item-given-self").replace("{name}", name));
            return true;
        }

        // /db add <name>
        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(msg.get("only-players"));
                return true;
            }
            if (!sender.hasPermission("net.devvoxel.itemdb.add")) {
                sender.sendMessage(msg.get("no-permission"));
                return true;
            }
            String name = args[1].toLowerCase(Locale.ROOT);
            if (plugin.items().exists(name)) {
                p.sendMessage(msg.get("item-exists").replace("{name}", name));
                return true;
            }

            ItemStack hand = p.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                p.sendMessage(msg.get("no-hand"));
                return true;
            }

            plugin.items().add(name, hand);
            p.sendMessage(msg.get("item-added").replace("{name}", name));
            return true;
        }

        // /db remove <name>
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("net.devvoxel.itemdb.remove")) {
                sender.sendMessage(msg.get("no-permission"));
                return true;
            }
            String name = args[1];
            if (plugin.items().remove(name)) {
                sender.sendMessage(msg.get("item-removed").replace("{name}", name));
            } else {
                sender.sendMessage(msg.get("item-not-found").replace("{name}", name));
            }
            return true;
        }

        // /db giveitem <name> <player>
        if (args.length == 3 && args[0].equalsIgnoreCase("giveitem")) {
            if (!sender.hasPermission("net.devvoxel.itemdb.giveitem")) {
                sender.sendMessage(msg.get("no-permission"));
                return true;
            }
            String name = args[1];
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(msg.get("player-not-found").replace("{player}", args[2]));
                return true;
            }
            ItemStack item = plugin.items().get(name);
            if (item == null) {
                sender.sendMessage(msg.get("item-not-found").replace("{name}", name));
                return true;
            }
            target.getInventory().addItem(item.clone());
            sender.sendMessage(msg.get("item-given-other")
                    .replace("{name}", name)
                    .replace("{player}", target.getName()));
            target.sendMessage(msg.get("item-given-self").replace("{name}", name));
            return true;
        }

        sender.sendMessage(msg.get("usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        // 1. Argument
        if (args.length == 1) {
            if (sender.hasPermission("net.devvoxel.itemdb.show")) out.add("show");
            if (sender.hasPermission("net.devvoxel.itemdb.add")) out.add("add");
            if (sender.hasPermission("net.devvoxel.itemdb.remove")) out.add("remove");
            if (sender.hasPermission("net.devvoxel.itemdb.giveitem")) out.add("giveitem");
            out.addAll(plugin.items().keys()); // Item-Namen
            return filter(out, args[0]);
        }

        // 2. Argument
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("giveitem")) {
                return filter(plugin.items().keys(), args[1]);
            }
        }

        // 3. Argument (bei giveitem → Spieler)
        if (args.length == 3 && args[0].equalsIgnoreCase("giveitem")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                out.add(p.getName());
            }
            return filter(out, args[2]);
        }

        return out;
    }

    private List<String> filter(List<String> list, String start) {
        String s = start.toLowerCase(Locale.ROOT);
        return list.stream().filter(x -> x.toLowerCase(Locale.ROOT).startsWith(s)).toList();
    }
}

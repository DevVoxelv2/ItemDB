package net.devvoxel.itemDB.ui;

import net.devvoxel.itemDB.ItemDB;
import net.devvoxel.itemDB.i18n.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemsGui implements Listener {
    private final ItemDB plugin;

    public ItemsGui(ItemDB plugin) {
        this.plugin = plugin;
    }

    /**
     * Öffnet die Items-Datenbank-GUI für einen Spieler
     */
    public void open(Player player) {
        int rows = Math.max(1, Math.min(6, plugin.getConfig().getInt("Gui.Rows", 6)));
        int size = rows * 9;
        Inventory inv = Bukkit.createInventory(player, size, plugin.messages().guiTitle());

        // === Border mit grauem Glas ===
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta gm = glass.getItemMeta();
        gm.setDisplayName(" ");
        glass.setItemMeta(gm);

        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) {
                inv.setItem(i, glass);
            }
        }

        // === Items aus DB anzeigen ===
        int slotIndex = 0;
        for (String name : plugin.items().keys()) {
            ItemStack item = plugin.items().get(name);
            if (item == null) continue;

            ItemStack display = item.clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + name);
                meta.setLore(plugin.messages().getList("gui-lore")
                        .stream().map(s -> s.replace("{name}", name)).toList());
                display.setItemMeta(meta);
            }

            // freien Slot im Innenbereich berechnen
            int slot = nextInnerSlot(slotIndex, rows);
            if (slot >= size) break;

            inv.setItem(slot, display);
            slotIndex++;
        }

        player.openInventory(inv);
    }

    private int nextInnerSlot(int index, int rows) {
        // Innerer Bereich = ohne erste/letzte Reihe + ohne erste/letzte Spalte
        int innerCols = 7; // 9 - 2 (links+rechts)
        int row = index / innerCols; // 0..(rows-3)
        int col = index % innerCols; // 0..6
        return (row + 1) * 9 + (col + 1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null) return;
        String title = e.getView().getTitle();
        if (!title.equals(plugin.messages().guiTitle())) return;

        e.setCancelled(true);
        HumanEntity clicker = e.getWhoClicked();
        if (!(clicker instanceof Player p)) return;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : null;
        if (name == null || name.isBlank()) return;
        name = name.replace("§f", "").trim();

        ItemStack dbItem = plugin.items().get(name);
        if (dbItem == null) {
            p.sendMessage(plugin.messages().get("item-not-found").replace("{name}", name));
            return;
        }

        // Item geben
        p.getInventory().addItem(dbItem.clone());
        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
        p.sendMessage(plugin.messages().get("item-given-self").replace("{name}", name));
    }
}

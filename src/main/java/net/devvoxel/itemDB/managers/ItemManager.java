package net.devvoxel.itemDB.managers;

import net.devvoxel.itemDB.data.Database;
import net.devvoxel.itemDB.ItemDB;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ItemManager {
    private final ItemDB plugin;
    private final Database db;
    private final String table;
    private final Map<String, ItemStack> cache = new ConcurrentHashMap<>();

    public ItemManager(ItemDB plugin, Database db) {
        this.plugin = plugin;
        this.db = db;
        this.table = plugin.getConfig().getString("Database.Table", "itemdb_items");
        load(); // beim Start alle Items aus DB laden
    }

    /**
     * Lädt alle Items aus der Datenbank in den Cache.
     */
    public void load() {
        load(true);
    }

    /**
     * Lädt alle Items aus der Datenbank in den Cache.
     *
     * @param logResult Soll das Ergebnis geloggt werden?
     */
    public void load(boolean logResult) {
        Map<String, ItemStack> updatedCache = new ConcurrentHashMap<>();

        try (Statement st = db.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT name,item FROM `" + table + "`")) {

            while (rs.next()) {
                String name = rs.getString("name").toLowerCase(Locale.ROOT);
                String yaml = rs.getString("item");

                try {
                    YamlConfiguration cfg = new YamlConfiguration();
                    cfg.loadFromString(yaml); // <- korrekt für String-Input
                    ItemStack stack = cfg.getItemStack("item");
                    if (stack != null) updatedCache.put(name, stack);
                } catch (Exception parseEx) {
                    plugin.getLogger().warning("Konnte Item '" + name + "' nicht laden: " + parseEx.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Laden der Items: " + e.getMessage());
            return; // Bei Fehler Cache nicht überschreiben
        }

        cache.clear();
        cache.putAll(updatedCache);

        if (logResult) {
            plugin.getLogger().info("Geladene Items aus DB: " + cache.size());
        }
    }

    /**
     * Fügt ein Item hinzu (falls es noch nicht existiert).
     */
    public boolean add(String name, ItemStack stack) {
        String key = name.toLowerCase(Locale.ROOT);
        if (cache.containsKey(key)) return false;

        try {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("item", stack);
            String data = yaml.saveToString(); // <- WICHTIG: statt save(Writer)

            try (PreparedStatement ps = db.getConnection().prepareStatement(
                    "INSERT INTO `" + table + "` (name,item) VALUES (?,?)")) {
                ps.setString(1, key);
                ps.setString(2, data);
                ps.executeUpdate();
            }

            cache.put(key, stack.clone());
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Fehler beim Speichern des Items '" + name + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Entfernt ein Item aus DB + Cache.
     */
    public boolean remove(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (!cache.containsKey(key)) return false;

        try (PreparedStatement ps = db.getConnection().prepareStatement(
                "DELETE FROM `" + table + "` WHERE name=?")) {
            ps.setString(1, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Fehler beim Löschen des Items '" + name + "': " + e.getMessage());
        }

        cache.remove(key);
        return true;
    }

    /**
     * Holt ein Item aus dem Cache (Kopie).
     */
    public ItemStack get(String name) {
        ItemStack s = cache.get(name.toLowerCase(Locale.ROOT));
        return s == null ? null : s.clone();
    }

    /**
     * Gibt alle gespeicherten Namen zurück (alphabetisch sortiert).
     */
    public List<String> keys() {
        List<String> list = new ArrayList<>(cache.keySet());
        list.sort(String.CASE_INSENSITIVE_ORDER);
        return list;
    }

    /**
     * Anzahl gespeicherter Items.
     */
    public int size() {
        return cache.size();
    }

    /**
     * Prüft, ob ein Item existiert.
     */
    public boolean exists(String name) {
        return cache.containsKey(name.toLowerCase(Locale.ROOT));
    }
}

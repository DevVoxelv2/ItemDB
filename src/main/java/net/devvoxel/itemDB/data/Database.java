package net.devvoxel.itemDB.data;

import net.devvoxel.itemDB.ItemDB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private final ItemDB plugin;
    private Connection connection;

    public Database(ItemDB plugin) {
        this.plugin = plugin;
    }

    public void connect() throws SQLException {
        var cfg = plugin.getConfig();
        String host = cfg.getString("Database.Host");
        int port = cfg.getInt("Database.Port");
        String db = cfg.getString("Database.Database");
        String user = cfg.getString("Database.User");
        String pass = cfg.getString("Database.Password");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";
        connection = DriverManager.getConnection(url, user, pass);

        plugin.getLogger().info("Connected to MySQL database.");
        initTable(cfg.getString("Database.Table"));
    }

    private void initTable(String table) throws SQLException {
        var sql = "CREATE TABLE IF NOT EXISTS `" + table + "` (" +
                "`name` VARCHAR(64) PRIMARY KEY," +
                "`item` MEDIUMTEXT NOT NULL" +
                ");";
        connection.createStatement().executeUpdate(sql);
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

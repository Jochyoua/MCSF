package com.github.Jochyoua.MCSF.shared;

import com.github.Jochyoua.MCSF.MCSF;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.List;
import java.util.UUID;

public class MySQL {
    MCSF plugin;
    Utils utils;

    private Connection con;
    private HikariDataSource hikari = null;

    public MySQL(MCSF plugin) {
        this.plugin = plugin;
        this.utils = new Utils(plugin, this);
        if (hikari == null) {
            hikari = new HikariDataSource();
        }
    }

    public Connection getConnection() {
        return con;
    }

    public void setConnection(String host, String user, String password, String database, String port, boolean ssl, boolean unicode) {
        if (host == null || user == null || password == null || database == null)
            return;
        if (!isConnected()) {
            try {
                hikari.setDataSourceClassName("org.mariadb.jdbc.MySQLDataSource");
                hikari.addDataSourceProperty("serverName", host);
                hikari.addDataSourceProperty("port", port);
                hikari.addDataSourceProperty("databaseName", database);
                hikari.addDataSourceProperty("user", user);
                hikari.addDataSourceProperty("password", password);
                if (unicode)
                    hikari.addDataSourceProperty("properties", "useUnicode=true;characterEncoding=utf8;autoReconnect=true");
                con = hikari.getConnection();
                utils.debug("SQL connected.");
                if (!tableExists("swears") || countRows("swears") == 0
                        || !tableExists("users") || countRows("users") == 0
                        || !tableExists("whitelist") || countRows("whitelist") == 0) {
                    utils.createTable(false);
                }
                utils.reload();
                utils.debug("MySQL has been enabled & information has been set");
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("SQL Connect Error: " + e.getMessage());
            }
        }
    }

    public void connect() {
        connect(true);
    }

    private void connect(boolean message) {
        if (isConnected()) {
            if (message)
                utils.debug("SQL Connect Error: Already connected");
        } else {
            plugin.reloadConfig();
            setConnection(
                    plugin.getConfig().getString("mysql.host"),
                    plugin.getConfig().getString("mysql.username"),
                    plugin.getConfig().getString("mysql.password"),
                    plugin.getConfig().getString("mysql.database"),
                    plugin.getConfig().getString("mysql.port"),
                    plugin.getConfig().getBoolean("mysql.ssl"),
                    plugin.getConfig().getBoolean("mysql.use_unicode"));
        }
    }

    public boolean isConnected() {
        if (con != null)
            try {
                return !con.isClosed();
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("SQL Connection Error:" + e.getMessage());
                e.printStackTrace();
            }
        return false;
    }

    public void update(String command) {
        /*if (command == null)
            return false;
        boolean result = false;*/
        connect(false);
        try {
            Statement st = getConnection().createStatement();
            st.executeUpdate(command);
            st.close();
            //result = true;
        } catch (Exception e) {
            String message = e.getMessage();
            utils.debug("SQL Update Error: " + message);
            if(e.getMessage().equals("Connection is closed"))
                if(hikari != null)
                    hikari.close();
        }
        //return result;
    }

    public ResultSet query(String command) {
        if (command == null)
            return null;
        connect(false);
        ResultSet rs = null;
        try {
            Statement st = getConnection().createStatement();
            rs = st.executeQuery(command);
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null) {
                utils.debug("SQL Query Error: " + message);
                e.printStackTrace();
            }
        }
        return rs;
    }

    public boolean tableExists(String table) {
        try {
            if (con == null)
                return false;
            DatabaseMetaData metadata = con.getMetaData();
            if (metadata == null)
                return false;
            ResultSet rs = metadata.getTables(null, null, table, null);
            if (rs.next())
                return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public boolean exists(String column, String data, String table) {
        if (data != null)
            data = "'" + data + "'";
        try {
            ResultSet rs = query("SELECT * FROM " + table + " WHERE " + column + "=" + data + ";");
            if (rs.next())
                return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public int countRows(String table) {
        int i = 0;
        ResultSet rs = query("SELECT * FROM " + table + ";");
        try {
            while (rs.next())
                i++;
        } catch (Exception ignored) {
        }
        return i;
    }

    public void stateSwears(List<String> swears) {
        connect(false);
        PreparedStatement preparedStatement;
        StringBuilder query = new StringBuilder("INSERT INTO swears(word) VALUES (?)");
        for (int i = 0; i < swears.size() - 1; i++) {
            query.append(", (?)");
        }
        try {
            preparedStatement = con.prepareStatement(query.toString());
            for (int i = 0; i < swears.size(); i++) {
                preparedStatement.setString(i + 1, swears.get(i));
            }
            preparedStatement.executeUpdate();
            utils.debug("Successfully insert data into database consisting of " + swears.size() + " words");
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("Failed to insert data into database: " + e.getMessage());
        }
    }

    public void stateWhite(List<String> white) {
        connect(false);
        PreparedStatement preparedStatement;
        StringBuilder query = new StringBuilder("INSERT INTO whitelist(word) VALUES (?)");
        for (int i = 0; i < white.size() - 1; i++) {
            query.append(", (?)");
        }
        try {
            preparedStatement = con.prepareStatement(query.toString());
            for (int i = 0; i < white.size(); i++) {
                preparedStatement.setString(i + 1, white.get(i));
            }
            preparedStatement.executeUpdate();
            utils.debug("Successfully insert data into whitelist database consisting of " + white.size() + " words");
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("Failed to insert data into whitelist database: " + e.getMessage());
        }
    }

    public void stateInsert(String word) {
        StringBuilder query = new StringBuilder("INSERT INTO swears(word) VALUES (?)");
        try {
            PreparedStatement preparedStatement = con.prepareStatement(query.toString());
            preparedStatement.setString(1, word);
            preparedStatement.executeUpdate();
            utils.debug("added `" + word + "` to the database");
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("failed to add `" + word + "` to the database: " + e.getMessage());
        }
    }

    public void whiteInsert(String word) {
        StringBuilder query = new StringBuilder("INSERT INTO whitelist(word) VALUES (?)");
        try {
            PreparedStatement preparedStatement = con.prepareStatement(query.toString());
            preparedStatement.setString(1, word);
            preparedStatement.executeUpdate();
            utils.debug("added `" + word + "` to the whitelist database");
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("failed to add `" + word + "` to the whitelist database: " + e.getMessage());
        }
    }

    public void stateRemove(String word) {
        PreparedStatement preparedStatement;
        StringBuilder query = new StringBuilder("DELETE FROM swears WHERE word = (?);");
        try {
            preparedStatement = con.prepareStatement(query.toString());
            preparedStatement.setString(1, word);
            preparedStatement.executeUpdate();
            utils.debug("`" + word + "` has been removed from the database");
        } catch (SQLException throwables) {
            Bukkit.getConsoleSender().sendMessage("failed to remove  `" + word + "` from database: " + throwables.getMessage());
        }
    }

    public void whiteRemove(String word) {
        PreparedStatement preparedStatement;
        StringBuilder query = new StringBuilder("DELETE FROM whitelist WHERE word = (?);");
        try {
            preparedStatement = con.prepareStatement(query.toString());
            preparedStatement.setString(1, word);
            preparedStatement.executeUpdate();
            utils.debug("`" + word + "` has been removed from the whitelist database");
        } catch (SQLException throwables) {
            Bukkit.getConsoleSender().sendMessage("failed to remove  `" + word + "` from whitelist database: " + throwables.getMessage());
        }
    }

    public void stateRemovePlayer(UUID UUID) {
        PreparedStatement preparedStatement;
        StringBuilder query = new StringBuilder("DELETE FROM users WHERE uuid = (?);");
        try {
            preparedStatement = con.prepareStatement(query.toString());
            preparedStatement.setString(1, String.valueOf(UUID));
            preparedStatement.executeUpdate();
            utils.debug("player with UUID `" + UUID + "` has been removed from the database");
        } catch (SQLException throwables) {
            Bukkit.getConsoleSender().sendMessage("failed to remove player with uuid `" + UUID + "` from database: " + throwables.getMessage());
        }
    }
}
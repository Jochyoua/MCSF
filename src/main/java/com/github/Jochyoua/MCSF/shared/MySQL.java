package com.github.Jochyoua.MCSF.shared;

import com.github.Jochyoua.MCSF.MCSF;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.List;
import java.util.UUID;

public class MySQL {
    MCSF plugin;
    Utils utils;
    private Connection con;

    public MySQL(MCSF plugin) {
        this.plugin = plugin;
        this.utils = new Utils(plugin, this);
    }

    public Connection getConnection() {
        return con;
    }

    public void setConnection(String host, String user, String password, String database, String port, String connection, String unicode, String ssl) {
        if (host == null || user == null || password == null || database == null)
            return;
        disconnect(false);
        try {
            connection = connection.replaceAll("(?i)\\{host}|(?i)%host%", host)
                    .replaceAll("(?i)\\{port}|(?i)%port%", port)
                    .replaceAll("(?i)\\{database}|(?i)%database%", database)
                    .replaceAll("(?i)\\{unicode}|(?i)%unicode%", unicode)
                    .replaceAll("(?i)\\{ssl}|(?i)%ssl%", ssl);
            con = DriverManager.getConnection(connection, user, password);
            utils.debug("SQL connected.");
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("SQL Connect Error: " + e.getMessage());
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
                    plugin.getConfig().getString("mysql.connection", "jdbc:mysql://{host}:{port}/{database}?useUnicode={unicode}&characterEncoding=utf8&autoReconnect=true&useSSL{ssl}"),
                    plugin.getConfig().getBoolean("mysql.use_unicode", true) + "",
                    plugin.getConfig().getBoolean("mysql.ssl", false) + "");
        }
    }

    public void disconnect() {
        disconnect(true);
    }

    private void disconnect(boolean message) {
        try {
            if (isConnected()) {
                con.close();
                if (message)
                    utils.debug("SQL disconnected.");
            } else if (message) {
                utils.debug("SQL Disconnect Error: No existing connection");
            }
        } catch (Exception e) {
            if (message)
                utils.debug("SQL Disconnect Error: " + e.getMessage());
            e.printStackTrace();
        }
        con = null;
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
        }
        disconnect(false);
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
package me.vagdedes.mysql.database;

import com.github.Jochyoua.MCSF.Main;
import com.github.Jochyoua.MCSF.Utils;
import me.vagdedes.mysql.basic.Config;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.sql.*;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ALL")
public class MySQL {
    Main plugin;
    Config Config;
    Utils utils;
    private Connection con;

    public MySQL(Main plugin) {
        this.plugin = plugin;
        Config = new Config(plugin);
        this.utils = new Utils(plugin, this);
    }

    public Connection getConnection() {
        return con;
    }

    public void setConnection(String host, String user, String password, String database, String port) {
        if (host == null || user == null || password == null || database == null)
            return;
        disconnect(false);
        try {
            con = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?useUnicode=true&characterEncoding=utf8&autoReconnect=true&useSSL=" + Config.getSSL(), user, password);
            utils.debug("SQL connected.");
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("SQL Connect Error: " + e.getMessage());
        }
    }

    public void connect() {
        connect(true);
    }

    private void connect(boolean message) {
        String host = Config.getHost();
        String user = Config.getUser();
        String password = Config.getPassword();
        String database = Config.getDatabase();
        String port = Config.getPort();
        if (isConnected()) {
            if (message)
                utils.debug("SQL Connect Error: Already connected");
        } else if (host.equalsIgnoreCase("")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Config Error: Host is blank");
        } else if (user.equalsIgnoreCase("")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Config Error: User is blank");
        } else if (password.equalsIgnoreCase("")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Config Error: Password is blank");
        } else if (database.equalsIgnoreCase("")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Config Error: Database is blank");
        } else if (port.equalsIgnoreCase("")) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Config Error: Port is blank");
        } else {
            setConnection(host, user, password, database, port);
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
                Bukkit.getConsoleSender().sendMessage("SQL Disconnect Error: " + e.getMessage());
            e.printStackTrace();
        }
        con = null;
    }

    public void reconnect() {
        disconnect();
        connect();
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

    public boolean update(String command) {
        if (command == null)
            return false;
        boolean result = false;
        connect(false);
        try {
            Statement st = getConnection().createStatement();
            st.executeUpdate(command);
            st.close();
            result = true;
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null) {
                Bukkit.getConsoleSender().sendMessage("SQL Update Error: " + message);
            }
        }
        disconnect(false);
        return result;
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
                Bukkit.getConsoleSender().sendMessage("SQL Query Error: " + message);
                e.printStackTrace();
            }
        }
        return rs;
    }

    // SQL
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

    public boolean insertData(String columns, String values, String table) {
        return update("INSERT INTO " + table + " (" + columns + ") VALUES (" + values + ");");
    }

    public boolean deleteData(String column, String logic_gate, String data, String table) {
        if (data != null)
            data = "'" + data + "'";
        return update("DELETE FROM " + table + " WHERE " + column + logic_gate + data + ";");
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
        PreparedStatement preparedStatement = null;
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
        } catch (SQLException throwables) {
            Bukkit.getConsoleSender().sendMessage("Failed to insert data into database: " + throwables.getMessage());
        }
    }

    public void stateInsert(String word) {
        PreparedStatement preparedStatement = null;
        StringBuilder query = new StringBuilder("INSERT INTO swears(word) VALUES (?)");
        try {
            preparedStatement = con.prepareStatement(query.toString());
            preparedStatement.setString(1, word);
            preparedStatement.executeUpdate();
            utils.debug("added `" + word + "` to the database");
        } catch (SQLException throwables) {
            Bukkit.getConsoleSender().sendMessage("failed to add `" + word + "` to the database: " + throwables.getMessage());
        }
    }

    public void stateRemove(String word) {
        PreparedStatement preparedStatement = null;
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
    public void stateRemovePlayer(UUID UUID){
            PreparedStatement preparedStatement = null;
            StringBuilder query = new StringBuilder("DELETE FROM users WHERE uuid = (?);");
            try {
                preparedStatement = con.prepareStatement(query.toString());
                preparedStatement.setString(1, String.valueOf(UUID));
                preparedStatement.executeUpdate();
                utils.debug("player with UUID `" + UUID+ "` has been removed from the database");
            } catch (SQLException throwables) {
                Bukkit.getConsoleSender().sendMessage("failed to remove player with uuid `" + UUID + "` from database: " + throwables.getMessage());
            }
    }
}
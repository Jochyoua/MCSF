package io.github.jochyoua.mychristianswearfilter.shared;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.HikariCP.HikariCP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class User {
    UUID id;
    MCSF plugin;
    Connection connection;
    Connection userConnection;
    Utils utils;

    public User(Utils utils, UUID id) {
        this.id = id;
        this.plugin = utils.getProvider();
        this.utils = utils;
        this.connection = utils.getConnection();
        this.userConnection = utils.getUserConnection();
    }

    public boolean toggle() {
        plugin.reloadConfig();
        if (plugin.getConfig().getBoolean("settings.filtering.force"))
            return true;
        Boolean value = null;
        if (utils.supported("mysql")) {
            utils.setTable("users");
            boolean exists = false;
            try {
                PreparedStatement ps = connection.prepareStatement(HikariCP.Query.USERS.exists);
                ps.setString(1, id.toString());
                if (ps.executeQuery().next()) {
                    exists = true;
                }
                ps.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                exists = false;
            }
            if (!exists) {
                value = plugin.getConfig().getBoolean("settings.filtering.default");
                try {
                    PreparedStatement ps = connection.prepareStatement("INSERT INTO users VALUES (?, ?, ?)");
                    ps.setString(1, id.toString());
                    ps.setString(2, "placestopholder");
                    ps.setBoolean(3, value);
                    ps.execute();
                    ps.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            } else {
                boolean result = false;
                try (
                        PreparedStatement ps = connection.prepareStatement("SELECT status FROM users WHERE uuid=?")) {
                    ps.setString(1, String.valueOf(id));
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        result = rs.getBoolean("status");
                    }
                    ps.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                    result = plugin.getConfig().getBoolean("settings.filtering.default");
                }
                value = !result;
                try (
                        PreparedStatement ps = connection.prepareStatement("UPDATE users SET status=? WHERE uuid=?")) {
                    ps.setBoolean(1, value);
                    ps.setString(2, String.valueOf(id));
                    ps.execute();
                    ps.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            }
            plugin.saveConfig();
        }
        if (!this.exists()) {
            value = plugin.getConfig().getBoolean("settings.filtering.default");
        }
        if (value == null) {
            value = this.status();
            value = !value;
        }
        this.set(value);
        return value;
    }

    public UUID getId() {
        return this.id;
    }

    public void set(boolean bool) {
        try {
            PreparedStatement ps;
            if (exists()) {
                ps = userConnection.prepareStatement("UPDATE users SET status=? WHERE uuid=?");
                ps.setBoolean(1, bool);
                ps.setString(2, String.valueOf(id));
            } else {
                ps = userConnection.prepareStatement("INSERT INTO users VALUES (?, ?, ?)");
                ps.setString(1, id.toString());
                ps.setString(2, "placeholder");
                ps.setBoolean(3, bool);
            }
            ps.execute();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public void create(String playername, boolean bool) {
        try {
            PreparedStatement ps;
            if (!exists()) {
                ps = userConnection.prepareStatement("INSERT INTO users VALUES (?, ?, ?)");
                ps.setString(1, id.toString());
                ps.setString(2, playername);
                ps.setBoolean(3, bool);
            } else {
                ps = userConnection.prepareStatement("UPDATE users SET status=?, playername=? WHERE uuid=?");
                ps.setBoolean(1, bool);
                ps.setString(2, playername);
            }
            ps.execute();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public String playerName(String string) {
        String name = "";
        try {
            PreparedStatement ps = userConnection.prepareStatement("UPDATE users SET name=? WHERE uuid=?");
            ps.setString(1, string);
            ps.setString(2, String.valueOf(id));
            ps.execute();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return name;
    }

    public String playerName() {
        String name = "";
        try {
            PreparedStatement ps = userConnection.prepareStatement("SELECT name FROM users WHERE uuid = ?");
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                name = rs.getString("name");
            ps.execute();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return name;
    }

    public boolean status() {
        boolean status = false;
        try {
            PreparedStatement ps = userConnection.prepareStatement("SELECT status FROM users WHERE uuid = ?");
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next())
                status = rs.getBoolean("status");
            ps.execute();
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return plugin.getConfig().getBoolean("settings.filtering.force") || status;
    }

    public boolean exists() {
        boolean exists = false;
        try {
            PreparedStatement ps = userConnection.prepareStatement("SELECT status FROM users WHERE uuid = ?");
            ps.setString(1, id.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                exists = true;
            }
            ps.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return exists;
    }
}
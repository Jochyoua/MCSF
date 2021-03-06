package io.github.jochyoua.mychristianswearfilter.shared.hikaricp;

import io.github.jochyoua.mychristianswearfilter.MCSF;
import io.github.jochyoua.mychristianswearfilter.shared.Manager;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.logging.Level;

import static io.github.jochyoua.mychristianswearfilter.shared.Manager.debug;

public class HikariCP {
    private final MCSF plugin;
    @Getter
    @Setter
    private boolean enabled = false;
    private DatabaseConnector connector;

    public HikariCP(MCSF plugin, DatabaseConnector connector) {
        this.plugin = plugin;
        this.connector = connector;
        reload();
    }

    public DatabaseConnector getConnector() {
        return this.connector;
    }

    public void reload() {
        FileConfiguration sql = Manager.FileManager.getFile(plugin, "sql");
        if (sql.getBoolean("mysql.enabled")) {
            if (connector == null)
                connector = new DatabaseConnector(plugin);
            if (!connector.isWorking()) {
                debug("(MYSQL) Loading database info....", true, Level.INFO);
                try {
                    String driverClass = sql.getString("mysql.driverClass");
                    String url = Objects.requireNonNull(sql.getString("mysql.connection", "jdbc:mysql://{host}:{port}/{database}?useUnicode={unicode}&characterEncoding=utf8&autoReconnect=true&useSSL={ssl}"))
                            .replaceAll("(?i)\\{host}|(?i)%host%", Objects.requireNonNull(sql.getString("mysql.host")))
                            .replaceAll("(?i)\\{port}|(?i)%port%", Objects.requireNonNull(sql.getString("mysql.port", "3306")))
                            .replaceAll("(?i)\\{database}|(?i)%database%", Objects.requireNonNull(sql.getString("mysql.database", "MCSF")))
                            .replaceAll("(?i)\\{unicode}|(?i)%unicode%", String.valueOf(sql.getBoolean("mysql.use_unicode")))
                            .replaceAll("(?i)\\{ssl}|(?i)%ssl%", String.valueOf(sql.getBoolean("mysql.ssl")));
                    String username = sql.getString("mysql.username");
                    String password = sql.getString("mysql.password");
                    int maxPoolSize = sql.getInt("mysql.maxPoolSize");
                    debug("(MYSQL) Using URL: " + url, true, Level.INFO);
                    connector.setInfo(
                            new DatabaseConnector.Info(
                                    driverClass,
                                    url,
                                    username,
                                    password,
                                    maxPoolSize
                            )
                    );
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    setEnabled(false);
                }
                debug("(MYSQL) Trying a database connection....", true, Level.INFO);
                try {
                    connector.tryFirstConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                    setEnabled(false);
                }
                debug("(MYSQL) The connection has been established!", true, Level.INFO);
                setEnabled(true);
            }
        } else {
            setEnabled(false);
        }
    }


    public enum Query {
        SWEARS("swears", "CREATE TABLE IF NOT EXISTS {table} (" +
                "word varchar(255) UNIQUE" +
                ");", "INSERT INTO {table} VALUES (?)", "DELETE FROM {table} WHERE word=?", "SELECT * FROM {table}", "SELECT * FROM {table} WHERE word = ?", "DROP TABLE {table}"),
        WHITELIST("whitelist", "CREATE TABLE IF NOT EXISTS {table} (" +
                "word varchar(255) UNIQUE" +
                ");", "INSERT INTO {table} VALUES (?)", "DELETE FROM {table} WHERE word=?", "SELECT * FROM {table}", "SELECT * FROM {table} WHERE word = ?", "DROP TABLE {table}"),
        GLOBAL("global", "CREATE TABLE IF NOT EXISTS {table} (" +
                "word varchar(255) UNIQUE" +
                ");", "INSERT INTO {table} VALUES (?)", "DELETE FROM {table} WHERE word=?", "SELECT * FROM {table}", "SELECT * FROM {table} WHERE word = ?", "DROP TABLE {table}"),
        USERS("users", "CREATE TABLE IF NOT EXISTS {table} (" +
                "uuid varchar(255) UNIQUE," +
                "name varchar(255)," +
                "status varchar(255)" +
                ");", "INSERT INTO {table} VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE status=?", "DELETE FROM {table} WHERE uuid=?", "SELECT * FROM {table}", "SELECT * FROM {table} WHERE uuid = ?", "DROP TABLE {table}");
        public String table;
        public String create;
        public String insert;
        public String delete;
        public String select_all;
        public String exists;
        public String reset;

        Query(String table, String create, String insert, String delete, String select_all, String exists, String reset) {
            this.table = table;
            this.create = create.replaceAll("(?i)\\{table}", table);
            this.insert = insert.replaceAll("(?i)\\{table}", table);
            this.delete = delete.replaceAll("(?i)\\{table}", table);
            this.select_all = select_all.replaceAll("(?i)\\{table}", table);
            this.exists = exists.replaceAll("(?i)\\{table}", table);
            this.reset = reset;
        }
    }
}

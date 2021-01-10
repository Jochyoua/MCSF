package io.github.jochyoua.mychristianswearfilter.shared.HikariCP;

import java.sql.SQLException;

public class HikariCP {

    public HikariCP(Connector connector) throws SQLException {
        connector.execute(Query.SWEARS.create);

        connector.execute(Query.WHITELIST.create);

        connector.execute(Query.GLOBAL.create);

        connector.execute(Query.USERS.create);
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

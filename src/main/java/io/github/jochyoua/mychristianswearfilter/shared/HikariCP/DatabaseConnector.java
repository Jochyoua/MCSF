package io.github.jochyoua.mychristianswearfilter.shared.HikariCP;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.jochyoua.mychristianswearfilter.MCSF;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

@RequiredArgsConstructor
public class DatabaseConnector implements Connector {

    MCSF plugin;
    private HikariConfig config;
    private HikariDataSource dataSource;
    private boolean working = false;

    public DatabaseConnector(MCSF plugin) {
        this.plugin = plugin;
    }

    public void setInfo(Info info) throws IllegalStateException {
        try {
                config = new HikariConfig();
                    config.setDriverClassName(info.driverClass);
                    config.setJdbcUrl(info.url);
                    config.setUsername(info.username);
                    config.setPassword(info.password);
                    config.setMaximumPoolSize(info.maxPoolSize);
                    config.setAutoCommit(true);
                    config.validate();
        } catch (Exception e) {
            throw new IllegalStateException("Error trying to load information from config.", e);
        }
    }

    public void tryFirstConnection() {
        try {
            dataSource = new HikariDataSource(config);

            final Connection connection = dataSource.getConnection();
            if (!connection.isClosed())
                connection.close();
            working = true;
        } catch (Exception e) {
            throw new IllegalStateException("Error trying to make the First Connection. See below for more errors:", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            working = false;
            throw new SQLException(e);
        }
    }

    public boolean isWorking() {
        return working;
    }

    @RequiredArgsConstructor
    public static class Info {
        private final String driverClass, url, username, password;
        private final int maxPoolSize;
    }
}
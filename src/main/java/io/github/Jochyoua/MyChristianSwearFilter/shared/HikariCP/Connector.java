package io.github.Jochyoua.MyChristianSwearFilter.shared.HikariCP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface Connector {

    Connection getConnection() throws SQLException;

    default void execute(String query) throws SQLException {
        try (Connection connection = getConnection();
             PreparedStatement ps = connection.prepareStatement(query)) {
            ps.execute();
            ps.close();
        }
    }

}
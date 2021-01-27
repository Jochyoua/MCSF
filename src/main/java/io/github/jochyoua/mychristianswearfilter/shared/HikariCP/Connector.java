package io.github.jochyoua.mychristianswearfilter.shared.HikariCP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface Connector {

    Connection getConnection() throws SQLException;

    d

}
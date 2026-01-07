package org.example.footmanage.db;

import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public final class Database {
    private static Connection connection;

    public static synchronized Connection get() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initialize();
        }
        return connection;
    }

    private static void initialize() throws SQLException {
        String url = "jdbc:mysql://localhost:8889/footmanage?useSSL=false&serverTimezone=UTC";
        connection = DriverManager.getConnection(url, "root", "root");
        try { runSchemaMigrations(); } catch (SQLException ignored) {}
        try { seedAdminUser(); } catch (SQLException ignored) {}
    }

    private static void runSchemaMigrations() throws SQLException {
        try (Statement st = connection.createStatement()) {
            String sql = readResource("/db/schema.sql");
            for (String stmt : sql.split(";\n")) {
                String s = stmt.trim();
                if (!s.isEmpty()) {
                    st.execute(s);
                }
            }
        }
    }

    private static void seedAdminUser() throws SQLException {
        try (var ps = connection.prepareStatement(
                "INSERT INTO users(username, password_hash) \n" +
                        "SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = ?)")) {
            String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
            ps.setString(1, "admin");
            ps.setString(2, hash);
            ps.setString(3, "admin");
            ps.executeUpdate();
        }
    }

    private static String readResource(String name) {
        try (InputStream is = Database.class.getResourceAsStream(name)) {
            if (is == null) throw new IllegalStateException("Missing resource: " + name);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                return br.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

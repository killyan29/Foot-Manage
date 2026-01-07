package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    public User authenticate(String username, String password) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT id, username, password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    if (BCrypt.checkpw(password, hash)) {
                        return new User(rs.getInt("id"), rs.getString("username"), hash);
                    }
                }
                return null;
            }
        }
    }

    public List<User> list(String query) throws SQLException {
        String sql = "SELECT id, username, password_hash FROM users " +
                (query == null || query.isBlank() ? "" : "WHERE username LIKE ?") + " ORDER BY username";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (sql.contains("LIKE")) ps.setString(1, "%" + query + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<User> res = new ArrayList<>();
                while (rs.next()) res.add(new User(rs.getInt("id"), rs.getString("username"), rs.getString("password_hash")));
                return res;
            }
        }
    }

    public User create(String username, String password) throws SQLException {
        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("INSERT INTO users(username, password_hash) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, hash);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new User(id, username, hash);
            }
        }
    }

    public void updatePassword(int id, String newPassword) throws SQLException {
        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
            ps.setString(1, hash);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

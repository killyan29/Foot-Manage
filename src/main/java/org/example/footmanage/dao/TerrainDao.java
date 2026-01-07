package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.Terrain;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TerrainDao {
    public List<Terrain> list(String query) throws SQLException {
        String sql = "SELECT id, name, description, status, capacity FROM terrains " +
                (query == null || query.isBlank() ? "" : "WHERE name LIKE ? OR description LIKE ?") +
                " ORDER BY name";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (sql.contains("LIKE")) {
                String q = "%" + query + "%";
                ps.setString(1, q);
                ps.setString(2, q);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Terrain> res = new ArrayList<>();
                while (rs.next()) {
                    res.add(new Terrain(
                            rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                            rs.getString("status"), rs.getInt("capacity")));
                }
                return res;
            }
        }
    }

    public Terrain add(String name, String description, String status, int capacity) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO terrains(name, description, status, capacity) VALUES(?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, status);
            ps.setInt(4, capacity);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new Terrain(id, name, description, status, capacity);
            }
        }
    }

    public void update(int id, String name, String description, String status, int capacity) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE terrains SET name=?, description=?, status=?, capacity=? WHERE id=?")) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, status);
            ps.setInt(4, capacity);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM terrains WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}

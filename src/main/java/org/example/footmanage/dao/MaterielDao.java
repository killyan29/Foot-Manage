package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.Materiel;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaterielDao {
    public List<Materiel> list() throws SQLException {
        try (Connection c = Database.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM materiel ORDER BY type")) {
            List<Materiel> res = new ArrayList<>();
            while (rs.next()) res.add(map(rs));
            return res;
        }
    }

    public Materiel findByType(String type) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM materiel WHERE type = ?")) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
                return null;
            }
        }
    }

    public Materiel add(String type, int stock) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("INSERT INTO materiel(type, stock, borrowed) VALUES(?,?,0)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, type);
            ps.setInt(2, stock);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new Materiel(id, type, stock, 0);
            }
        }
    }

    public void update(int id, String type, int stock, int borrowed) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("UPDATE materiel SET type=?, stock=?, borrowed=? WHERE id=?")) {
            ps.setString(1, type);
            ps.setInt(2, stock);
            ps.setInt(3, borrowed);
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM materiel WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Materiel map(ResultSet rs) throws SQLException {
        return new Materiel(rs.getInt("id"), rs.getString("type"), rs.getInt("stock"), rs.getInt("borrowed"));
    }
}

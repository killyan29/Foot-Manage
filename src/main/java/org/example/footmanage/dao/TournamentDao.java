package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.Tournament;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TournamentDao {
    public List<Tournament> list() throws SQLException {
        try (Connection c = Database.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM tournaments ORDER BY start_date DESC, name")) {
            List<Tournament> res = new ArrayList<>();
            while (rs.next()) res.add(map(rs));
            return res;
        }
    }

    public Tournament add(String name, LocalDate startDate) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("INSERT INTO tournaments(name, start_date) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, startDate == null ? null : startDate.toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new Tournament(id, name, startDate);
            }
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM tournaments WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Tournament map(ResultSet rs) throws SQLException {
        String sd = rs.getString("start_date");
        LocalDate date = sd == null ? null : LocalDate.parse(sd);
        return new Tournament(rs.getInt("id"), rs.getString("name"), date);
    }
}


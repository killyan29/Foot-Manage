package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.LocationMateriel;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LocationMaterielDao {
    public LocationMateriel add(LocationMateriel l) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO locations_materiel(materiel_id, quantity, reservation_id, date) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, l.materielId());
            ps.setInt(2, l.quantity());
            if (l.reservationId() == null) ps.setNull(3, Types.INTEGER); else ps.setInt(3, l.reservationId());
            ps.setString(4, l.date().toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new LocationMateriel(id, l.materielId(), l.quantity(), l.reservationId(), l.date());
            }
        }
    }

    public List<LocationMateriel> findByReservation(int reservationId) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM locations_materiel WHERE reservation_id=?")) {
            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LocationMateriel> res = new ArrayList<>();
                while (rs.next()) res.add(map(rs));
                return res;
            }
        }
    }

    public List<LocationMateriel> listAll() throws SQLException {
        try (Connection c = Database.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM locations_materiel ORDER BY date DESC")) {
            List<LocationMateriel> res = new ArrayList<>();
            while (rs.next()) res.add(map(rs));
            return res;
        }
    }

    private LocationMateriel map(ResultSet rs) throws SQLException {
        Integer rid = rs.getObject("reservation_id") == null ? null : rs.getInt("reservation_id");
        return new LocationMateriel(rs.getInt("id"), rs.getInt("materiel_id"), rs.getInt("quantity"), rid, LocalDate.parse(rs.getString("date")));
    }
}

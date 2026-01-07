package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.Reservation;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReservationDao {
    public boolean hasConflict(int terrainId, LocalDate date, LocalTime start, int durationMinutes, Integer excludeId) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT id, start_time, duration_minutes FROM reservations WHERE terrain_id=? AND date=?")) {
            ps.setInt(1, terrainId);
            ps.setString(2, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    if (excludeId != null && id == excludeId) continue;
                    LocalTime s = LocalTime.parse(rs.getString("start_time"));
                    int d = rs.getInt("duration_minutes");
                    LocalTime e = s.plusMinutes(d);
                    LocalTime newEnd = start.plusMinutes(durationMinutes);
                    boolean overlap = !(newEnd.compareTo(s) <= 0 || start.compareTo(e) >= 0);
                    if (overlap) return true;
                }
                return false;
            }
        }
    }
    public Reservation add(Reservation r) throws SQLException {
        if (hasConflict(r.terrainId(), r.date(), r.startTime(), r.durationMinutes(), null)) {
            throw new SQLException("Conflit de réservation");
        }
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO reservations(terrain_id, date, start_time, duration_minutes, client_name, client_phone) " +
                             "VALUES(?,?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.terrainId());
            ps.setString(2, r.date().toString());
            ps.setString(3, r.startTime().toString());
            ps.setInt(4, r.durationMinutes());
            ps.setString(5, r.clientName());
            ps.setString(6, r.clientPhone());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new Reservation(id, r.terrainId(), r.date(), r.startTime(), r.durationMinutes(), r.clientName(), r.clientPhone());
            }
        }
    }

    public void update(Reservation r) throws SQLException {
        if (hasConflict(r.terrainId(), r.date(), r.startTime(), r.durationMinutes(), r.id())) {
            throw new SQLException("Conflit de réservation");
        }
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE reservations SET terrain_id=?, date=?, start_time=?, duration_minutes=?, client_name=?, client_phone=?, status=? WHERE id=?")) {
            ps.setInt(1, r.terrainId());
            ps.setString(2, r.date().toString());
            ps.setString(3, r.startTime().toString());
            ps.setInt(4, r.durationMinutes());
            ps.setString(5, r.clientName());
            ps.setString(6, r.clientPhone());
            ps.setString(7, r.status());
            ps.setInt(8, r.id());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int id, String status) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("UPDATE reservations SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM reservations WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Reservation> listByDate(LocalDate day) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations WHERE date=? ORDER BY start_time")) {
            ps.setString(1, day.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> res = new ArrayList<>();
                while (rs.next()) {
                    res.add(map(rs));
                }
                return res;
            }
        }
    }

    public List<Reservation> listAll() throws SQLException {
        try (Connection c = Database.get(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM reservations ORDER BY date DESC, start_time DESC")) {
            List<Reservation> res = new ArrayList<>();
            while (rs.next()) res.add(map(rs));
            return res;
        }
    }

    public List<Reservation> listBetween(LocalDate start, LocalDate end) throws SQLException {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM reservations WHERE date BETWEEN ? AND ? ORDER BY date, start_time")) {
            ps.setString(1, start.toString());
            ps.setString(2, end.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<Reservation> res = new ArrayList<>();
                while (rs.next()) res.add(map(rs));
                return res;
            }
        }
    }

    private Reservation map(ResultSet rs) throws SQLException {
        String status = "planned";
        try { status = rs.getString("status"); } catch (SQLException ignored) {}
        return new Reservation(
                rs.getInt("id"), rs.getInt("terrain_id"),
                LocalDate.parse(rs.getString("date")),
                LocalTime.parse(rs.getString("start_time")),
                rs.getInt("duration_minutes"),
                rs.getString("client_name"), rs.getString("client_phone"),
                status
        );
    }
}

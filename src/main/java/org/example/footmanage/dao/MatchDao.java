package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.Match;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class MatchDao {
    public List<Match> listByTournament(int tournamentId) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM matches WHERE tournament_id=? ORDER BY match_date, match_time")) {
            ps.setInt(1, tournamentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Match> res = new ArrayList<>();
                while (rs.next()) res.add(map(rs));
                return res;
            }
        }
    }

    public Match add(int tournamentId, int homeTeamId, int awayTeamId, LocalDate date, LocalTime time) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("INSERT INTO matches(tournament_id, home_team_id, away_team_id, match_date, match_time) VALUES(?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tournamentId);
            ps.setInt(2, homeTeamId);
            ps.setInt(3, awayTeamId);
            ps.setString(4, date.toString());
            ps.setString(5, time.toString());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new Match(id, tournamentId, homeTeamId, awayTeamId, date, time, null, null, "scheduled");
            }
        }
    }

    public void updateScore(int id, Integer home, Integer away) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("UPDATE matches SET score_home=?, score_away=?, status=? WHERE id=?")) {
            if (home == null) ps.setNull(1, Types.INTEGER); else ps.setInt(1, home);
            if (away == null) ps.setNull(2, Types.INTEGER); else ps.setInt(2, away);
            ps.setString(3, (home != null && away != null) ? "played" : "scheduled");
            ps.setInt(4, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM matches WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private Match map(ResultSet rs) throws SQLException {
        String d = rs.getString("match_date");
        String t = rs.getString("match_time");
        return new Match(
                rs.getInt("id"), rs.getInt("tournament_id"), rs.getInt("home_team_id"), rs.getInt("away_team_id"),
                LocalDate.parse(d), LocalTime.parse(t),
                (Integer) rs.getObject("score_home"), (Integer) rs.getObject("score_away"), rs.getString("status")
        );
    }
}


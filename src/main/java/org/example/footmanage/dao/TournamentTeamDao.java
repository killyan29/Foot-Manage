package org.example.footmanage.dao;

import org.example.footmanage.db.Database;
import org.example.footmanage.model.TournamentTeam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TournamentTeamDao {
    public List<TournamentTeam> listByTournament(int tournamentId) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("SELECT * FROM tournament_teams WHERE tournament_id=? ORDER BY team_name")) {
            ps.setInt(1, tournamentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<TournamentTeam> res = new ArrayList<>();
                while (rs.next()) res.add(map(rs));
                return res;
            }
        }
    }

    public TournamentTeam add(int tournamentId, String teamName) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("INSERT INTO tournament_teams(tournament_id, team_name) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tournamentId);
            ps.setString(2, teamName);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int id = rs.next() ? rs.getInt(1) : 0;
                return new TournamentTeam(id, tournamentId, teamName);
            }
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement("DELETE FROM tournament_teams WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private TournamentTeam map(ResultSet rs) throws SQLException {
        return new TournamentTeam(rs.getInt("id"), rs.getInt("tournament_id"), rs.getString("team_name"));
    }
}


package org.example.footmanage.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Match(
        int id,
        int tournamentId,
        int homeTeamId,
        int awayTeamId,
        LocalDate matchDate,
        LocalTime matchTime,
        Integer scoreHome,
        Integer scoreAway,
        String status
) {}


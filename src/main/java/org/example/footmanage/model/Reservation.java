package org.example.footmanage.model;

import java.time.LocalDate;
import java.time.LocalTime;

public record Reservation(int id, int terrainId, LocalDate date, LocalTime startTime, int durationMinutes,
                          String clientName, String clientPhone, String status) {
    public Reservation(int id, int terrainId, LocalDate date, LocalTime startTime, int durationMinutes,
                       String clientName, String clientPhone) {
        this(id, terrainId, date, startTime, durationMinutes, clientName, clientPhone, "planned");
    }
}

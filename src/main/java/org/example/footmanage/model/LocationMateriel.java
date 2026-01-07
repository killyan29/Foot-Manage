package org.example.footmanage.model;

import java.time.LocalDate;

public record LocationMateriel(int id, int materielId, int quantity, Integer reservationId, LocalDate date) {}

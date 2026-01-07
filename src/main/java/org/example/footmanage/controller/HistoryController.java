package org.example.footmanage.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.example.footmanage.dao.LocationMaterielDao;
import org.example.footmanage.dao.MaterielDao;
import org.example.footmanage.dao.ReservationDao;
import org.example.footmanage.dao.TerrainDao;
import org.example.footmanage.model.LocationMateriel;
import org.example.footmanage.model.Materiel;
import org.example.footmanage.model.Reservation;
import org.example.footmanage.model.Terrain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryController {
    @FXML private TableView<Reservation> reservationsTable;
    @FXML private TableColumn<Reservation, LocalDate> colResDate;
    @FXML private TableColumn<Reservation, LocalTime> colResStart;
    @FXML private TableColumn<Reservation, String> colResTerrain;
    @FXML private TableColumn<Reservation, Integer> colResDuration;
    @FXML private TableColumn<Reservation, String> colResClient;
    @FXML private TableColumn<Reservation, String> colResPhone;
    @FXML private TableColumn<Reservation, String> colResPrice;

    @FXML private TableView<LocationMateriel> locationsTable;
    @FXML private TableColumn<LocationMateriel, LocalDate> colLocDate;
    @FXML private TableColumn<LocationMateriel, String> colLocMateriel;
    @FXML private TableColumn<LocationMateriel, Integer> colLocQuantity;
    @FXML private TableColumn<LocationMateriel, String> colLocReservation;

    private final ReservationDao reservationDao = new ReservationDao();
    private final TerrainDao terrainDao = new TerrainDao();
    private final LocationMaterielDao locationDao = new LocationMaterielDao();
    private final MaterielDao materielDao = new MaterielDao();

    private final ObservableList<Reservation> reservations = FXCollections.observableArrayList();
    private final ObservableList<LocationMateriel> locations = FXCollections.observableArrayList();

    private Map<Integer, String> terrainNames = new HashMap<>();
    private Map<Integer, String> materielTypes = new HashMap<>();

    @FXML
    private void initialize() {
        try {
            List<Terrain> ts = terrainDao.list("");
            for (Terrain t : ts) terrainNames.put(t.id(), t.name());
            for (Materiel m : materielDao.list()) materielTypes.put(m.id(), m.type());
        } catch (Exception ignored) {}

        colResDate.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().date()));
        colResStart.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().startTime()));
        colResTerrain.setCellValueFactory(data -> new ReadOnlyStringWrapper(terrainName(data.getValue().terrainId())));
        colResDuration.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().durationMinutes()).asObject());
        colResClient.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().clientName()));
        colResPhone.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().clientPhone()));
        colResPrice.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatPrice(data.getValue())));
        reservationsTable.setItems(reservations);

        colLocDate.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().date()));
        colLocMateriel.setCellValueFactory(data -> new ReadOnlyStringWrapper(materielName(data.getValue().materielId())));
        colLocQuantity.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().quantity()).asObject());
        colLocReservation.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatReservationLink(data.getValue().reservationId())));
        locationsTable.setItems(locations);

        try { refresh(); } catch (Exception ignored) {}
    }

    private void refresh() throws Exception {
        reservations.setAll(reservationDao.listAll());
        locations.setAll(locationDao.listAll());
    }

    private String terrainName(int terrainId) {
        return terrainNames.getOrDefault(terrainId, String.valueOf(terrainId));
    }

    private String materielName(int id) {
        return materielTypes.getOrDefault(id, String.valueOf(id));
    }

    private String formatPrice(Reservation r) {
        double hours = r.durationMinutes() / 60.0;
        // capacité fixée à 10 par le schéma, mais on tente le lookup
        int participants = 10;
        String name = terrainNames.get(r.terrainId());
        if (name != null) {
            // pas de capacité dans ce contrôleur; valeur par défaut 10
        }
        double price = 10.0 * participants * hours;
        return String.format("%.2f €", price);
    }

    private String formatReservationLink(Integer reservationId) {
        if (reservationId == null) return "—";
        return "#" + reservationId;
    }
}


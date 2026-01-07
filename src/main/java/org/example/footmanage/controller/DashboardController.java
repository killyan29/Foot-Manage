package org.example.footmanage.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import org.example.footmanage.App;
import org.example.footmanage.dao.ReservationDao;
import org.example.footmanage.dao.TerrainDao;
import org.example.footmanage.Session;

import java.sql.SQLException;
import java.time.LocalDate;

public class DashboardController {
    @FXML private TitledPane tileTerrainsOccupes;
    @FXML private TitledPane tileReservationsJour;
    @FXML private TitledPane tileTerrainsDisponibles;
    @FXML private TitledPane tileMaintenance;
    @FXML private javafx.scene.control.Label userInfo;

    private final TerrainDao terrainDao = new TerrainDao();
    private final ReservationDao reservationDao = new ReservationDao();

    @FXML
    private void initialize() {
        try {
            var u = Session.getCurrentUser();
            if (userInfo != null) userInfo.setText(u != null ? ("Connecté: " + u.username()) : "Non connecté");
            var terrains = terrainDao.list("");
            int occupes = (int) terrains.stream().filter(t -> t.status().equals("occupé")).count();
            int disponibles = (int) terrains.stream().filter(t -> t.status().equals("disponible")).count();
            int maintenance = (int) terrains.stream().filter(t -> t.status().equals("maintenance")).count();
            var reservationsJourList = reservationDao.listByDate(LocalDate.now());
            int reservationsJour = reservationsJourList.size();

            tileTerrainsOccupes.setText("⛔ Occupés: " + occupes);
            tileTerrainsDisponibles.setText("✅ Disponibles: " + disponibles);
            tileMaintenance.setText("🛠️ Maintenance: " + maintenance);
            tileReservationsJour.setText("📅 Aujourd’hui: " + reservationsJour);

            ListView<String> occLv = new ListView<>();
            occLv.getItems().addAll(terrains.stream()
                    .filter(t -> t.status().equals("occupé"))
                    .map(t -> t.name() + " • cap: " + t.capacity() + (t.description() == null || t.description().isBlank() ? "" : " • " + t.description()))
                    .toList());
            Button occBtn = new Button("Ouvrir Terrains");
            occBtn.setOnAction(e -> openView("views/terrains.fxml"));
            VBox occBox = new VBox(6, occLv, occBtn);
            tileTerrainsOccupes.setContent(occBox);

            ListView<String> dispLv = new ListView<>();
            dispLv.getItems().addAll(terrains.stream()
                    .filter(t -> t.status().equals("disponible"))
                    .map(t -> t.name() + " • cap: " + t.capacity() + (t.description() == null || t.description().isBlank() ? "" : " • " + t.description()))
                    .toList());
            Button dispBtn = new Button("Ouvrir Terrains");
            dispBtn.setOnAction(e -> openView("views/terrains.fxml"));
            VBox dispBox = new VBox(6, dispLv, dispBtn);
            tileTerrainsDisponibles.setContent(dispBox);

            ListView<String> maintLv = new ListView<>();
            maintLv.getItems().addAll(terrains.stream()
                    .filter(t -> t.status().equals("maintenance"))
                    .map(t -> t.name() + (t.description() == null || t.description().isBlank() ? "" : " • " + t.description()))
                    .toList());
            Button maintBtn = new Button("Ouvrir Terrains");
            maintBtn.setOnAction(e -> openView("views/terrains.fxml"));
            VBox maintBox = new VBox(6, maintLv, maintBtn);
            tileMaintenance.setContent(maintBox);

            ListView<String> resLv = new ListView<>();
            resLv.getItems().addAll(reservationsJourList.stream()
                    .map(r -> r.startTime() + " • Terrain " + r.terrainId() + " • " + r.clientName() + " • " + r.clientPhone())
                    .toList());
            Button resBtn = new Button("Ouvrir Réservations");
            resBtn.setOnAction(e -> openView("views/reservations.fxml"));
            VBox resBox = new VBox(6, resLv, resBtn);
            tileReservationsJour.setContent(resBox);
        } catch (Exception e) {
            tileReservationsJour.setText("Réservations du jour: -");
        }
    }

    private void openView(String resource) {
        try {
            BorderPane bp = (BorderPane) tileTerrainsOccupes.getScene().getRoot();
            Node view = FXMLLoader.load(App.class.getResource(resource));
            Node center = bp.getCenter();
            if (center instanceof javafx.scene.layout.StackPane sp) {
                sp.getChildren().setAll(view);
            } else {
                bp.setCenter(view);
            }
        } catch (Exception ignored) {}
    }
}

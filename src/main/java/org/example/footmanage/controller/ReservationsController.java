package org.example.footmanage.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.example.footmanage.dao.ReservationDao;
import org.example.footmanage.dao.TerrainDao;
import org.example.footmanage.dao.MaterielDao;
import org.example.footmanage.dao.LocationMaterielDao;
import org.example.footmanage.model.Reservation;
import org.example.footmanage.model.Terrain;
import org.example.footmanage.model.Materiel;
import org.example.footmanage.model.LocationMateriel;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ReservationsController {
    @FXML private DatePicker datePicker;
    @FXML private TableView<Reservation> table;
    @FXML private TableColumn<Reservation, String> colTerrain;
    @FXML private TableColumn<Reservation, LocalDate> colDate;
    @FXML private TableColumn<Reservation, LocalTime> colStart;
    @FXML private TableColumn<Reservation, Integer> colDuration;
    @FXML private TableColumn<Reservation, String> colPrice;
    @FXML private TableColumn<Reservation, String> colClient;
    @FXML private TableColumn<Reservation, String> colPhone;
    @FXML private TableColumn<Reservation, String> colStatus;
    @FXML private TableColumn<Reservation, Void> colActions;
    @FXML private VBox calendar;
    @FXML private VBox weekContainer;

    private final ReservationDao reservationDao = new ReservationDao();
    private final TerrainDao terrainDao = new TerrainDao();
    private final MaterielDao materielDao = new MaterielDao();
    private final LocationMaterielDao locationDao = new LocationMaterielDao();
    private final ObservableList<Reservation> items = FXCollections.observableArrayList();
    private List<Terrain> terrains;
    private Map<Integer, Integer> terrainColumnIndex;

    @FXML
    private void initialize() {
        datePicker.setValue(LocalDate.now());
        colTerrain.setCellValueFactory(data -> new ReadOnlyStringWrapper(terrainName(data.getValue().terrainId())));
        colDate.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().date()));
        colStart.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().startTime()));
        colDuration.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().durationMinutes()).asObject());
        colPrice.setCellValueFactory(data -> new ReadOnlyStringWrapper(formatPrice(data.getValue())));
        colClient.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().clientName()));
        colPhone.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().clientPhone()));
        addActionsColumn();
        table.setItems(items);
        try {
            terrains = terrainDao.list("");
            terrainColumnIndex = new HashMap<>();
            for (int i = 0; i < terrains.size(); i++) terrainColumnIndex.put(terrains.get(i).id(), i + 1);
            refresh();
            renderWeek();
        } catch (Exception e) { showError(e); }
        datePicker.valueProperty().addListener((obs, o, v) -> refresh());
    }

    @FXML
    private void newReservation() {
        try {
            List<Terrain> list = terrainDao.list("");
            ReservationDialog dialog = new ReservationDialog(list, null);
            dialog.showAndWait().ifPresent(r -> {
                try {
                    Reservation saved = reservationDao.add(r);
                    try { allocateDefaultMaterials(saved); } catch (Exception e) { showError(e); }
                    refresh();
                } catch (SQLException ex) { showError(ex); }
            });
        } catch (SQLException ex) { showError(ex); }
    }

    private void edit(Reservation r) {
        try {
            List<Terrain> list = terrainDao.list("");
            ReservationDialog dialog = new ReservationDialog(list, r);
            dialog.showAndWait().ifPresent(updated -> {
                try { reservationDao.update(updated); refresh(); } catch (SQLException ex) { showError(ex); }
            });
        } catch (SQLException ex) { showError(ex); }
    }

    private void delete(Reservation r) {
        try { reservationDao.delete(r.id()); refresh(); } catch (SQLException ex) { showError(ex); }
    }

    private void addActionsColumn() {
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Annuler");
            private final Button matBtn = new Button("Matériel");
            private final Button endBtn = new Button("Terminer");
            {
                editBtn.setOnAction(e -> edit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> delete(getTableView().getItems().get(getIndex())));
                matBtn.setOnAction(e -> assignMaterial(getTableView().getItems().get(getIndex())));
                endBtn.setOnAction(e -> finishReservation(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Reservation r = getTableView().getItems().get(getIndex());
                    if ("completed".equals(r.status()) || "cancelled".equals(r.status())) {
                        setGraphic(null);
                    } else {
                        setGraphic(new ToolBar(editBtn, delBtn, matBtn, endBtn));
                    }
                }
            }
        });
    }

    private void finishReservation(Reservation r) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Terminer la réservation et rendre le matériel ?");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    // Return materials
                    List<LocationMateriel> locs = locationDao.findByReservation(r.id());
                    for (LocationMateriel loc : locs) {
                        Materiel m = materielDao.findByType(getMaterielType(loc.materielId()));
                        if (m != null) {
                            materielDao.update(m.id(), m.type(), m.stock() + loc.quantity(), Math.max(0, m.borrowed() - loc.quantity()));
                        }
                    }
                    reservationDao.updateStatus(r.id(), "completed");
                    refresh();
                } catch (Exception e) { showError(e); }
            }
        });
    }

    private String getMaterielType(int id) {
        try {
            for (Materiel m : materielDao.list()) {
                if (m.id() == id) return m.type();
            }
        } catch (Exception ignored) {}
        return "";
    }
    
    private String formatStatus(String s) {
        if ("completed".equals(s)) return "Terminée";
        if ("cancelled".equals(s)) return "Annulée";
        return "Prévue";
    }

    private void refresh() {
        try {
            items.setAll(reservationDao.listByDate(datePicker.getValue()));
            renderCalendar();
        } catch (SQLException ex) { showError(ex); }
    }

    private void renderCalendar() {
        calendar.getChildren().clear();
        GridPane grid = new GridPane();
        grid.getStyleClass().add("calendar-grid");
        grid.setHgap(6); grid.setVgap(6);
        LocalTime start = LocalTime.of(8, 0);
        LocalTime end = LocalTime.of(23, 0);
        int slot = 30;
        int rows = ((end.getHour() * 60 + end.getMinute()) - (start.getHour() * 60 + start.getMinute())) / slot;
        for (int i = 0; i < terrains.size(); i++) {
            Label header = new Label(terrains.get(i).name());
            header.getStyleClass().add("calendar-header");
            grid.add(header, i + 1, 0);
        }
        for (int r = 0; r < rows; r++) {
            LocalTime t = start.plusMinutes((long) r * slot);
            Label timeLbl = new Label(t.toString());
            timeLbl.getStyleClass().add("time-cell");
            grid.add(timeLbl, 0, r + 1);
        }
        String[] colors = {"#4CAF50","#3F51B5","#F44336","#009688","#FF9800","#9C27B0","#795548","#2196F3"};
        for (Reservation r : items) {
            Integer col = terrainColumnIndex.get(r.terrainId());
            if (col == null) continue;
            int startMinutes = r.startTime().getHour() * 60 + r.startTime().getMinute();
            int base = start.getHour() * 60 + start.getMinute();
            int rowIndex = (startMinutes - base) / slot + 1;
            int span = Math.max(1, r.durationMinutes() / slot);
            Pane block = new Pane();
            Label lbl = new Label(r.clientName() + " (" + r.startTime() + ", " + r.durationMinutes() + "min)");
            block.getChildren().add(lbl);
            block.getStyleClass().add("reservation-block");
            int colorIndex = (col - 1) % colors.length;
            block.setStyle("-fx-background-color: " + colors[colorIndex] + "; -fx-background-radius: 6; -fx-padding: 6;");
            Tooltip.install(block, new Tooltip("Client: " + r.clientName() + "\nTéléphone: " + r.clientPhone() + "\nTerrain: " + terrainName(r.terrainId()) + "\nHeure: " + r.startTime() + "\nDurée: " + r.durationMinutes() + " min" + "\nPrix: " + formatPrice(r)));
            GridPane.setRowSpan(block, span);
            grid.add(block, col, Math.max(1, rowIndex));
        }
        calendar.getChildren().add(grid);
    }

    private void renderWeek() {
        weekContainer.getChildren().clear();
        LocalDate selected = datePicker.getValue();
        LocalDate monday = selected.minusDays((selected.getDayOfWeek().getValue() + 6) % 7);
        LocalDate sunday = monday.plusDays(6);
        try {
            List<Reservation> week = reservationDao.listBetween(monday, sunday);
            String[] dayNames = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
            for (Terrain t : terrains) {
                TitledPane pane = new TitledPane();
                pane.setText(t.name());
                GridPane grid = new GridPane();
                grid.setHgap(6); grid.setVgap(6);
                for (int d = 0; d < 7; d++) {
                    Label header = new Label(dayNames[d]);
                    header.getStyleClass().add("calendar-header");
                    grid.add(header, d + 1, 0);
                }
                LocalTime start = LocalTime.of(8, 0);
                LocalTime end = LocalTime.of(23, 0);
                int slot = 60;
                int rows = ((end.getHour() * 60 + end.getMinute()) - (start.getHour() * 60 + start.getMinute())) / slot;
                for (int r = 0; r < rows; r++) {
                    LocalTime time = start.plusMinutes((long) r * slot);
                    grid.add(new Label(time.toString()), 0, r + 1);
                }
                for (Reservation r : week) {
                    if (r.terrainId() != t.id()) continue;
                    int dayIndex = (int) (r.date().toEpochDay() - monday.toEpochDay());
                    int startMinutes = r.startTime().getHour() * 60 + r.startTime().getMinute();
                    int base = start.getHour() * 60 + start.getMinute();
                    int rowIndex = (startMinutes - base) / slot + 1;
                    int span = Math.max(1, Math.ceilDiv(r.durationMinutes(), slot));
                    Pane block = new Pane();
                    Label lbl = new Label(r.clientName() + " (" + r.startTime() + ")");
                    block.getChildren().add(lbl);
                    block.getStyleClass().add("reservation-block");
                    GridPane.setRowSpan(block, span);
                    grid.add(block, dayIndex + 1, Math.max(1, rowIndex));
                }
                pane.setContent(grid);
                pane.setExpanded(false);
                weekContainer.getChildren().add(pane);
            }
        } catch (Exception e) { showError(e); }
    }

    private String terrainName(int terrainId) {
        if (terrains == null) return String.valueOf(terrainId);
        for (Terrain t : terrains) if (t.id() == terrainId) return t.name();
        return String.valueOf(terrainId);
    }

    private void allocateDefaultMaterials(Reservation r) throws Exception {
        int cap = terrainCapacity(r.terrainId());
        int chasubles = Math.max(0, cap / 2);
        allocateTypeToReservation("ballon", 1, r);
        allocateTypeToReservation("chasuble", chasubles, r);
    }

    private void assignMaterial(Reservation r) {
        try {
            MaterialDialog dlg = new MaterialDialog(r);
            dlg.showAndWait().ifPresent(q -> {
                try { allocateTypeToReservation(dlg.getSelectedType(), q, r); refresh(); } catch (Exception e) { showError(e); }
            });
        } catch (Exception e) { showError(e); }
    }

    private void allocateTypeToReservation(String type, int requestedQty, Reservation r) throws Exception {
        if (requestedQty <= 0) return;
        Materiel m = materielDao.findByType(type);
        if (m == null) { new Alert(Alert.AlertType.WARNING, "Matériel manquant: " + type).showAndWait(); return; }
        int avail = Math.max(0, m.stock());
        int qty = Math.min(requestedQty, avail);
        if (qty <= 0) { new Alert(Alert.AlertType.INFORMATION, "Stock insuffisant pour " + type).showAndWait(); return; }
        locationDao.add(new LocationMateriel(0, m.id(), qty, r.id(), r.date()));
        materielDao.update(m.id(), m.type(), m.stock() - qty, m.borrowed() + qty);
    }

    private int terrainCapacity(int terrainId) {
        if (terrains == null) return 0;
        for (Terrain t : terrains) if (t.id() == terrainId) return t.capacity();
        return 0;
    }

    private String formatPrice(Reservation r) {
        int participants = terrainCapacity(r.terrainId());
        double hours = r.durationMinutes() / 60.0;
        double price = 10.0 * participants * hours;
        return String.format("%.2f €", price);
    }

    @FXML private void previousDay() { datePicker.setValue(datePicker.getValue().minusDays(1)); renderWeek(); }
    @FXML private void nextDay() { datePicker.setValue(datePicker.getValue().plusDays(1)); renderWeek(); }

    private void showError(Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
}

class ReservationDialog extends Dialog<Reservation> {
    private final ComboBox<Terrain> terrainBox = new ComboBox<>();
    private final DatePicker date = new DatePicker(LocalDate.now());
    private final Spinner<Integer> duration = new Spinner<>(30, 180, 60, 30);
    private final TextField start = new TextField("18:00");
    private final TextField clientName = new TextField();
    private final TextField clientPhone = new TextField();
    private final Label priceLbl = new Label();

    ReservationDialog(List<Terrain> terrains, Reservation existing) {
        setTitle(existing == null ? "Nouvelle réservation" : "Modifier la réservation");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        terrainBox.setItems(FXCollections.observableArrayList(terrains));
        terrainBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Terrain t) { return t == null ? "" : t.name(); }
            @Override public Terrain fromString(String s) { return null; }
        });
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Terrain"), terrainBox);
        grid.addRow(1, new Label("Date"), date);
        grid.addRow(2, new Label("Heure de début"), start);
        grid.addRow(3, new Label("Durée (min)"), duration);
        grid.addRow(4, new Label("Nom client"), clientName);
        grid.addRow(5, new Label("Téléphone"), clientPhone);
        grid.addRow(6, new Label("Prix estimé"), priceLbl);
        getDialogPane().setContent(grid);
        if (existing != null) {
            terrainBox.getSelectionModel().select(terrains.stream().filter(t -> t.id() == existing.terrainId()).findFirst().orElse(null));
            date.setValue(existing.date());
            start.setText(existing.startTime().toString());
            duration.getValueFactory().setValue(existing.durationMinutes());
            clientName.setText(existing.clientName());
            clientPhone.setText(existing.clientPhone());
        } else {
            terrainBox.getSelectionModel().selectFirst();
        }

        Runnable updatePrice = () -> {
            Terrain t = terrainBox.getValue();
            int cap = t != null ? t.capacity() : 0;
            double hours = duration.getValue() / 60.0;
            double price = 10.0 * cap * hours;
            priceLbl.setText(String.format("%.2f €", price));
        };
        terrainBox.valueProperty().addListener((obs, o, v) -> updatePrice.run());
        duration.valueProperty().addListener((obs, o, v) -> updatePrice.run());
        updatePrice.run();
        setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            try {
                LocalTime st = LocalTime.parse(start.getText());
                if (st.getMinute() % 30 != 0) { new Alert(Alert.AlertType.WARNING, "Heure de début par pas de 30 min").showAndWait(); return null; }
                LocalTime open = LocalTime.of(8, 0); LocalTime close = LocalTime.of(23, 0);
                if (st.isBefore(open) || st.isAfter(close)) { new Alert(Alert.AlertType.WARNING, "Heure hors plage 08:00-23:00").showAndWait(); return null; }
                if (terrainBox.getValue() == null) { new Alert(Alert.AlertType.WARNING, "Sélectionner un terrain").showAndWait(); return null; }
                return new Reservation(existing == null ? 0 : existing.id(), terrainBox.getValue().id(), date.getValue(), st, duration.getValue(), clientName.getText(), clientPhone.getText());
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
                return null;
            }
        });
    }
}

class MaterialDialog extends Dialog<Integer> {
    private final ComboBox<String> typeBox = new ComboBox<>();
    private final Spinner<Integer> quantity = new Spinner<>(1, 100, 1, 1);
    private final Reservation reservation;

    MaterialDialog(Reservation reservation) {
        this.reservation = reservation;
        setTitle("Attribuer du matériel");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        typeBox.setItems(FXCollections.observableArrayList("ballon", "chasuble", "cône"));
        typeBox.getSelectionModel().selectFirst();
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Type"), typeBox);
        grid.addRow(1, new Label("Quantité"), quantity);
        getDialogPane().setContent(grid);
        setResultConverter(bt -> bt == ButtonType.OK ? quantity.getValue() : null);
    }

    String getSelectedType() { return typeBox.getValue(); }
}

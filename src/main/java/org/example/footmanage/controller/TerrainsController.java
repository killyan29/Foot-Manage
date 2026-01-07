package org.example.footmanage.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.example.footmanage.dao.TerrainDao;
import javafx.scene.layout.GridPane;
import org.example.footmanage.model.Terrain;

import java.sql.SQLException;

public class TerrainsController {
    @FXML private TextField searchField;
    @FXML private TableView<Terrain> table;
    @FXML private TableColumn<Terrain, String> colName;
    @FXML private TableColumn<Terrain, String> colStatus;
    @FXML private TableColumn<Terrain, Integer> colCapacity;
    @FXML private TableColumn<Terrain, String> colDescription;
    @FXML private TableColumn<Terrain, Void> colActions;

    private final TerrainDao dao = new TerrainDao();
    private final ObservableList<Terrain> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().name()));
        colStatus.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().status()));
        colCapacity.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().capacity()).asObject());
        colDescription.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().description()));
        addActionsColumn();
        table.setItems(items);
        try { refresh(); } catch (Exception e) { showError(e); }
        searchField.textProperty().addListener((obs, old, v) -> refresh());
    }

    private void addActionsColumn() {
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button delBtn = new Button("Supprimer");
            {
                editBtn.setOnAction(e -> edit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> delete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(new ToolBar(editBtn, delBtn));
            }
        });
    }

    @FXML
    private void addTerrain() {
        TerrainDialog dialog = new TerrainDialog(null);
        dialog.showAndWait().ifPresent(t -> {
            try { dao.add(t.name(), t.description(), t.status(), t.capacity()); refresh(); } catch (SQLException ex) { showError(ex); }
        });
    }

    private void edit(Terrain t) {
        TerrainDialog dialog = new TerrainDialog(t);
        dialog.showAndWait().ifPresent(updated -> {
            try { dao.update(t.id(), updated.name(), updated.description(), updated.status(), updated.capacity()); refresh(); } catch (SQLException ex) { showError(ex); }
        });
    }

    private void delete(Terrain t) {
        try { dao.delete(t.id()); refresh(); } catch (SQLException ex) { showError(ex); }
    }

    private void refresh() {
        try { items.setAll(dao.list(searchField.getText())); } catch (SQLException ex) { showError(ex); }
    }

    private void showError(Exception ex) {
        new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait();
    }
}

class TerrainDialog extends Dialog<Terrain> {
    private final TextField name = new TextField();
    private final ComboBox<String> status = new ComboBox<>(FXCollections.observableArrayList("disponible","occupé","maintenance"));
    private final Spinner<Integer> capacity = new Spinner<>(10, 10, 10);
    private final TextArea description = new TextArea();

    TerrainDialog(Terrain existing) {
        setTitle(existing == null ? "Ajouter un terrain" : "Modifier le terrain");
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Nom"), name);
        grid.addRow(1, new Label("Statut"), status);
        grid.addRow(2, new Label("Capacité"), capacity);
        grid.addRow(3, new Label("Description"), description);
        getDialogPane().setContent(grid);
        if (existing != null) {
            name.setText(existing.name());
            status.getSelectionModel().select(existing.status());
            capacity.getValueFactory().setValue(existing.capacity());
            description.setText(existing.description());
        } else {
            status.getSelectionModel().selectFirst();
        }
        setResultConverter(bt -> bt == ButtonType.OK ? new Terrain(existing == null ? 0 : existing.id(), name.getText(), description.getText(), status.getValue(), capacity.getValue()) : null);
    }
}

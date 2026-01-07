package org.example.footmanage.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.example.footmanage.dao.MaterielDao;
import javafx.scene.layout.GridPane;
import org.example.footmanage.model.Materiel;

import java.sql.SQLException;

public class MaterielController {
    @FXML private ComboBox<String> typeBox;
    @FXML private Spinner<Integer> stockSpinner;
    @FXML private TableView<Materiel> table;
    @FXML private TableColumn<Materiel, String> colType;
    @FXML private TableColumn<Materiel, Integer> colStock;
    @FXML private TableColumn<Materiel, Integer> colBorrowed;
    @FXML private TableColumn<Materiel, Void> colActions;

    private final MaterielDao dao = new MaterielDao();
    private final ObservableList<Materiel> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        typeBox.setItems(FXCollections.observableArrayList("ballon","chasuble","cône"));
        stockSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 500, 10));
        colType.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().type()));
        colStock.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().stock()).asObject());
        colBorrowed.setCellValueFactory(data -> new ReadOnlyIntegerWrapper(data.getValue().borrowed()).asObject());
        addActionsColumn();
        table.setItems(items);
        try { refresh(); } catch (Exception e) { showError(e); }
    }

    @FXML
    private void add() {
        String type = typeBox.getValue();
        Integer stock = stockSpinner.getValue();
        if (type == null || stock == null) return;
        try { dao.add(type, stock); refresh(); } catch (SQLException ex) { showError(ex); }
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

    private void edit(Materiel m) {
        Dialog<Materiel> dialog = new Dialog<>();
        dialog.setTitle("Modifier le matériel");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Spinner<Integer> stock = new Spinner<>(0, 500, m.stock());
        Spinner<Integer> borrowed = new Spinner<>(0, 500, m.borrowed());
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Stock"), stock);
        grid.addRow(1, new Label("Empruntés"), borrowed);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? new Materiel(m.id(), m.type(), stock.getValue(), borrowed.getValue()) : null);
        dialog.showAndWait().ifPresent(updated -> {
            try { dao.update(updated.id(), updated.type(), updated.stock(), updated.borrowed()); refresh(); } catch (SQLException ex) { showError(ex); }
        });
    }

    private void delete(Materiel m) {
        try { dao.delete(m.id()); refresh(); } catch (SQLException ex) { showError(ex); }
    }

    private void refresh() {
        try { items.setAll(dao.list()); } catch (SQLException ex) { showError(ex); }
    }

    private void showError(Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
}

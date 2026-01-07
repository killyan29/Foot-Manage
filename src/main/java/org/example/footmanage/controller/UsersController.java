package org.example.footmanage.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.example.footmanage.dao.UserDao;
import org.example.footmanage.model.User;
import org.mindrot.jbcrypt.BCrypt;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;

public class UsersController {
    @FXML private TextField searchField;
    @FXML private TableView<User> table;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, Void> colActions;

    private final UserDao dao = new UserDao();
    private final ObservableList<User> items = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        colUsername.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().username()));
        addActionsColumn();
        table.setItems(items);
        try { refresh(); } catch (Exception e) { showError(e); }
        searchField.textProperty().addListener((obs, old, v) -> refresh());
    }

    private void addActionsColumn() {
        colActions.setCellFactory(tc -> new TableCell<>() {
            private final Button resetBtn = new Button("Réinitialiser MDP");
            private final Button delBtn = new Button("Supprimer");
            {
                resetBtn.setOnAction(e -> resetPassword(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> delete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(new ToolBar(resetBtn, delBtn));
            }
        });
    }

    @FXML
    private void addUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Nouvel utilisateur");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField username = new TextField();
        PasswordField password = new PasswordField();
        PasswordField confirm = new PasswordField();
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label("Identifiant"), username);
        grid.addRow(1, new Label("Mot de passe"), password);
        grid.addRow(2, new Label("Confirmation"), confirm);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> bt == ButtonType.OK ? new User(0, username.getText(), BCrypt.hashpw(password.getText(), BCrypt.gensalt())) : null);
        dialog.showAndWait().ifPresent(u -> {
            try {
                if (!password.getText().equals(confirm.getText())) { showError(new Exception("Les mots de passe ne correspondent pas")); return; }
                dao.create(u.username(), password.getText());
                refresh();
            } catch (SQLException ex) { showError(ex); }
        });
    }

    private void resetPassword(User u) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Réinitialiser le mot de passe");
        dialog.setHeaderText("Utilisateur: " + u.username());
        dialog.setContentText("Nouveau mot de passe:");
        dialog.showAndWait().ifPresent(pw -> {
            try { dao.updatePassword(u.id(), pw); refresh(); } catch (SQLException ex) { showError(ex); }
        });
    }

    private void delete(User u) {
        try { dao.delete(u.id()); refresh(); } catch (SQLException ex) { showError(ex); }
    }

    private void refresh() {
        try { items.setAll(dao.list(searchField.getText())); } catch (SQLException ex) { showError(ex); }
    }

    private void showError(Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
}

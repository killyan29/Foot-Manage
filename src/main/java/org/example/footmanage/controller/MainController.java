package org.example.footmanage.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.footmanage.App;
import org.example.footmanage.Session;

public class MainController {
    @FXML private StackPane content;
    @FXML private VBox sidebar;
    @FXML private Label currentUserLabel;
    @FXML private Button logoutBtn;
    @FXML private Button usersBtn;
    private boolean fullscreen;

    @FXML
    private void initialize() {
        try { setCenter("views/dashboard.fxml"); } catch (Exception e) { showError(e); }
        var user = Session.getCurrentUser();
        if (currentUserLabel != null) {
            currentUserLabel.setText(user != null ? ("Connecté: " + user.username()) : "Non connecté");
        }
        if (logoutBtn != null) logoutBtn.setDisable(user == null);
        
        // Only admin can see users button
        if (usersBtn != null) {
            boolean isAdmin = user != null && "admin".equals(user.username());
            usersBtn.setVisible(isAdmin);
            usersBtn.setManaged(isAdmin);
        }
    }

    @FXML private void showDashboard() { try { setCenter("views/dashboard.fxml"); } catch (Exception e) { showError(e); } }
    @FXML private void showTerrains() { try { setCenter("views/terrains.fxml"); } catch (Exception e) { showError(e); } }
    @FXML private void showReservations() { try { setCenter("views/reservations.fxml"); } catch (Exception e) { showError(e); } }
    @FXML private void showMateriel() { try { setCenter("views/materiel.fxml"); } catch (Exception e) { showError(e); } }
    @FXML
    private void showUsers() {
        var u = Session.getCurrentUser();
        if (u != null && "admin".equals(u.username())) {
            try { setCenter("views/users.fxml"); } catch (Exception e) { showError(e); }
        } else {
            // Optional: alert user? For now just log
            System.err.println("Access denied");
        }
    }
    @FXML private void showHistory() { try { setCenter("views/history.fxml"); } catch (Exception e) { showError(e); } }
    @FXML private void showTournaments() { try { setCenter("views/tournaments.fxml"); } catch (Exception e) { showError(e); } }

    private void setCenter(String resource) throws Exception {
        Node view = FXMLLoader.load(App.class.getResource(resource));
        content.getChildren().setAll(view);
    }

    private void showError(Exception e) {
        System.err.println("Erreur de navigation: " + e.getMessage());
    }

    @FXML
    private void toggleFullscreen() {
        fullscreen = !fullscreen;
        Stage stage = (Stage) content.getScene().getWindow();
        if (stage != null) {
            stage.setFullScreen(fullscreen);
        }
    }

    @FXML
    private void logout() {
        try {
            Session.setCurrentUser(null);
            Stage stage = (Stage) content.getScene().getWindow();
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("views/login.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            stage.setScene(scene);
        } catch (Exception e) { showError(e); }
    }

}

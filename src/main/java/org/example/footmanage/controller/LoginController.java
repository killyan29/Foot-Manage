package org.example.footmanage.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.example.footmanage.App;
import org.example.footmanage.Session;
import org.example.footmanage.dao.UserDao;
import org.example.footmanage.model.User;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final UserDao userDao = new UserDao();

    @FXML
    private void login(ActionEvent e) {
        try {
            String u = usernameField.getText().trim();
            String p = passwordField.getText();
            User user = userDao.authenticate(u, p);
            if (user == null) {
                errorLabel.setText("Identifiants invalides");
                return;
            }
            Session.setCurrentUser(user);
            Stage stage = (Stage) loginButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(App.class.getResource("views/main.fxml"));
            Scene scene = new Scene(loader.load());
            stage.setScene(scene);
        } catch (Exception ex) {
            errorLabel.setText("Erreur: " + ex.getMessage());
        }
    }
}

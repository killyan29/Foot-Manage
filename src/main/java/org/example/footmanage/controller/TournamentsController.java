package org.example.footmanage.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.layout.GridPane;
import org.example.footmanage.dao.TournamentDao;
import org.example.footmanage.dao.TournamentTeamDao;
import org.example.footmanage.dao.MatchDao;
import org.example.footmanage.model.Tournament;
import org.example.footmanage.model.TournamentTeam;
import org.example.footmanage.model.Match;

import java.time.LocalDate;

public class TournamentsController {
    @FXML private ComboBox<Tournament> tournamentBox;
    @FXML private TextField teamField;
    @FXML private TableView<TournamentTeam> teamsTable;
    @FXML private TableColumn<TournamentTeam, String> colTeamName;
    @FXML private TableColumn<TournamentTeam, Void> colTeamActions;

    @FXML private ComboBox<TournamentTeam> homeTeamBox;
    @FXML private ComboBox<TournamentTeam> awayTeamBox;
    @FXML private javafx.scene.control.DatePicker matchDate;
    @FXML private TextField matchTime;
    @FXML private TableView<Match> matchesTable;
    @FXML private TableColumn<Match, java.time.LocalDate> colMatchDate;
    @FXML private TableColumn<Match, java.time.LocalTime> colMatchTime;
    @FXML private TableColumn<Match, String> colMatchHome;
    @FXML private TableColumn<Match, String> colMatchAway;
    @FXML private TableColumn<Match, String> colMatchScore;
    @FXML private TableColumn<Match, Void> colMatchActions;

    private final TournamentDao tournamentDao = new TournamentDao();
    private final TournamentTeamDao teamDao = new TournamentTeamDao();
    private final ObservableList<Tournament> tournaments = FXCollections.observableArrayList();
    private final ObservableList<TournamentTeam> teams = FXCollections.observableArrayList();
    private final ObservableList<Match> matches = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        tournamentBox.setItems(tournaments);
        tournamentBox.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Tournament t) { return t == null ? "" : t.name(); }
            @Override public Tournament fromString(String s) { return null; }
        });
        colTeamName.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().teamName()));
        addActionsColumn();
        teamsTable.setItems(teams);
        try { refreshTournaments(); } catch (Exception ignored) {}
        tournamentBox.valueProperty().addListener((obs, o, v) -> { refreshTeams(); refreshMatches(); });

        homeTeamBox.setItems(teams);
        awayTeamBox.setItems(teams);

        colMatchDate.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyObjectWrapper<>(data.getValue().matchDate()));
        colMatchTime.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyObjectWrapper<>(data.getValue().matchTime()));
        colMatchHome.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(teamName(data.getValue().homeTeamId())));
        colMatchAway.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(teamName(data.getValue().awayTeamId())));
        colMatchScore.setCellValueFactory(data -> new javafx.beans.property.ReadOnlyStringWrapper(formatScore(data.getValue())));
        addMatchActionsColumn();
        matchesTable.setItems(matches);
    }

    @FXML
    private void addTournament() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nouveau tournoi");
        dlg.setHeaderText(null);
        dlg.setContentText("Nom du tournoi:");
        dlg.showAndWait().ifPresent(name -> {
            try {
                Tournament t = tournamentDao.add(name.trim(), LocalDate.now());
                refreshTournaments();
                tournamentBox.getSelectionModel().select(t);
            } catch (Exception e) { showError(e); }
        });
    }

    @FXML
    private void addTeam() {
        Tournament t = tournamentBox.getValue();
        if (t == null) { showWarn("Sélectionnez un tournoi"); return; }
        String name = teamField.getText();
        if (name == null || name.isBlank()) { showWarn("Entrez un nom d'équipe"); return; }
        try {
            teamDao.add(t.id(), name.trim());
            teamField.clear();
            refreshTeams();
        } catch (Exception e) { showError(e); }
    }

    @FXML
    private void addMatch() {
        Tournament t = tournamentBox.getValue();
        if (t == null) { showWarn("Sélectionnez un tournoi"); return; }
        TournamentTeam home = homeTeamBox.getValue();
        TournamentTeam away = awayTeamBox.getValue();
        if (home == null || away == null || home.id() == away.id()) { showWarn("Choisissez deux équipes différentes"); return; }
        var d = matchDate.getValue();
        if (d == null) { showWarn("Choisissez une date"); return; }
        java.time.LocalTime time;
        try {
            time = java.time.LocalTime.parse(matchTime.getText());
        } catch (Exception e) { showWarn("Heure invalide (HH:mm)"); return; }
        try {
            new MatchDao().add(t.id(), home.id(), away.id(), d, time);
            refreshMatches();
        } catch (Exception e) { showError(e); }
    }

    private void addActionsColumn() {
        colTeamActions.setCellFactory(tc -> new TableCell<>() {
            private final Button delBtn = new Button("Supprimer");
            {
                delBtn.setOnAction(e -> delete(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(new ToolBar(delBtn));
            }
        });
    }

    private void delete(TournamentTeam tt) {
        try { teamDao.delete(tt.id()); refreshTeams(); } catch (Exception e) { showError(e); }
    }

    private void addMatchActionsColumn() {
        colMatchActions.setCellFactory(tc -> new TableCell<>() {
            private final Button scoreBtn = new Button("Score");
            private final Button delBtn = new Button("Supprimer");
            {
                scoreBtn.setOnAction(e -> editScore(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> deleteMatch(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(new ToolBar(scoreBtn, delBtn));
            }
        });
    }

    private void editScore(Match m) {
        Dialog<int[]> dlg = new Dialog<>();
        dlg.setTitle("Score");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Spinner<Integer> home = new Spinner<>(0, 99, m.scoreHome() == null ? 0 : m.scoreHome(), 1);
        Spinner<Integer> away = new Spinner<>(0, 99, m.scoreAway() == null ? 0 : m.scoreAway(), 1);
        GridPane grid = new GridPane(); grid.setHgap(8); grid.setVgap(8);
        grid.addRow(0, new Label(teamName(m.homeTeamId())), home);
        grid.addRow(1, new Label(teamName(m.awayTeamId())), away);
        dlg.getDialogPane().setContent(grid);
        dlg.setResultConverter(bt -> bt == ButtonType.OK ? new int[]{home.getValue(), away.getValue()} : null);
        dlg.showAndWait().ifPresent(vals -> {
            try { new MatchDao().updateScore(m.id(), vals[0], vals[1]); refreshMatches(); } catch (Exception e) { showError(e); }
        });
    }

    private void deleteMatch(Match m) {
        try { new MatchDao().delete(m.id()); refreshMatches(); } catch (Exception e) { showError(e); }
    }

    private void refreshTournaments() throws Exception {
        tournaments.setAll(tournamentDao.list());
    }

    private void refreshTeams() {
        Tournament t = tournamentBox.getValue();
        if (t == null) { teams.clear(); return; }
        try { teams.setAll(teamDao.listByTournament(t.id())); } catch (Exception e) { showError(e); }
    }

    private void refreshMatches() {
        Tournament t = tournamentBox.getValue();
        if (t == null) { matches.clear(); return; }
        try { matches.setAll(new MatchDao().listByTournament(t.id())); } catch (Exception e) { showError(e); }
    }

    private String formatScore(Match m) {
        Integer h = m.scoreHome(); Integer a = m.scoreAway();
        if (h == null || a == null) return "—";
        return h + " - " + a;
    }

    private String teamName(int teamId) {
        for (TournamentTeam tt : teams) if (tt.id() == teamId) return tt.teamName();
        return String.valueOf(teamId);
    }

    private void showError(Exception ex) { new Alert(Alert.AlertType.ERROR, ex.getMessage()).showAndWait(); }
    private void showWarn(String msg) { new Alert(Alert.AlertType.WARNING, msg).showAndWait(); }
}

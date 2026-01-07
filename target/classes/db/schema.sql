CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS terrains (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    status ENUM('disponible','occupé','maintenance') NOT NULL,
    capacity INT NOT NULL DEFAULT 10
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reservations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    terrain_id INT NOT NULL,
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    duration_minutes INT NOT NULL,
    client_name VARCHAR(255) NOT NULL,
    client_phone VARCHAR(64) NOT NULL,
    FOREIGN KEY (terrain_id) REFERENCES terrains(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS materiel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type ENUM('ballon','chasuble','cône') NOT NULL,
    stock INT NOT NULL,
    borrowed INT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS locations_materiel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    materiel_id INT NOT NULL,
    quantity INT NOT NULL,
    reservation_id INT,
    date DATE NOT NULL,
    FOREIGN KEY (materiel_id) REFERENCES materiel(id) ON DELETE CASCADE,
    FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tournaments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    start_date DATE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS tournament_teams (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tournament_id INT NOT NULL,
    team_name VARCHAR(255) NOT NULL,
    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS matches (
    id INT AUTO_INCREMENT PRIMARY KEY,
    tournament_id INT NOT NULL,
    home_team_id INT NOT NULL,
    away_team_id INT NOT NULL,
    match_date DATE NOT NULL,
    match_time TIME NOT NULL,
    score_home INT DEFAULT NULL,
    score_away INT DEFAULT NULL,
    status ENUM('scheduled','played') NOT NULL DEFAULT 'scheduled',
    FOREIGN KEY (tournament_id) REFERENCES tournaments(id) ON DELETE CASCADE,
    FOREIGN KEY (home_team_id) REFERENCES tournament_teams(id) ON DELETE CASCADE,
    FOREIGN KEY (away_team_id) REFERENCES tournament_teams(id) ON DELETE CASCADE
) ENGINE=InnoDB;

ALTER TABLE reservations ADD COLUMN status ENUM('planned','completed','cancelled') NOT NULL DEFAULT 'planned';

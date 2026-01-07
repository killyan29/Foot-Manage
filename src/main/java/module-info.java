module org.example.footmanage {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jbcrypt;
    

    opens org.example.footmanage to javafx.fxml;
    opens org.example.footmanage.controller to javafx.fxml;
    exports org.example.footmanage;
}

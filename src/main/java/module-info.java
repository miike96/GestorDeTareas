module org.miguel.gestordetareas {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires java.sql;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;

    opens org.miguel.gestordetareas to javafx.fxml;
    opens org.miguel.gestordetareas.controller to javafx.fxml;

    exports org.miguel.gestordetareas;
}
module com.example.pdffetcher {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.text;
    requires java.net.http;
    requires openhtmltopdf.pdfbox;

    opens com.example.pdffetcher to javafx.fxml;
    exports com.example.pdffetcher;
}
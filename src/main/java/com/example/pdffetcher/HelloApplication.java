package com.example.pdffetcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.Supplier;

public class HelloApplication extends Application {
    private static Integer languageCode = 1035;

    @Override
    public void start(Stage primaryStage) {
        Label welcomeLabel = new Label("PDF-generaattori");
        Label result = new Label("Odotetaan syötteitä...");
        Label instructions = new Label("Käyttöohjeet löytyvät HUS Sisäisestä.");

        TextField cookieInputField = new TextField();
        ComboBox<String> contentApiInputField = new ComboBox<>();
        ComboBox<String> formatChoice = new ComboBox<>();
        TextField questionsApiInputField = new TextField();
        questionsApiInputField.setVisible(false);

        cookieInputField.setPromptText("Syötä cookie");
        contentApiInputField.setPromptText("Valitse API URL");

        formatChoice.getItems().addAll("PDF", "DOCX");
        formatChoice.setValue("PDF");
        formatChoice.setPromptText("Valitse formaatti");

        Map<String, String> apiMap = getApiMap();

        contentApiInputField.getItems().addAll(apiMap.keySet());

        RadioButton finnishChoice = new RadioButton("Suomi");
        RadioButton swedishChoice = new RadioButton("Ruotsi");

        ToggleGroup group = new ToggleGroup();
        finnishChoice.setToggleGroup(group);
        swedishChoice.setToggleGroup(group);

        finnishChoice.setSelected(true);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.toString().contains("Ruotsi")){
                languageCode = 1053;
            } else {
                languageCode = 1035;
            }
        });

        Button submitButton = getSubmitButton(result, cookieInputField, () -> apiMap.get(contentApiInputField.getValue()), () -> formatChoice.getValue());

        VBox vbox = new VBox(20);
        vbox.getChildren().addAll(welcomeLabel, instructions, contentApiInputField, formatChoice, cookieInputField, finnishChoice, swedishChoice, submitButton, result);

        Scene scene = new Scene(vbox, 450, 450);

        primaryStage.setTitle("PDF Fetcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Map<String, String> getApiMap() {
        Map<String, String> apiMap = new LinkedHashMap<>();
        apiMap.put("Alkoholin liikakäyttö", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/8527bf59-9165-e811-8136-5065f38aea61");
        apiMap.put("Masennus", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/3cd9f01c-9365-e811-8136-5065f38aea61");
        apiMap.put("Pitkäaikaiset keholliset oireet", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/d6db6dd0-d93a-eb11-a813-000d3ab82d69");
        apiMap.put("Syöpään sairastuneet", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/b8f61edb-460f-ee11-8f6d-000d3adf734d");
        apiMap.put("Uupumus", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/f753fb8b-887c-4925-42c6-08dcb6cf3870");
        apiMap.put("Ahmiminen", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/143cb0f0-a5d5-4298-b67d-08dd468e341e");
        apiMap.put("Kaksisuuntainen mielealahäiriö", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/83685891-871a-49ad-e2a9-08ddea150fbe");
        apiMap.put("Pakko-oireinen häiriö (OCD)", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/6f94e395-9465-e811-8136-5065f38aea61");
        apiMap.put("Sosiaalisten tilanteiden pelko", "https://omapolku.terveyskyla.fi/api/treatmentfeed/gettreatmenttasks/6629ff64-9a65-e811-8136-5065f38aea61");
        return apiMap;
    }

    private static Button getSubmitButton(Label result, TextField cookieInputField, Supplier<String> apiUrlSupplier, Supplier<String> formatSupplier) {
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            String format = formatSupplier.get();
            if (format == null) format = "PDF";
            result.setText("Generointi käynnissä, odota hetki...");
            System.out.println("Generating " + format + "...");

            String cookieInput = cookieInputField.getText();
            String contentApiInput = apiUrlSupplier.get();
            if (contentApiInput == null) {
                result.setText("Valitse API URL.");
                return;
            }
            try {
                String fileName;
                if ("DOCX".equalsIgnoreCase(format)) {
                    result.setText("Generoidaan DOCX-tiedostoa, odota hetki...");
                    fileName = JsonService.generateDocx(cookieInput, contentApiInput, languageCode);
                    result.setText("DOCX valmis: " + fileName);
                } else {
                    result.setText("Generoidaan PDF-tiedostoa, odota hetki...");
                    fileName = JsonService.generatePdf(cookieInput, contentApiInput, languageCode);
                    result.setText("PDF valmis: " + fileName);
                }
            } catch (Exception ex) {
                result.setText("Error occurred: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        });
        return submitButton;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

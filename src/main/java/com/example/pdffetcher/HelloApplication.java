package com.example.pdffetcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Map;
import java.util.LinkedHashMap;

public class HelloApplication extends Application {
    private static Integer languageCode = 1035;

    @Override
    public void start(Stage primaryStage) {
        Label welcomeLabel = new Label("PDF-generaattori");
        Label result = new Label("Odotetaan syötteitä...");

        TextField cookieInputField = new TextField();
        ComboBox<String> contentApiInputField = new ComboBox<>();
        TextField questionsApiInputField = new TextField();
        questionsApiInputField.setVisible(false);

        cookieInputField.setPromptText("Syötä cookie");
        contentApiInputField.setPromptText("Valitse API URL");

        Map<String, String> apiMap = new LinkedHashMap<>();
        apiMap.put("Production", "https://api.example.com/content");
        apiMap.put("Staging", "https://staging.api.example.com/content");
        // Add descriptive names to the dropdown (preserves insertion order)
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

        Button nextPageButton = new Button("Kuinka saan arvot?");

        nextPageButton.setOnAction(e -> {
            Label secondPageLabel = new Label("""
                    Ohjeet arvojen hakemiseen:

                    Jotain ohjeita tänne. Semmoset mistä saa kuka vaan selvää, ei mitään ammattijargonia tai isoja sanoja.
                    """);
            Button backButton = new Button("Takaisin");

            VBox secondPageLayout = new VBox(20);
            secondPageLayout.getChildren().addAll(secondPageLabel, backButton);

            Scene secondScene = new Scene(secondPageLayout, 640, 550);

            Stage stage = (Stage) ((Button) e.getSource()).getScene().getWindow();
            stage.setScene(secondScene);

            backButton.setOnAction(ev -> start(primaryStage));
        });

        Button submitButton = getSubmitButton(result, cookieInputField, contentApiInputField, questionsApiInputField, apiMap);

        VBox vbox = new VBox(20);
        vbox.getChildren().addAll(welcomeLabel, nextPageButton, contentApiInputField, cookieInputField, finnishChoice, swedishChoice, questionsApiInputField, submitButton, result);

        Scene scene = new Scene(vbox, 450, 450);

        primaryStage.setTitle("PDF Fetcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Button getSubmitButton(Label result, TextField cookieInputField, ComboBox<String> apiInputField, TextField questionsApiInputField, Map<String, String> apiMap) {
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            result.setText("Generoidaan PDF-tiedostoa, odota hetki...");
            System.out.println("Generating...");

            String cookieInput = cookieInputField.getText();
            String selectedName = apiInputField.getValue();
            if (selectedName == null) {
                result.setText("Valitse API URL.");
                return;
            }
            String contentApiInput = apiMap.get(selectedName);
            if (contentApiInput == null) {
                result.setText("Valittu API ei löydy.");
                return;
            }
            try {
                String fileName = JsonService.generatePdf(cookieInput, contentApiInput, languageCode);
                result.setText("PDF valmis: " + fileName);
            } catch (Exception ex) {
                result.setText("Error occurred.");
                throw new RuntimeException(ex);
            }
        });
        return submitButton;
    }

    public static void main(String[] args) {
        launch(args);
    }
}

package com.example.pdffetcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class HelloApplication extends Application {
    private static Integer languageCode = 1035;

    @Override
    public void start(Stage primaryStage) {
        Label welcomeLabel = new Label("PDF-generaattori");
        Label result = new Label("Odotetaan syötteitä...");

        TextField cookieInputField = new TextField();
        TextField contentApiInputField = new TextField();
        TextField questionsApiInputField = new TextField();
        questionsApiInputField.setVisible(false);

        cookieInputField.setPromptText("Syötä cookie");
        contentApiInputField.setPromptText("Syötä API url");

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

        Button submitButton = getSubmitButton(result, cookieInputField, contentApiInputField, questionsApiInputField);

        VBox vbox = new VBox(20);
        vbox.getChildren().addAll(welcomeLabel, nextPageButton, contentApiInputField, cookieInputField, finnishChoice, swedishChoice, questionsApiInputField, submitButton, result);

        Scene scene = new Scene(vbox, 450, 450);

        primaryStage.setTitle("PDF Fetcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Button getSubmitButton(Label result, TextField cookieInputField, TextField apiInputField, TextField questionsApiInputField) {
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            result.setText("Generoidaan PDF-tiedostoa, odota hetki...");
            System.out.println("Generating...");

            String cookieInput = cookieInputField.getText();
            String contentApiInput = apiInputField.getText();
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

package com.example.pdffetcher;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicReference;

public class HelloApplication extends Application {
    private static final AtomicReference<Boolean> hasVideo = new AtomicReference<>(false);
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
        questionsApiInputField.setPromptText("Syödä kyselyn API url");

        CheckBox hasQuestionsCheckbox = new CheckBox("Sisältää kysymyksiä");
        CheckBox hasVideoCheckbox = new CheckBox("Sisältää videon");

        hasQuestionsCheckbox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            questionsApiInputField.setVisible(isSelected);
            questionsApiInputField.clear();
        });

        hasVideoCheckbox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            hasVideo.set(isSelected);
        });

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

                    API url:
                    1. Avaa selaimesi ja kirjaudu Omapolkuun potilaana.
                    2. Valitse haluamasi nettiterapia.
                    3. Paina hiiren oikeaa näppäintä ja valitse 'inspect element' tai 'tarkista'.
                    4. Valitse ruudulle ilmestyneen osion yläpalkista 'network'.
                    5. Näet ruudun yläosassa vaihtoehtoja, kuten 'doc', 'CSS', 'font', 'JS' ja muuta. Valitse 'Fetch/XHR'.
                    6. Valitse ruudun vasemmalla puolella näkyvästä sivustosta haluamasi nettiterapian osa.
                    7. Valitse name-osiosta se jonka viisi viimeistä ovat '3ea75'
                    8. Oikealle ilmestyneen osion yläpalkista valitse 'headers'.
                    9. Kopioi 'general' osion ylin url-osoite ja liitä se sovelluksen 'API url' kenttään. Url voi olla mm. nimellä 'request url'
                    tai ihan vain 'url'. Url on muotoa 'https://omapolku.terveyskyla.fi/api/treatmentfeed/getusertreatmenttask/'.

                    Cookie:
                    1. Samasta paikasta mihin jäit API url:n hakemisessa, scrollaa oikeata osiota alas kunnes näet osion 'request headers'.
                    2. Kopioi koko 'cookie'. Cookien massiivisesta koosta huolimatta liitä koko cookie sovelluksen 'cookie' kenttään.

                    Kyselyn API url:
                    1. Valitse Omapolusta osio, joka sisältää kyselyn. Jos olet jo vastannut kyselyyn, valitse 'siirry kyselyyn'.
                    2. Aiemmasta 'name' osiosta valitse alhaalta ensimmäinen viiden numeron sarja.
                    3. Kopioi sen 'request url' ja liitä se sovelluksen kenttään. Url on muotoa
                    'https://omapolku.terveyskyla.fi/api/questionnaires/getquestionnaire/'.

                    Huomioithan, että et voi kerralla tehdä useammasta sivusta PDF-tiedostoa. Ne on tehtävä yksitellen, ja
                    jokaiselle sivulle ja kyselylle on haettava sen oma 'request url'. Cookie voi pysyä samana.
                    Jos saat virheen generoinnissa, varmista että:
                    - Url-osoitteista ei puutu kirjaimia tai etteivät ne sisällä ylimääräisiä kirjaimia.
                    - Cookiesta ei puutu mitään tai siinä ei ole mitään ylimääräistä. Voit varmuuden vuoksi hakea arvot uudestaan.
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
        vbox.getChildren().addAll(welcomeLabel, nextPageButton, contentApiInputField, cookieInputField, finnishChoice, swedishChoice, hasVideoCheckbox, hasQuestionsCheckbox, questionsApiInputField, submitButton, result);

        Scene scene = new Scene(vbox, 450, 450);

        primaryStage.setTitle("PDF Fetcher");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private static Button getSubmitButton(Label result, TextField cookieInputField, TextField apiInputField, TextField questionsApiInputField) {
        Button submitButton = new Button("Submit");
        submitButton.setOnAction(e -> {
            String cookieInput = cookieInputField.getText();
            String contentApiInput = apiInputField.getText();
            String questionsApiInput = questionsApiInputField.getText();

            result.setText("Generoidaan PDF-tiedostoa...");
            try {
                String fileName = JsonService.generatePdf(cookieInput, contentApiInput, questionsApiInput, hasVideo.get(), languageCode);
                result.setText("PDF luotu: " + fileName);
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

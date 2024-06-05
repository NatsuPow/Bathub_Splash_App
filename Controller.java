package fr.ul.miage.hamouta;

import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import javafx.scene.shape.StrokeType;

import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {
    @FXML
    public Button buttonExporter;
    @FXML
    private Baignoire baignoire;
    @FXML
    private Button buttonDemarrer;
    @FXML
    private Rectangle rectangleLarge;
    @FXML
    private TextArea textAreaMain;
    @FXML
    private TextArea textAreaDebitInitial;
    @FXML
    private TextArea textAreaDebitFuites;
    @FXML
    private ChoiceBox<Integer> comboBoxNbRobinets;

    @FXML
    private ChoiceBox<Integer> comboBoxNbFuites;
    @FXML
    private List<Rectangle> fuitesRectangles = new ArrayList<>();

    private List<Robinet> robinets = new ArrayList<>();
    private List<Fuite> fuites = new ArrayList<>();
    private Map<TextArea, Robinet> robinetMap = new HashMap<>(); // Map pour stocker les objets Robinet

    private Map<Fuite, Button> fuiteButtonMap = new HashMap<>();

    @FXML
    private HBox hboxFuites;

    @FXML
    private LineChart<Number, Number> lineChart;
    private XYChart.Series<Number, Number> series;

    @FXML
    private VBox vboxDebitsRobinet;

    @FXML
    private VBox vboxBoutonsReparer;

    @FXML
    private TextArea textAreaCapacite;

    @FXML
    private HBox hboxRectangles;

    /**
     * Méthode pour initialiser et contrôler l'application.
     */
    @FXML
    public void initialize() {
        textAreaDebitInitial.setText("1.0");
        textAreaDebitFuites.setText("0.0");
        textAreaCapacite.setText("120.0");

        comboBoxNbRobinets.getItems().addAll(1, 2, 3, 4, 5);
        comboBoxNbRobinets.setValue(1);

        comboBoxNbFuites.getItems().addAll(0,1, 2, 3, 4, 5);
        comboBoxNbFuites.setValue(0);

        // Configuration du LineChart
        lineChart.setTitle("Remplissage de la baignoire");
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(false);
        lineChart.setLegendVisible(false);

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Temps (s)");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Pourcentage de remplissage");
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(Double.valueOf(textAreaCapacite.getText()));
        yAxis.setTickUnit(10);

        series = new XYChart.Series<>();
        lineChart.getData().add(series);
    }

    /**
     * Méthode pour démarrer la simulation.
     */
    @FXML
    public void action_demarrer() {
        Instant top = Instant.now();
        if (buttonDemarrer != null) {
            buttonDemarrer.setDisable(true);
        }
        rectangleLarge.setHeight(0);
        rectangleLarge.setTranslateY(150);
        String capaciteText = textAreaCapacite.getText();
        baignoire = new Baignoire(Double.parseDouble(capaciteText));

        int nbRobinets = comboBoxNbRobinets.getValue();

        for (int i = 0; i < nbRobinets; i++) {
            TextArea textAreaDebitRobinet = getTextAreaById("debitRobinet" + (i + 1));
            double debitRobinet = Double.parseDouble(textAreaDebitRobinet.getText());
            Robinet robinet = new Robinet(baignoire, debitRobinet);

            double period = calculerPeriodePourDebit(debitRobinet);
            robinet.setPeriod(Duration.millis(period));

            robinet.setOnSucceeded((WorkerStateEvent e) -> {
                double newHeight = baignoire.getVolume();
                double translateY = rectangleLarge.getY() + rectangleLarge.getHeight() - newHeight;
                rectangleLarge.setHeight(baignoire.getVolume());
                rectangleLarge.setTranslateY(translateY);

                if (baignoire.estPlein()) {
                    java.time.Duration duration = java.time.Duration.between(top, Instant.now());
                    robinet.cancel();

                    if (buttonDemarrer != null) {
                        buttonDemarrer.setDisable(false);
                    }
                }
                // Ajouter un point au graphique
                long tempsEcoule = java.time.Duration.between(top, Instant.now()).toSeconds();
                double pourcentageRemplissage = (baignoire.getVolume() / baignoire.getCapacite()) * 100;
                series.getData().add(new XYChart.Data<>(tempsEcoule, pourcentageRemplissage));
            });

            robinet.start();
            robinets.add(robinet);
            robinetMap.put(textAreaDebitRobinet, robinet); // Ajouter une entrée dans la Map
        }

        int nbFuites = comboBoxNbFuites.getValue();
        double debitFuites = Double.parseDouble(textAreaDebitFuites.getText());

        for (int i = 0; i < nbFuites; i++) {
            Fuite fuite = new Fuite(baignoire, debitFuites);

            double period = calculerPeriodePourDebit(debitFuites);
            fuite.setPeriod(Duration.millis(period));

            fuite.start();
            fuites.add(fuite);
        }

        // Réinitialiser la série à chaque nouvelle simulation
        series.getData().clear();
    }

    /**
     * Méthode pour calculer la période d'exécution en fonction du débit.
     *
     * @param debit le débit
     * @return la période d'exécution
     */
    private double calculerPeriodePourDebit(double debit) {
        if (debit <= 0) {
            return 180.0/0.001;
        } else {
            return 180.0 / debit;
        }
    }

    /**
     * Méthode pour démarrer la simulation.
     * Elle utilise {@code buttonDemarrer} pour désactiver le bouton pendant la simulation.
     */
    @FXML
    public void action_reparerFuite() {
        if (!fuites.isEmpty()) {
            Fuite fuite = fuites.remove(0);
            fuite.cancel();
            textAreaMain.appendText("Fuite réparée.\n");
        } else {
            textAreaMain.appendText("Pas de fuites à réparer.\n");
        }
    }

    /**
     * Méthode pour gérer le changement du nombre de robinets.
     *
     * @param actionEvent l'événement : choisir le nombre de robinets dans la choicebox correspondante
     */
    @FXML
    public void onNombreRobinetsChange(javafx.event.ActionEvent actionEvent) {
        if (vboxDebitsRobinet != null) {
            vboxDebitsRobinet.getChildren().clear(); // Nettoyer les anciens champs texte
            hboxRectangles.getChildren().clear(); // Nettoyer les anciens rectangles

            int nbRobinets = comboBoxNbRobinets.getValue();
            String debitInitialValue = textAreaDebitInitial.getText(); // Récupérer la valeur du débit initial

            // Vérifier si le texte de textAreaDebitInitial n'est pas vide
            if (!debitInitialValue.isEmpty()) {
                double debitInitial = Double.parseDouble(debitInitialValue); // Convertir en double
                // Liste pour stocker les textArea
                List<TextArea> textAreas = new ArrayList<>();

                for (int i = 1; i <= nbRobinets; i++) {
                    // Création des boutons "+" et "-"
                    Button buttonMinus = new Button("-");
                    Button buttonPlus = new Button("+");

                    // Création du TextArea pour le débit du robinet
                    TextArea textAreaDebit = new TextArea();
                    textAreaDebit.setPromptText("Débit du robinet " + i);
                    textAreaDebit.setId("debitRobinet" + i); // Définir l'ID du TextArea
                    textAreaDebit.setText(debitInitialValue); // Assigner la valeur du débit initial au TextArea
                    textAreaDebit.setPrefHeight(20); // Définir une hauteur préférée pour le TextArea

                    // Ajouter un écouteur sur le texte saisi dans le TextArea
                    textAreaDebit.textProperty().addListener((observable, oldValue, newValue) -> {
                        try {
                            double debit = Double.parseDouble(newValue);
                            // Récupérer le robinet correspondant à partir de la Map
                            Robinet robinetToUpdate = robinetMap.get(textAreaDebit);
                            if (robinetToUpdate != null) { // Vérification null ajoutée
                                // Mettre à jour le débit et la période d'exécution du robinet
                                robinetToUpdate.setDebit(debit);
                                robinetToUpdate.setPeriod(Duration.millis(calculerPeriodePourDebit(debit)));

                                // Créer et démarrer un nouveau robinet si le débit est supérieur à zéro et que le robinet actuel est arrêté
                                if (debit > 0 && robinetToUpdate.getState() == Worker.State.SUCCEEDED) {
                                    Robinet newRobinet = new Robinet(baignoire, debit);
                                    newRobinet.setPeriod(Duration.millis(calculerPeriodePourDebit(debit)));
                                    newRobinet.start();
                                    robinetMap.put(textAreaDebit, newRobinet); // Mettre à jour l'entrée dans la Map
                                }
                            }
                        } catch (NumberFormatException e) {
                            // Gérer le cas où le texte saisi n'est pas un nombre valide
                            textAreaDebit.setText(oldValue);
                            System.err.println("Erreur : La valeur du débit de robinet n'est pas un nombre valide.");
                        }
                    });

                    // Action du bouton "+" : incrémenter le débit
                    buttonPlus.setOnAction(event -> {
                        double debit = Double.parseDouble(textAreaDebit.getText());
                        textAreaDebit.setText(String.valueOf(debit + 0.5)); // Incrémenter le débit de 0.5

                        // Mettre à jour le débit et la période d'exécution du robinet correspondant
                        Robinet robinetToUpdate = robinetMap.get(textAreaDebit);
                        if (robinetToUpdate != null) { // Vérification null ajoutée
                            robinetToUpdate.setDebit(debit + 0.5);
                            robinetToUpdate.setPeriod(Duration.millis(calculerPeriodePourDebit(debit + 0.5)));

                            // Créer et démarrer un nouveau robinet si le débit est supérieur à zéro et que le robinet actuel est arrêté
                            if ((debit + 0.5) > 0 && robinetToUpdate.getState() == Worker.State.SUCCEEDED) {
                                Robinet newRobinet = new Robinet(baignoire, debit + 0.5);
                                newRobinet.setPeriod(Duration.millis(calculerPeriodePourDebit(debit + 0.5)));
                                newRobinet.start();
                                robinetMap.put(textAreaDebit, newRobinet); // Mettre à jour l'entrée dans la Map
                            }
                        }
                    });

                    // Action du bouton "-" : décrémenter le débit
                    buttonMinus.setOnAction(event -> {
                        double debit = Double.parseDouble(textAreaDebit.getText());
                        if (debit >= 0.5) {
                            textAreaDebit.setText(String.valueOf(debit - 0.5)); // Décrémenter le débit de 0.5

                            // Mettre à jour le débit et la période d'exécution du robinet correspondant
                            Robinet robinetToUpdate = robinetMap.get(textAreaDebit);
                            if (robinetToUpdate != null) { // Vérification null ajoutée
                                robinetToUpdate.setDebit(debit - 0.5);
                                robinetToUpdate.setPeriod(Duration.millis(calculerPeriodePourDebit(debit - 0.5)));

                                // Créer et démarrer un nouveau robinet si le débit est supérieur à zéro et que le robinet actuel est arrêté
                                if ((debit - 0.5) > 0 && robinetToUpdate.getState() == Worker.State.SUCCEEDED) {
                                    Robinet newRobinet = new Robinet(baignoire, debit - 0.5);
                                    newRobinet.setPeriod(Duration.millis(calculerPeriodePourDebit(debit - 0.5)));
                                    newRobinet.start();
                                    robinetMap.put(textAreaDebit, newRobinet); // Mettre à jour l'entrée dans la Map
                                }
                            }
                        }
                    });

                    // Création d'un conteneur pour les boutons
                    HBox buttonsBox = new HBox(buttonMinus, buttonPlus);
                    // Alignement des boutons à gauche et à droite
                    buttonsBox.setSpacing(5);
                    buttonsBox.setStyle("-fx-alignment: center-right;");

                    // Création d'un conteneur pour le TextArea et les boutons
                    HBox robinetBox = new HBox(textAreaDebit, buttonsBox);
                    robinetBox.setSpacing(5);

                    // Ajout du TextArea et des boutons à un VBox
                    vboxDebitsRobinet.getChildren().add(robinetBox); // Ajout de la ligne à la VBox parente

                    // Ajout d'un rectangleSmall à côté du dernier déjà présent
                    Rectangle dernierRectangle = new Rectangle(19, 18, Color.DODGERBLUE);
                    dernierRectangle.setTranslateY(20); // Même translation Y que la baignoire
                    hboxRectangles.getChildren().add(dernierRectangle);

                    // Ajouter un espace entre les rectangles
                    hboxRectangles.setSpacing(10);

                }
            } else {
                // Gérer le cas où le texte de textAreaDebitInitial est vide
                System.err.println("Erreur : La valeur du débit initial est vide.");
            }
        } else {
            System.err.println("Erreur : vboxDebitsRobinet est null.");
        }
    }

    /**
     * Méthode pour récupérer un TextArea par son ID.
     *
     * @param id l'ID du TextArea
     * @return le TextArea correspondant à l'ID, ou null si aucun TextArea ne correspond
     */
    //fonction nécessaire pour ajouter le controller
    public TextArea getTextAreaById(String id) {
        for (Node node : vboxDebitsRobinet.getChildren()) {
            if (node instanceof HBox) {
                for (Node childNode : ((HBox) node).getChildren()) {
                    if (childNode instanceof TextArea && childNode.getId().equals(id)) {
                        return (TextArea) childNode;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Méthode pour gérer le changement du nombre de fuites.
     *
     * @param actionEvent l'événement : changer le nombre de fuites dans la choicebox
     */
    @FXML
    public void onNombreFuitesChange(javafx.event.ActionEvent actionEvent) {
        vboxBoutonsReparer.getChildren().clear();

        for (Rectangle fuiteRectangle : fuitesRectangles) {
            hboxFuites.getChildren().remove(fuiteRectangle);
        }
        fuitesRectangles.clear();

        int nbFuites = comboBoxNbFuites.getValue();
        Map<Button, Integer> boutonIndexMap = new HashMap<>();

        // Vérifier que le nombre de fuites est supérieur à zéro
        if (nbFuites > 0) {
            for (int i = 0; i < nbFuites; i++) {
                final int index = i; // pour que l'index soit accessible dans la lambda

                // Créer un petit rectangle noir pour représenter la fuite sur la baignoire
                Rectangle rectangleFuite = new Rectangle(10, 10);
                rectangleFuite.setArcHeight(5);
                rectangleFuite.setArcWidth(5);
                rectangleFuite.setFill(Color.BLACK);
                rectangleFuite.setStroke(Color.BLACK);
                rectangleFuite.setStrokeType(StrokeType.INSIDE);

                // Ajustez la position des rectangles de fuite en fonction du nombre de fuites et de l'espace entre elles
                rectangleFuite.setTranslateX((20 * i) + (10 * (i - 1)));

                // Ajouter le rectangle de fuite à la liste et à la HBox existante avec l'ID hboxFuites
                fuitesRectangles.add(rectangleFuite);
                hboxFuites.getChildren().add(1, rectangleFuite);

                // Créer un bouton de réparation pour la fuite correspondante
                Button boutonReparer = new Button("Réparer la fuite " + (i + 1));
                boutonIndexMap.put(boutonReparer, index); // Ajouter le bouton de réparation et son index correspondant à la Map
                boutonReparer.setOnAction(event -> {
                    // Vérifier que l'index est toujours valide
                    if (index < fuites.size()) {
                        // Arrêter la fuite correspondante
                        Fuite fuite = fuites.get(index);
                        fuite.cancel();
                        fuites.remove(index);

                        // Supprimer le rectangle noir correspondant à la fuite réparée
                        int indexToRepair = boutonIndexMap.get(boutonReparer);
                        if (indexToRepair < fuitesRectangles.size()) {
                            Rectangle fuiteRectangleToRemove = fuitesRectangles.get(indexToRepair);
                            hboxFuites.getChildren().remove(fuiteRectangleToRemove);
                            fuitesRectangles.remove(indexToRepair);
                        }

                        // Mettre à jour l'espacement entre les rectangles de fuite restants
                        for (int j = 0; j < fuitesRectangles.size(); j++) {
                            Rectangle fuiteRectangle = fuitesRectangles.get(j);
                            fuiteRectangle.setTranslateX((20 * j) + (10 * (j - 1)));
                        }

                        double debitTotalFuites = fuites.stream()
                                .mapToDouble(Fuite::getDebit)
                                .sum();
                        textAreaDebitFuites.setText(String.valueOf(debitTotalFuites));

                        vboxBoutonsReparer.getChildren().remove(boutonReparer);
                        boutonIndexMap.remove(boutonReparer);
                    }
                });
                vboxBoutonsReparer.getChildren().add(boutonReparer);
            }
        }
    }


    /**
     * Méthode pour exporter les données du graphique.
     *
     * @param event l'événement : cliquer sur le bouton exporter pour exporter les données de remplissage de la baignoire au fichier CSV
     */
    @FXML
    public void action_exporter(ActionEvent event) {
        // Récupérer les données du LineChart
        ObservableList<XYChart.Data<Number, Number>> data = lineChart.getData().get(0).getData();

        StringBuilder csvBuilder = new StringBuilder();

        csvBuilder.append("Temps (s),Niveau d'eau (L)\n");

        for (XYChart.Data<Number, Number> datum : data) {
            csvBuilder.append(datum.getXValue()).append(",");
            csvBuilder.append(datum.getYValue()).append("\n");
        }

        // Écrire le fichier CSV dans le répertoire data du projet
        try {
            Path dataPath = Paths.get("src/data");
            Files.createDirectories(dataPath);
            Files.writeString(dataPath.resolve("remplissage.csv"), csvBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

package fr.ul.miage.hamouta;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * La classe principale de l'application.
 */
public class App extends Application {
    private static Scene scene;

    /**
     * Lance l'application.
     *
     * @param args les arguments de ligne de commande
     */
    public static void main(String[] args) {
        launch();
    }

    /**
     * Démarre l'application.
     *
     * @param stage la scène principale de l'application
     * @throws IOException en cas d'erreur lors du chargement de l'interface utilisateur
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/gui.fxml"));
        scene = new Scene(fxmlLoader.load());
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Change la racine de la scène avec le fichier FXML spécifié.
     *
     * @param fxml le nom du fichier FXML
     * @throws IOException en cas d'erreur lors du chargement du fichier FXML
     */
    static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    /**
     * Charge le fichier FXML spécifié.
     *
     * @param fxml le nom du fichier FXML
     * @return la racine du fichier FXML chargé
     * @throws IOException en cas d'erreur lors du chargement du fichier FXML
     */
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }
}

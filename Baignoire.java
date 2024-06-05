package fr.ul.miage.hamouta;

import java.util.logging.Logger;

/**
 * La classe Baignoire représente une baignoire avec une capacité maximale et un volume actuel.
 */
public class Baignoire {
    private static final Logger LOG = Logger.getLogger(Baignoire.class.getName());
    public static final double MAX = 50.0;
    private double capacite;
    private double volume;

    /**
     * Constructeur de la classe Baignoire avec une capacité spécifiée.
     *
     * @param capacite la capacité maximale de la baignoire
     */
    public Baignoire(double capacite) {
        this.capacite = capacite;
        this.volume = 0.0;
    }

    /**
     * Constructeur de la classe Baignoire avec une capacité maximale par défaut.
     */
    public Baignoire() {
        this(MAX);
    }

    /**
     * Vérifie si la baignoire est pleine.
     *
     * @return true si la baignoire est pleine, sinon false
     */
    public boolean estPlein() {
        return (capacite <= volume);
    }

    /**
     * Obtient la capacité maximale de la baignoire.
     *
     * @return la capacité maximale de la baignoire
     */
    public double getCapacite() {
        return capacite;
    }

    /**
     * Obtient le volume actuel de la baignoire.
     *
     * @return le volume actuel de la baignoire
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Définit le volume actuel de la baignoire.
     *
     * @param volume le nouveau volume actuel de la baignoire
     */
    public void setVolume(double volume) {
        this.volume = volume;
    }

    /**
     * Simule l'écoulement de l'eau dans la baignoire lorsqu'elle est remplie.
     *
     * @return le nouveau volume après l'écoulement d'eau
     */
    public double glou() {
        volume += 1.0;
        if (volume >= capacite) {
            volume = capacite;
        }
        return volume;
    }

    /**
     * Simule une fuite d'eau de la baignoire.
     *
     * @param debitFuite le débit de la fuite
     * @return le nouveau volume après la fuite d'eau
     */
    public double fuite(double debitFuite) {
        volume -= debitFuite;
        if (volume < 0) {
            volume = 0;
        }
        return volume;
    }
}

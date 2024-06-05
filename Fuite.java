package fr.ul.miage.hamouta;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * La classe pour les robinets.
 * Elle utilise {@link Baignoire#fuite(double)} pour simuler une fuite.
 */
public class Fuite extends ScheduledService<Void> {

    private Baignoire baignoire;
    private double debit;
    private final Lock lock = new ReentrantLock();

    /**
     * Constructeur de la classe Fuite.
     *
     * @param baignoire la baignoire concernée par la fuite
     * @param debit     le débit de la fuite
     */
    public Fuite(Baignoire baignoire, double debit) {
        super();
        this.baignoire = baignoire;
        this.debit = debit;
    }

    /**
     * Crée une tâche pour la fuite.
     *
     * @return la tâche pour la fuite
     */
    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                lock.lock();
                try {
                    baignoire.fuite(debit);
                } finally {
                    lock.unlock();
                }
                return null;
            }
        };
    }

    /**
     * Obtient le débit de la fuite.
     *
     * @return le débit de la fuite
     */
    public double getDebit() {
        return this.debit;
    }
}

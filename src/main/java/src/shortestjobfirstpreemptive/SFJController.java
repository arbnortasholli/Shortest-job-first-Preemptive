package src.shortestjobfirstpreemptive;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.*;
import java.util.concurrent.*;


public class SFJController {
    // Label per te shfaqur mesataren e turnaround time
    @FXML private Label avgTurnaroundLabel;
    // Label per te shfaqur mesataren e waiting time
    @FXML private Label avgWaitingLabel;
    // Label per te shfaqur mesataren e response time
    @FXML private Label avgResponseLabel;
    // Input per futjen e burst time te nje procesi
    @FXML private TextField burstInput;
    // Butoni per te shtuar nje proces te ri
    @FXML private Button addButton;


    // Label qe tregon procesin qe po ekzekutohet aktualisht
    @FXML private Label currentProcessLabel;
    // Tabela qe shfaq proceset ne pritje (ready queue)
    @FXML private TableView<Process> processTable;
    @FXML private TableColumn<Process, Integer> idCol;
    @FXML private TableColumn<Process, Integer> burstCol;
    @FXML private TableColumn<Process, Integer> remainingCol;
    // Tabela qe shfaq proceset e perfunduara
    @FXML private TableView<Process> completedTable;
    @FXML private TableColumn<Process, Integer> completedIdCol;
    @FXML private TableColumn<Process, Integer> completedBurstCol;
    @FXML private TableColumn<Process, Integer> waitingTimeCol;
    @FXML private TableColumn<Process, Integer> responseTimeCol;
    @FXML private TableColumn<Process, Integer> turnaroundTimeCol;
    // Lista qe perfaqeson proceset ne gatishmeri
    private final ObservableList<Process> readyQueue = FXCollections.observableArrayList();
    // Lista qe permban proceset e perfunduara
    private final ObservableList<Process> completedList = FXCollections.observableArrayList();
    // PriorityQueue per te zgjedhur procesin me kohen me te vogel te mbetur
    private final PriorityQueue<Process> pq = new PriorityQueue<>(Comparator.comparingInt(p -> p.remainingTime));
    // Ekzekutues qe do te pershkruaje "tik-takun" e sistemit
    private ScheduledExecutorService scheduler;
    // Koha aktuale e sistemit qe rritet çdo sekonde
    private int systemTime = 0;


    // Inicializimi i komponenteve te GUI dhe nisja e mekanizmit te kohes
    @FXML
    public void initialize() {
        // Lidhja e kolonave te tabeles se proceseve me fushat e klases Process
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        remainingCol.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));
        // Lidhja e kolonave te tabeles se proceseve te perfunduara
        completedIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        completedBurstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        waitingTimeCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));
        responseTimeCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));
        turnaroundTimeCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));
        // Vendosja e listave ne tabelat perkatese
        processTable.setItems(readyQueue);
        completedTable.setItems(completedList);
        // Krijimi i nje executor qe do te therrase metoden tick çdo sekonde
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }


    // Metode qe ekzekutohet kur shtypet butoni per te shtuar proces
    @FXML
    private void onAddProcess() {
        try {
            // Leximi i burst time nga input-i
            int burstTime = Integer.parseInt(burstInput.getText());
            if (burstTime <= 0) throw new NumberFormatException();


            // Krijimi i procesit te ri me kohen aktuale si arrivalTime
            Process p = new Process(burstTime, systemTime);


            // Shtimi i procesit ne strukturat e te dhenave dhe perditesimi i GUI-se
            Platform.runLater(() -> {
                pq.add(p);
                readyQueue.add(p);
                burstInput.clear(); // Pastrimi i input-it
            });
        } catch (NumberFormatException e) {
            // Ne rast gabimi, shfaqet nje mesazh per perdoruesin
            showError("Please enter a valid burst time > 0.");
        }
    }


    // Metode qe perfaqeson nje tik te sistemit (ekzekutohet çdo sekonde)
    private void tick() {
        systemTime++; // Rritja e kohes se sistemit


        Platform.runLater(() -> {
            // Marrja e procesit me kohen me te vogel te mbetur
            Process current = pq.peek();


            // Perditesimi i kohes se pritjes per te gjithe proceset ne pritje perveç atij qe po ekzekutohet
            for (Process p : pq) {
                if (p != current && p.arrivalTime <= systemTime && p.remainingTime > 0) {
                    p.waitingTime++;
                }
            }


            // Nese ka proces per t'u ekzekutuar
            if (current != null) {
                // Nese procesi po fillon per here te pare, vendos responseTime
                if (!current.started) {
                    current.responseTime = systemTime - current.arrivalTime - 1;
                    current.started = true;
                }


                // Ulja e kohes se mbetur per procesin aktual
                current.remainingTime--;
                processTable.refresh(); // Perditesimi i tabeles vizualisht
                currentProcessLabel.setText("Running Process: P" + current.id);


                // Nese procesi perfundon
                if (current.remainingTime == 0) {
                    pq.poll(); // Largohet nga queue
                    current.completionTime = systemTime;
                    current.turnaroundTime = current.completionTime - current.arrivalTime;


                    // Hiqet nga lista e proceseve ne gatishmeri dhe shtohet tek e perfunduarat
                    readyQueue.remove(current);
                    completedList.add(current);


                    currentProcessLabel.setText("Running Process: None");
                    updateAverages(); // Perditesimi i statistikave
                }
            } else {
                // Nese nuk ka proces per t'u ekzekutuar
                currentProcessLabel.setText("Running Process: None");
            }
        });
    }


    // Shfaqje e mesazhit te gabimit me Alert ne JavaFX
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }


    // Llogaritja dhe perditesimi i statistikave mesatare
    private void updateAverages() {
        int n = completedList.size();
        if (n == 0) {
            avgTurnaroundLabel.setText("0");
            avgWaitingLabel.setText("0");
            avgResponseLabel.setText("0");
            return;
        }


        double totalTurnaround = 0;
        double totalWaiting = 0;
        double totalResponse = 0;


        for (Process p : completedList) {
            totalTurnaround += p.turnaroundTime;
            totalWaiting += p.waitingTime;
            totalResponse += p.responseTime;
        }


        // Vendosja e mesatareve ne etiketat perkatese
        avgTurnaroundLabel.setText(String.format("%.2f", totalTurnaround / n));
        avgWaitingLabel.setText(String.format("%.2f", totalWaiting / n));
        avgResponseLabel.setText(String.format("%.2f", totalResponse / n));
    }


    // Klasa e brendshme qe perfaqeson nje proces
    public static class Process {
        private static int idCounter = 1; // Perdoret per te dhene ID unike per çdo proces
        public int id;
        public int burstTime;
        public int remainingTime;
        public int arrivalTime;
        public int responseTime = -1;
        public int completionTime;
        public int turnaroundTime;
        public int waitingTime;
        public boolean started = false; // Tregon nese procesi ka filluar ndonjehere


        // Konstruktori per nje proces te ri
        public Process(int burstTime, int arrivalTime) {
            this.id = idCounter++;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
            this.arrivalTime = arrivalTime;
        }


        // Getters te nevojshem per tabelat JavaFX
        public int getId() { return id; }
        public int getBurstTime() { return burstTime; }
        public int getRemainingTime() { return remainingTime; }
        public int getWaitingTime() { return waitingTime; }
        public int getResponseTime() { return responseTime; }
        public int getTurnaroundTime() { return turnaroundTime; }
    }
}

// Paketa dhe importet e nevojshme për JavaFX, koleksione dhe ekzekutim periodik
package com.example.shortestjobfirstpreemptive;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.concurrent.*;

public class SFJControllerAutomatic {

    // Etiketa për mesataret e kohëve dhe për procesin aktual
    @FXML private Label avgTurnaroundLabel;
    @FXML private Label avgWaitingLabel;
    @FXML private Label avgResponseLabel;
    @FXML private Label currentProcessLabel;

    // Tabela për proceset në ekzekutim (Ready Queue)
    @FXML private TableView<Process> processTable;
    @FXML private TableColumn<Process, Integer> idCol;
    @FXML private TableColumn<Process, Integer> burstCol;
    @FXML private TableColumn<Process, Integer> remainingCol;

    // Tabela për proceset e përfunduara
    @FXML private TableView<Process> completedTable;
    @FXML private TableColumn<Process, Integer> completedIdCol;
    @FXML private TableColumn<Process, Integer> completedBurstCol;
    @FXML private TableColumn<Process, Integer> waitingTimeCol;
    @FXML private TableColumn<Process, Integer> responseTimeCol;
    @FXML private TableColumn<Process, Integer> turnaroundTimeCol;
    @FXML private TableColumn<Process, Integer> arrivalTimeCol;

    // Fusha tekstuale për inputin e përdoruesit (arrival dhe burst time për deri në 5 procese)
    @FXML private TextField arrivalInput1, burstInput1;
    @FXML private TextField arrivalInput2, burstInput2;
    @FXML private TextField arrivalInput3, burstInput3;
    @FXML private TextField arrivalInput4, burstInput4;
    @FXML private TextField arrivalInput5, burstInput5;

    // Butoni për të nisur simulimin
    @FXML private Button startButton;

    // Ruajtja e proceseve të futura automatikisht me arrival time unik
    private final Map<Integer, Integer> autoProcesses = new HashMap<>();

    // Metodë që ekzekutohet kur klikohet butoni Start
    @FXML
    private void onStartSimulation() {
        autoProcesses.clear();

        try {
            // Merr të dhënat nga inputet dhe shton proceset
            addProcessFromInputs(arrivalInput1, burstInput1);
            addProcessFromInputs(arrivalInput2, burstInput2);
            addProcessFromInputs(arrivalInput3, burstInput3);
            addProcessFromInputs(arrivalInput4, burstInput4);
            addProcessFromInputs(arrivalInput5, burstInput5);
        } catch (NumberFormatException e) {
            showError("Please enter valid arrival and burst times. Arrival times must be unique, >= 0, and burst times must be > 0.");
            return;
        }

        // Reseton kohën dhe listat për një simulim të ri
        systemTime = 0;
        pq.clear();
        readyQueue.clear();
        completedList.clear();
    }

    // Merr një çift arrival/burst nga inputet dhe e shton në hartën e proceseve automatike
    private void addProcessFromInputs(TextField arrivalInput, TextField burstInput) throws NumberFormatException {
        String arrivalText = arrivalInput.getText().trim();
        String burstText = burstInput.getText().trim();

        if (arrivalText.isEmpty() && burstText.isEmpty()) return;
        if (arrivalText.isEmpty() || burstText.isEmpty()) throw new NumberFormatException();

        int arrival = Integer.parseInt(arrivalText);
        int burst = Integer.parseInt(burstText);

        if (arrival < 0 || burst <= 0) throw new NumberFormatException();
        if (autoProcesses.containsKey(arrival)) throw new NumberFormatException("Arrival times must be unique.");

        autoProcesses.put(arrival, burst);
    }

    // Lista e proceseve në pritje (ready queue)
    private final ObservableList<Process> readyQueue = FXCollections.observableArrayList();
    // Lista e proceseve të përfunduara
    private final ObservableList<Process> completedList = FXCollections.observableArrayList();
    // Prioritet queue për të zgjedhur procesin me kohën më të shkurtër të mbetur
    private final PriorityQueue<Process> pq = new PriorityQueue<>(Comparator.comparingInt(p -> p.remainingTime));

    private ScheduledExecutorService scheduler; // Ekzekutues për çdo sekondë (simulimi në kohë reale)
    private int systemTime = 0; // Koha aktuale e sistemit

    // Inicializimi i komponentëve UI dhe fillimi i ekzekutimit të tick() çdo sekondë
    @FXML
    public void initialize() {
        // Lidh kolonat me fushat përkatëse të klasës Process
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        remainingCol.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));

        completedIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        completedBurstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        waitingTimeCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));
        responseTimeCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));
        turnaroundTimeCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        // Vendos të dhënat për tabelat
        processTable.setItems(readyQueue);
        completedTable.setItems(completedList);

        // Nis simulimin e kohës në mënyrë periodike çdo 1 sekondë
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    // Shton një proces në queue dhe në listën e paraqitjes
    private void addProcess(Process p) {
        pq.add(p);
        readyQueue.add(p);
    }

    // Ekzekutohet çdo sekondë për të simuluar kalimin e kohës dhe menaxhimin e proceseve
    private void tick() {
        systemTime++;

        // Kontrollon nëse ndonjë proces duhet të mbërrijë në këtë kohë
        if (autoProcesses.containsKey(systemTime - 1)) {
            Process p = new Process(autoProcesses.get(systemTime - 1), systemTime - 1);
            Platform.runLater(() -> addProcess(p));
        }

        Platform.runLater(() -> {
            Process current = pq.peek(); // Merr procesin me kohën më të shkurtër të mbetur
            if (current != null) {
                // Nëse nuk ka filluar ende, vendos kohën e përgjigjes
                if (!current.started) {
                    current.responseTime = systemTime - current.arrivalTime;
                    current.started = true;
                }
                // Zvogëlon kohën e mbetur për procesin aktual
                current.remainingTime--;
                processTable.refresh();
                currentProcessLabel.setText("Running Process: P" + current.id);

                // Nëse procesi ka përfunduar, e heq nga queue dhe e shton në listën e përfunduara
                if (current.remainingTime == 0) {
                    pq.poll();
                    current.completionTime = systemTime;
                    current.turnaroundTime = current.completionTime - current.arrivalTime;
                    current.waitingTime = current.turnaroundTime - current.burstTime;
                    if (current.waitingTime < 0) current.waitingTime = 0;

                    readyQueue.remove(current);
                    completedList.add(current);
                    currentProcessLabel.setText("Running Process: None");
                    updateAverages();
                }
            } else {
                currentProcessLabel.setText("Running Process: None");
            }
        });
    }

    // Tregon një dritare gabimi nëse inputi është i pavlefshëm
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Përditëson mesataret për turnaround, waiting dhe response time në UI
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

        avgTurnaroundLabel.setText(String.format("%.2f", totalTurnaround / n));
        avgWaitingLabel.setText(String.format("%.2f", totalWaiting / n));
        avgResponseLabel.setText(String.format("%.2f", totalResponse / n));
    }

    // Klasa e brendshme që përfaqëson një proces
    public static class Process {
        private static int idCounter = 1;

        public int id;
        public int burstTime;
        public int remainingTime;
        public int arrivalTime;
        public int responseTime = -1;
        public int completionTime;
        public int turnaroundTime;
        public int waitingTime;
        public boolean started = false;

        // Konstruktori për një proces të ri
        public Process(int burstTime, int arrivalTime) {
            this.id = idCounter++;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
            this.arrivalTime = arrivalTime;
        }

        // Metodat getter për t’u përdorur në tabelat JavaFX
        public int getId() { return id; }
        public int getBurstTime() { return burstTime; }
        public int getRemainingTime() { return remainingTime; }
        public int getWaitingTime() { return waitingTime; }
        public int getResponseTime() { return responseTime; }
        public int getTurnaroundTime() { return turnaroundTime; }
        public int getArrivalTime() { return arrivalTime; }
    }
}

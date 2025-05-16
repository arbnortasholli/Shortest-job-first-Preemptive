package com.example.shortestjobfirstpreemptive;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;
import java.util.concurrent.*;

public class SFJController {

    @FXML
    private Label avgTurnaroundLabel;

    @FXML
    private Label avgWaitingLabel;

    @FXML
    private Label avgResponseLabel;
    @FXML
    private TextField burstInput;
    @FXML
    private Button addButton;
    @FXML
    private Label currentProcessLabel;
    @FXML
    private TableView<Process> processTable;
    @FXML
    private TableColumn<Process, Integer> idCol;
    @FXML
    private TableColumn<Process, Integer> burstCol;
    @FXML
    private TableColumn<Process, Integer> remainingCol;

    @FXML
    private TableView<Process> completedTable;
    @FXML
    private TableColumn<Process, Integer> completedIdCol;
    @FXML
    private TableColumn<Process, Integer> completedBurstCol;
    @FXML
    private TableColumn<Process, Integer> waitingTimeCol;
    @FXML
    private TableColumn<Process, Integer> responseTimeCol;
    @FXML
    private TableColumn<Process, Integer> turnaroundTimeCol;

    private final ObservableList<Process> readyQueue = FXCollections.observableArrayList();
    private final ObservableList<Process> completedList = FXCollections.observableArrayList();
    private final PriorityQueue<Process> pq = new PriorityQueue<>(Comparator.comparingInt(p -> p.remainingTime));

    private ScheduledExecutorService scheduler;
    private int systemTime = 0;

    @FXML  // Ky annotation lidh metoden me komponentin FXML kur inicializohet GUI-ja
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id")); // Lidhet kolona 'idCol' me pronën 'id' të objektit Process
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime")); // Lidhet kolona 'burstCol' me pronën 'burstTime'
        remainingCol.setCellValueFactory(new PropertyValueFactory<>("remainingTime")); // Lidhet kolona 'remainingCol' me 'remainingTime'

        completedIdCol.setCellValueFactory(new PropertyValueFactory<>("id")); // Lidhet kolona për proceset e përfunduara me 'id'
        completedBurstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime")); // Lidhet me 'burstTime' për të përfunduarit
        waitingTimeCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime")); // Lidhet me 'waitingTime' për të përfunduarit
        responseTimeCol.setCellValueFactory(new PropertyValueFactory<>("responseTime")); // Lidhet me 'responseTime'
        turnaroundTimeCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime")); // Lidhet me 'turnaroundTime'

        processTable.setItems(readyQueue); // Vendos listen e proceseve të gatshme në tabelën 'processTable'
        completedTable.setItems(completedList); // Vendos listen e proceseve të përfunduara në tabelën 'completedTable'


        scheduler = Executors.newSingleThreadScheduledExecutor();  // Krijon një executor për të drejtuar periodikisht një task
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS); // Thërret metodën tick çdo sekondë për të simuluar kohën
    }

    @FXML // Lidh metodën me butonin për shtimin e një procesi të ri
    private void onAddProcess() {
        try {
            int burstTime = Integer.parseInt(burstInput.getText()); // Merr vlerën e burst time nga inputi dhe e konverton në int
            if (burstTime <= 0) throw new NumberFormatException(); // Kontrollon që burst time të jetë > 0, ndryshe hedh përjashtim
            Process p = new Process(burstTime, systemTime); // Krijon një objekt të ri Process me burst time dhe kohën aktuale
            Platform.runLater(() -> {  // Siguron që veprimet në GUI të ndodhin në thread-in e duhur (JavaFX UI Thread)
                pq.add(p); // Shton procesin në prioritet queue
                readyQueue.add(p); // Shton procesin në listen e paraqitur në tabelë
                burstInput.clear(); // Pastron input-in pasi shtohet procesi
            });
        } catch (NumberFormatException e) {
            showError("Please enter a valid burst time > 0."); // Në rast gabimi, shfaq mesazh për përdoruesin
        }
    }

    private void tick() { // Kjo metodë simolon 'tik-takun' e kohës, thirret çdo sekondë
        systemTime++;

        Platform.runLater(() -> { // Siguron që ndryshimet në GUI të ndodhin në thread-in e duhur
            SFJController.Process current = pq.peek(); // Merr procesin aktual që do të ekzekutohet (me burst më të vogël)

            for (Process p : pq) {
                if (p != current && p.arrivalTime <= systemTime && p.remainingTime > 0) { // Nëse nuk është procesi aktual, ka arritur dhe ka ende kohë për t'u ekzekutuar
                    p.waitingTime++; // Rrit kohën e pritjes për këtë proces
                }
            }

            if (current != null) { // Nëse ka një proces që po ekzekutohet
                if (!current.started) { // Nëse ky proces po fillon për herë të parë
                    current.responseTime = systemTime - current.arrivalTime;  // Llogarit response time
                    current.started = true; // E shënon si të filluar
                }
                current.remainingTime--;
                processTable.refresh();
                currentProcessLabel.setText("Running Process: P" + current.id);
                if (current.remainingTime == 0) {
                    pq.poll();
                    current.completionTime = systemTime;
                    current.turnaroundTime = current.completionTime - current.arrivalTime;

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


    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

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

        for (SFJController.Process p : completedList) {
            totalTurnaround += p.turnaroundTime;
            totalWaiting += p.waitingTime;
            totalResponse += p.responseTime;
        }

        avgTurnaroundLabel.setText(String.format("%.2f", totalTurnaround / n));
        avgWaitingLabel.setText(String.format("%.2f", totalWaiting / n));
        avgResponseLabel.setText(String.format("%.2f", totalResponse / n));
    }

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

        public Process(int burstTime, int arrivalTime) {
            this.id = idCounter++;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
            this.arrivalTime = arrivalTime;
        }

        public int getId() { return id; }
        public int getBurstTime() { return burstTime; }
        public int getRemainingTime() { return remainingTime; }
        public int getWaitingTime() { return waitingTime; }
        public int getResponseTime() { return responseTime; }
        public int getTurnaroundTime() { return turnaroundTime; }
    }
}
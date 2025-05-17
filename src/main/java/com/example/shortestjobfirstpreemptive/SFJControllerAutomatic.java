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

    @FXML
    private Label avgTurnaroundLabel;

    @FXML
    private Label avgWaitingLabel;

    @FXML
    private Label avgResponseLabel;

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
    @FXML
    private TableColumn<Process, Integer> arrivalTimeCol;

    @FXML
    private TextField arrivalInput1;
    @FXML
    private TextField burstInput1;
    @FXML
    private TextField arrivalInput2;
    @FXML
    private TextField burstInput2;
    @FXML
    private TextField arrivalInput3;
    @FXML
    private TextField burstInput3;
    @FXML
    private TextField arrivalInput4;
    @FXML
    private TextField burstInput4;
    @FXML
    private TextField arrivalInput5;
    @FXML
    private TextField burstInput5;

    @FXML
    private Button startButton;

    private final Map<Integer, Integer> autoProcesses = new HashMap<>();

    @FXML
    private void onStartSimulation() {
        autoProcesses.clear();

        try {
            addProcessFromInputs(arrivalInput1, burstInput1);
            addProcessFromInputs(arrivalInput2, burstInput2);
            addProcessFromInputs(arrivalInput3, burstInput3);
            addProcessFromInputs(arrivalInput4, burstInput4);
            addProcessFromInputs(arrivalInput5, burstInput5);
        } catch (NumberFormatException e) {
            showError("Please enter valid arrival and burst times. Arrival times must be unique, >= 0, and burst times must be > 0.");
            return;
        }

        systemTime = 0;
        pq.clear();
        readyQueue.clear();
        completedList.clear();

    }

    private void addProcessFromInputs(TextField arrivalInput, TextField burstInput) throws NumberFormatException {
        String arrivalText = arrivalInput.getText().trim();
        String burstText = burstInput.getText().trim();

        if (arrivalText.isEmpty() && burstText.isEmpty()) {
            return;
        }

        if (arrivalText.isEmpty() || burstText.isEmpty()) {
            throw new NumberFormatException("Arrival and burst time must both be provided or both empty.");
        }

        int arrival = Integer.parseInt(arrivalText);
        int burst = Integer.parseInt(burstText);

        if (arrival < 0 || burst <= 0) throw new NumberFormatException();

        if (autoProcesses.containsKey(arrival)) {
            throw new NumberFormatException("Arrival times must be unique.");
        }

        autoProcesses.put(arrival, burst);
    }



    private final ObservableList<Process> readyQueue = FXCollections.observableArrayList();
    private final ObservableList<Process> completedList = FXCollections.observableArrayList();
    private final PriorityQueue<Process> pq = new PriorityQueue<>(Comparator.comparingInt(p -> p.remainingTime));

    private ScheduledExecutorService scheduler;
    private int systemTime = 0;

    @FXML
    public void initialize() {
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        remainingCol.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));

        completedIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        completedBurstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        waitingTimeCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));
        responseTimeCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));
        turnaroundTimeCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));

        processTable.setItems(readyQueue);
        completedTable.setItems(completedList);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }


    private void addProcess(Process p) {
        pq.add(p);
        readyQueue.add(p);
    }

    private void tick() {
        systemTime++;

        if (autoProcesses.containsKey(systemTime - 1)) {
            Process p = new Process(autoProcesses.get(systemTime - 1), systemTime - 1);
            Platform.runLater(() -> addProcess(p));
        }

        Platform.runLater(() -> {
            Process current = pq.peek();
            if (current != null) {
                if (!current.started) {
                    current.responseTime = systemTime - current.arrivalTime;
                    current.started = true;
                }
                current.remainingTime--;
                processTable.refresh();
                currentProcessLabel.setText("Running Process: P" + current.id);
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

        for (Process p : completedList) {
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
        public int getArrivalTime() { return arrivalTime; }
    }
}

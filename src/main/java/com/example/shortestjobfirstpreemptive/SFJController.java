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

        processTable.setItems(readyQueue);
        completedTable.setItems(completedList);

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    @FXML
    private void onAddProcess() {
        try {
            int burstTime = Integer.parseInt(burstInput.getText());
            if (burstTime <= 0) throw new NumberFormatException();
            Process p = new Process(burstTime, systemTime);
            Platform.runLater(() -> {
                pq.add(p);
                readyQueue.add(p);
                burstInput.clear();
            });
        } catch (NumberFormatException e) {
            showError("Please enter a valid burst time > 0.");
        }
    }

    private void tick() {
        systemTime++;
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
                    readyQueue.remove(current);
                    completedList.add(current);
                    currentProcessLabel.setText("Running Process: None");
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

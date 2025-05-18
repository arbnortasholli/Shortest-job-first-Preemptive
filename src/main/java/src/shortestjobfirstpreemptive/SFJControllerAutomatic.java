package src.shortestjobfirstpreemptive;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;


import java.util.*;
import java.util.concurrent.*;


// Controller i JavaFX per simulimin e algoritmit Shortest Job First (preemptive)
public class SFJControllerAutomatic {


    // Label ne UI per shfaqjen e mesatareve dhe procesit aktual ne ekzekutim
    @FXML private Label avgTurnaroundLabel;
    @FXML private Label avgWaitingLabel;
    @FXML private Label avgResponseLabel;
    @FXML private Label currentProcessLabel;


    // Tabela per proceset aktive qe jane ne pritje apo ne ekzekutim
    @FXML private TableView<Process> processTable;
    @FXML private TableColumn<Process, Integer> idCol;
    @FXML private TableColumn<Process, Integer> burstCol;
    @FXML private TableColumn<Process, Integer> remainingCol;


    // Tabela per proceset e perfunduara me te dhenat per id, burst time, arrival time, waiting time, respone time, turnaround time
    @FXML private TableView<Process> completedTable;
    @FXML private TableColumn<Process, Integer> completedIdCol;
    @FXML private TableColumn<Process, Integer> completedBurstCol;
    @FXML private TableColumn<Process, Integer> waitingTimeCol;
    @FXML private TableColumn<Process, Integer> responseTimeCol;
    @FXML private TableColumn<Process, Integer> turnaroundTimeCol;
    @FXML private TableColumn<Process, Integer> arrivalTimeCol;


    // Fushat tekstuale per inputet e perdoruesit - arrival dhe burst time per 5 procese
    @FXML private TextField arrivalInput1, burstInput1;
    @FXML private TextField arrivalInput2, burstInput2;
    @FXML private TextField arrivalInput3, burstInput3;
    @FXML private TextField arrivalInput4, burstInput4;
    @FXML private TextField arrivalInput5, burstInput5;


    // Butonat per nisje, ndalim, vazhdim dhe resetim te simulimit
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button resetButton;
    @FXML private Button continueButton;


    // Lista qe ruan proceset e dhena nga perdoruesi para nisjes se simulimit
    private final List<Process> autoProcesses = new ArrayList<>();


    // Metoda qe ekzekutohet kur perdoruesi klikon Start - merr inputet dhe fillon simulimin
    @FXML
    private void onStartSimulation() {
        autoProcesses.clear(); // Pastron listen e proceseve te meparshme


        try {
            // Merr inputet dhe i konverton ne procese, Exception nese inputi nuk eshte valid
            addProcessFromInputs(arrivalInput1, burstInput1);
            addProcessFromInputs(arrivalInput2, burstInput2);
            addProcessFromInputs(arrivalInput3, burstInput3);
            addProcessFromInputs(arrivalInput4, burstInput4);
            addProcessFromInputs(arrivalInput5, burstInput5);
        } catch (NumberFormatException e) {
            // Shfaq error per perdoruesin nese ka gabim ne input
            showError("Please enter valid arrival and burst times. Arrival times must be unique, >= 0, and burst times must be > 0.");
            return;
        }


        // Rinisin variablat dhe listat per simulim te ri
        systemTime = 0;
        pq.clear();
        readyQueue.clear();
        completedList.clear();
    }


    // Ndalon ekzekutimin e simulimit
    @FXML
    private void onStopSimulation() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();  // Ndalon tick-un periodik
        }
        currentProcessLabel.setText("Simulation paused at time: " + systemTime);
    }


    // Vazhdo ekzekutimin ne rast se ishte ndalur me pare
    @FXML
    private void onContinueSimulation() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            // Planifikon ekzekutimin e metodes tick() çdo 1 sekonde
            scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
            currentProcessLabel.setText("Simulation resumed at time: " + systemTime);
        }
    }


    // Reseton simulimin dhe fshin te gjitha te dhenat dhe inputet
    @FXML
    private void onResetSimulation() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        systemTime = 0;
        autoProcesses.clear();
        pq.clear();
        readyQueue.clear();
        completedList.clear();
        currentProcessLabel.setText("Simulation reset.");


        // Reseton mesataret
        avgTurnaroundLabel.setText("0");
        avgWaitingLabel.setText("0");
        avgResponseLabel.setText("0");


        // Fshin fushat e inputit
        arrivalInput1.clear(); burstInput1.clear();
        arrivalInput2.clear(); burstInput2.clear();
        arrivalInput3.clear(); burstInput3.clear();
        arrivalInput4.clear(); burstInput4.clear();
        arrivalInput5.clear(); burstInput5.clear();
    }


    // Merr vlerat e arrival dhe burst time nga fushat input dhe i shton ne listen e proceseve
    private void addProcessFromInputs(TextField arrivalInput, TextField burstInput) throws NumberFormatException {
        String arrivalText = arrivalInput.getText().trim();
        String burstText = burstInput.getText().trim();


        // Nese fushat jane bosh, nuk ben asgje
        if (arrivalText.isEmpty() && burstText.isEmpty()) return;
        if (arrivalText.isEmpty() || burstText.isEmpty()) throw new NumberFormatException();


        int arrival = Integer.parseInt(arrivalText);
        int burst = Integer.parseInt(burstText);


        // Kontrollon vlefshmerine e inputit (arrival >= 0, burst > 0)
        if (arrival < 0 || burst <= 0) throw new NumberFormatException();


        // Krijon dhe shton procesin ne listen e proceseve automatike
        autoProcesses.add(new Process(burst, arrival));
    }


    // ObservableList per proceset qe jane gati per ekzekutim dhe shfaqen ne tabelen e proceseve aktive
    private final ObservableList<Process> readyQueue = FXCollections.observableArrayList();
    // ObservableList per proceset e perfunduara qe shfaqen ne tabelen e perfunduar
    private final ObservableList<Process> completedList = FXCollections.observableArrayList();


    // PriorityQueue qe zgjedh procesin me kohen me te shkurter te mbetur (shortest remaining time).
    // Nese remaining time eshte e barabarte zgjedh procesin me arrival time me te shkurter
    private final PriorityQueue<Process> pq = new PriorityQueue<>(
            Comparator.comparingInt((Process p) -> p.remainingTime)
                    .thenComparingInt(p -> p.arrivalTime)
    );
    private ScheduledExecutorService scheduler; // Executor qe planifikon tick() çdo sekonde
    private int systemTime = 0; // Koha e sistemit qe rritet me secilin tick


    // Metoda initialize e JavaFX, lidh kolonat e tabelave me fushat e objekteve Process dhe nis tick-un periodik
    @FXML
    public void initialize() {
        // Lidh kolonat e tabeles se proceseve aktive me fushat e objektit Process
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        burstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        remainingCol.setCellValueFactory(new PropertyValueFactory<>("remainingTime"));


        // Lidh kolonat e tabeles se proceseve te perfunduara
        completedIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        completedBurstCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        waitingTimeCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));
        responseTimeCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));
        turnaroundTimeCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));
        arrivalTimeCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));


        // Vendos te dhenat e tabelave ne ObservableList-et e pershtatshme
        processTable.setItems(readyQueue);
        completedTable.setItems(completedList);


        // Nis scheduler-in qe therret tick() çdo 1 sekonde per simulimin ne kohe reale
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::tick, 1, 1, TimeUnit.SECONDS);
    }


    // Shton nje proces ne PriorityQueue dhe ne listen e proceseve aktive (readyQueue)
    private void addProcess(Process p) {
        pq.add(p);
        readyQueue.add(p);
    }


    // Metoda kryesore qe ekzekutohet çdo sekonde per te simuluar kalimin e kohes dhe perpunimin e proceseve
    private void tick() {
        systemTime++; // Rrit kohen e sistemit


        // Kontrollon nese ndonje proces ka ardhur ne kete kohe (arrivalTime == systemTime-1)
        List<Process> toAdd = new ArrayList<>();
        for (Process p : autoProcesses) {
            if (p.arrivalTime == systemTime - 1) {
                toAdd.add(p);
            }
        }
        // Shton proceset e reja ne queue dhe ne tabele ne thread-in e UI-se
        for (Process p : toAdd) {
            Platform.runLater(() -> addProcess(p));
        }


        Platform.runLater(() -> {
            Process current = pq.peek(); // Merr procesin me kohen me te shkurter te mbetur
            if (current != null) {
                // Nese procesi nuk ka filluar me pare, cakton kohen e pergjigjes (response time)
                if (!current.started) {
                    current.responseTime = systemTime - current.arrivalTime - 1;
                    current.started = true;
                }
                // Zvogelon kohen e mbetur per ekzekutim
                current.remainingTime--;
                processTable.refresh(); // Perditeson tabelen e proceseve aktive
                currentProcessLabel.setText("Running Process: P" + current.id);


                // Nese procesi perfundon, e heq nga queue dhe e shton ne listen e perfunduara
                if (current.remainingTime == 0) {
                    pq.poll(); // Hiq nga prioriteti
                    current.completionTime = systemTime;
                    current.turnaroundTime = current.completionTime - current.arrivalTime;
                    current.waitingTime = current.turnaroundTime - current.burstTime;
                    if (current.waitingTime < 0) current.waitingTime = 0;


                    readyQueue.remove(current);
                    completedList.add(current);
                    currentProcessLabel.setText("Running Process: None");


                    // Perditeson mesataret ne UI
                    updateAverages();
                }
            } else {
                // Nese nuk ka procese per ekzekutim
                currentProcessLabel.setText("Running Process: None");
            }
        });
    }


    // Shfaq dritaren e gabimit ne UI ne rast te inputeve te pavlefshme
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Input Error");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }


    // Perditeson vlerat mesatare te turnaround, waiting dhe response time
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


        // Shton te gjitha vlerat per te llogaritur mesataret
        for (Process p : completedList) {
            totalTurnaround += p.turnaroundTime;
            totalWaiting += p.waitingTime;
            totalResponse += p.responseTime;
        }


        // Shfaq mesataret me dy shifra pas presjes dhjetore
        avgTurnaroundLabel.setText(String.format("%.2f", totalTurnaround / n));
        avgWaitingLabel.setText(String.format("%.2f", totalWaiting / n));
        avgResponseLabel.setText(String.format("%.2f", totalResponse / n));
    }


    // Klasa brenda qe perfaqeson nje proces me te gjitha fushat dhe metodat e nevojshme
    public static class Process {
        private static int idCounter = 1; // Numerues unik per çdo proces


        public int id;             // ID e procesit
        public int burstTime;      // Koha totale per ekzekutim
        public int remainingTime;  // Koha e mbetur per ekzekutim
        public int arrivalTime;    // Koha kur procesi mberrin ne sistem
        public int responseTime = -1;   // Koha e pergjigjes (first run - arrival)
        public int completionTime; // Koha kur procesi perfundon
        public int turnaroundTime; // Koha totale qe procesi kalon ne sistem
        public int waitingTime;    // Koha qe procesi pret ne queue
        public boolean started = false; // Nese procesi ka filluar ekzekutimin


        // Konstruktor qe merr burst dhe arrival time dhe cakton id automatikisht
        public Process(int burstTime, int arrivalTime) {
            this.id = idCounter++;
            this.burstTime = burstTime;
            this.remainingTime = burstTime;
            this.arrivalTime = arrivalTime;
        }


        // Getters per t’i lidhur me kolonat e tabelave ne JavaFX
        public int getId() { return id; }
        public int getBurstTime() { return burstTime; }
        public int getRemainingTime() { return remainingTime; }
        public int getWaitingTime() { return waitingTime; }
        public int getResponseTime() { return responseTime; }
        public int getTurnaroundTime() { return turnaroundTime; }
        public int getArrivalTime() { return arrivalTime; }
    }
}

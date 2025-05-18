module src.shortestjobfirstpreemptive {
    requires javafx.controls;
    requires javafx.fxml;


    opens src.shortestjobfirstpreemptive to javafx.fxml;
    exports src.shortestjobfirstpreemptive;
}
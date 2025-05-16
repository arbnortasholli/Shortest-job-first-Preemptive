module com.example.shortestjobfirstpreemptive {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.shortestjobfirstpreemptive to javafx.fxml;
    exports com.example.shortestjobfirstpreemptive;
}
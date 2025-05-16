package com.example.shortestjobfirstpreemptive;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("SFJ.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 552, 591);
        stage.setTitle("Shortest-Job-First (Preemptive)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
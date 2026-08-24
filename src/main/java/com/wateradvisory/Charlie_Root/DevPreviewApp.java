package com.wateradvisory.Charlie_Root;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * LOCAL TESTING ONLY. Lets you preview your own screens in isolation
 * while App_Root / the login flow are still being built by the rest
 * of the team. This is NOT the app's real entry point -- Main.java
 * (owned by the team, not to be modified) remains that.
 *
 * This class deliberately extends Application, so it must NOT be
 * launched directly on the plain classpath -- use DevPreviewLauncher
 * instead (same pattern as Main.java / Launcher.java).
 */
public class DevPreviewApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/Charlie_FXML/ConservationTipsView.fxml"));
        Scene scene = new Scene(loader.load(), 900, 700);
        stage.setTitle("Charlie preview (not the real app entry point)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
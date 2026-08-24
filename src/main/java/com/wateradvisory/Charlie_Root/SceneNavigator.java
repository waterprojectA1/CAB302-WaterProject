package com.wateradvisory.Charlie_Root;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

/**
 * Self-contained navigation between Charlie's own screens.
 * Swaps the root of whatever Scene the clicked button already belongs
 * to -- does NOT touch Main.java and does NOT depend on App_Root being
 * finished, so it works regardless of how the rest of the team wires
 * up the shared main menu later.
 */
public final class SceneNavigator {

    private SceneNavigator() {}

    public static void goTo(ActionEvent event, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();

            Node source = (Node) event.getSource();
            Scene scene = source.getScene();
            scene.setRoot(root);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + fxmlPath, e);
        }
    }
}

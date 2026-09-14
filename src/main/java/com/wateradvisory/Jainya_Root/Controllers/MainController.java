package com.wateradvisory.Jainya_Root.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private TabPane tabPane;

    /** Selects a tab by index; lets NavShell deep-link a tab from the drawer. */
    public void selectTab(int index) {
        if (tabPane != null && index >= 0 && index < tabPane.getTabs().size()) {
            tabPane.getSelectionModel().select(index);
        }
    }
}

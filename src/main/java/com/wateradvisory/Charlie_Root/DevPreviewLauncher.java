package com.wateradvisory.Charlie_Root;

/**
 * LOCAL TESTING ONLY. This is the class you actually point exec:exec
 * (or a run command) at -- it deliberately does NOT extend
 * javafx.application.Application, which is what lets it be launched
 * on a plain classpath without triggering "JavaFX runtime components
 * are missing". It just hands off to DevPreviewApp, which does the
 * real JavaFX startup work. Same pattern as Main.java / Launcher.java.
 */
public class DevPreviewLauncher {
    public static void main(String[] args) {
        DevPreviewApp.main(args);
    }
}
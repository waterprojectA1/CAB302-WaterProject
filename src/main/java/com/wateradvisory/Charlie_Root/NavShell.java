package com.wateradvisory.Charlie_Root;

import java.io.IOException;

import com.wateradvisory.database.AuthService;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * The app's global navigation shell: a fixed 54px top bar plus a 268px
 * slide-over drawer.
 *
 * <p>Every post-login screen is loaded through {@link #go(ActionEvent, Route)},
 * which loads the screen's FXML, wraps it in this shell and swaps the scene
 * root. Pre-auth screens (login, register, postregister) are deliberately NOT
 * wrapped ; they load bare, which is also why {@code Main.java} needs no change.
 *
 * <p>This is a shared shell that lives in {@code Charlie_Root} to respect the
 * per-member package rule in CLAUDE.md. It can move to {@code com.wateradvisory}
 * once the team agrees who owns it.
 */
public final class NavShell {

    /** Every screen the drawer (or a page button) can reach. */
    public enum Route {
        HOME("/App_Root-view.fxml", "Home", -1),
        RECORD_WATER("/Arjay_FXML/recordwater.fxml", "Record water", -1),
        DAILY("/Jainya_FXML/MainView.fxml", "My data / Daily trends", 0),
        SEASONAL("/Jainya_FXML/MainView.fxml", "My data / Seasonal", 1),
        COMPARE("/Jainya_FXML/MainView.fxml", "My data / Compare periods", 2),
        DATA_TABLE("/Michael_FXML/TableDisplayPage.fxml", "Reports / Data table", -1),
        DETAIL("/Michael_FXML/DetailDataPage.fxml", "Reports / Record detail", -1),
        NOTIFICATIONS("/Michael_FXML/NotificationPage.fxml", "Reports / Notifications", -1),
        TIPS("/Charlie_FXML/ConservationTipsView.fxml", "Conservation tips", -1),
        CHAT("/Charlie_FXML/ChatView.fxml", "Ripple", -1),
        LEADERBOARD("/Steve_FXML/LeaderboardView.fxml", "Leaderboard", -1),
        HOUSEHOLD("/Arjay_FXML/householdview.fxml", "Household", -1),
        PROFILE("/Arjay_FXML/profile.fxml", "Profile", -1);

        public final String fxml;
        public final String crumb;
        /** Tab to select after load, or -1 if the screen has no TabPane. */
        public final int tabIndex;

        Route(String fxml, String crumb, int tabIndex) {
            this.fxml = fxml;
            this.crumb = crumb;
            this.tabIndex = tabIndex;
        }
    }

    /** Bar height; content height. The window is BAR_HEIGHT + CONTENT_HEIGHT tall. */
    public static final double BAR_HEIGHT = 54;
    public static final double CONTENT_WIDTH = 882;
    public static final double CONTENT_HEIGHT = 516;

    private static final double DRAWER_WIDTH = 268;
    private static final Duration SLIDE = Duration.millis(180);

    private NavShell() {}

    /* ------------------------------------------------------------------ */
    /* Navigation                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * Loads {@code route}'s FXML, wraps it in the nav shell and swaps the root
     * of whatever Scene the clicked node belongs to. Same contract as
     * {@link SceneNavigator#goTo} ; it does not touch Main.java.
     */
    public static void go(ActionEvent event, Route route) {
        goFromNode((Node) event.getSource(), route);
    }

    /** Signs the user out and returns to the bare (unwrapped) login screen. */
    public static void logout(ActionEvent event) {
        AuthService.logout();
        Scene scene = ((Node) event.getSource()).getScene();
        try {
            Parent login = FXMLLoader.load(
                    NavShell.class.getResource("/Arjay_FXML/login.fxml"));
            scene.setRoot(login);
            resizeWindow(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load login", e);
        }
    }

    private static void goFromNode(Node source, Route route) {
        Scene scene = source.getScene();
        try {
            FXMLLoader loader = new FXMLLoader(NavShell.class.getResource(route.fxml));
            Parent page = loader.load();

            if (route.tabIndex >= 0) {
                Node tabs = page.lookup("#tabPane");
                if (tabs instanceof TabPane tabPane) {
                    tabPane.getSelectionModel().select(route.tabIndex);
                }
            }

            scene.setRoot(wrap(page, route));
            resizeWindow(scene);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load " + route.fxml, e);
        }
    }

    private static void resizeWindow(Scene scene) {
        if (scene.getWindow() instanceof Stage stage) {
            stage.sizeToScene();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Shell construction                                                  */
    /* ------------------------------------------------------------------ */

    /**
     * Wraps a loaded page in the top bar + drawer. Public so a controller that
     * loads a page itself (e.g. a row-click detail jump with no ActionEvent)
     * can still get the shell.
     */
    public static Parent wrap(Parent page, Route active) {
        // The page fills the content area; the drawer and scrim overlay only the
        // content area, never the bar.
        StackPane content = new StackPane(page);
        content.setMinSize(0, 0);
        content.setPrefSize(CONTENT_WIDTH, CONTENT_HEIGHT);
        VBox.setVgrow(content, Priority.ALWAYS);

        Region scrim = new Region();
        scrim.getStyleClass().add("nav-scrim");
        scrim.setVisible(false);
        scrim.setOpacity(0);

        VBox drawer = buildDrawer(active);
        StackPane.setAlignment(drawer, Pos.TOP_LEFT);
        drawer.setTranslateX(-DRAWER_WIDTH);

        HBox bar = buildTopBar(active, drawer, scrim);

        content.getChildren().addAll(scrim, drawer);
        scrim.setOnMouseClicked(e -> setDrawerOpen(drawer, scrim, false));

        VBox column = new VBox(bar, content);
        column.setMinSize(0, 0);

        StackPane shell = new StackPane(column);
        shell.getStyleClass().addAll("root", "app-shell");
        shell.setMinSize(0, 0);
        shell.setPrefSize(CONTENT_WIDTH, BAR_HEIGHT + CONTENT_HEIGHT);
        shell.getStylesheets().add(
                NavShell.class.getResource("/Charlie_FXML/app.css").toExternalForm());
        return shell;
    }

    private static HBox buildTopBar(Route active, VBox drawer, Region scrim) {
        Button hamburger = new Button("\u2630");
        hamburger.getStyleClass().add("nav-icon-btn");
        hamburger.setFocusTraversable(false);
        hamburger.setOnAction(e ->
                setDrawerOpen(drawer, scrim, drawer.getTranslateX() < 0));

        Label brand = new Label("Water Advisory");
        brand.getStyleClass().add("nav-brand");

        Label crumb = new Label("\u00B7  " + active.crumb);
        crumb.getStyleClass().add("nav-breadcrumb");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button bell = new Button("\uD83D\uDD14");
        bell.getStyleClass().add("nav-icon-btn");
        bell.setFocusTraversable(false);
        bell.setOnAction(e -> go(e, Route.NOTIFICATIONS));

        Circle dot = new Circle(3.5);
        dot.getStyleClass().add("nav-bell-dot");
        StackPane bellStack = new StackPane(bell, dot);
        StackPane.setAlignment(dot, Pos.TOP_RIGHT);
        bellStack.setMaxSize(34, 34);

        StackPane avatar = new StackPane(initialsLabel());
        avatar.getStyleClass().add("nav-avatar");
        avatar.setOnMouseClicked(e -> goFromNode((Node) e.getSource(), Route.PROFILE));

        HBox bar = new HBox(10, hamburger, brand, crumb, spacer, bellStack, avatar);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("app-topbar");
        bar.setMinWidth(0);
        return bar;
    }

    private static Label initialsLabel() {
        Label label = new Label(initialsOf(AuthService.getUsername()));
        label.getStyleClass().add("nav-avatar-initials");
        return label;
    }

    /** First letter of each of a name's first two words, or one letter for a single word. */
    public static String initialsOf(String name) {
        if (name == null || name.isBlank()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        return parts.length > 1
                ? ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase()
                : name.trim().substring(0, 1).toUpperCase();
    }

    /** A round avatar circle showing a name's initials, for reuse outside the nav bar. */
    public static StackPane avatar(String name, double size) {
        Label label = new Label(initialsOf(name));
        label.getStyleClass().add("avatar-initials");
        StackPane circle = new StackPane(label);
        circle.getStyleClass().add("avatar-circle");
        circle.setMinSize(size, size);
        circle.setMaxSize(size, size);
        return circle;
    }

    private static VBox buildDrawer(Route active) {
        Label head = new Label("Menu");
        head.getStyleClass().add("nav-drawer-head");

        String username = AuthService.getUsername();
        Label sub = new Label(username == null || username.isBlank() ? "Signed in" : username);
        sub.getStyleClass().add("nav-drawer-sub");

        VBox body = new VBox(4,
                head,
                sub,
                item("Home", Route.HOME, active, false),
                item("Record water", Route.RECORD_WATER, active, false),
                group("MY DATA", "nav-group-data", active,
                        Route.DAILY, Route.SEASONAL, Route.COMPARE),
                group("REPORTS", "nav-group-reports", active,
                        Route.DATA_TABLE, Route.NOTIFICATIONS),
                item("Conservation tips", Route.TIPS, active, false),
                item("Leaderboard", Route.LEADERBOARD, active, false),
                item("Household", Route.HOUSEHOLD, active, false),
                item("Profile", Route.PROFILE, active, false),
                rule(),
                logoutItem());
        body.setMinWidth(0);

        ScrollPane scroll = new ScrollPane(body);
        scroll.getStyleClass().add("nav-drawer-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox drawer = new VBox(scroll);
        drawer.getStyleClass().add("nav-drawer");
        drawer.setMaxHeight(Double.MAX_VALUE);
        return drawer;
    }

    private static VBox group(String label, String tintClass, Route active, Route... routes) {
        Label caption = new Label(label);
        caption.getStyleClass().add("nav-group-label");

        VBox box = new VBox(2, caption);
        box.getStyleClass().addAll("nav-group", tintClass);
        for (Route route : routes) {
            box.getChildren().add(item(shortLabel(route), route, active, true));
        }
        return box;
    }

    /** "My data / Seasonal" becomes "Seasonal" for the in-group label. */
    private static String shortLabel(Route route) {
        int slash = route.crumb.indexOf('/');
        return slash < 0 ? route.crumb : route.crumb.substring(slash + 1).trim();
    }

    private static Button item(String text, Route route, Route active, boolean sub) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-item");
        if (sub) {
            button.getStyleClass().add("nav-item-sub");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        if (route == active) {
            button.getStyleClass().add("nav-item-active");
        }
        button.setOnAction(e -> {
            if (route != active) {
                go(e, route);
            }
        });
        return button;
    }

    private static Button logoutItem() {
        Button button = new Button("Log out");
        button.getStyleClass().addAll("nav-item", "nav-item-danger");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setFocusTraversable(false);
        button.setOnAction(NavShell::logout);
        return button;
    }

    private static Region rule() {
        Region region = new Region();
        region.getStyleClass().add("nav-rule");
        VBox.setMargin(region, new Insets(6, 8, 6, 8));
        return region;
    }

    /* ------------------------------------------------------------------ */
    /* Drawer open/close                                                   */
    /* ------------------------------------------------------------------ */

    private static void setDrawerOpen(VBox drawer, Region scrim, boolean open) {
        TranslateTransition slide = new TranslateTransition(SLIDE, drawer);
        slide.setInterpolator(Interpolator.EASE_OUT);
        slide.setToX(open ? 0 : -DRAWER_WIDTH);

        FadeTransition fade = new FadeTransition(SLIDE, scrim);
        fade.setToValue(open ? 1 : 0);

        if (open) {
            scrim.setVisible(true);
        } else {
            fade.setOnFinished(e -> scrim.setVisible(false));
        }
        slide.play();
        fade.play();
    }
}

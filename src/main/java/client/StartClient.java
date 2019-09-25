package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

public class StartClient extends Application {
    private static final Logger LOGGER = Logger.getLogger(StartClient.class);

    private static final double WIDTH = 1000.0;
    private static final double HEIGHT = 600.0;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/static/ui.fxml"));
        primaryStage.setTitle("FXChat");
        primaryStage.setMinWidth(WIDTH);
        primaryStage.setMinHeight(HEIGHT);
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> this.onCloseRequest());
    }

    private void onCloseRequest() {
        LOGGER.info("Windows is closing");
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

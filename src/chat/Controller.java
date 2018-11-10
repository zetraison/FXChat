package chat;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import java.util.List;


public class Controller {

    private static final double MIN_SIDE_PANE_WIDTH = 300.0;
    private static final double MAX_SIDE_PANE_WIDTH = 300.0;
    private static final int STICKER_ROW_LENGTH = 4;
    private static final int STICKER_SIZE = 200;

    @FXML
    SplitPane splitPane;

    @FXML
    AnchorPane lPane;

    @FXML
    AnchorPane cPane;

    @FXML
    AnchorPane rPane;

    @FXML
    ScrollPane cScrollPane;

    @FXML
    ScrollPane rScrollPane;

    @FXML
    VBox cVBox;

    @FXML
    VBox rVBox;

    @FXML
    HBox cHBox;

    @FXML
    TextFlow msgFlow;

    @FXML
    TextField msgField;

    @FXML
    Button stickerBtn;

    @FXML
    Button sendBtn;

    public void initialize() {
        initStickerWidget(Utils.getStickerPackCat(), "Cat");
        initStickerWidget(Utils.getStickerPackDog(), "Dog");
        initStickerWidget(Utils.getStickerPackPepe(), "Pepe");

        // Set autoscrolling
        cScrollPane.vvalueProperty().bind(msgFlow.heightProperty());
    }

    private void handleStickerMouseClick(MouseEvent event, ImageView imageView) {
        Image copyImage = imageView.getImage();
        ImageView copyImageView = new ImageView(copyImage);
        copyImageView.setFitWidth(STICKER_SIZE);
        copyImageView.setFitHeight(STICKER_SIZE);
        msgFlow.getChildren().add(copyImageView);
        Text time = new Text(Utils.getCurrentTime() + "\n");
        time.setFill(Color.GREY);
        msgFlow.getChildren().add(time);
        msgField.requestFocus();
    }

    private void initStickerWidget(List<String> urls, String title) {
        rVBox.getChildren().add(new Label(title));

        HBox hBox = new HBox();

        int index = 1;
        for (String url: urls) {
            Image image = new Image(url, true);

            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(MAX_SIDE_PANE_WIDTH / STICKER_ROW_LENGTH - 10);
            imageView.setFitWidth(MAX_SIDE_PANE_WIDTH / STICKER_ROW_LENGTH - 10);

            BorderPane imageViewWrapper = new BorderPane(imageView);
            imageViewWrapper.setPrefWidth(MAX_SIDE_PANE_WIDTH / STICKER_ROW_LENGTH);
            imageViewWrapper.setPrefHeight(MAX_SIDE_PANE_WIDTH / STICKER_ROW_LENGTH);
            imageViewWrapper.getStyleClass().add("image-view-wrapper");
            imageViewWrapper.setOnMouseClicked(event -> handleStickerMouseClick(event, imageView));

            hBox.getChildren().add(imageViewWrapper);
            if (index % STICKER_ROW_LENGTH == 0) {
                rVBox.getChildren().add(hBox);
                hBox = new HBox();
            }
            if (index == urls.size()) {
                rVBox.getChildren().add(hBox);
            }
            index++;
        }
    }

    @FXML
    private void toogleRightPane() {
        SplitPane.Divider rightDivider = splitPane.getDividers().get(1);
        double currentPosition = rightDivider.getPosition();
        double rightPaneWidth = rPane.getWidth();
        if (rightPaneWidth != 0) {
            rPane.setMinWidth(0);
            rPane.setMaxWidth(0);
            rightDivider.setPosition(1.0);
        } else {
            rPane.setMinWidth(MIN_SIDE_PANE_WIDTH);
            rPane.setMaxWidth(MAX_SIDE_PANE_WIDTH);
            rightDivider.setPosition(currentPosition);
        }
    }

    @FXML
    private void sendMsg() {
        if (msgField.getText().isEmpty()) {
            return;
        }
        Text time = new Text(Utils.getCurrentTime() + " ");
        time.setFill(Color.GREY);
        time.setLineSpacing(100);
        msgFlow.getChildren().add(time);
        Text text = new Text(msgField.getText() + "\n");
        text.setLineSpacing(100);
        text.setFill(Color.GHOSTWHITE);
        msgFlow.getChildren().add(text);
        msgField.clear();
        msgField.requestFocus();
    }

    @FXML
    private void handleOnKeyPressed(KeyEvent event)
    {
        if (event.getCode().equals(KeyCode.ENTER)) {
            sendMsg();
        }
    }
}

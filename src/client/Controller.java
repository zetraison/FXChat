package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;


public class Controller {

    @FXML SplitPane splitPane;
    @FXML AnchorPane lPane;
    @FXML AnchorPane cPane;
    @FXML AnchorPane rPane;
    @FXML ScrollPane cScrollPane;
    @FXML ScrollPane rScrollPane;
    @FXML VBox cVBox;
    @FXML VBox rVBox;
    @FXML HBox cHBox;
    @FXML TextFlow msgFlow;
    @FXML TextField msgField;
    @FXML Button stickerBtn;
    @FXML Button sendBtn;
    @FXML VBox authVBox;
    @FXML TextField loginField;
    @FXML PasswordField passwordField;

    private static final double MIN_SIDE_PANE_WIDTH = 300.0;
    private static final double MAX_SIDE_PANE_WIDTH = 300.0;
    private static final int STICKER_ROW_LENGTH = 4;
    private static final int STICKER_SIZE = 200;

    private boolean isAuthorized;

    Socket socket;
    DataOutputStream out;
    DataInputStream in;

    public void initialize() {
        // Init auth form
        setAuthorized(false);
        // Init main form
        initStickerWidget(Utils.getStickerPackCat(), "Cat");
        initStickerWidget(Utils.getStickerPackDog(), "Dog");
        initStickerWidget(Utils.getStickerPackPepe(), "Pepe");

        // Set autoscrolling
        cScrollPane.vvalueProperty().bind(msgFlow.heightProperty());
    }

    public void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if (!this.isAuthorized) {
            authVBox.setVisible(true);
            cVBox.setVisible(false);
            lPane.setMaxWidth(0);
            rPane.setMaxWidth(0);
            lPane.setMinWidth(0);
            rPane.setMinWidth(0);
        } else {
            authVBox.setVisible(false);
            cVBox.setVisible(true);
            lPane.setMinWidth(MIN_SIDE_PANE_WIDTH);
            lPane.setMaxWidth(MAX_SIDE_PANE_WIDTH);
        }

    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8183);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/authok")) {
                            String[] tokens = str.split(" ");
                            setAuthorized(true);
                            break;
                        } else {
                            appendMessage(str);
                        }
                    }

                    while (true) {
                        String str = in.readUTF();
                        if (str.equals("/serverclosed")) {
                            break;
                        }
                        appendMessage(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendMessage(String msg) {
        Platform.runLater(() -> {
            Text time = new Text(Utils.getCurrentTime() + " ");
            time.setFill(Color.GREY);
            time.setLineSpacing(100);
            msgFlow.getChildren().add(time);

            Text text = new Text(msg + "\n");
            text.setLineSpacing(100);
            text.setFill(Color.GHOSTWHITE);
            msgFlow.getChildren().add(text);
        });
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleOnKeyPressed(KeyEvent event)
    {
        if (event.getCode().equals(KeyCode.ENTER)) {
            sendMsg();
        }
    }

    public void toogleRightPane() {
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
}

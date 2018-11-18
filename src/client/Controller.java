package client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class Controller {

    @FXML SplitPane splitPane;
    @FXML AnchorPane lPane;
    @FXML AnchorPane cPane;
    @FXML AnchorPane rPane;
    @FXML ScrollPane lScrollPane;
    @FXML ScrollPane cScrollPane;
    @FXML ScrollPane rScrollPane;
    @FXML VBox lVBox;
    @FXML VBox cVBox;
    @FXML VBox rVBox;
    @FXML HBox cHBox;
    @FXML HBox errorHBox;
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
    private String currentUser;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

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

    private void setAuthorized(boolean isAuthorized) {
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

    private void connect() {
        try {
            socket = new Socket("localhost", 8082);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    while (true) {
                        String str = in.readUTF();
                        /*
                         * /authok <user>
                         */
                        if (str.startsWith("/authok")) {
                            hideError();
                            String[] tokens = str.split(" ");
                            currentUser = tokens[1];
                            setAuthorized(true);
                            continue;
                        }
                        /*
                         * /userlogin <users>
                         */
                        if (str.startsWith("/userlogin")) {
                            LinkedList<String> tokens = new LinkedList<>(Arrays.asList(str.split(" ")));
                            tokens.remove(0);
                            appendUser(tokens);
                            continue;
                        }
                        /*
                         * /sticker <username> <url>
                         */
                        if (str.startsWith("/sticker")) {
                            String[] tokens = str.split(" ");
                            appendTime();
                            appendNickname(tokens[1]);
                            appendSticker(tokens[2]);
                            continue;
                        }
                        /*
                         * /error <message>
                         */
                        if (str.startsWith("/error")) {
                            String[] tokens = str.split(" ");
                            showError(tokens[1]);
                            continue;
                        }
                        /*
                         * /serverclosed
                         */
                        if (str.equals("/serverclosed")) {
                            break;
                        }
                        appendTime();
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

    private void appendTime() {
        Platform.runLater(() -> {
            Text time = new Text(Utils.getCurrentTime() + " ");
            time.setFill(Color.GREY);
            msgFlow.getChildren().add(time);
        });
    }

    private void appendNickname(String nickname) {
        Platform.runLater(() -> {
            Text time = new Text(nickname + " ");
            time.setFill(Color.GOLD);
            msgFlow.getChildren().add(time);
        });
    }

    private void appendMessage(String msg) {
        Platform.runLater(() -> {
            Text text = new Text(msg + "\n");
            text.setFill(Color.GHOSTWHITE);
            msgFlow.getChildren().add(text);
        });
    }

    private void appendUser(LinkedList<String> users) {
        Platform.runLater(() -> {
            lVBox.getChildren().clear();
            for (String user: users) {
                lVBox.getChildren().add(
                        new Label(user + (user.equals(currentUser) ? " (you)" : ""))
                );
            }
        });
    }

    private void appendSticker(String url) {
        Platform.runLater(() -> {
            Image image = new Image(url, true);

            ImageView imageView = new ImageView(image);
            imageView.setFitHeight(STICKER_SIZE);
            imageView.setFitWidth(STICKER_SIZE);

            msgFlow.getChildren().add(new Text("\n"));
            msgFlow.getChildren().add(imageView);
            msgFlow.getChildren().add(new Text("\n\n\n"));
        });
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            errorHBox.getChildren().clear();
            Text text = new Text(msg);
            text.setFill(Color.RED);
            errorHBox.getChildren().add(text);
            loginField.requestFocus();
        });
    }

    private void hideError() {
        Platform.runLater(() -> errorHBox.getChildren().clear());
    }

    public void sendMsg() {
        if (msgField.getText().isEmpty())
            return;
        try {
            out.writeUTF(currentUser + " " + msgField.getText());
            msgField.clear();
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendSticker(String url) {
        try {
            out.writeUTF("/sticker " + currentUser + " " + url);
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

    public void loginFieldOnKeyTyped(KeyEvent event) {
        hideError();
    }

    public void msgFieldOnKeyPressed(KeyEvent event)
    {
        if (event.getCode().equals(KeyCode.ENTER)) {
            sendMsg();
        }
    }

    public void stickerBtnOnAction() {
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
            imageViewWrapper.setOnMouseClicked(event -> {
                sendSticker(url);
                msgField.requestFocus();
            });

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

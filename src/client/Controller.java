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
    @FXML TextField usernameField;
    @FXML TextField loginField;
    @FXML PasswordField passwordField;
    @FXML Button registerBtn;
    @FXML Button loginBtn;
    @FXML Button saveUserBtn;
    @FXML Label usernameLabel;

    private static final double MIN_SIDE_PANE_WIDTH = 300.0;
    private static final double MAX_SIDE_PANE_WIDTH = 300.0;
    private static final int STICKER_ROW_LENGTH = 4;
    private static final int STICKER_SIZE = 200;

    private boolean isAuthorized;
    private boolean isRegister;
    private String currentUser;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public void initialize() {
        // Init auth form
        setAuthorized(false);
        setRegister(false);
        // Init main form
        initStickerWidget(Utils.getStickerPackCat(), "Cat");
        initStickerWidget(Utils.getStickerPackDog(), "Dog");
        initStickerWidget(Utils.getStickerPackPepe(), "Pepe");

        // Set autoscrolling
        cScrollPane.vvalueProperty().bind(msgFlow.heightProperty());

        Text text = new Text("Type /help for get more information about available service command.\n");
        text.setFill(Color.GREEN);
        msgFlow.getChildren().add(text);
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 8082);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(this::resetSocketOnTimeout).start();
            new Thread(this::eventLoop).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetSocketOnTimeout() {
        try {
            Thread.sleep(120000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!isAuthorized) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  Available service command
     *
     *  /auth <login> <password>
     *  /authok <username>
     *  /sticker <username> <url>
     *  /error <message>
     *  /register <username> <login> <password>
     *  /serverclosed
     *  /message <username> <message>
     *  /w <username> <message>
     */
    private void eventLoop() {
        try {
            while (true) {
                String str = in.readUTF();
                List<String> tokens = Arrays.asList(str.split(" "));
                String event = tokens.get(0);
                System.out.println("[EVENT]: " + tokens);

                EventEnum eventEnum = EventEnum.fromValue(event);

                switch (eventEnum) {
                    case AUTH_OK: {
                        hideError();
                        setAuthorized(true);
                        currentUser = tokens.get(1);
                        continue;
                    }
                    case ERROR: {
                        showError(String.join(" ", tokens.subList(1, tokens.size())));
                        continue;
                    }
                    case MESSAGE:
                    case PRIVATE_MESSAGE: {
                        appendTime();
                        appendNickname(tokens.get(1));
                        appendMessage(String.join(" ", tokens.subList(2, tokens.size())));
                        continue;
                    }
                    case STICKER: {
                        appendTime();
                        appendNickname(tokens.get(1));
                        appendSticker(tokens.get(2));
                        continue;
                    }
                    case IMAGE: {
                        appendTime();
                        appendNickname(tokens.get(1));
                        appendImage(tokens.get(2));
                        continue;
                    }
                    case HELP: {
                        appendHelpInfo();
                        continue;
                    }
                    case CLIENTLIST: {
                        appendUsers(tokens.subList(1, tokens.size()));
                        continue;
                    }
                    case SERVER_CLOSED: {
                        break;
                    }
                }
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
    }

    private void appendHelpInfo() {
        Platform.runLater(() -> {
            Text text = new Text("\nAvailable service command list:\n\n");
            text.setFill(Color.GREEN);
            msgFlow.getChildren().add(text);
            text = new Text("- Send image by url:\n");
            text.setFill(Color.GREEN);
            msgFlow.getChildren().add(text);
            text = new Text("      /image <username> <url>\n");
            text.setFill(Color.GREENYELLOW);
            msgFlow.getChildren().add(text);
            text = new Text("- Send private message:\n");
            text.setFill(Color.GREEN);
            msgFlow.getChildren().add(text);
            text = new Text("      /w <username> <message>\n\n");
            text.setFill(Color.GREENYELLOW);
            msgFlow.getChildren().add(text);
        });
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

    private void appendUsers(List<String> users) {
        Platform.runLater(() -> {
            lVBox.getChildren().clear();
            for (String user: users) {
                lVBox.getChildren().add(new Label(user + (user.equals(currentUser) ? " (you)" : "")));
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

    private void appendImage(String url) {
        Platform.runLater(() -> {
            Image image = new Image(url, true);

            ImageView imageView = new ImageView(image);

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

    private void sendEvent(String ...args) {
        try {
            out.writeUTF(String.join(" ", Arrays.asList(args)));
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        if (msgField.getText().isEmpty())
            return;

        List<String> tokens = Arrays.asList(msgField.getText().split(" "));
        switch (EventEnum.fromValue(tokens.get(0))) {
            case PRIVATE_MESSAGE: {
                String toUser = tokens.get(1);
                String message = String.join(" ", tokens.subList(2, tokens.size()));
                sendEvent(EventEnum.PRIVATE_MESSAGE.getValue(), toUser, currentUser, message);
                break;
            }
            case BLACKLIST: {
                String toUser = tokens.get(1);
                sendEvent(EventEnum.BLACKLIST.getValue(), toUser);
                break;
            }
            case WHITELIST: {
                String toUser = tokens.get(1);
                sendEvent(EventEnum.WHITELIST.getValue(), toUser);
                break;
            }
            case HELP: {
                sendEvent(EventEnum.HELP.getValue());
                break;
            }
            case IMAGE: {
                String toUser = tokens.get(1);
                String url = tokens.get(2);
                sendEvent(EventEnum.IMAGE.getValue(), toUser, url);
                break;
            }
            case END: {
                sendEvent(EventEnum.END.getValue());
                break;
            }
            default: {
                sendEvent(EventEnum.MESSAGE.getValue(), currentUser, msgField.getText());
                break;
            }
        }
        msgField.clear();
        msgField.requestFocus();
    }

    private void sendSticker(String url) {
        sendEvent(EventEnum.STICKER.getValue(), currentUser, url);
    }

    public void tryToAuth() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        sendEvent(EventEnum.AUTH.getValue(), loginField.getText(), passwordField.getText());
        loginField.clear();
        passwordField.clear();
    }

    public void showRegister() {
        setRegister(true);
    }

    public void setRegister(boolean isRegister) {
        this.isRegister = isRegister;
        if (isRegister) {
            usernameLabel.setVisible(true);
            usernameField.setVisible(true);
            saveUserBtn.setVisible(true);
            loginBtn.setVisible(false);
            registerBtn.setVisible(false);
            usernameField.clear();
            loginField.clear();
            passwordField.clear();
            usernameField.requestFocus();
        } else {
            usernameLabel.setVisible(false);
            usernameField.setVisible(false);
            saveUserBtn.setVisible(false);
            loginBtn.setVisible(true);
            registerBtn.setVisible(true);
            loginField.requestFocus();
        }
    }

    public void saveUser() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        sendEvent(EventEnum.REGISTER.getValue(), usernameField.getText(), loginField.getText(), passwordField.getText());
        usernameField.clear();
        setRegister(false);
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

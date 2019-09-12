package client;

import client.io.HistoryReader;
import client.io.HistoryWriter;
import client.models.EventType;
import client.models.Event;
import client.utils.ImageUtil;
import client.utils.TimeUtil;
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
import org.sqlite.util.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class Controller {
    private static final double MIN_SIDE_PANE_WIDTH = 300.0;
    private static final double MAX_SIDE_PANE_WIDTH = 300.0;
    private static final int STICKER_ROW_LENGTH = 4;
    private static final int STICKER_SIZE = 200;

    private static final String HELP_INFO_MESSAGE = "Type /help for get more information about available service command.\n";
    private static final String USERNAME_LOGIN_IS_EMPTY = "Login is empty!";
    private static final String ERROR_LOGIN_IS_EMPTY = "Login is empty!";
    private static final String ERROR_PASSWORD_IS_EMPTY = "Password is empty!";

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
    @FXML Button cancelRegisterBtn;
    @FXML Label usernameLabel;

    private boolean isAuthorized;
    private boolean isRegister;
    private String currentUser;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private HistoryReader<List<Event>> historyReader = new HistoryReader<>();
    private HistoryWriter<List<Event>> historyWriter = new HistoryWriter<>();
    private List<Event> events = new ArrayList<>();

    public void initialize() {
        // Init auth form
        setAuthorized(false);
        setRegister(false);
        cancelRegisterBtn.setVisible(false);
        // Init main form
        initStickerWidget(ImageUtil.getStickerPackCat(), "Cat");
        initStickerWidget(ImageUtil.getStickerPackDog(), "Dog");
        initStickerWidget(ImageUtil.getStickerPackPepe(), "Pepe");

        // Set autoscrolling
        cScrollPane.vvalueProperty().bind(msgFlow.heightProperty());

        appendText(HELP_INFO_MESSAGE, msgFlow, Color.GREEN);
        loadHistory();
    }

    private void connect() {
        try {
            socket = new Socket("localhost", 8082);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(this::resetSocketOnTimeout).start();
            new Thread(this::readEvent).start();
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
     * Send event data to socket
     * @param event instance of Event class
     */
    private void sendEvent(Event event) {
        try {
            out.writeUTF(event.toString());
            msgField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read event data from socket
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
    private void readEvent() {
        try {
            while (true) {
                String data = in.readUTF();
                Event event = new Event(data);

                switch (event.getType()) {
                    case AUTH_OK:
                        hideError();
                        setAuthorized(true);
                        currentUser = event.getAuthor();
                        continue;
                    case PRIVATE_MESSAGE:
                    case MESSAGE:
                        appendTime(event.getTime());
                        appendNickname(event.getAuthor());
                        appendMessage(StringUtils.join(event.getArgs(), " "));
                        events.add(event);
                        historyWriter.write(events);
                        continue;
                    case STICKER: {
                        appendTime(event.getTime());
                        appendNickname(event.getAuthor());
                        appendSticker(event.getArgs().get(0));
                        continue;
                    }
                    case IMAGE: {
                        appendTime(event.getTime());
                        appendNickname(event.getAuthor());
                        appendImage(event.getArgs().get(0));
                        continue;
                    }
                    case HELP: {
                        appendHelpInfo();
                        continue;
                    }
                    case CLIENT_LIST: {
                        appendUsers(event.getArgs());
                        continue;
                    }
                    default:
                        continue;
                    case SERVER_CLOSED:
                    case END:
                        break;
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

    public void sendMsg() {
        String text = msgField.getText();
        if (text.isEmpty())
            return;
        Event event = new Event(currentUser, text);
        sendEvent(event);
        msgField.clear();
        msgField.requestFocus();
    }

    /**
     * Deserialize user message history form text file
     */
    private void loadHistory() {
        try {
            List<Event> events = historyReader.read();
            if (events == null) {
                return;
            }
            this.events = events;
            for (Event event: events) {
                if (
                        event.getType() == EventType.MESSAGE ||
                        event.getType() == EventType.PRIVATE_MESSAGE ||
                        event.getType() == EventType.IMAGE ||
                        event.getType() == EventType.STICKER
                ) {
                    appendTime(event.getTime());
                    appendNickname(event.getAuthor());
                }
                if (event.getType() == EventType.MESSAGE || event.getType() == EventType.PRIVATE_MESSAGE) {
                    appendMessage(StringUtils.join(event.getArgs(), " "));
                }
                if (event.getType() == EventType.IMAGE) {
                    appendImage(event.getArgs().get(0));
                }
                if (event.getType() == EventType.STICKER) {
                    appendSticker(event.getArgs().get(0));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void appendText(String msg, TextFlow parent, Color color) {
        Platform.runLater(() -> {
            Text text = new Text(msg + " ");
            text.setFill(color);
            parent.getChildren().add(text);
        });
    }

    private void appendHelpInfo() {
        Platform.runLater(() -> {
            appendText("\nAvailable service command list:\n", msgFlow, Color.GREEN);
            appendText("- Send private message:\n", msgFlow, Color.GREEN);
            appendText("    /w <username> <message>\n", msgFlow, Color.GREENYELLOW);
            appendText("- Add user to blacklist:\n", msgFlow, Color.GREEN);
            appendText("    /blacklist <username> \n", msgFlow, Color.GREENYELLOW);
            appendText("- Remove user from blacklist:\n", msgFlow, Color.GREEN);
            appendText("    /whitelist <username> \n", msgFlow, Color.GREENYELLOW);
            appendText("- Send image by url:\n", msgFlow, Color.GREEN);
            appendText("    /image <username> <url>\n", msgFlow, Color.GREENYELLOW);
            appendText("- Change your login:\n", msgFlow, Color.GREEN);
            appendText("    /changelogin <login>\n\n", msgFlow, Color.GREENYELLOW);
        });
    }

    private void appendTime(String time) {
        Platform.runLater(() -> appendText(time, msgFlow, Color.GREY));
    }

    private void appendNickname(String nickname) {
        Platform.runLater(() -> appendText(nickname, msgFlow, Color.GOLD));
    }

    private void appendMessage(String msg) {
        Platform.runLater(() -> appendText(msg + "\n", msgFlow, Color.GHOSTWHITE));
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

    private void sendSticker(String url) {
        Event event = new Event(currentUser, EventType.STICKER, Collections.singletonList(url));
        sendEvent(event);
    }

    private void checkSocket() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
    }

    public void authUser() {
        checkSocket();
        if (loginField.getText().isEmpty()) {
            showError(ERROR_LOGIN_IS_EMPTY);
            loginField.requestFocus();
            return;
        }
        if (passwordField.getText().isEmpty()) {
            showError(ERROR_PASSWORD_IS_EMPTY);
            passwordField.requestFocus();
            return;
        }
        Event event = new Event(currentUser, EventType.AUTH, Arrays.asList(loginField.getText(), passwordField.getText()));
        sendEvent(event);
        loginField.clear();
        passwordField.clear();
    }

    public void saveUser() {
        checkSocket();
        if (usernameField.getText().isEmpty()) {
            showError(USERNAME_LOGIN_IS_EMPTY);
            usernameField.requestFocus();
            return;
        }
        if (loginField.getText().isEmpty()) {
            showError(ERROR_LOGIN_IS_EMPTY);
            loginField.requestFocus();
            return;
        }
        if (passwordField.getText().isEmpty()) {
            showError(ERROR_PASSWORD_IS_EMPTY);
            passwordField.requestFocus();
            return;
        }
        Event event = new Event(
                currentUser,
                EventType.REGISTER,
                Arrays.asList(usernameField.getText(), loginField.getText(), passwordField.getText())
        );
        sendEvent(event);
        usernameField.clear();
        cancelRegisterBtn.setVisible(false);
        setRegister(false);
    }

    public void showRegister() {
        hideError();
        setRegister(true);
    }

    private void setRegister(boolean isRegister) {
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
            cancelRegisterBtn.setVisible(true);
        } else {
            usernameLabel.setVisible(false);
            usernameField.setVisible(false);
            saveUserBtn.setVisible(false);
            loginBtn.setVisible(true);
            registerBtn.setVisible(true);
            loginField.requestFocus();
        }
    }

    public void cancelRegister() {
        setRegister(false);
        hideError();
        cancelRegisterBtn.setVisible(false);
    }

    public void fieldOnKeyTyped(KeyEvent event) {
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

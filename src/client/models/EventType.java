package client.models;

public enum EventType {
    AUTH("/auth"),
    AUTH_OK("/authok"),
    BLACKLIST("/blacklist"),
    CHANGE_LOGIN("/changelogin"),
    CLIENT_LIST("/clientlist"),
    END("/end"),
    ERROR("/error"),
    HELP("/help"),
    IMAGE("/image"),
    MESSAGE("/message"),
    PRIVATE_MESSAGE("/w"),
    REGISTER("/register"),
    SERVER_CLOSED("/serverclosed"),
    STICKER("/sticker"),
    USER_LOGIN("/userlogin"),
    WHITELIST("/whitelist");

    private final String value;

    EventType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EventType fromValue(String value) {
        for(EventType eventType : EventType.values()) {
            if(eventType.getValue().equals(value))
                return eventType;
        }
        return MESSAGE;
    }
}

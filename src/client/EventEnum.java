package client;

public enum EventEnum {
  AUTH("/auth"),
  AUTH_OK("/authok"),
  BLACKLIST("/blacklist"),
  CLIENTLIST("/clientlist"),
  END("/end"),
  ERROR("/error"),
  MESSAGE("/message"),
  PRIVATE_MESSAGE("/w"),
  SERVER_CLOSED("/serverclosed"),
  STICKER("/sticker"),
  USER_LOGIN("/userlogin"),
  WHITELIST("/whitelist");

  private final String value;

  EventEnum(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static final EventEnum fromValue(String value) {
    for(EventEnum eventEnum : EventEnum .values())
    {
      if(eventEnum.getValue().equals(value))
        return eventEnum;
    }
    return MESSAGE;
  }
}

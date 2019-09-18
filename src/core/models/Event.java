package core.models;

import client.utils.TimeUtil;
import com.sun.istack.internal.NotNull;
import core.enums.EventType;
import org.sqlite.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Event implements Serializable {
    private EventType type;
    private String author;
    private List<String> args;

    public Event(String author, @NotNull EventType type, List<String> args) {
        this.author = author != null ? author : "Guest";
        this.type = type;
        this.args = args;
    }

    public Event(String author, @NotNull String text) {
        List<String> tokens = Arrays.asList(text.split(" "));
        this.type = EventType.fromValue(tokens.get(0));
        if (this.type == EventType.MESSAGE) {
            this.args = new ArrayList<>(tokens.subList(0, tokens.size()));
        } else {
            this.args = tokens.size() > 1 ? new ArrayList<>(tokens.subList(1, tokens.size())) : null;
        }
        this.author = author != null ? author : "Guest";;
    }

    public Event(@NotNull String text) {
        List<String> tokens = Arrays.asList(text.split(" "));
        this.type = EventType.fromValue(tokens.get(0));
        this.author = tokens.get(1);
        this.args = tokens.size() > 2 ? new ArrayList<>(tokens.subList(2, tokens.size())) : null;
    }

    @Override
    public String toString() {
        return StringUtils.join(
                Arrays.asList(
                        this.type.getValue(),
                        this.author,
                        this.args != null ? StringUtils.join(this.args, " ") : ""
                ),
                " ");
    }

    public String log() {
        return StringUtils.join(
                Arrays.asList(
                        "Author=[" +this.author + "]",
                        "[" + this.type + "]:",
                        "{" + (this.args != null ? StringUtils.join(this.args, " ") : "") + "}"
                ),
                " ");
    }

    public EventType getType() {
        return type;
    }
    public String getAuthor() {
        return author;
    }

    public List<String> getArgs() {
        return args;
    }
}

package client.models;

import client.utils.TimeUtil;
import com.sun.istack.internal.NotNull;
import org.sqlite.util.StringUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Event implements Serializable {
    private EventType type;
    private String time;
    private String author;
    private List<String> args;

    public Event(@NotNull String author, @NotNull EventType type, List<String> args) {
        this.author = author != null ? author : "Guest";
        this.type = type;
        this.args = args;
        this.time = TimeUtil.getCurrentTime();
    }

    public Event(@NotNull String author, @NotNull String text) {
        List<String> tokens = Arrays.asList(text.split(" "));
        this.type = EventType.fromValue(tokens.get(0));
        if (this.type == EventType.MESSAGE) {
            this.args = new ArrayList<>(tokens.subList(0, tokens.size()));
        } else {
            this.args = tokens.size() > 1 ? new ArrayList<>(tokens.subList(1, tokens.size())) : null;
        }
        this.author = author != null ? author : "Guest";;
        this.time = TimeUtil.getCurrentTime();
    }

    public Event(@NotNull String text) {
        List<String> tokens = Arrays.asList(text.split(" "));
        this.type = EventType.fromValue(tokens.get(0));
        this.author = tokens.get(1);
        this.args = tokens.size() > 2 ? new ArrayList<>(tokens.subList(2, tokens.size())) : null;
        this.time = TimeUtil.getCurrentTime();
    }

    @Override
    public String toString() {
        String event = this.type.getValue() + " " + this.author;
        if (this.args != null) {
            event += " " + StringUtils.join(this.args, " ");
        }
        return event;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }
}

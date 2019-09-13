package client.models;

import client.ConfigLoader;
import server.services.AuthService;

import java.util.*;

public class Censor {
    private List<String> forbiddenWords = Arrays.asList(ConfigLoader.load().getProperty("forbiddenWords").split(","));
    private Map<String, Integer> dangerUsers = new HashMap<>();

    public Boolean checkMessage(String author, List<String> words) {
        for (String word: words) {
            if (this.forbiddenWords.contains(word)) {
                Integer count = dangerUsers.getOrDefault(author, 0);
                dangerUsers.put(author, ++count);
            };
        }
        if (dangerUsers.get(author) != null) {
            return dangerUsers.get(author) >= 3;
        }
        return false;
    }

    public List<String> getForbiddenWords() {
        return forbiddenWords;
    }

    public void setForbiddenWords(List<String> forbiddenWords) {
        this.forbiddenWords = forbiddenWords;
    }

    public Map<String, Integer> getDangerUsers() {
        return dangerUsers;
    }

    public Integer getDangerUserForbiddenWordCount(String author) {
        return dangerUsers.get(author);
    }

    public void setDangerUsers(Map<String, Integer> dangerUsers) {
        this.dangerUsers = dangerUsers;
    }
}

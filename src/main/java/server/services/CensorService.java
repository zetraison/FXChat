package server.services;

import core.config.ConfigLoader;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CensorService {
    private static final Logger LOGGER = Logger.getLogger(CensorService.class);

    private String forbiddenWordsConfig = ConfigLoader.load().getProperty("censor.forbiddenWords");
    private List<String> forbiddenWords = Arrays.asList(forbiddenWordsConfig.split(","));
    private Map<String, Integer> dangerUsers = new HashMap<>();

    /**
     * Check user message
     *
     * @param author username of message author
     * @param words author message
     * @return boolean value of result author message checking
     */
    public Boolean checkMessage(String author, List<String> words) {
        for (String word: words) {
            if (this.forbiddenWords.contains(word)) {
                LOGGER.info("User " + author + " used forbidden words!");
                Integer count = dangerUsers.getOrDefault(author, 0);
                dangerUsers.put(author, ++count);
            };
        }
        if (dangerUsers.get(author) != null) {
            return dangerUsers.get(author) >= 3;
        }
        return false;
    }
}

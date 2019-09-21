package server.services;

import java.util.*;

public class ArrayService {
    private static final int NUMBER_1 = 1;
    private static final int NUMBER_4 = 4;

    public Integer[] erase(Integer[] array) {
        List<Integer> list = Arrays.asList(array);
        if (list.indexOf(NUMBER_4) < 0) {
            throw new RuntimeException();
        }
        return list.subList(list.lastIndexOf(NUMBER_4) + 1, list.size()).toArray(new Integer[0]);
    }

    public boolean check(Integer[] array) {
        List<Integer> list = Arrays.asList(array);
        return list.indexOf(NUMBER_1) >= 0 && list.indexOf(NUMBER_4) >= 0;
    }

    public Map.Entry<Character, Integer> find(String str) {
        HashMap<Character, Integer> map = new HashMap<>();
        for (int i = 0; i < str.length(); i++) {
            Integer count = map.getOrDefault(str.charAt(i), 0);
            map.put(str.charAt(i), ++count);
        }
        for (Map.Entry<Character, Integer> entry : map.entrySet()) {
            Character key = entry.getKey();
            Integer value = entry.getValue();
            if (value == 1) {
                return new AbstractMap.SimpleEntry<>(key, str.indexOf(key));
            }
        }
        return null;
    }
}

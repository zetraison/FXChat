package server.services;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@RunWith(Parameterized.class)
public class ArrayServiceFindTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {"sssssssAsssssssBssssssssC", 'A', 7},
                {"ssssssssssssssBssssssssC", 'B', 14}
        });
    }

    private String input;
    private Character c;
    private Integer index;

    public ArrayServiceFindTest(String input, Character c, Integer index) {
        this.input = input;
        this.c = c;
        this.index = index;
    }

    private static ArrayService arrayService;

    @Before
    public void init() {
        arrayService = new ArrayService();
    }

    @Test
    public void checkTest() {
        Map.Entry<Character, Integer> entry = arrayService.find(input);
        Assert.assertEquals(c, entry.getKey());
        Assert.assertEquals(index, entry.getValue());
    }
}
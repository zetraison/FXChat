package server.services;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ArrayServiceCheckTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new Integer[]{1, 1, 1, 4, 4, 1, 4, 4}, true},
                {new Integer[]{1, 1, 1, 1, 1, 1}, false},
                {new Integer[]{4, 4, 4, 4}, false}
        });
    }

    private Integer[] input;
    private boolean checkResult;

    public ArrayServiceCheckTest(Integer[] input, boolean checkResult) {
        this.input = input;
        this.checkResult = checkResult;
    }

    private static ArrayService arrayService;

    @Before
    public void init() {
        arrayService = new ArrayService();
    }

    @Test
    public void checkTest() {
        Assert.assertEquals(checkResult, arrayService.check(input));
    }
}
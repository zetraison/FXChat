package server.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

@RunWith(Parameterized.class)
public class ArrayServiceEraseTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {new Integer[]{1, 2, 4, 4, 2, 3, 4, 1, 7}, new Integer[]{1,7}},
                {new Integer[]{4, 2, 5, 7, 2, 3, 3}, new Integer[]{2, 5, 7, 2, 3, 3}},
                {new Integer[]{1, 2, 4, 4, 2, 3, 4}, new Integer[]{}}
        });
    }

    private Integer[] input, output;

    public ArrayServiceEraseTest(Integer[] input, Integer[] output) {
        this.input = input;
        this.output = output;
    }

    private static ArrayService arrayService;

    @Before
    public void init() {
        arrayService = new ArrayService();
    }

    @Test
    public void eraseTest() {
        Assert.assertArrayEquals(output, arrayService.erase(input));
    }

    @Test(expected = RuntimeException.class)
    public void eraseExceptionTest() {
        arrayService.erase(new Integer[]{});
    }
}
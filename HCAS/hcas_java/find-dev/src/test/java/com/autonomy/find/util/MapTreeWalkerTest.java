package com.autonomy.find.util;


import com.google.gson.internal.Pair;

import static junit.framework.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.autonomy.find.util.CollUtils.*;

public class MapTreeWalkerTest {

    @SuppressWarnings("unchecked")
    private final Map<String, Object> data = pairMap(
            new Pair<String, Object>("a", "19"),
            new Pair<String, Object>("b", 42),
            new Pair<String, Object>("c", pairMap(
                    new Pair<String, Object>("foo", "bar"),
                    new Pair<String, Object>("baz", pairMap(
                            new Pair<String, Object>("14", "val")
                    )),
                    new Pair<String, Object>("zuux", "xuuz")
            ))
    );

    private final MapTreeWalker readOnlyWalker = Mockito.spy(new MapTreeWalker(data));


    private Pair<List<String>, Object> checkPair(final List<String> first, final Object second) {
        return new Pair<>(first, second);
    }

    private void checkSelectionPairs(final List<Pair<List<String>, Object>> checks) {
        for (final Pair<List<String>, Object> check : checks) {
            assertEquals(check.second, readOnlyWalker.select(check.first));
        }
    }


    @Before
    public void setup() {
        Mockito.doThrow(new RuntimeException("Walker instance is read-only."))
                .when(readOnlyWalker).update(Mockito.anyList(), Mockito.any());
    }


    @Test
    public void testMapTreeWalkerUpdate() {
        final MapTreeWalker walker = new MapTreeWalker(new HashMap<String, Object>());

        walker.update(list("added:0", "added:0:0"), 192);
        assertEquals(192, walker.select(list("added:0", "added:0:0")));

        walker.update(list("added:0", "added:0:1"), 84);
        assertEquals(84, walker.select(list("added:0", "added:0:1")));

        assertEquals(pairMap(
                new Pair<String, Object>("added:0:0", 192),
                new Pair<String, Object>("added:0:1", 84)
        ), walker.select(list("added:0")));
    }


    @Test
    public void testMapTreeWalkerSelect() {
        checkSelectionPairs(list(
                checkPair(list("a"), "19"),
                checkPair(list("b"), 42),
                checkPair(list("c", "zuux"), "xuuz"),
                checkPair(list("c", "baz", "14"), "val")
        ));
    }


    @Test(expected = RuntimeException.class)
    public void testMapTreeWalkerSelectErrorBlank() {
        readOnlyWalker.select(CollUtils.<String>list());
    }


    @Test(expected = RuntimeException.class)
    public void testMapTreeWalkerSelectErrorSingle() {
        readOnlyWalker.select(list("invalid"));
    }


    @Test(expected = RuntimeException.class)
    public void testMapTreeWalkerSelectErrorPath() {
        readOnlyWalker.select(list("invalid", "path"));
    }
}

package com.autonomy.find.util;


import junit.framework.Assert;
import org.junit.Test;

public class MTreeTest {

    @Test
    public void nullEqualityTest() {
        final MTree<Integer> one, two;
        one = MTree.node(null);
        two = MTree.node(null);
        Assert.assertEquals(one, two);
    }

    @Test
    public void singleEqualityTest() {
        final MTree<Integer> one, two;
        one = MTree.node(42);
        two = MTree.node(42);
        Assert.assertEquals(one, two);
    }

    @Test
    public void diffEqualityTest() {
        final MTree<Integer> one, two;
        one = MTree.node(42);
        two = MTree.node(37);
        Assert.assertFalse(one.equals(two));
    }

    @Test
    public void structureEqualityTest() {
        final MTree<Integer> one, two;
        one = MTree.node(42, MTree.node(36, MTree.node(27)), MTree.node(27));
        two = MTree.node(42, MTree.node(36, MTree.node(27)), MTree.node(27));
        Assert.assertEquals(one, two);
    }

    @Test
    public void functorEqualityTest() {
        final MTree<Integer> before, after, check;
        before = MTree.node(1,
                MTree.node(2,
                        MTree.node(3,
                                MTree.node(4),
                                MTree.node(4))),
                MTree.node(2,
                        MTree.node(3),
                        MTree.node(3,
                                MTree.node(4)),
                        MTree.node(3)));

        check = MTree.node(10,
                MTree.node(20,
                        MTree.node(30,
                                MTree.node(40),
                                MTree.node(40))),
                MTree.node(20,
                        MTree.node(30),
                        MTree.node(30,
                                MTree.node(40)),
                        MTree.node(30)));

        after = before.fmap(new F1<Integer, Integer>() {
            public Integer apply(final Integer v) {
                return v * 10;
            }
        });

        Assert.assertEquals(after, check);
    }
}

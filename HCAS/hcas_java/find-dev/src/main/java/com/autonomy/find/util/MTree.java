package com.autonomy.find.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class MTree<A>
        implements MultiTree<A>, Functor<A> {
    private final A element;
    private final List<MTree<A>> children;

    public A getElement() {
        return this.element;
    }

    public List<MTree<A>> getChildren() {
        return this.children;
    }

    /**
     * Builds a string representation of the tree.
     *
     * @return
     */
    public String toString() {
        return "<" + element.toString() + (children.isEmpty() ? "" : "." + children.toString()) + ">";
    }


    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final MTree mTree = (MTree) o;

        if (!children.equals(mTree.children)) return false;
        if (element != null ? !element.equals(mTree.element) : mTree.element != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = element != null ? element.hashCode() : 0;
        result = 31 * result + children.hashCode();
        return result;
    }

    /**
     * Maps a function object f over the nodes in the tree.
     *
     * @param f   - The function object
     * @param <B> - New node type B
     * @return New Tree
     */
    public <B> MTree<B> fmap(
            final F1<A, B> f
    ) {
        return node(f.apply(getElement()),
                CollUtils.map(new F1<MTree<A>, MTree<B>>() {
                    public MTree<B> apply(final MTree<A> tree) {
                        return tree.fmap(f);
                    }
                }, getChildren()));
    }

    /**
     * Constructor, setups a new tree.
     *
     * @param element
     * @param children
     */
    public MTree(
            final A element,
            final List<MTree<A>> children
    ) {
        this.element = element;
        this.children = (children == null) ? new LinkedList<MTree<A>>() : children;
    }

    /**
     * Static utility for creating a tree.
     *
     * @param element  - Root node
     * @param children - List of children
     * @param <A>      - Type of nodes
     * @return
     */
    public static <A> MTree<A> node(
            final A element,
            final List<MTree<A>> children
    ) {
        return new MTree<>(element, children);
    }

    /**
     * Static utility for creating a tree.
     *
     * @param element  - Root node
     * @param children - Array Sequence of trees.
     * @param <A>      - Type of nodes
     * @return
     */
    public static <A> MTree<A> node(
            final A element,
            final MTree<A>... children
    ) {
        return new MTree<>(element, new LinkedList<>(Arrays.asList(children)));
    }

}
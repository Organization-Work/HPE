package com.autonomy.find.util;


import java.util.List;

public interface MultiTree<A> {
    A getElement();

    List<? extends MultiTree<? extends A>> getChildren();
}

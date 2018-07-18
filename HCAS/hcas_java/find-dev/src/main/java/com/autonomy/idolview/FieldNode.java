package com.autonomy.idolview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/FieldNode.java#1 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
public class FieldNode {
    public String name;
    public int size;
    public List<FieldNode> children;

    public FieldNode() {
    }

    public FieldNode(final String name, final int size, final List<FieldNode> children) {
        this.name = name;
        this.size = size;
        this.children = children;
    }

    public FieldNode(final String name, final int size, final FieldNode... children) {
        this.name = name;
        this.size = size;
        this.children = new ArrayList<FieldNode>(Arrays.asList(children));
    }
}

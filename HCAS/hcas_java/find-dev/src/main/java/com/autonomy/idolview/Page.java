package com.autonomy.idolview;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/Page.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
@Controller
public class Page {
    @RequestMapping("/p/viewer.do")
    void idolview() {}

    @RequestMapping("/resources/js/Autn/i18n.js")
    String i18n() {
        return "/js/i18n";
    }
}

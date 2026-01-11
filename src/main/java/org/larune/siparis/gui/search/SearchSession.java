package org.larune.siparis.gui.search;

import org.larune.siparis.model.CategoryDef;

public class SearchSession {
    public final CategoryDef category;
    public final int backPage;

    public SearchSession(CategoryDef category, int backPage) {
        this.category = category;
        this.backPage = backPage;
    }
}

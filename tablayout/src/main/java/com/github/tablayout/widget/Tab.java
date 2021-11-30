package com.github.tablayout.widget;

import com.github.tablayout.listener.ITab;

/**
 * author : qfxl
 * e-mail : xuyonghong0822@gmail.com
 * time   : 2021/11/30
 * desc   :
 * version: 1.0
 */

public class Tab implements ITab {
    private final String title;
    private final int icon;
    private final int iconSelected;

    public Tab(String title, int icon, int iconSelected) {
        this.title = title;
        this.icon = icon;
        this.iconSelected = iconSelected;
    }

    @Override
    public String getTabTitle() {
        return title;
    }

    @Override
    public int getTabSelectedIcon() {
        return iconSelected;
    }

    @Override
    public int getTabUnselectedIcon() {
        return icon;
    }
}

package com.github.tablayoutsamples.entity;

import com.github.tablayout.listener.ITab;

public class Tab implements ITab {
    public String title;
    public int selectedIcon;
    public int unSelectedIcon;

    public Tab(String title, int selectedIcon, int unSelectedIcon) {
        this.title = title;
        this.selectedIcon = selectedIcon;
        this.unSelectedIcon = unSelectedIcon;
    }

    @Override
    public String getTabTitle() {
        return title;
    }

    @Override
    public int getTabSelectedIcon() {
        return selectedIcon;
    }

    @Override
    public int getTabUnselectedIcon() {
        return unSelectedIcon;
    }
}

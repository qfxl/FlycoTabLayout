package com.github.tablayout.listener;

import androidx.annotation.DrawableRes;

public interface CustomTabEntity {
    /**
     * 获取tabTitle
     * @return
     */
    String getTabTitle();

    /**
     * 获取Tab选中icon
     * @return
     */
    @DrawableRes
    int getTabSelectedIcon();

    /**
     * 获取Tab未选中icon
     * @return
     */
    @DrawableRes
    int getTabUnselectedIcon();
}
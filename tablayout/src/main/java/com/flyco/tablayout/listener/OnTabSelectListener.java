package com.flyco.tablayout.listener;

public interface OnTabSelectListener {
    /**
     * tab选中
     * @param position
     */
    void onTabSelect(int position);

    /**
     * tab重新选中
     * @param position
     */
    void onTabReselect(int position);
}
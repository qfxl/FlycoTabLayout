package com.flyco.tablayout.utils;

import android.content.Context;

/**
 * author : qfxl
 * e-mail : xuyonghong0822@gmail.com
 * time   : 2021/11/12
 * desc   :
 * version: 1.0
 */

public class DimensionUtils {

    /**
     * dp转px
     * @param context
     * @param dp
     * @return
     */
    public static int dp2px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * sp转px
     * @param context
     * @param sp
     * @return
     */
    public static int sp2px(Context context, float sp) {
        final float scale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (sp * scale + 0.5f);
    }

}

package com.github.tablayout;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.tablayout.listener.OnTabSelectListener;
import com.github.tablayout.utils.DimensionUtils;
import com.github.tablayout.utils.FragmentChangeManager;
import com.github.tablayout.utils.UnreadMsgUtils;
import com.github.tablayout.widget.MsgView;

import java.util.ArrayList;

/**
 * segment 使用，不需要与ViewPager关联
 */
public class SegmentTabLayout extends FrameLayout implements ValueAnimator.AnimatorUpdateListener {
    private final Context mContext;
    private String[] mTitles;
    private final LinearLayout mTabsContainer;
    private int mCurrentTab;
    private int mLastTab;
    private int mTabCount;
    /* -- 用于绘制显示器 -- */
    private final Rect mIndicatorRect = new Rect();
    private final GradientDrawable mIndicatorDrawable = new GradientDrawable();
    private final GradientDrawable mRectDrawable = new GradientDrawable();

    private final Paint mDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float mTabPadding;
    private boolean mTabSpaceEqual;
    private float mTabWidth;

    /* -- indicator -- */
    private int mIndicatorColor;
    private float mIndicatorHeight;
    private float mIndicatorCornerRadius;
    private float mIndicatorMarginLeft;
    private float mIndicatorMarginTop;
    private float mIndicatorMarginRight;
    private float mIndicatorMarginBottom;
    private long mIndicatorAnimDuration;
    private boolean mIndicatorAnimEnable;
    private boolean mIndicatorBounceEnable;

    /* -- divider -- */
    private int mDividerColor;
    private float mDividerWidth;
    private float mDividerPadding;

    /* -- title -- */
    private static final int TEXT_BOLD_NONE = 0;
    private static final int TEXT_BOLD_WHEN_SELECT = 1;
    private static final int TEXT_BOLD_BOTH = 2;
    private float mTextSize;
    private float mSelectedTextSize;
    private int mTextSelectColor;
    private int mTextUnselectColor;
    private int mTextBold;
    private boolean mTextAllCaps;

    private int mBarColor;
    private int mBarStrokeColor;
    private float mBarStrokeWidth;

    private int mHeight;

    /* -- anim -- */
    private final ValueAnimator mValueAnimator;
    private final OvershootInterpolator mInterpolator = new OvershootInterpolator(0.8f);

    private FragmentChangeManager mFragmentChangeManager;
    private final float[] mRadiusArr = new float[8];

    public SegmentTabLayout(Context context) {
        this(context, null, 0);
    }

    public SegmentTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SegmentTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //重写onDraw方法,需要调用这个方法来清除flag
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        this.mContext = context;
        mTabsContainer = new LinearLayout(context);
        addView(mTabsContainer);

        obtainAttributes(context, attrs);

        //get layout_height
        String height = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "layout_height");

        //create ViewPager
        if (height.equals(ViewGroup.LayoutParams.MATCH_PARENT + "")) {
        } else if (height.equals(ViewGroup.LayoutParams.WRAP_CONTENT + "")) {
        } else {
            int[] systemAttrs = {android.R.attr.layout_height};
            TypedArray a = context.obtainStyledAttributes(attrs, systemAttrs);
            mHeight = a.getDimensionPixelSize(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            a.recycle();
        }

        mValueAnimator = ValueAnimator.ofObject(new PointEvaluator(), mLastP, mCurrentP);
        mValueAnimator.addUpdateListener(this);
    }

    private void obtainAttributes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SegmentTabLayout);

        mIndicatorColor = ta.getColor(R.styleable.SegmentTabLayout_tl_indicator_color, Color.parseColor("#222831"));
        mIndicatorHeight = ta.getDimension(R.styleable.SegmentTabLayout_tl_indicator_height, -1);
        mIndicatorCornerRadius = ta.getDimension(R.styleable.SegmentTabLayout_tl_indicator_corner_radius, -1);
        mIndicatorMarginLeft = ta.getDimension(R.styleable.SegmentTabLayout_tl_indicator_margin_left, DimensionUtils.dp2px(context,0));
        mIndicatorMarginTop = ta.getDimension(R.styleable.SegmentTabLayout_tl_indicator_margin_top, 0);
        mIndicatorMarginRight = ta.getDimension(R.styleable.SegmentTabLayout_tl_indicator_margin_right, DimensionUtils.dp2px(context,0));
        mIndicatorMarginBottom = ta.getDimension(R.styleable.SegmentTabLayout_tl_indicator_margin_bottom, 0);
        mIndicatorAnimEnable = ta.getBoolean(R.styleable.SegmentTabLayout_tl_indicator_anim_enable, false);
        mIndicatorBounceEnable = ta.getBoolean(R.styleable.SegmentTabLayout_tl_indicator_bounce_enable, true);
        mIndicatorAnimDuration = ta.getInt(R.styleable.SegmentTabLayout_tl_indicator_anim_duration, -1);

        mDividerColor = ta.getColor(R.styleable.SegmentTabLayout_tl_divider_color, mIndicatorColor);
        mDividerWidth = ta.getDimension(R.styleable.SegmentTabLayout_tl_divider_width, DimensionUtils.dp2px(context,1));
        mDividerPadding = ta.getDimension(R.styleable.SegmentTabLayout_tl_divider_padding, 0);

        mTextSize = ta.getDimension(R.styleable.SegmentTabLayout_tl_textSize, DimensionUtils.sp2px(context, 13f));
        mSelectedTextSize = ta.getDimension(R.styleable.SegmentTabLayout_tl_textSelectSize, mTextSize);
        mTextSelectColor = ta.getColor(R.styleable.SegmentTabLayout_tl_textSelectColor, Color.parseColor("#ffffff"));
        mTextUnselectColor = ta.getColor(R.styleable.SegmentTabLayout_tl_textUnselectColor, mIndicatorColor);
        mTextBold = ta.getInt(R.styleable.SegmentTabLayout_tl_textBold, TEXT_BOLD_NONE);
        mTextAllCaps = ta.getBoolean(R.styleable.SegmentTabLayout_tl_textAllCaps, false);

        mTabSpaceEqual = ta.getBoolean(R.styleable.SegmentTabLayout_tl_tab_space_equal, true);
        mTabWidth = ta.getDimension(R.styleable.SegmentTabLayout_tl_tab_width, DimensionUtils.dp2px(context,-1));
        mTabPadding = ta.getDimension(R.styleable.SegmentTabLayout_tl_tab_padding, mTabSpaceEqual || mTabWidth > 0 ? DimensionUtils.dp2px(context,0) : DimensionUtils.dp2px(context,10));

        mBarColor = ta.getColor(R.styleable.SegmentTabLayout_tl_bar_color, Color.TRANSPARENT);
        mBarStrokeColor = ta.getColor(R.styleable.SegmentTabLayout_tl_bar_stroke_color, mIndicatorColor);
        mBarStrokeWidth = ta.getDimension(R.styleable.SegmentTabLayout_tl_bar_stroke_width, DimensionUtils.dp2px(context,1));

        ta.recycle();
    }

    public void setTabData(String[] titles) {
        if (titles == null || titles.length == 0) {
            throw new IllegalStateException("Titles can not be NULL or EMPTY !");
        }

        this.mTitles = titles;

        notifyDataSetChanged();
    }

    /**
     * 关联数据支持同时切换fragments
     *
     * @param titles
     * @param fa
     * @param containerViewId
     * @param fragments
     */
    public void setTabData(String[] titles, FragmentActivity fa, int containerViewId, ArrayList<Fragment> fragments) {
        mFragmentChangeManager = new FragmentChangeManager(fa.getSupportFragmentManager(), containerViewId, fragments);
        setTabData(titles);
    }

    /**
     * 更新数据
     */
    public void notifyDataSetChanged() {
        mTabsContainer.removeAllViews();
        this.mTabCount = mTitles.length;
        View tabView;
        for (int i = 0; i < mTabCount; i++) {
            tabView = View.inflate(mContext, R.layout.layout_tab_segment, null);
            tabView.setTag(i);
            addTab(i, tabView);
        }

        updateTabStyles();
    }

    /**
     * 创建并添加tab
     *
     * @param position
     * @param tabView
     */
    private void addTab(final int position, View tabView) {
        TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
        tabTitleView.setText(mTitles[position]);

        tabView.setOnClickListener(v -> {
            int childPosition = (Integer) v.getTag();
            if (mCurrentTab != childPosition) {
                setCurrentTab(childPosition);
                if (mListener != null) {
                    mListener.onTabSelect(childPosition);
                }
            } else {
                if (mListener != null) {
                    mListener.onTabReselect(childPosition);
                }
            }
        });

        // 每一个Tab的布局参数
        LinearLayout.LayoutParams itemTabLp = mTabSpaceEqual ?
                new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f) :
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        if (mTabWidth > 0) {
            itemTabLp = new LinearLayout.LayoutParams((int) mTabWidth, LayoutParams.MATCH_PARENT);
        }
        mTabsContainer.addView(tabView, position, itemTabLp);
    }

    /**
     * 更新tab样式
     */
    private void updateTabStyles() {
        for (int i = 0; i < mTabCount; i++) {
            View tabView = mTabsContainer.getChildAt(i);
            tabView.setPadding((int) mTabPadding, 0, (int) mTabPadding, 0);
            TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
            tabTitleView.setTextColor(i == mCurrentTab ? mTextSelectColor : mTextUnselectColor);
            tabTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, i == mCurrentTab ? mSelectedTextSize : mTextSize);
//            tabTitleView.setPadding((int) mTabPadding, 0, (int) mTabPadding, 0);
            if (mTextAllCaps) {
                tabTitleView.setText(tabTitleView.getText().toString().toUpperCase());
            }

            if (mTextBold == TEXT_BOLD_BOTH) {
                tabTitleView.getPaint().setFakeBoldText(true);
            } else if (mTextBold == TEXT_BOLD_NONE) {
                tabTitleView.getPaint().setFakeBoldText(false);
            }
        }
    }

    /**
     * 更新tab选中位置
     *
     * @param position
     */
    private void updateTabSelection(int position) {
        for (int i = 0; i < mTabCount; ++i) {
            View tabView = mTabsContainer.getChildAt(i);
            final boolean isSelect = i == position;
            TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
            tabTitleView.setTextColor(isSelect ? mTextSelectColor : mTextUnselectColor);
            tabTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, isSelect ? mSelectedTextSize : mTextSize);
            if (mTextBold == TEXT_BOLD_WHEN_SELECT) {
                tabTitleView.getPaint().setFakeBoldText(isSelect);
            }
        }
    }

    private void calcOffset() {
        final View currentTabView = mTabsContainer.getChildAt(this.mCurrentTab);
        mCurrentP.left = currentTabView.getLeft();
        mCurrentP.right = currentTabView.getRight();

        final View lastTabView = mTabsContainer.getChildAt(this.mLastTab);
        mLastP.left = lastTabView.getLeft();
        mLastP.right = lastTabView.getRight();
        if (mLastP.left == mCurrentP.left && mLastP.right == mCurrentP.right) {
            invalidate();
        } else {
            mValueAnimator.setObjectValues(mLastP, mCurrentP);
            if (mIndicatorBounceEnable) {
                mValueAnimator.setInterpolator(mInterpolator);
            }

            if (mIndicatorAnimDuration < 0) {
                mIndicatorAnimDuration = mIndicatorBounceEnable ? 500 : 250;
            }
            mValueAnimator.setDuration(mIndicatorAnimDuration);
            mValueAnimator.start();
        }
    }

    private void calcIndicatorRect() {
        View currentTabView = mTabsContainer.getChildAt(this.mCurrentTab);
        float left = currentTabView.getLeft();
        float right = currentTabView.getRight();

        mIndicatorRect.left = (int) left;
        mIndicatorRect.right = (int) right;

        if (!mIndicatorAnimEnable) {
            if (mCurrentTab == 0) {
                // The corners are ordered top-left, top-right, bottom-right, bottom-left
                mRadiusArr[0] = mIndicatorCornerRadius;
                mRadiusArr[1] = mIndicatorCornerRadius;
                mRadiusArr[2] = 0;
                mRadiusArr[3] = 0;
                mRadiusArr[4] = 0;
                mRadiusArr[5] = 0;
                mRadiusArr[6] = mIndicatorCornerRadius;
                mRadiusArr[7] = mIndicatorCornerRadius;
            } else if (mCurrentTab == mTabCount - 1) {
                // The corners are ordered top-left, top-right, bottom-right, bottom-left
                mRadiusArr[0] = 0;
                mRadiusArr[1] = 0;
                mRadiusArr[2] = mIndicatorCornerRadius;
                mRadiusArr[3] = mIndicatorCornerRadius;
                mRadiusArr[4] = mIndicatorCornerRadius;
                mRadiusArr[5] = mIndicatorCornerRadius;
                mRadiusArr[6] = 0;
                mRadiusArr[7] = 0;
            } else {
                // The corners are ordered top-left, top-right, bottom-right, bottom-left
                mRadiusArr[0] = 0;
                mRadiusArr[1] = 0;
                mRadiusArr[2] = 0;
                mRadiusArr[3] = 0;
                mRadiusArr[4] = 0;
                mRadiusArr[5] = 0;
                mRadiusArr[6] = 0;
                mRadiusArr[7] = 0;
            }
        } else {
            // The corners are ordered top-left, top-right, bottom-right, bottom-left
            mRadiusArr[0] = mIndicatorCornerRadius;
            mRadiusArr[1] = mIndicatorCornerRadius;
            mRadiusArr[2] = mIndicatorCornerRadius;
            mRadiusArr[3] = mIndicatorCornerRadius;
            mRadiusArr[4] = mIndicatorCornerRadius;
            mRadiusArr[5] = mIndicatorCornerRadius;
            mRadiusArr[6] = mIndicatorCornerRadius;
            mRadiusArr[7] = mIndicatorCornerRadius;
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        IndicatorPoint p = (IndicatorPoint) animation.getAnimatedValue();
        mIndicatorRect.left = (int) p.left;
        mIndicatorRect.right = (int) p.right;
        invalidate();
    }

    private boolean mIsFirstDraw = true;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode() || mTabCount <= 0) {
            return;
        }

        int height = getHeight();
        int paddingLeft = getPaddingLeft();

        if (mIndicatorHeight < 0) {
            mIndicatorHeight = height - mIndicatorMarginTop - mIndicatorMarginBottom;
        }

        if (mIndicatorCornerRadius < 0 || mIndicatorCornerRadius > mIndicatorHeight / 2) {
            mIndicatorCornerRadius = mIndicatorHeight / 2;
        }

        // draw rect
        mRectDrawable.setColor(mBarColor);
        mRectDrawable.setStroke((int) mBarStrokeWidth, mBarStrokeColor);
        mRectDrawable.setCornerRadius(mIndicatorCornerRadius);
        mRectDrawable.setBounds(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(), getHeight() - getPaddingBottom());
        mRectDrawable.draw(canvas);

        // draw divider
        if (!mIndicatorAnimEnable && mDividerWidth > 0) {
            mDividerPaint.setStrokeWidth(mDividerWidth);
            mDividerPaint.setColor(mDividerColor);
            for (int i = 0; i < mTabCount - 1; i++) {
                View tab = mTabsContainer.getChildAt(i);
                canvas.drawLine(paddingLeft + tab.getRight(), mDividerPadding, paddingLeft + tab.getRight(), height - mDividerPadding, mDividerPaint);
            }
        }


        // draw indicator line
        if (mIndicatorAnimEnable) {
            if (mIsFirstDraw) {
                mIsFirstDraw = false;
                calcIndicatorRect();
            }
        } else {
            calcIndicatorRect();
        }

        mIndicatorDrawable.setColor(mIndicatorColor);
        mIndicatorDrawable.setBounds(paddingLeft + (int) mIndicatorMarginLeft + mIndicatorRect.left,
                (int) mIndicatorMarginTop, (int) (paddingLeft + mIndicatorRect.right - mIndicatorMarginRight),
                (int) (mIndicatorMarginTop + mIndicatorHeight));
        mIndicatorDrawable.setCornerRadii(mRadiusArr);
        mIndicatorDrawable.draw(canvas);

    }

    /* -- setter and getter -- */
    public void setCurrentTab(int currentTab) {
        mLastTab = this.mCurrentTab;
        this.mCurrentTab = currentTab;
        updateTabSelection(currentTab);
        if (mFragmentChangeManager != null) {
            mFragmentChangeManager.setFragments(currentTab);
        }
        if (mIndicatorAnimEnable) {
            calcOffset();
        } else {
            invalidate();
        }
    }

    public void setTabPadding(float tabPadding) {
        this.mTabPadding = DimensionUtils.dp2px(getContext(), tabPadding);
        updateTabStyles();
    }

    public void setTabSpaceEqual(boolean tabSpaceEqual) {
        this.mTabSpaceEqual = tabSpaceEqual;
        updateTabStyles();
    }

    public void setTabWidth(float tabWidth) {
        this.mTabWidth = DimensionUtils.dp2px(getContext(), tabWidth);
        updateTabStyles();
    }

    public void setIndicatorColor(int indicatorColor) {
        this.mIndicatorColor = indicatorColor;
        invalidate();
    }

    public void setIndicatorHeight(float indicatorHeight) {
        this.mIndicatorHeight = DimensionUtils.dp2px(getContext(), indicatorHeight);
        invalidate();
    }

    public void setIndicatorCornerRadius(float indicatorCornerRadius) {
        this.mIndicatorCornerRadius = DimensionUtils.dp2px(getContext(), indicatorCornerRadius);
        invalidate();
    }

    public void setIndicatorMargin(float indicatorMarginLeft, float indicatorMarginTop,
                                   float indicatorMarginRight, float indicatorMarginBottom) {
        this.mIndicatorMarginLeft = DimensionUtils.dp2px(getContext(), indicatorMarginLeft);
        this.mIndicatorMarginTop = DimensionUtils.dp2px(getContext(), indicatorMarginTop);
        this.mIndicatorMarginRight = DimensionUtils.dp2px(getContext(), indicatorMarginRight);
        this.mIndicatorMarginBottom = DimensionUtils.dp2px(getContext(), indicatorMarginBottom);
        invalidate();
    }

    public void setIndicatorAnimDuration(long indicatorAnimDuration) {
        this.mIndicatorAnimDuration = indicatorAnimDuration;
    }

    public void setIndicatorAnimEnable(boolean indicatorAnimEnable) {
        this.mIndicatorAnimEnable = indicatorAnimEnable;
    }

    public void setIndicatorBounceEnable(boolean indicatorBounceEnable) {
        this.mIndicatorBounceEnable = indicatorBounceEnable;
    }

    public void setDividerColor(int dividerColor) {
        this.mDividerColor = dividerColor;
        invalidate();
    }

    public void setDividerWidth(float dividerWidth) {
        this.mDividerWidth = DimensionUtils.dp2px(getContext(), dividerWidth);
        invalidate();
    }

    public void setDividerPadding(float dividerPadding) {
        this.mDividerPadding = DimensionUtils.dp2px(getContext(), dividerPadding);
        invalidate();
    }

    public void setTextSize(float textSize) {
        this.mTextSize = DimensionUtils.sp2px(getContext(),textSize);
        updateTabStyles();
    }

    public void setTextSelectedSize(float textSize) {
        this.mSelectedTextSize = DimensionUtils.sp2px(getContext(),textSize);
        updateTabStyles();
    }

    public void setTextSelectColor(int textSelectColor) {
        this.mTextSelectColor = textSelectColor;
        updateTabStyles();
    }

    public void setTextUnselectColor(int textUnselectColor) {
        this.mTextUnselectColor = textUnselectColor;
        updateTabStyles();
    }

    public void setTextBold(int textBold) {
        this.mTextBold = textBold;
        updateTabStyles();
    }

    public void setTextAllCaps(boolean textAllCaps) {
        this.mTextAllCaps = textAllCaps;
        updateTabStyles();
    }

    public int getTabCount() {
        return mTabCount;
    }

    public int getCurrentTab() {
        return mCurrentTab;
    }

    public float getTabPadding() {
        return mTabPadding;
    }

    public boolean isTabSpaceEqual() {
        return mTabSpaceEqual;
    }

    public float getTabWidth() {
        return mTabWidth;
    }

    public int getIndicatorColor() {
        return mIndicatorColor;
    }

    public float getIndicatorHeight() {
        return mIndicatorHeight;
    }

    public float getIndicatorCornerRadius() {
        return mIndicatorCornerRadius;
    }

    public float getIndicatorMarginLeft() {
        return mIndicatorMarginLeft;
    }

    public float getIndicatorMarginTop() {
        return mIndicatorMarginTop;
    }

    public float getIndicatorMarginRight() {
        return mIndicatorMarginRight;
    }

    public float getIndicatorMarginBottom() {
        return mIndicatorMarginBottom;
    }

    public long getIndicatorAnimDuration() {
        return mIndicatorAnimDuration;
    }

    public boolean isIndicatorAnimEnable() {
        return mIndicatorAnimEnable;
    }

    public boolean isIndicatorBounceEnable() {
        return mIndicatorBounceEnable;
    }

    public int getDividerColor() {
        return mDividerColor;
    }

    public float getDividerWidth() {
        return mDividerWidth;
    }

    public float getDividerPadding() {
        return mDividerPadding;
    }

    public float getTextsize() {
        return mTextSize;
    }

    public int getTextSelectColor() {
        return mTextSelectColor;
    }

    public int getTextUnselectColor() {
        return mTextUnselectColor;
    }

    public int getTextBold() {
        return mTextBold;
    }

    public boolean isTextAllCaps() {
        return mTextAllCaps;
    }

    public TextView getTitleView(int tab) {
        View tabView = mTabsContainer.getChildAt(tab);
        return (TextView) tabView.findViewById(R.id.tv_tab_title);
    }


    // show MsgTipView
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SparseBooleanArray mInitSetMap = new SparseBooleanArray();

    /**
     * 显示未读消息
     *
     * @param position 显示tab位置
     * @param num      num小于等于0显示红点,num大于0显示数字
     */
    public void showMsg(int position, int num) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }

        View tabView = mTabsContainer.getChildAt(position);
        MsgView tipView = (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
        if (tipView != null) {
            UnreadMsgUtils.show(tipView, num);

            if (mInitSetMap.get(position)) {
                return;
            }

            setMsgMargin(position, 2, 2);

            mInitSetMap.put(position, true);
        }
    }

    /**
     * 显示未读红点
     *
     * @param position 显示tab位置
     */
    public void showDot(int position) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }
        showMsg(position, 0);
    }

    /**
     * 隐藏未读消息
     *
     * @param position
     */
    public void hideMsg(int position) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }

        View tabView = mTabsContainer.getChildAt(position);
        MsgView tipView = (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
        if (tipView != null) {
            tipView.setVisibility(View.GONE);
        }
    }

    /**
     * 设置提示红点偏移,注意
     * 1.控件为固定高度:参照点为tab内容的右上角
     * 2.控件高度不固定(WRAP_CONTENT):参照点为tab内容的右上角,此时高度已是红点的最高显示范围,所以这时bottomPadding其实就是topPadding
     */
    public void setMsgMargin(int position, float leftPadding, float bottomPadding) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }
        View tabView = mTabsContainer.getChildAt(position);
        MsgView tipView = (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
        if (tipView != null) {
            TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
            mTextPaint.setTextSize(mTextSize);
            float textWidth = mTextPaint.measureText(tabTitleView.getText().toString());
            float textHeight = mTextPaint.descent() - mTextPaint.ascent();
            MarginLayoutParams lp = (MarginLayoutParams) tipView.getLayoutParams();

            lp.leftMargin = DimensionUtils.dp2px(getContext(), leftPadding);
            lp.topMargin = mHeight > 0 ? (int) (mHeight - textHeight) / 2 - DimensionUtils.dp2px(getContext(), bottomPadding) : DimensionUtils.dp2px(getContext(), bottomPadding);

            tipView.setLayoutParams(lp);
        }
    }

    /**
     * 当前类只提供了少许设置未读消息属性的方法,可以通过该方法获取MsgView对象从而各种设置
     *
     * @param position
     * @return
     */
    public MsgView getMsgView(int position) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }
        View tabView = mTabsContainer.getChildAt(position);
        return (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
    }

    private OnTabSelectListener mListener;

    public void setOnTabSelectListener(OnTabSelectListener listener) {
        this.mListener = listener;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putInt("mCurrentTab", mCurrentTab);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mCurrentTab = bundle.getInt("mCurrentTab");
            state = bundle.getParcelable("instanceState");
            if (mCurrentTab != 0 && mTabsContainer.getChildCount() > 0) {
                updateTabSelection(mCurrentTab);
            }
        }
        super.onRestoreInstanceState(state);
    }

    static class IndicatorPoint {
        public float left;
        public float right;
    }

    private final IndicatorPoint mCurrentP = new IndicatorPoint();
    private final IndicatorPoint mLastP = new IndicatorPoint();

    static class PointEvaluator implements TypeEvaluator<IndicatorPoint> {
        @Override
        public IndicatorPoint evaluate(float fraction, IndicatorPoint startValue, IndicatorPoint endValue) {
            float left = startValue.left + fraction * (endValue.left - startValue.left);
            float right = startValue.right + fraction * (endValue.right - startValue.right);
            IndicatorPoint point = new IndicatorPoint();
            point.left = left;
            point.right = right;
            return point;
        }
    }
}

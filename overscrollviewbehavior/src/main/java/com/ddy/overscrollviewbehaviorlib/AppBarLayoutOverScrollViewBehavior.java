package com.ddy.overscrollviewbehaviorlib;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * 下拉图片放大
 * Created by Administrator on 2018/3/5 0005.
 */

public class AppBarLayoutOverScrollViewBehavior extends AppBarLayout.Behavior {
    private static final String TAG = "overScroll";
    private static final float TARGET_HEIGHT = 1500;
    private View mTargetView;
    private int mParentHeight;
    private int mTargetViewHeight;
    private float mTotalDy;
    private float mLastScale;
    private int mLastBottom;
    private boolean isAnimate;
    private boolean isRecovering = false;//是否正在自动回弹中

    private final float MAX_REFRESH_LIMIT = 0.3f;//达到这个下拉临界值就开始刷新动画

    public AppBarLayoutOverScrollViewBehavior() {
    }

    public AppBarLayoutOverScrollViewBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, AppBarLayout abl, int layoutDirection) {
        boolean handled = super.onLayoutChild(parent, abl, layoutDirection);

//        if (mToolBar == null) {
//            mToolBar = (Toolbar) parent.findViewWithTag(TAG_TOOLBAR);
//        }
//        if (middleLayout == null) {
//            middleLayout = (ViewGroup) parent.findViewWithTag(TAG_MIDDLE);
//        }
        // 需要在调用过super.onLayoutChild()方法之后获取
        if (mTargetView == null) {
            mTargetView = parent.findViewWithTag(TAG);
            if (mTargetView != null) {
                initial(abl);
            }
        }
//        abl.addOnOffsetChangedListener((appBarLayout, i) -> {
////                mToolBar.setAlpha(Float.valueOf(Math.abs(i)) / Float.valueOf(appBarLayout.getTotalScrollRange()));
//
//        });
        return handled;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child, View directTargetChild, View target, int nestedScrollAxes) {
        isAnimate = true;
        Log.e("onNestedPreFling", "onStartNestedScroll  ");
        return super.onStartNestedScroll(parent, child, directTargetChild, target, nestedScrollAxes);
    }

    /**
     * 2重写onNestedPreScroll()修改AppBarLayou滑动的顶部后的行为
     * 3.上滑处理
     下滑时目标View放大，AppBarLayout变高，如果此时用户不松开手指，直接上滑，需要目标View缩小，并且AppBarLayout变高。
     默认情况下AppBarLayout的滑动是通过修改top和bottom实现的，所以上滑时，AppBarLayout为整体向上移动，高度不会发生改变
     ，并且AppBarLayout下面的ScrollView也会向上滚动；而我们需要的是在AppBarLayout的高度大于原始高度时
     ，减小AppBarLayout的高度，top不发生改变，并且AppBarLayout下面的ScrollView不会向上滚动。
     AppBarLayout上滑时不会调用onNestedScroll()，所以只能在onNestedPreScroll()方法中修改，这也是为什么选择onNestedPreScroll()方法的原因
     * @param coordinatorLayout
     * @param child
     * @param target
     * @param dx
     * @param dy
     * @param consumed
     */
    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, int dx, int dy, int[] consumed) {
        if (!isRecovering) {
            if (mTargetView != null && ((dy < 0 && child.getBottom() >= mParentHeight)
                    || (dy > 0 && child.getBottom() > mParentHeight))) {
                scale(child, target, dy);
                return;
            }
        }
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);

    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, AppBarLayout child, View target, float velocityX, float velocityY) {
        if (velocityY > 100) {//当y速度>100,就秒弹回
            Log.e("onNestedPreFling", "onNestedPreFling  velocityY:  " + velocityY);
            isAnimate = false;
        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }


    /**
     * 4.还原
     * 当AppBarLayout处于越界时，如果用户松开手指，此时应该让目标View和AppBarLayout都还原到原始状态，重写onStopNestedScroll()方法
     *
     * @param coordinatorLayout
     * @param abl
     * @param target
     */
    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, AppBarLayout abl, View target) {
        recovery(abl);
        super.onStopNestedScroll(coordinatorLayout, abl, target);
    }

    private void initial(AppBarLayout abl) {
        abl.setClipChildren(false);
        mParentHeight = abl.getHeight();
        mTargetViewHeight = mTargetView.getHeight();
    }

    private void scale(AppBarLayout abl, View target, int dy) {
        mTotalDy += -dy;
        mTotalDy = Math.min(mTotalDy, TARGET_HEIGHT);

        mLastScale = Math.max(1f, 1f + mTotalDy / TARGET_HEIGHT);
        ViewCompat.setScaleX(mTargetView, mLastScale);
        ViewCompat.setScaleY(mTargetView, mLastScale);
        mLastBottom = mParentHeight + (int) (mTargetViewHeight / 2 * (mLastScale - 1));
        abl.setBottom(mLastBottom);
        target.setScrollY(0);
        if (onProgressChangeListener != null) {
            float progress = Math.min((mLastScale - 1) / MAX_REFRESH_LIMIT, 1);//计算0~1的进度
            onProgressChangeListener.onProgressChange(progress, false);
            Log.e("ssss", "onProgressChange");
        }
    }

    private void recovery(final AppBarLayout abl) {
        if (isRecovering) return;
        if (mTotalDy > 0) {
            isRecovering = true;
            mTotalDy = 0;
            if (isAnimate) {
                ValueAnimator anim = ValueAnimator.ofFloat(mLastScale, 1f).setDuration(200);
                anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        ViewCompat.setScaleX(mTargetView, value);
                        ViewCompat.setScaleY(mTargetView, value);
                        abl.setBottom((int) (mLastBottom - (mLastBottom - mParentHeight) * animation.getAnimatedFraction()));

                        if (onProgressChangeListener != null) {
                            float progress = Math.min((value - 1) / MAX_REFRESH_LIMIT, 1);//计算0~1的进度
                            onProgressChangeListener.onProgressChange(progress, true);
                        }
                    }
                });
                anim.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isRecovering = false;
                        if (onRefreshListener != null) {
                            onRefreshListener.onRefresh();
                        }
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                });
                anim.start();
            } else {
                ViewCompat.setScaleX(mTargetView, 1f);
                ViewCompat.setScaleY(mTargetView, 1f);
                abl.setBottom(mParentHeight);
                isRecovering = false;
                if (onProgressChangeListener != null) {
                    onProgressChangeListener.onProgressChange(0, true);
                }
            }
        }
    }

    private onProgressChangeListener onProgressChangeListener;

    public interface onProgressChangeListener {
        /**
         * 范围 0~1
         *
         * @param progress  progress
         * @param isRelease 是否是释放状态
         */
        void onProgressChange(float progress, boolean isRelease);
    }

    public void setOnProgressChangeListener(AppBarLayoutOverScrollViewBehavior.onProgressChangeListener onProgressChangeListener) {
        this.onProgressChangeListener = onProgressChangeListener;
    }

    private onRefreshListener onRefreshListener;

    public interface onRefreshListener {
        void onRefresh();
    }

    public void setOnRefreshListener(AppBarLayoutOverScrollViewBehavior.onRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }
}
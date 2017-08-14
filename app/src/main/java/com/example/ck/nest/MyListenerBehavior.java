package com.example.ck.nest;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.lang.ref.SoftReference;

/**
 * Created by ck on 2017/8/14.
 */

public class MyListenerBehavior extends CoordinatorLayout.Behavior {

    public static final String TAG = "MyBehavior";
    private SoftReference<View> dependencyView;
    private SoftReference<CoordinatorLayout> parent;
    private ScrollerCompat scrollCompat;
    //dependencyView原始高度
    private int originalHeight = -1;
    //Scroll距离
    private int scroll = 0;
    private int overScroll = 0;

    //滑动Runnable
    private FlingRunnable runnable;

    //监听
    private FinishScrollListener flistener;

    public static MyListenerBehavior getBehavior(View view){
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) view.getLayoutParams();
        MyListenerBehavior behavior = (MyListenerBehavior) layoutParams.getBehavior();
        return behavior;
    }

    public MyListenerBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        scrollCompat = ScrollerCompat.create(context);
        flistener = new FinishScrollListener() {
            @Override
            public void onFinsh(final CoordinatorLayout parent, View dependency, View child, int type, final int speed) {
                if (type == FlingRunnable.PRE_FLING) {
                    ((RecyclerView) child).fling(0, speed);
                } else if (type == FlingRunnable.SCROLL_CCONTINUE) {
                    resetType(FlingRunnable.FLING);
                    scrollCompat.fling(0, parent.getScrollY(), 0, -speed, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    ViewCompat.postOnAnimation(dependencyView.get(), runnable);
                }
            }
        };
    }

    public MyListenerBehavior setCanOverScroll(int height) {
        this.overScroll = height;
        return this;
    }

    /**
     * @param parent
     * @param child      xml配置behavior的View
     * @param dependency 配合behavior的View
     * @return
     */
    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {

        if (runnable == null) {
            this.parent=new SoftReference<>(parent);
            runnable = new FlingRunnable(parent, scrollCompat, dependency, child, flistener);
        }

        if (dependency instanceof NestedScrollView) {
            dependencyView = new SoftReference<>(dependency);
            return true;
        }
        return super.layoutDependsOn(parent, child, dependency);
    }

    /**
     * @param parent
     * @param child
     * @param dependency 配合的View行为变动时调用
     * @return
     */
    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        //@1获取dependency的初始高度用于记录还原
        if (originalHeight == -1) {
            originalHeight = dependency.getMeasuredHeight();
        }
        Log.i(TAG, "onDependentViewChanged: ---------------" + originalHeight);
        return true;
    }

    /**
     * 设置child高宽与父控件一致
     *
     * @return
     */
    @Override
    public boolean onMeasureChild(CoordinatorLayout parent, View child, int parentWidthMeasureSpec, int widthUsed, int parentHeightMeasureSpec, int heightUsed) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth(), View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredHeight(), View.MeasureSpec.EXACTLY);
        child.measure(widthSpec, heightSpec);
        Log.i(TAG, "onMeasureChild: getMeasuredHeight" + parent.getMeasuredHeight() + "--getMeasuredWidth" + parent.getMeasuredWidth());
        return true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        if (dependencyView == null) {
            throw new NullPointerException("没有找到配合behavior的View NestScrollview");
        }
        child.layout(0, dependencyView.get().getHeight(), parent.getWidth(), (parent.getHeight() + dependencyView.get().getHeight()));
        return true;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
        scrollCompat.abortAnimation();
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target) {
        super.onStopNestedScroll(coordinatorLayout, child, target);
        resetOverScroll();
    }

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        if(valueAnimator!=null)
            valueAnimator.cancel();

        //下滑动 child滑动接着dependency 滑动，
        if (dyUnconsumed < 0 && coordinatorLayout.getScrollY() > -overScroll) {
            scrollBy(coordinatorLayout, dyUnconsumed);
        }
    }

    private void reset() {
        if (!scrollCompat.isFinished())
            scrollCompat.abortAnimation();
        dependencyView.get().removeCallbacks(runnable);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        if(valueAnimator!=null)
            valueAnimator.cancel();

        //上滑动 dependency 先滑动，接着child滑动
        if (dy > 0 && coordinatorLayout.getScrollY() < originalHeight) {
            Log.i(TAG, "onNestedPreScroll: " + dy);
            scrollBy(coordinatorLayout, dy);

            if (scroll + dy > originalHeight) {
                consumed[1] = originalHeight - scroll;
            } else
                consumed[1] = dy;
        }
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, View child, View target, float velocityX, float velocityY, boolean consumed) {
        if(valueAnimator!=null)
            valueAnimator.cancel();

        if (velocityY < 0 && coordinatorLayout.getScrollY() >0) {
            Log.i(TAG, "onNestedFling: " + velocityY);
            resetType(FlingRunnable.SCROLL_CCONTINUE);
            scrollCompat.fling(0, 0, 0, (int) velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            ViewCompat.postOnAnimation(dependencyView.get(), runnable);
            return true;
        }
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }



    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, View child, View target, float velocityX, float velocityY) {
        if(valueAnimator!=null)
            valueAnimator.cancel();
//        //上滑
        if (velocityY > 0 && coordinatorLayout.getScrollY() != originalHeight) {
            resetType(FlingRunnable.PRE_FLING);
            scrollCompat.fling(0, coordinatorLayout.getScrollY(), 0, (int) velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            ViewCompat.postOnAnimation(dependencyView.get(), runnable);
            return true;

        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }


    class FlingRunnable implements Runnable {
        private ScrollerCompat scroller;
        private View dependency;
        private View child;
        private CoordinatorLayout parent;
        public static final int SCROLL_CCONTINUE = 0, PRE_FLING = 1, FLING = 2;
        private int type;
        private FinishScrollListener listener;

        public FlingRunnable(CoordinatorLayout parent, ScrollerCompat scroller, View dependency, View child, FinishScrollListener listener) {
            this.scroller = scroller;
            this.child = child;
            this.dependency = dependency;
            this.listener = listener;
            this.parent = parent;
        }


        public FlingRunnable setType(int type) {
            this.type = type;
            return this;
        }

        @Override
        public void run() {
            if (scroller.computeScrollOffset()) {
                if (PRE_FLING == type) {
                    handlerPreFling();
                } else if (FLING == type) {
                    handleFling();
                } else if (SCROLL_CCONTINUE == type) {
                    handlerComputerSpeed();
                }
            }

        }

        private void handleFling() {
            if (scroller.getCurrY() <= 0) {
                scrollTo(parent, 0);

                scroller.abortAnimation();
            } else {
                scrollTo(parent, scroller.getCurrY());

                ViewCompat.postOnAnimation(dependency, this);
            }
        }

        private void handlerComputerSpeed() {
            if (!child.canScrollVertically(-1)) {
                float currVelocity = scroller.getCurrVelocity();
                Log.i(TAG, "handlerComputerSpeed: " + currVelocity);
                scroller.abortAnimation();
                listener.onFinsh(parent, dependency, child, type, (int) currVelocity);

            } else {
                ViewCompat.postOnAnimation(dependency, this);
            }
        }

        private void handlerPreFling() {
            if (scroller.getCurrY() > originalHeight) {
                scroller.abortAnimation();
                float currVelocity = scroller.getCurrVelocity();
                scrollTo(parent, originalHeight);
                listener.onFinsh(parent, dependency, child, type, (int) currVelocity);
            } else {
                Log.i(TAG, "handlerPreFling: " + scroller.getCurrY());
                scrollTo(parent, scroller.getCurrY());
                ViewCompat.postOnAnimation(dependency, this);
            }
        }
    }
    private void resetType(int type) {
        reset();
        runnable.setType(type);
    }
    ValueAnimator valueAnimator;
    public void resetOverScroll(){
        if(scroll<0){
            valueAnimator=ValueAnimator.ofInt(scroll,0);
            valueAnimator.setInterpolator(new FastOutSlowInInterpolator());
            valueAnimator.setDuration(450);
            valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    scrollTo(parent.get(),(int)animation.getAnimatedValue());
                }
            });
            valueAnimator.start();
        }
    }

    /**
     * 边界限定
     *
     * @param view 滑动的View
     * @param dy
     */
    private void scrollBy(View view, int dy) {
           //超过了再下拉不回掉
        if(scroll==-overScroll&&dy<0)
            return;

        //范围边界约束
        scroll += dy;
        if (scroll < -overScroll) {
            scroll = -overScroll;
        } else if (scroll > originalHeight) {
            scroll = originalHeight;
        }
        scrollTo(view, scroll);
    }

    private void scrollTo(View view, int dy) {
        scroll = dy;
        view.scrollTo(0, dy);

        //监听
        if (listener != null) {
            if (scroll >= 0)
                listener.onScroll(scroll);
            if (scroll <= 0)
                listener.overScroll(Math.abs(scroll));
        }
    }


    /**
     * 设置监听
     * --------------------------
     */

    private OnScrollListener listener;

    public MyListenerBehavior setScrollListener(OnScrollListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnScrollListener {
        void onScroll(int scroll);

        void overScroll(int overScroll);

    }

    private interface FinishScrollListener {
        void onFinsh(CoordinatorLayout parent, View dependency, View child, int type, int speed);
    }
    /**
     * --------------------------
     */
}

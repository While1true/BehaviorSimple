package com.example.ck.nest;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.ScrollerCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import java.lang.ref.SoftReference;

/**
 * Created by ck on 2017/8/11.
 */

public class MyBehavior extends CoordinatorLayout.Behavior {

    private int measuredHeight = -1;
    private RecyclerView.OnScrollListener listener;
    private boolean canover = false;

    public MyBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public static final String TAG = "MyBehavior";
    SoftReference<View> dependencyView;

    public void setCanOver(boolean canOver) {
        this.canover = canOver;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        if (dependency instanceof NestedScrollView) {
            dependencyView = new SoftReference<>(dependency);
            return true;
        }
        return false;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        if (measuredHeight == -1) {
            measuredHeight = dependency.getMeasuredHeight();
            ViewGroup.LayoutParams layoutParams = dependencyView.get().getLayoutParams();
            layoutParams.height = measuredHeight;
            dependencyView.get().setLayoutParams(layoutParams);
        }
        child.setTranslationY(dependencyView.get().getLayoutParams().height);
        return true;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        child.layout(0, 0, parent.getWidth(), (parent.getHeight() - dependencyView.get().getHeight()));
        child.setTranslationY(dependencyView.get().getHeight());
        return true;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }

    @Override
    public void onNestedScrollAccepted(CoordinatorLayout coordinatorLayout, View child, View directTargetChild, View target, int nestedScrollAxes) {
        super.onNestedScrollAccepted(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target) {
        super.onStopNestedScroll(coordinatorLayout, child, target);
        resetOverScrolol();
    }

    int overscroll;

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {

        int height = dependencyView.get().getHeight();
        ViewGroup.LayoutParams layoutParams = dependencyView.get().getLayoutParams();
        //下滑
        if (dyUnconsumed < 0 && height < measuredHeight) {
            Log.i(TAG, "onNestedScroll: " + dyUnconsumed);
            if (height - dyUnconsumed < measuredHeight) {
                layoutParams.height = height - dyUnconsumed;
            } else {
                layoutParams.height = measuredHeight;
            }
            if (listner != null)
                listner.changed(layoutParams.height);
            dependencyView.get().setLayoutParams(layoutParams);

        } else if (canover) {
            overscroll = overscroll - dyUnconsumed;
            listner.overScroll(overscroll);
        }
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
    }

    public void resetOverScrolol() {
        if (overscroll == 0)
            return;
        ValueAnimator valueAnimator = ValueAnimator.ofInt(overscroll, 0);
        valueAnimator.setDuration(450);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                overscroll = (int) animation.getAnimatedValue();
                if(listner!=null)
                listner.overScroll(overscroll);
            }
        });
        valueAnimator.start();
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
        //overscroll
        if (overscroll > 0 && dy > 0) {
            overscroll = overscroll - dy;
            if (overscroll < 0)
                overscroll = 0;
            listner.overScroll(overscroll);
            return;
        }

        int height = dependencyView.get().getHeight();
        ViewGroup.LayoutParams layoutParams = dependencyView.get().getLayoutParams();

        if (dy > 0 && height > 0) {//上画
            Log.i(TAG, "onNestedPreScroll:layoutParams " + layoutParams.height);
            if (height > dy) {
                layoutParams.height = height - dy;
            } else {
                layoutParams.height = 0;
            }
            dependencyView.get().setLayoutParams(layoutParams);

            if (listner != null)
                listner.changed(layoutParams.height);

            consumed[1] = dy;
        }

        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
    }

    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, final View child, View target, final float velocityX, final float velocityY, boolean consumed) {
        final int height = dependencyView.get().getLayoutParams().height;
        if (velocityY < 0&&Math.abs(velocityY)>50) {
            if (scrollerCompat == null)
                scrollerCompat = ScrollerCompat.create(child.getContext());
            if (runnable2 != null) {

                coordinatorLayout.removeCallbacks(runnable2);
            }
            runnable2 = new ScrollObservveRunnable(scrollerCompat, (RecyclerView) child, new ScrollUtils2.onFinshListener() {
                @Override
                public void finsh(final float speed) {
                    Log.i(TAG, "finsh: " + speed);
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            flingg(child, -(int) speed , height, listner);
                        }
                    });

                }
            });
            scrollerCompat.fling(0, 0, 0, (int) velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            ViewCompat.postOnAnimation(coordinatorLayout, runnable2);
            return true;
        }
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }

    OnHeightChangenListener listner;

    public void setOnHeightChangenListner(OnHeightChangenListener listner) {
        this.listner = listner;
    }

    public interface OnHeightChangenListener {
        void changed(int height);

        void overScroll(int over);
    }

    static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    private class ScrollObservveRunnable implements Runnable {
        private final ScrollerCompat scrollerCompat;
        private final RecyclerView recyclerView;
        ScrollUtils2.onFinshListener listener;

        public ScrollObservveRunnable(ScrollerCompat scrollerCompat, RecyclerView recyclerView, ScrollUtils2.onFinshListener listener) {
            this.scrollerCompat = scrollerCompat;
            this.recyclerView = recyclerView;
            this.listener = listener;

        }

        @Override
        public void run() {
            if (recyclerView != null && scrollerCompat != null) {
                if (scrollerCompat.computeScrollOffset()) {
                    if (!recyclerView.canScrollVertically(-1)) {
                        float currVelocity = scrollerCompat.getCurrVelocity();
                        Log.i(TAG, "runzz: " + currVelocity);
                        listener.finsh(currVelocity);
                        scrollerCompat.abortAnimation();
                    } else {
                        ViewCompat.postOnAnimation(dependencyView.get(), this);
                    }
                } else {
                    //end
                }
            }
        }
    }

    private class FlingRunnable implements Runnable {
        private final ScrollerCompat scrollerCompat;
        private final View mLayout;
        ScrollUtils2.onFinshListener listener;
        int current, max;
        boolean show = true;
        OnHeightChangenListener hlistener;

        FlingRunnable(ScrollerCompat scrollerCompat, View layout, ScrollUtils2.onFinshListener listener, int max, OnHeightChangenListener hlistener) {
            this.scrollerCompat = scrollerCompat;
            mLayout = layout;
            this.listener = listener;
            this.hlistener = hlistener;
            this.max = max;
        }

        public void setHeight(int height) {
            Log.i(TAG, "setHeight: " + height);
            ViewGroup.LayoutParams layoutParams = mLayout.getLayoutParams();
            layoutParams.height = height;
            if(hlistener!=null)
            hlistener.changed(layoutParams.height);
            mLayout.setLayoutParams(layoutParams);
        }

        private FlingRunnable setCurrent(int current) {
            this.current = current;
            return this;
        }

        private FlingRunnable setshowend(boolean show) {
            this.show = show;
            return this;
        }

        @Override
        public void run() {
            if (mLayout != null && scrollerCompat != null) {
                if (scrollerCompat.computeScrollOffset()) {
                    Log.i(TAG, "run:aaa " + scrollerCompat.getCurrY());
                    //上画
                    if (scrollerCompat.getCurrY() > 0) {
                        if (current - scrollerCompat.getCurrY() < 0) {
                            setHeight(0);
                            if (show)
                                listener.finsh(scrollerCompat.getCurrVelocity());
                            scrollerCompat.abortAnimation();
                        } else {
                            setHeight(current - scrollerCompat.getCurrY());
                            ViewCompat.postOnAnimation(mLayout, this);
                        }
                    } else if (scrollerCompat.getCurrY() < 0) {
                        //下滑
                        if (current - scrollerCompat.getCurrY() > max) {
                            setHeight(max);
                            if (show)
                                listener.finsh(scrollerCompat.getCurrVelocity());
                            scrollerCompat.abortAnimation();
                        } else {
                            setHeight(current - scrollerCompat.getCurrY());
                            ViewCompat.postOnAnimation(mLayout, this);
                        }
                        // Post ourselves so that we run on the next animation
                    }
                } else {
//                    onFlingFinished(mParent, mLayout);
                }
            }

        }
    }

    FlingRunnable runnable;
    ScrollObservveRunnable runnable2;
    ScrollerCompat scrollerCompat;

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, final View child, View target, float velocityX, final float velocityY) {

        Log.i(TAG, "onNestedPreFling: " + velocityY + "ppp" + dependencyView.get().getHeight());
        int height = dependencyView.get().getLayoutParams().height;
        if (velocityY > 0 && height > 0&&velocityY>50) {
            flingg(child, (int) velocityY, height, listner);
            return true;
        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);


    }

    private void flingg(final View child, final int velocityY, int height, OnHeightChangenListener listener) {
        if (runnable != null) {
            child.removeCallbacks(runnable);
        }

        if (scrollerCompat == null)
            scrollerCompat = ScrollerCompat.create(child.getContext());
        if(!scrollerCompat.isFinished())
            scrollerCompat.abortAnimation();
        if (runnable == null)
            runnable = new FlingRunnable(scrollerCompat, dependencyView.get(), new ScrollUtils2.onFinshListener() {
                @Override
                public void finsh(float speed) {
                    //先行动
                    ((RecyclerView) child).fling(0, (int) speed);
                    //后行动 不动
                }
            }, measuredHeight, listener);
        runnable.setCurrent(height).setshowend(velocityY > 0);
        scrollerCompat.fling(0, 0, 0, velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        ViewCompat.postOnAnimation(child, runnable);
    }
}

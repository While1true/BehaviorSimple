# BehaviorSimple
[shili](https://github.com/While1true/BehaviorSimple/blob/master/2017-08-14-18-03-43.gif)

[Github地址](https://github.com/While1true/BehaviorSimple)

![2017-08-14-18-03-43.gif](http://upload-images.jianshu.io/upload_images/6456519-d697bde91ebe5651.gif?imageMogr2/auto-orient/strip)

#### 1.效果
-      实现onNestedPreFling，onNestedFling实现了快速滑动的连贯性，头部是NestScrollView，触摸头部也能滑动，之前其他教程很少看到写这些的
-      onNestedPreFling实现思路： 先调用ScrollCompat的fling计算头部滑动完时的加速度，传递给RecyclerView fling
-      onNestedFling实现思路：先用ScrollCompat模拟Recyrview，计算RecyclerView滚动到边界时的加速度，再根据加速度，调用ScrollCompa的fling进行头部Scroll
-      同样本例子只提供实现思路，不提供具体使用，因为不同项目要求不一样
-      

    xml 
```
<android.support.design.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/coordinate"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ProgressBar
        android:layout_width="40dp"
        android:layout_height="40dp"
        android:indeterminate="true"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="-400px" />

    <ImageView
        android:layout_width="100dp"
        android:layout_height="100dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="-200px"
        android:src="@mipmap/ic_launcher" />

    <android.support.v4.widget.NestedScrollView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="bottom">

        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom"
            android:background="@color/colorPrimary"
            android:orientation="vertical">

            <ImageView
                android:id="@+id/ic1"
                android:layout_width="match_parent"
                android:layout_height="90dp"
                android:scaleType="centerCrop"
                android:src="@drawable/snap" />

            <ImageView
                android:id="@+id/ic2"
                android:layout_width="match_parent"
                android:layout_height="211dp"
                android:layout_marginTop="90dp"
                android:scaleType="centerCrop"
                android:src="@drawable/grid" />
        </FrameLayout>
    </android.support.v4.widget.NestedScrollView>

    <android.support.v7.widget.RecyclerView
        android:id="@+id/recyclerview"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#f00"
        android:fadingEdge="none"
        android:overScrollMode="never"
        app:layout_behavior="com.example.ck.nest.MyListenerBehavior"/>
   
</android.support.design.widget.CoordinatorLayout>
```

     

#### 2.使用



```
      MyListenerBehavior behavior = MyListenerBehavior.getBehavior(recyclerView);
        behavior.setCanOverScroll(500)
                .setScrollListener(new MyListenerBehavior.OnScrollListener() {
                    @Override
                    public void onScroll(int scroll) {
                        Log.i(TAG, "onScroll: "+scroll);

                    }

                    @Override
                    public void overScroll(int overScroll) {
                        Log.i(TAG, "overScroll: "+overScroll);
                    }
                });
```
#### 3.主要代码

```
 @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        //下滑动 child滑动接着dependency 滑动，
        if (dyUnconsumed < 0 && coordinatorLayout.getScrollY() > -overScroll) {
            scrollBy(coordinatorLayout, dyUnconsumed);
        }
    }
```
```

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target, int dx, int dy, int[] consumed) {
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
```
```
    @Override
    public boolean onNestedFling(CoordinatorLayout coordinatorLayout, View child, View target, float velocityX, float velocityY, boolean consumed) {

        if (velocityY < 0 && coordinatorLayout.getScrollY() >0) {
            Log.i(TAG, "onNestedFling: " + velocityY);
            resetType(FlingRunnable.SCROLL_CCONTINUE);
            scrollCompat.fling(0, 0, 0, (int) velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            ViewCompat.postOnAnimation(dependencyView.get(), runnable);
            return true;
        }
        return super.onNestedFling(coordinatorLayout, child, target, velocityX, velocityY, consumed);
    }

```
```

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout, View child, View target, float velocityX, float velocityY) {
//        //上滑
        if (velocityY > 0 && coordinatorLayout.getScrollY() != originalHeight) {
            resetType(FlingRunnable.PRE_FLING);
            scrollCompat.fling(0, coordinatorLayout.getScrollY(), 0, (int) velocityY, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
            ViewCompat.postOnAnimation(dependencyView.get(), runnable);
            return true;

        }
        return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY);
    }
```


##### 详细使用请根据Demo，结合自己的实际需求来做

package org.malcdevelop.cyclicview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A cyclic view pager
 *
 * @author Malchenko Alexey "pozitiffcat2@gmail.com"
 */
public class CyclicView extends ViewGroup {
    private final Set<OnPositionChangeListener> onPositionChangeListeners = new HashSet<>();
    private int currentPosition;
    private ImageView lastViewRenderImageView;
    private ImageView firstViewRenderImageView;
    private final List<View> views = new ArrayList<>();
    private CyclicAdapter adapter;
    private float offsetX;
    private float touchX;
    private float touchY;
    private boolean isScrolling;
    private int maxCacheAroundCurrent = 3;
    private int changePositionFactor = 6;

    public CyclicView(Context context) {
        super(context);
        init();
    }

    public CyclicView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        createRenderImageViews();
    }

    /**
     * setup elements count on left side and on right side
     * @param count should be greater or equals than one
     */
    public void setMaxCacheAroundCurrent(int count) {
        this.maxCacheAroundCurrent = Math.max(1, count);
    }

    /**
     * setup adapter and create views around current position
     * uses adapter for create views
     * @param adapter
     */
    public void setAdapter(CyclicAdapter adapter) {
        if (this.adapter != null)
            removeAllCached();

        this.adapter = adapter;
        if (adapter.getItemsCount() != 0) {
            createViewsList();
            createViewAndSetIfNotExistsAround(currentPosition);
        }
    }

    /**
     * switch page to new position
     * @param position
     */
    public void setCurrentPosition(int position) {
        currentPosition = cyclicPositionAt(position);
        boolean isViewMeasured = getMeasuredWidth() != 0;
        if (!isViewMeasured) {
            currentPosition = position;
            postCurrentPosition(position);
            return;
        }

        setOffsetXOfPosition(currentPosition);
        createViewAndSetIfNotExistsAround(currentPosition);
        notifyOnPositionChangeListener(currentPosition);
    }

    /**
     * current selected position
     * @return
     */
    public int getCurrentPosition() {
        return currentPosition;
    }

    /**
     * calculates position use cyclic algorithm
     * @param position raw position eg. -1 or over items count
     * @return calculated position eg. last position instead -1
     */
    public int cyclicPositionAt(int position) {
        int itemsCount = views.size();
        position = position < 0 ? itemsCount + position : position;
        position = position >= itemsCount ? position - itemsCount : position;
        return position;
    }

    /**
     * swipe length for switch position (screen width divide by factor)
     * @param factor
     */
    public void setChangePositionFactor(int factor) {
        this.changePositionFactor = factor;
    }

    /**
     * create views around current if they is null
     * uses adapter for create views
     */
    public void refreshViewsAroundCurrent() {
        createViewAndSetIfNotExistsAround(currentPosition);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int viewsCount = views.size();
        for (int i = 0; i < viewsCount; ++i) {
            View view = views.get(i);
            if (view != null)
                measureChild(view, widthMeasureSpec, heightMeasureSpec);
        }

        measureChild(lastViewRenderImageView, widthMeasureSpec, heightMeasureSpec);
        measureChild(firstViewRenderImageView, widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean c, int l, int t, int r, int b) {
        if (!isScrolling)
            setOffsetXOfPosition(getCurrentPosition(), false);

        int viewsCount = views.size();
        layoutViewOnPosition(lastViewRenderImageView, -1);
        layoutViewOnPosition(firstViewRenderImageView, viewsCount);
        for (int i = 0; i < viewsCount; ++i) {
            View view = views.get(i);
            if (view != null)
                layoutViewOnPosition(view, i);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        final int action = event.getAction();

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            isScrolling = false;
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                touchY = event.getY();
                prepareFirstAndLastImages();
                isScrolling = false;
                break;
            case MotionEvent.ACTION_MOVE: {
                if (isScrolling)
                    return true;

                final int yDiff = (int) Math.abs(event.getY() - touchY);
                if (yDiff > getMeasuredHeight() / (float) changePositionFactor) {
                    isScrolling = false;
                    return false;
                }

                final int xDiff = (int) Math.abs(event.getX() - touchX);
                if (xDiff > getMeasuredWidth() / (float) changePositionFactor) {
                    isScrolling = true;
                    return true;
                }

                break;
            }
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                prepareFirstAndLastImages();
                isScrolling = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(event.getX() - touchX);
                final float eX = event.getX();

                if (xDiff > getMeasuredWidth() / (float) changePositionFactor) {
                    touchX = eX;
                    isScrolling = true;
                }

                if (isScrolling) {
                    float deltaX = eX - touchX;
                    touchX = eX;

                    if (canOffsetX(deltaX))
                        offsetX += deltaX;

                    requestLayout();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                animateScrollToCloserPosition();
                return true;
        }

        return false;
    }

    private void prepareFirstAndLastImages() {
        Bitmap firstBitmap = captureImageFromView(adapter.getItemsCount() - 1);
        Bitmap lastBitmap = captureImageFromView(0);
        lastViewRenderImageView.setImageBitmap(firstBitmap);
        firstViewRenderImageView.setImageBitmap(lastBitmap);
    }

    private void createViewAndSetIfNotExistsAround(int position) {
        int previousPosition = position - 1;
        int nextPosition = position + 1;
        createViewAndSetIfNotExists(previousPosition);
        createViewAndSetIfNotExists(currentPosition);
        createViewAndSetIfNotExists(nextPosition);
    }

    private void createViewAndSetIfNotExists(int position) {
        position = cyclicPositionAt(position);

        View view = views.get(position);
        if (view != null)
            return;

        view = adapter.createView(position);
        if (view == null)
            return;

        views.set(position, view);
        addView(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        removeAvailableCachedItems();
    }

    private void removeAvailableCachedItems() {
        int maxCachedItems = maxCacheAroundCurrent * 2 + 1;
        boolean cacheAvailable = maxCachedItems < adapter.getItemsCount();
        if (!cacheAvailable)
            return;

        int previousPosition = currentPosition - maxCacheAroundCurrent;
        int nextPosition = currentPosition + maxCacheAroundCurrent;
        removeCachedViewIfExists(previousPosition);
        removeCachedViewIfExists(nextPosition);
    }

    private void removeAllCached() {
        for (int i = 0; i < views.size(); ++i)
            removeCachedViewIfExists(i);
    }

    private void removeCachedViewIfExists(int position) {
        position = cyclicPositionAt(position);
        View view = views.get(position);
        if (view != null) {
            adapter.removeView(position, view);
            removeView(view);
            views.set(position, null);
        }
    }

    private void layoutViewOnPosition(View view, int position) {
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int viewOffset = (int) (offsetX + width * position);
        view.layout(viewOffset, 0, viewOffset + width, height);
    }

    private void setOffsetXOfPosition(int position) {
        setOffsetXOfPosition(position, true);
    }

    private void setOffsetXOfPosition(int position, boolean withRequestLayout) {
        int width = getMeasuredWidth();
        offsetX = -width * (position);

        if (withRequestLayout)
            requestLayout();
    }

    private boolean canOffsetX(float deltaX) {
        if (adapter.getItemsCount() <= 1)
            return false;

        offsetX += deltaX;
        int toPosition = cyclicPositionAt(calculateScrollToPosition(0));
        View toView = views.get(toPosition);
        offsetX -= deltaX;
        return toView != null;
    }

    private void animateScrollToCloserPosition() {
        final int width = getMeasuredWidth();
        final int toPosition = calculateScrollToPosition(width / (float) changePositionFactor);
        final float startX = offsetX;
        final float stopX = -(toPosition * width);

        post(new Runnable() {
            private float frame = 0.0f;

            @Override
            public void run() {
                frame += 0.1f;
                offsetX = startX + (stopX - startX) * frame;
                requestLayout();

                if (frame >= 1.0f) {
                    setCurrentPosition(toPosition);
                    isScrolling = false;
                } else {
                    post(this);
                }
            }
        });
    }

    private void postCurrentPosition(final int position) {
        post(new Runnable() {
            @Override
            public void run() {
                setCurrentPosition(position);
            }
        });
    }

    private int calculateScrollToPosition(float factor) {
        int width = getMeasuredWidth();
        float currentPositionOffsetX = -(width * currentPosition);

        if (Math.abs(offsetX - currentPositionOffsetX) < factor)
            return currentPosition;

        int scrollDirection = offsetX > currentPositionOffsetX ? -1 : 1;
        return currentPosition + scrollDirection;
    }

    private void createViewsList() {
        views.clear();
        views.addAll(Collections.<View>nCopies(adapter.getItemsCount(), null));
    }

    private void createRenderImageViews() {
        lastViewRenderImageView = new ImageView(getContext());
        firstViewRenderImageView = new ImageView(getContext());
        addView(lastViewRenderImageView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        addView(firstViewRenderImageView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    private Bitmap captureImageFromView(int position) {
        position = cyclicPositionAt(position);
        View view = views.get(position);
        if (view == null)
            return null;

        Bitmap bitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    public void addOnPositionChangeListener(OnPositionChangeListener listener) {
        onPositionChangeListeners.add(listener);
    }

    public void removeOnPositionChangeListener(OnPositionChangeListener listener) {
        onPositionChangeListeners.remove(listener);
    }

    public void removeAllListeners() {
        onPositionChangeListeners.clear();
    }

    private void notifyOnPositionChangeListener(int position) {
        for (OnPositionChangeListener listener : onPositionChangeListeners)
            listener.onPositionChange(position);
    }

    public interface OnPositionChangeListener {
        void onPositionChange(int position);
    }
}

package org.malcdevelop.cyclicview;

import android.view.View;

/**
 * A cyclic adapter interface for cyclic view pager
 *
 * @author Malchenko Alexey "pozitiffcat2@gmail.com"
 */
public abstract class CyclicAdapter {
    /**
     * real items count
     * @return
     */
    public abstract int getItemsCount();

    /**
     * called when cyclic view wants show real view
     * @param position real position from 0 to itemsCount
     * @return
     */
    public abstract View createView(int position);

    /**
     * called when cyclic view wants remove real view
     * @param position real position from 0 to itemsCount
     * @param view
     */
    public abstract void removeView(int position, View view);
}

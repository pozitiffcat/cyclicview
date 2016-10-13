package org.malcdevelop.cyclicview;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A cyclic fragment adapter for cyclic view pager
 *
 * @author Malchenko Alexey <pozitiffcat2@gmal.com>
 */
public abstract class CyclicFragmentAdapter extends CyclicAdapter {
    private final Context context;
    private final FragmentManager fragmentManager;
    private int id;

    public CyclicFragmentAdapter(Context context, FragmentManager fragmentManager) {
        this.context = context;
        this.fragmentManager = fragmentManager;
    }

    @Override
    public View createView(int position) {
        Fragment fragment = createFragment(position);
        if (fragment == null)
            return null;

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setId(++id);
        fragmentManager.beginTransaction().replace(id, fragment).commit();
        return frameLayout;
    }

    @Override
    public void removeView(int position, View view) {
        FrameLayout frameLayout = (FrameLayout) view;
        Fragment fragment = fragmentManager.findFragmentById(frameLayout.getId());

        if (fragment != null)
            fragmentManager.beginTransaction().remove(fragment).commit();
    }

    /**
     * called when cyclic fragment adapter wants real view
     * @param position real position from 0 to itemsCount
     * @return
     */
    protected abstract Fragment createFragment(int position);
}

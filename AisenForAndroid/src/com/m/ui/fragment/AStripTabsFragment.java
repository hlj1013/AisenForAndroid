package com.m.ui.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.LayoutInflater;

import com.m.R;
import com.m.common.utils.ActivityHelper;
import com.m.support.adapter.FragmentPagerAdapter;
import com.m.support.inject.ViewInject;
import com.m.ui.activity.basic.BaseActivity;
import com.m.ui.widget.SlidingTabLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by wangdan on 15-1-20.
 */
public abstract class AStripTabsFragment<T extends AStripTabsFragment.StripTabItem> extends ABaseFragment
                                implements ViewPager.OnPageChangeListener {

    static final String TAG = AStripTabsFragment.class.getSimpleName();

    public static final String SET_INDEX = "com.m.ui.SET_INDEX";// 默认选择第几个

    @ViewInject(idStr = "slidingTabs")
    SlidingTabLayout slidingTabs;
    @ViewInject(idStr = "pager")
    ViewPager viewPager;
    MyViewPagerAdapter mViewPagerAdapter;

    ArrayList<T> mItems;
    Map<String, Fragment> fragments;
    int mCurrentPosition = 0;

    @Override
    protected int inflateContentView() {
        return R.layout.comm_ui_tabs;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mCurrentPosition = viewPager.getCurrentItem();
        outState.putSerializable("items", mItems);
        outState.putInt("current", mCurrentPosition);
    }

    @Override
    protected void layoutInit(LayoutInflater inflater, final Bundle savedInstanceSate) {
        super.layoutInit(inflater, savedInstanceSate);

        setHasOptionsMenu(true);

        setTab(savedInstanceSate);
    }

    @SuppressWarnings("unchecked")
    protected void setTab(final Bundle savedInstanceSate) {
        if (getActivity() == null)
            return;

        if (savedInstanceSate == null) {
            mItems = generateTabs();

            mCurrentPosition = 0;
            if (getArguments() != null && getArguments().containsKey(SET_INDEX)) {
                mCurrentPosition = getArguments().getInt(SET_INDEX);
            }
            else {
                if (configLastPositionKey() != null) {
                    // 记录了最后阅读的标签
                    String type = ActivityHelper.getShareData("PagerLastPosition" + configLastPositionKey(), "");
                    if (!TextUtils.isEmpty(type)) {
                        for (int i = 0; i < mItems.size(); i++) {
                            StripTabItem item = mItems.get(i);
                            if (item.getType().equals(type)) {
                                mCurrentPosition = i;
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            mItems = (ArrayList<T>) savedInstanceSate.getSerializable("items");
            mCurrentPosition = savedInstanceSate.getInt("current");
        }

        fragments = new HashMap<String, Fragment>();

        if (mItems == null)
            return;

        for (int i = 0; i < mItems.size(); i++) {
            Fragment fragment = getActivity().getFragmentManager().findFragmentByTag(makeFragmentName(i));
            if (fragment != null)
                fragments.put(makeFragmentName(i), fragment);
        }

        mViewPagerAdapter = new MyViewPagerAdapter(getFragmentManager());
//					viewPager.setOffscreenPageLimit(mViewPagerAdapter.getCount());
        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(mViewPagerAdapter);
        if (mCurrentPosition >= mViewPagerAdapter.getCount())
            mCurrentPosition = 0;
        viewPager.setCurrentItem(mCurrentPosition);

        slidingTabs.setCustomTabView(R.layout.comm_lay_tab_indicator, android.R.id.text1);
        Resources res = getResources();
        slidingTabs.setSelectedIndicatorColors(res.getColor(R.color.comm_tab_selected_strip));
        slidingTabs.setDistributeEvenly(true);
        slidingTabs.setViewPager(viewPager);
        slidingTabs.setOnPageChangeListener(this);
    }

    protected void destoryFragments() {
        if (getActivity() != null) {
            if (getActivity() instanceof BaseActivity) {
                BaseActivity mainActivity = (BaseActivity) getActivity();
                if (mainActivity.mIsDestoryed())
                    return;
            }

            try {
                FragmentTransaction trs = getFragmentManager().beginTransaction();
                Set<String> keySet = fragments.keySet();
                for (String key : keySet) {
                    if (fragments.get(key) != null)
                        trs.remove(fragments.get(key));
                }
                trs.commit();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {

    }

    @Override
    public void onPageSelected(int position) {
        mCurrentPosition = position;

        if (configLastPositionKey() != null) {
            ActivityHelper.putShareData("PagerLastPosition" + configLastPositionKey(), mItems.get(position).getType());
        }

        // 查看是否需要拉取数据
        Fragment fragment = getCurrentFragment();
        if (fragment instanceof IStripTabInitData) {
            ((IStripTabInitData) fragment).onStripTabRequestData();
        }
    }

    @Override
    public void onPageScrollStateChanged(int i) {

    }

    protected String makeFragmentName(int position) {
        return mItems.get(position).getTitle();
    }

    // 是否保留最后阅读的标签
    protected String configLastPositionKey() {
        return null;
    }

    abstract protected ArrayList<T> generateTabs();

    abstract protected Fragment newFragment(T bean);

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            destoryFragments();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Fragment getCurrentFragment() {
        if (mViewPagerAdapter == null || mViewPagerAdapter.getCount() < mCurrentPosition)
            return null;

        return fragments.get(makeFragmentName(mCurrentPosition));
    }

    public Fragment getFragment(String tabTitle) {
        if (fragments == null || TextUtils.isEmpty(tabTitle))
            return null;

        for (int i = 0; i < mItems.size(); i++) {
            if (tabTitle.equals(mItems.get(i).getTitle())) {
                return fragments.get(makeFragmentName(i));
            }
        }

        return null;
    }

    class MyViewPagerAdapter extends FragmentPagerAdapter {

        public MyViewPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = fragments.get(makeFragmentName(position));
            if (fragment == null) {
                fragment = newFragment(mItems.get(position));

                fragments.put(makeFragmentName(position), fragment);
            }

            return fragment;
        }

        @Override
        protected void freshUI(Fragment fragment) {
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mItems.get(position).getTitle();
        }

        @Override
        protected String makeFragmentName(int position) {
            return AStripTabsFragment.this.makeFragmentName(position);
        }

    }

    public static class StripTabItem implements Serializable {

        private static final long serialVersionUID = 3680682035685685311L;

        private String type;

        private String title;

        private Serializable tag;

        public StripTabItem() {

        }

        public StripTabItem(String type, String title) {
            this.type = type;
            this.title = title;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Serializable getTag() {
            return tag;
        }

        public void setTag(Serializable tag) {
            this.tag = tag;
        }
    }

    // 这个接口用于多页面时，只有当前的页面才加载数据，其他不显示的页面暂缓加载
    // 当每次onPagerSelected的时候，再调用这个接口初始化数据
    public interface IStripTabInitData {

        public void onStripTabRequestData();

    }

}
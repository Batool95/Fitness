package com.nikhu.fitness.app;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;


public class HomeActivity extends FragmentActivity implements
        ActionBar.TabListener {
    /**
     * Provides fragments for each of the tab. {@link android.support.v4.app.FragmentPagerAdapter} has been used, which will keep every loaded fragment in memory. If this becomes too memory intensive, switch to {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    TabFragmentPagerAdapter tabFragmentPagerAdapter;
    /**
     * Holds the tab page one at a time.
     */
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Create the adapter that will return a fragment for each of the tab.
        tabFragmentPagerAdapter = new TabFragmentPagerAdapter(getSupportFragmentManager());

        // Set up the action bar.
        final ActionBar actionBar = getActionBar();

        // Home/Up button should not be enabled, since there is no hierarchical parent.
        actionBar.setHomeButtonEnabled(false);

        // Specifies displaying tabs in the action bar.
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Specify that we will be displaying the adapter and setting up a listener for when user swipes between sections/tab.
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(tabFragmentPagerAdapter);
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different tabs/sections, select the corresponding tab.
               actionBar.setSelectedNavigationItem(position);
            }
        });
        // For each of pages, add a tab to the action bar.
        for(int i = 0; i < tabFragmentPagerAdapter.getCount(); i++) {
           actionBar.addTab(actionBar.newTab().setText(tabFragmentPagerAdapter.getPageTitle(i)).setTabListener(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        // When the given tab is selected, switch to the corresponding page.
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    /**
     * A {@link android.support.v4.app.FragmentPagerAdapter} that returns a fragment corresponding to of the tabs.
     */
    public static class TabFragmentPagerAdapter extends FragmentPagerAdapter {

        public TabFragmentPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    // The first tab fragment
                    return new StatsFragment();
                case 1:
                    return new StatsFragment();
                case 2:
                    return new WalkFragment();
                default:
                    return new StatsFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    // The first tab fragment
                    return "Stats";
                case 1:
                    return "Schedule";
                case 2:
                    return "Workout";
                default:
                    return "Stats";
            }
        }


    }

    public static class StatsFragment extends Fragment {
        private WorkoutDBAdapter workoutDBAdapter;
        private SimpleCursorAdapter dataAdapter;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_stats_tab, container, false);

            displayListView(rootView);

            return rootView;
        }

        private void displayListView(View rootView) {

            workoutDBAdapter = new WorkoutDBAdapter(getActivity());

            Cursor cursor = workoutDBAdapter.getStepsData();

            // The desired columns to be bound
            String[] columns = new String[]{
                    WorkoutDBAdapter.WorkoutHelper.STEPS,
                    WorkoutDBAdapter.WorkoutHelper.DISTANCE,
                    WorkoutDBAdapter.WorkoutHelper.CALORIES
            };

            // the XML defined views which the data will be bound to
            int[] to = new int[]{
                    R.id.txtSteps,
                    R.id.txtDistance,
                    R.id.txtCalories
            };

            dataAdapter = new SimpleCursorAdapter(
                    getActivity(), R.layout.steps_frame,
                    cursor,
                    columns,
                    to,
                    0);

            ListView listView = (ListView) rootView.findViewById(R.id.listView);
            // Assign adapter to ListView
            listView.setAdapter(dataAdapter);
        }
    }
}
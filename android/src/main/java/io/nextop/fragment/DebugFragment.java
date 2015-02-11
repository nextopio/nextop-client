package io.nextop.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.nextop.R;

public class DebugFragment extends Fragment {

    public static DebugFragment newInstance() {
        return new DebugFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        ViewPager viewPager = (ViewPager) view.findViewById(R.id.pager);
        viewPager.setAdapter(new DebugPagerAdapter());

    }


    final class DebugPagerAdapter extends FragmentPagerAdapter {

        DebugPagerAdapter() {
            super(getFragmentManager());
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int i) {
            switch (i) {
                case 0:
                    return "Messages";
                case 1:
                    return "Subscriptions";
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return DebugMessagesFragment.newInstance();
                case 1:
                    return DebugSubscriptionsFragment.newInstance();
                default:
                    throw new IllegalArgumentException();
            }
        }

    }
}

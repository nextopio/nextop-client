package io.nextop.v15.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.nextop.v15.R;
import io.nextop.view.DebugOverlayView;

public class DebugFragment extends Fragment {

    public static DebugFragment newInstance() {
        return new DebugFragment();
    }



    @Nullable
    DebugOverlayView debugOverlayView = null;


    @Nullable
    public DebugOverlayView getDebugOverlayView() {
        return debugOverlayView;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        debugOverlayView = (DebugOverlayView) getActivity().findViewById(android.R.id.content
            ).findViewWithTag(DebugOverlayView.TAG);


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
                    return "Network";
                case 1:
                    return "Subscriptions";
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override
        public Fragment getItem(int i) {
            DebugChildFragment f;
            switch (i) {
                case 0:
                    f = DebugMessagesFragment.newInstance();
                    break;
                case 1:
                    f = DebugSubscriptionsFragment.newInstance();
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            f.debugFragment = DebugFragment.this;
            return f;
        }

    }
}

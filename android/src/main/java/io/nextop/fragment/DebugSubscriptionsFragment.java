package io.nextop.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.nextop.R;

public class DebugSubscriptionsFragment extends Fragment {

    public static DebugSubscriptionsFragment newInstance() {
        return new DebugSubscriptionsFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug_subscriptions, container, false);
    }
}

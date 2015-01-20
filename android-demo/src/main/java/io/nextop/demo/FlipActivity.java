package io.nextop.demo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.MenuItem;
import io.nextop.Id;

public class FlipActivity extends Activity {
    public static final String ACTION_RECORD = "io.nextop.demo.RECORD";


    public static Intent viewIntent(Context context, Id id) {
        Intent intent = new Intent(context, FlipActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(new Uri.Builder().path(id.toString()).build());
        return intent;

    }
    public static Intent recordIntent(Context context, Id id) {
        Intent intent = new Intent(context, FlipActivity.class);
        intent.setAction(ACTION_RECORD);
        intent.setData(new Uri.Builder().path(id.toString()).build());
        return intent;
    }



    Id flipId;
    ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        flipId = Id.valueOf(intent.getData().getPath());
        String action = intent.getAction();
        int initialIndex;
        if (Intent.ACTION_VIEW.equals(action)) {
            initialIndex = 0;
        } else if (ACTION_RECORD.equals(action)) {
            initialIndex = 1;
        } else {
            throw new IllegalArgumentException();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);


        setContentView(R.layout.activity_flip);

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new FlipAdapter());
        viewPager.setCurrentItem(initialIndex, false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class FlipAdapter extends FragmentPagerAdapter {
        FlipAdapter() {
            super(getFragmentManager());
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return FlipFragment.newInstance(flipId);
                case 1:
                    return RecordFragment.newInstance(flipId);
                default:
                    throw new IndexOutOfBoundsException();
            }
        }
    }
}

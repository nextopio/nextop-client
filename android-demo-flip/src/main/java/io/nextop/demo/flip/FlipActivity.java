package io.nextop.demo.flip;

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

    Flip flip;
    FlipAdapter flipAdapter;

    ViewPager viewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        flipId = Id.valueOf(intent.getData().getPath());
        String action = intent.getAction();
        boolean startRecording;
        if (Intent.ACTION_VIEW.equals(action)) {
            startRecording = false;
        } else if (ACTION_RECORD.equals(action)) {
            startRecording = true;
        } else {
            throw new IllegalArgumentException();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);


        setContentView(R.layout.activity_flip);


        flip = (Flip) getApplication();
        flipAdapter = new FlipAdapter();

        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(flipAdapter);


        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int i) {
                switch (i) {
                    case 0:
                        stopRecording();
                        break;
                    case 1:
                        startRecording();
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }

            @Override
            public void onPageScrolled(int i, float v, int i1) {
                // Do nothing
            }

            @Override
            public void onPageScrollStateChanged(int i) {
                // Do nothing
            }
        });


        if (startRecording) {
            // jump
            viewPager.setCurrentItem(1, false);
        }
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


//    void setIntro(String intro) {
//        flip.getFlipInfoVmm().setIntro(flipId, intro);
//    }



    void startRecording() {
        viewPager.setCurrentItem(1, true);
        ((FlipFragment) flipAdapter.getItem(0)).onStartRecording();
        ((RecordFragment) flipAdapter.getItem(1)).onStartRecording();
    }

    void stopRecording() {
        viewPager.setCurrentItem(0, true);
        ((FlipFragment) flipAdapter.getItem(0)).onStopRecording();
        ((RecordFragment) flipAdapter.getItem(1)).onStopRecording();
    }


    class FlipAdapter extends FragmentPagerAdapter {
        FlipFragment flipFragment = FlipFragment.newInstance(flipId);
        RecordFragment recordFragment = RecordFragment.newInstance(flipId);

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
                    return flipFragment;
                case 1:
                    return recordFragment;
                default:
                    throw new IndexOutOfBoundsException();
            }
        }
    }
}

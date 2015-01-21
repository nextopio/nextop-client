package io.nextop.demo;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import io.nextop.Id;

import java.util.List;
import java.util.Random;
import java.util.Set;

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

    Demo demo;
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
            setIntro();
            startRecording = true;
        } else {
            throw new IllegalArgumentException();
        }

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);


        setContentView(R.layout.activity_flip);


        demo = (Demo) getApplication();
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


    SpeechRecognizer recognizer;


    void setIntro() {
        Toast toast = Toast.makeText(FlipActivity.this, "Speak an intro now ...", Toast.LENGTH_SHORT);
        toast.show();


        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,
                1);

        recognizer = SpeechRecognizer
                .createSpeechRecognizer(FlipActivity.this);

        RecognitionListener listener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int error) {
                recognizer.stopListening();
                recognizer.destroy();
            }

            @Override
            public void onResults(Bundle results) {
                try {
//                Set<String> keys = results.keySet();
//                Log.d("", "" + keys);
                    Set<String> keys = results.keySet();

                    List<String> rr = results.getStringArrayList("results_recognition");
                    if (null != rr && !rr.isEmpty()) {
                        _setIntro(rr.get(0));
                    } else {

                        // TODO remove, random intro
                        Random r = new Random();
                        int n = 5 + r.nextInt(50);
                        StringBuilder sb = new StringBuilder(n);
                        for (int i = 0; i < n; ++i) {
                            switch (r.nextInt(4)) {
                                case 0:
                                    sb.append(' ');
                                    break;
                                default:
                                    sb.append((char) ('a' + r.nextInt(25)));
                            }
                        }
                        _setIntro(sb.toString());
                    }
                } finally {
                    recognizer.stopListening();
                    recognizer.destroy();
                }

            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        };

        recognizer.setRecognitionListener(listener);
        recognizer.startListening(intent);

    }
    void _setIntro(String intro) {
        demo.getFlipInfoVmm().setIntro(flipId, intro);
    }



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

    @Override
    protected void onPause() {
        super.onPause();
        if (null != recognizer) {
            recognizer.destroy();
        }

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

package io.nextop.demo;


import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import io.nextop.Nextop;
import io.nextop.Nextops;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        GridView gridView = findViewById(R.id.grid);
        gridView.setAdapter();

        // - set up grid adapter
        //
    }

    @Override
    protected void onStart() {
        super.onStart();

        // - kick off a polling load in the controller
    }

    @Override
    protected void onStop() {
        super.onStop();

        // - stop polling loader
    }


    private static class GridViewAdapter extends BaseAdapter {
        Demo demo;
        Demo.FeedModel feedModel;

        @Override
        public int getCount() {
            return feedModel.size();
        }

        @Override
        public Object getItem(int position) {
            return demo.getFlipInfo(feedModel.get(position));
        }

        @Override
        public long getItemId(int position) {
            return feedModel.get(position).longHashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // FIXME return image
            // FIXME bind
        }
    }
}

package io.nextop.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import io.nextop.R;

public class DebugMessagesFragment extends Fragment {

    public static DebugMessagesFragment newInstance() {
        return new DebugMessagesFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug_messages, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        ListView listView = (ListView) view.findViewById(R.id.list);
        listView.setAdapter(new MessageAdapter());

    }

    // FIXME adapter that drives from the message control state
    final class MessageAdapter extends BaseAdapter /* implements Observable<MessageControlState> */ {
        @Override
        public int getCount() {
            return 20;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // FIXME
            if (null == convertView) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_message, parent, false);
            }

            TextView routeView = (TextView) convertView.findViewById(R.id.route);
            routeView.setText("GET http://s3-us-west-2.amazonaws.com/nextop-demo-flip-frames/ff32de98078c4b6a9b83b36ba8e0394f0f300f0c9a2bb4964e243af3993f0b8d-1e2d3ccd0fc64da3afcd29cb94d00b057c91a20f5d8889bd2cff6a450ae14b0b.jpeg");

            return convertView;
        }
    }

}

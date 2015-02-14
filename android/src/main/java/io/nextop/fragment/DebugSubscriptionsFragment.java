package io.nextop.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import io.nextop.NextopAndroid;
import io.nextop.R;
import io.nextop.client.MessageControlState;
import io.nextop.rx.RxDebugger;
import io.nextop.rx.RxFragment;
import io.nextop.sortedlist.SortedList;
import io.nextop.sortedlist.SplaySortedList;
import io.nextop.view.DebugOverlayView;
import rx.Notification;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DebugSubscriptionsFragment extends DebugChildFragment {


    public static DebugSubscriptionsFragment newInstance() {
        return new DebugSubscriptionsFragment();
    }



    private SubscriptionAdapter subscriptionAdapter;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug_messages, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        subscriptionAdapter = new SubscriptionAdapter();



        View view = getView();

        ListView listView = (ListView) view.findViewById(R.id.list);
        listView.setAdapter(subscriptionAdapter);

        bind(
                RxDebugger.get().getStats()

//                .subscribeOn(AndroidSchedulers.mainThread())
        )
                .throttleLast(200, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe(subscriptionAdapter);
    }




    final class SubscriptionAdapter extends BaseAdapter implements Observer<RxDebugger.Stats> {

        // map subscription -> subscriptionstate
        // state:
        //  - stats
        //  - id
        //

//        private final Map<Subscriber<?>, RxDebugger.Stats> statsMap = new LinkedHashMap<Subscriber<?>, RxDebugger.Stats>(8);
        // FIXME sorted list, by update nanos (descending)

        Map<Long, RxDebugger.Stats> statsMap = new HashMap<Long, RxDebugger.Stats>(8);
        SortedList<RxDebugger.Stats> orderedStats = new SplaySortedList<RxDebugger.Stats>(COMPARATOR_MOST_RECENT);





        @Override
        public void onNext(final RxDebugger.Stats stats) {
            if (stats.removed) {
                @Nullable RxDebugger.Stats r = statsMap.remove(stats.subscriberId);
                if (null != r) {
                    orderedStats.remove(r);
                }
            } else {
                @Nullable RxDebugger.Stats r = statsMap.put(stats.subscriberId, stats);
                if (null != r) {
                    orderedStats.remove(r);
                }
                orderedStats.insert(stats);
            }


            if (null != debugFragment) {
                @Nullable DebugOverlayView debugOverlayView = debugFragment.getDebugOverlayView();
                if (null != debugOverlayView) {
                    DebugOverlayView.ViewSummary rootViewSummary = DebugOverlayView.ViewSummary.create(orderedStats);
                    debugOverlayView.setRootViewSummary(rootViewSummary);
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public void onCompleted() {
            statsMap.clear();
            orderedStats.clear();
            if (null != debugFragment) {
                @Nullable DebugOverlayView debugOverlayView = debugFragment.getDebugOverlayView();
                if (null != debugOverlayView) {
                    debugOverlayView.setRootViewSummary(null);
                }
            }

            notifyDataSetChanged();
        }

        @Override
        public void onError(Throwable e) {
            statsMap.clear();
            orderedStats.clear();
            if (null != debugFragment) {
                @Nullable DebugOverlayView debugOverlayView = debugFragment.getDebugOverlayView();
                if (null != debugOverlayView) {
                    debugOverlayView.setRootViewSummary(null);
                }
            }

            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            return orderedStats.size();
        }

        @Override
        public RxDebugger.Stats getItem(int position) {
            return orderedStats.get(position);
        }

        @Override
        public long getItemId(int position) {
            return orderedStats.get(position).subscriberId;
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
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_debug_subscriptions_stats, parent, false);
            }

            TextView countTextView = (TextView) convertView.findViewById(R.id.count);
            TextView infoTextView = (TextView) convertView.findViewById(R.id.info);
            RxDebugger.Stats stats = getItem(position);

            countTextView.setText("" + stats.onNextCount);
            if (null != stats.mostRecentNotification) {
                Notification n = stats.mostRecentNotification;
                switch (n.getKind()) {
                    case OnNext:
                        @Nullable Object value = n.getValue();
                        infoTextView.setText(String.format("NEXT %s", null != value ? value.getClass().getSimpleName() : "null"));
                        break;
                    case OnCompleted:
                        infoTextView.setText("COMPLETED");
                        break;
                    case OnError:
                        infoTextView.setText("ERROR");
                        break;
                }
            } else {
                infoTextView.setText("");
            }

            return convertView;
        }
    }

    private static final Comparator<RxDebugger.Stats> COMPARATOR_MOST_RECENT = new Comparator<RxDebugger.Stats>() {
        @Override
        public int compare(RxDebugger.Stats a, RxDebugger.Stats b) {
            if (a == b) {
                return 0;
            }

            if (a.nanos < b.nanos) {
                return 1;
            } else if (b.nanos < a.nanos) {
                return -1;
            }

            // id
            if (a.subscriberId < b.subscriberId) {
                return -1;
            } else if (b.subscriberId < a.subscriberId) {
                return 1;
            } else {
                // objects not equal but same subscriber id
                throw new IllegalStateException();
            }
        }
    };
}

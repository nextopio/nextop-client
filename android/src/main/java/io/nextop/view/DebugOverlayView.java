package io.nextop.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import io.nextop.rx.RxDebugger;
import io.nextop.rx.RxFragment;
import rx.Notification;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;

// TODO visualization and interactions to support the debug fragment
public class DebugOverlayView extends View {
    public static final Object TAG = new Object();


    @Nullable
    private ViewSummary rootViewSummary = null;


    // temp draw state
    final Paint paint = new Paint();
    final int[] wloc = new int[2];
    long nanos;

    public DebugOverlayView(Context context) {
        super(context);
        init();
    }
    public DebugOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public DebugOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    public DebugOverlayView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setTag(TAG);
    }

    // FIXME
    // visualizations/interactions:
    // - press and hold over debug fragment and drag, to resize debug fragment
    // - listen to Debugger.getStats() and maintain a list of stats to display (keyed by subscriber)
    //    -- draw stats either 1. over the associated view 2. in the bottom right as a circle coming out, representing no view
    // TODO is there ever a case of going from view to no view? seems rare (don't need an animation for this)

    // TODO flash the region when there is an update to a region


    public void setRootViewSummary(@Nullable ViewSummary rootViewSummary) {
        this.rootViewSummary = rootViewSummary;
        invalidate();

        // TODO check update times, run a poller until final animation time
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (null != rootViewSummary) {
            // - draw the root in the bottom right
            // - draw the view summaries recursively


            nanos = System.nanoTime();

            for (ViewSummary vs : rootViewSummary.nearestDescendants) {
                drawViewSummary(canvas, vs);
            }
        }
    }
    // FIXME saw a SO here
    private void drawViewSummary(Canvas canvas, ViewSummary viewSummary) {
        // FIXME
        View rview = viewSummary.view;
        float rw = rview.getWidth();
        float rh = rview.getHeight();
        rview.getLocationInWindow(wloc);
        float rx = wloc[0];
        float ry = wloc[1];
        getLocationInWindow(wloc);
        rx -= wloc[0];
        ry -= wloc[1];

        float p = 2.f;

        // FIXME lerpColor, maskColor utils (just port processing)
        // FIXME colors etc
        int millis = (int) TimeUnit.NANOSECONDS.toMillis(nanos - viewSummary.nanos);
        int fc, sc;
        float sw;
        if (millis < 1000) {
            fc = Color.argb(35 * (1000 - millis) / 1000, 255, 0, 0);
            sc = Color.argb(255, 255, 0, 0);
            sw = 2.f + 3 * (1000 - millis) / 1000.f;
        } else {
            fc = 0;
            sc = Color.argb(255, 255, 0, 0);
            sw = 2.f;
        }


        if (0 < Color.alpha(fc)) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(fc);
            canvas.drawRect(rx + p, ry + p, rx + rw - 2 * p, ry + rh - 2 * p, paint);
        }

        if (0 < Color.alpha(sc)) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(sw);
            paint.setColor(sc);
            canvas.drawRect(rx + p, ry + p, rx + rw - 2 * p, ry + rh - 2 * p, paint);
        }

        // FIXME
        // summary text
        if (millis < 2000) {
            // count + type

            paint.setStyle(Paint.Style.FILL);
//            paint.setStrokeWidth(1.f);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(32.f);
            paint.setColor(Color.argb(220, 255, 0, 0));
            canvas.drawText(String.format("%d", viewSummary.netOnNextCount), rx + 16, ry + rh - 16, paint);
        }



        for (ViewSummary vs : viewSummary.nearestDescendants) {
            drawViewSummary(canvas, vs);
        }
    }


    public static class ViewSummary {
        // take the flags from the most recent update
        public final int flags;
        public final long nanos;

        @Nullable
        public final View view;
        public final int netOnNextCount;
        public final int netOnCompletedCount;
        public final int netOnErrorCount;
        public final int netFailedNotificationCount;

        /** this reflects the descendants in the view hierarchy
         * that have no closer common descendant/ancestor */
        public final List<ViewSummary> nearestDescendants;

        ViewSummary(final int flags, @Nullable View view,
                    int netOnNextCount, int netOnCompletedCount, int netOnErrorCount,
                    int netFailedNotificationCount,
                    final long nanos,
                    List<ViewSummary> nearestDescendants) {
            this.nanos = nanos;
            this.view = view;
            this.netOnNextCount = netOnNextCount;
            this.netOnCompletedCount = netOnCompletedCount;
            this.netOnErrorCount = netOnErrorCount;
            this.netFailedNotificationCount = netFailedNotificationCount;
            this.flags = flags;
            this.nearestDescendants = nearestDescendants;
        }



        public static ViewSummary create(Collection<RxDebugger.Stats> allStats) {
            Multimap<View, RxDebugger.Stats> statsMap = ArrayListMultimap.create();
            for (RxDebugger.Stats stats : allStats) {
                @Nullable View v;
                if (null == stats.view
                        || !stats.view.isAttachedToWindow()
                        || View.GONE == stats.view.getVisibility()) {
                    v = null;
                } else {
                    v = stats.view;
                }
                statsMap.put(v, stats);
            }

            // create the graph
            Map<Object, View> nearestAncestors = new HashMap<Object, View>(statsMap.size());
            Multimap<View, View> nearestDescendants = ArrayListMultimap.create();

            Set<View> views = statsMap.keySet();
            for (@Nullable View view : views) {
                if (null != view) {
                    @Nullable View nearestAncestor;
                    if (!nearestAncestors.containsKey(view)) {
                        ViewParent p;
                        View a = null;
                        for (p = view.getParent(); null != p; p = p.getParent()) {
                            if (nearestAncestors.containsKey(p)) {
                                a = nearestAncestors.get(p);
                                break;
                            } else if (views.contains(p)) {
                                a = (View) p;
                                break;
                            }
                        }
                        nearestAncestor = a;

                        // propagate down
                        nearestAncestors.put(view, nearestAncestor);
                        for (ViewParent q = view.getParent(); q != p; q = q.getParent()) {
                            nearestAncestors.put(q, nearestAncestor);
                        }
                    } else {
                        nearestAncestor = nearestAncestors.get(view);
                    }
                    nearestDescendants.put(nearestAncestor, view);
                }
            }

            return create(statsMap, nearestDescendants, null);
        }
        private static ViewSummary create(Multimap<View, RxDebugger.Stats> statsMap,
                                          Multimap<View, View> nearestDescendants, @Nullable View view) {
            // roll up the stats
            long mostRecentNanos = -1L;
            int mostRecentFlags = 0;
            int netOnNextCount = 0;
            int netOnCompletedCount = 0;
            int netOnErrorCount = 0;
            int netFailedNotificationCount = 0;
            for (RxDebugger.Stats stats : statsMap.get(view)) {
                if (mostRecentNanos < stats.nanos) {
                    mostRecentNanos = stats.nanos;
                    mostRecentFlags = stats.flags;
                }
                netOnNextCount += stats.onNextCount;
                netOnCompletedCount += stats.onCompletedCount;
                netOnErrorCount += stats.onErrorCount;
                netFailedNotificationCount += stats.failedNotificationCount;
            }

            Collection<View> ndVs = nearestDescendants.get(view);
            List<ViewSummary> ndVss = new ArrayList<ViewSummary>(ndVs.size());
            for (View nearestDescendant : ndVs) {
                ndVss.add(create(statsMap, nearestDescendants, nearestDescendant));
            }

            return new ViewSummary(mostRecentFlags, view,
                    netOnNextCount, netOnCompletedCount, netOnErrorCount,
                    netFailedNotificationCount,
                    mostRecentNanos,
                    ndVss);
        }
    }
}

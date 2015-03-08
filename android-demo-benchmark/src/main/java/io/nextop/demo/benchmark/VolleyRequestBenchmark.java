package io.nextop.demo.benchmark;


import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class VolleyRequestBenchmark extends RequestBenchmark {
	static final String LOG_TAG = VolleyRequestBenchmark.class.getSimpleName();

	final Context viewContext;
	final Record record;

	RequestQueue volleyQueue;

	VolleyRequestBenchmark(Context context, List<URL> requests) {
		super(requests);
		viewContext = context;
		record = new Record();
	}

	@Override
	void execute(String id, Map<String, String> context, List<URL> urls) {
		volleyQueue = Volley.newRequestQueue(viewContext);
		volleyQueue.start();

		for(final URL url : requests) {
			final String urlStr = url.toString();
			final Stopwatch watch = Stopwatch.createUnstarted();
			final long startTime = System.currentTimeMillis();

			final Record requestRecord = new Record();
			requestRecord.putContextValue("runId", id);
			requestRecord.putAllContextValues(context);

			Response.Listener<String> onSucc =
							new Response.Listener<String>() {
								@Override
								public void onResponse(String response) {
									watch.stop();

									Measurement measurement = new Measurement(urlStr, Measurement.Status.SUCCESS);
									measurement.setValue("start", startTime);
									measurement.setValue("elapsed", watch.elapsed(TimeUnit.MILLISECONDS));
									measurement.setValue("len", (long) response.length());

									record(measurement);
								}
							};

			Response.ErrorListener onErr =
							new Response.ErrorListener() {
								@Override
								public void onErrorResponse(VolleyError error) {
									Log.d(LOG_TAG, "error: " + urlStr);

									Measurement measurement = new Measurement(urlStr, Measurement.Status.ERROR);

									record(measurement);
								}
							};

			watch.start();

			StringRequest request = new StringRequest(urlStr, onSucc, onErr);
			request.setShouldCache(false);

			volleyQueue.add(request);

		}
	}

	public static VolleyRequestBenchmark withURLStrings(Context context, List<String> strs) {
		List<URL> requests = Lists.newArrayListWithCapacity(strs.size());
		for (String str : strs) {
			try {
				requests.add(new URL(str));
			} catch (MalformedURLException mue) {
				Log.e(LOG_TAG, "", mue);
			}
		}

		return new VolleyRequestBenchmark(context, requests);
	}

}

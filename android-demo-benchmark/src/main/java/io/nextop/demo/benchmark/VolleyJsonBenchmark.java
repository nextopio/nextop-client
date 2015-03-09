package io.nextop.demo.benchmark;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class VolleyJsonBenchmark extends Benchmark<URL> {
	static final String LOG_TAG = VolleyJsonBenchmark.class.getSimpleName();

	private final Context context;

	RequestQueue volleyQueue;

	public VolleyJsonBenchmark(Context appContext, List<URL> urls) {
		super(urls);
		context = appContext;
	}

	@Override
	void before(Map<String, String> runContext) {
		result.addValues(runContext);
		result.addValue("method", "volley");

		volleyQueue = Volley.newRequestQueue(context);
		volleyQueue.start();
	}

	@Override
	void execute(ImmutableList<URL> urls) {
		for (final URL url: urls) {
			final String urlStr = url.toString();
			final Stopwatch watch = Stopwatch.createUnstarted();
			final long startTime = System.currentTimeMillis();

			Response.Listener<String> onSuccess =
							new Response.Listener<String>() {
								@Override
								public void onResponse(String response) {
									watch.stop();

									Measurement measurement = Measurement.asSuccessful(urlStr);
									measurement.setValue("start", startTime);
									measurement.setValue("elapsed", watch.elapsed(TimeUnit.MILLISECONDS));
									measurement.setValue("len", (long) response.length());
									record(measurement);
								}
							};

			Response.ErrorListener onError =
							new Response.ErrorListener() {
								@Override
								public void onErrorResponse(VolleyError error) {
									Measurement measurement = Measurement.asFailed(urlStr);
									record(measurement);
								}
							};

			watch.start();

			StringRequest request = new StringRequest(urlStr, onSuccess, onError);
			request.setShouldCache(false);

			volleyQueue.add(request);
		}
	}


	public static VolleyJsonBenchmark withStringUrls(Context context, List<String> strs) {
		List<URL> urls = Lists.newArrayListWithExpectedSize(strs.size());
		for (String str: strs) {
			try {
				urls.add(new URL(str));

			} catch(MalformedURLException mue) {
				Log.e(LOG_TAG, String.format("Skipping url: %s", str), mue);

			}
		}

		return new VolleyJsonBenchmark(context, urls);
	}
}

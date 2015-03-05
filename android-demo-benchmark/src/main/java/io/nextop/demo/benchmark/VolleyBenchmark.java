package io.nextop.demo.benchmark;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public class VolleyBenchmark extends Benchmark {
	public static final String DEBUG = VolleyBenchmark.class.getSimpleName();

	private final CountDownLatch latch;

	VolleyBenchmark(Context context, List<URL> urls) {
		super(context, urls);

		latch = new CountDownLatch(urls.size());
	}

	public Benchmark.Result result() {
		Uninterruptibles.awaitUninterruptibly(latch, 1, TimeUnit.MINUTES);
		return result;
	}

	public void run(String runId, Map<String, String> runContext) {
		result.addContext("type", "volley");
		result.addContext("runId", runId);
		result.addContext("timestamp", String.valueOf(System.currentTimeMillis()));
		result.addContext(runContext);

		RequestQueue queue = Volley.newRequestQueue(context);
		for (URL url : urls) {
			final String urlStr = url.toString();
			final Stopwatch watch = Stopwatch.createUnstarted();
			final long startTime = System.currentTimeMillis();

			Response.Listener<String> onSuccess =
							new Response.Listener<String>() {
								@Override
								public void onResponse(final String returned) {
									latch.countDown();
									watch.stop();

									result.addTiming(urlStr,
													ImmutableMap.of(
																	"start", startTime,
																	"elasped", watch.elapsed(TimeUnit.MILLISECONDS)));
								}
							};

			Response.ErrorListener onError =
							new Response.ErrorListener() {
								@Override
								public void onErrorResponse(final VolleyError err) {
									Log.d(DEBUG, "error", err);
									latch.countDown();
								}
							};

			watch.start();

			StringRequest request = new StringRequest(urlStr, onSuccess, onError);
			request.setShouldCache(false);
			queue.add(request);
		}

	}

	public static VolleyBenchmark using(Context context, List<String> benchmarkUrls) {
		List<URL> urls = Lists.transform(benchmarkUrls, new Function<String, URL>() {
			@Override
			public URL apply(@Nullable String str) {
				URL url = null;

				try {
					url = new URL(str);
				} catch (MalformedURLException mue) {
					Log.d(DEBUG, String.format("Bad url: %s", url), mue);
				}

				return url;
			}
		});

		return new VolleyBenchmark(context, urls);
	}

}

package io.nextop.demo.benchmark;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.List;
import java.util.Map;

abstract class Benchmark {
	public static final String DEBUG = Benchmark.class.getSimpleName();

	final Context context;
	final ImmutableList<URL> urls;

	final Benchmark.Result result;

	public Benchmark(Context runContext, List<URL> benchmarkUrls) {
		context = runContext;

		urls = ImmutableList.copyOf(benchmarkUrls);
		result = new Benchmark.Result();
	}

	public Result result() {
		return result;
	}

	abstract void run(String runId, Map<String, String> context);

	static class Result {
		private final ImmutableMap.Builder<String, Map<String, Long>> timings;
		private final ImmutableMap.Builder<String, String> context;

		Result() {
			timings = ImmutableMap.builder();
			context = ImmutableMap.builder();
		}

		public void addTiming(String id, Map<String, Long> timing) {
			timings.put(id, timing);
		}

		public void addContext(String name, String value) {
			context.put(name, value);
		}

		public void addContext(Map<String, String> data) {
			context.putAll(data);
		}

		public JSONObject toJSON() {
			final JSONObject result = new JSONObject(context.build());
			try {
				result.put("timing", new JSONObject(timings.build()));
			} catch (JSONException je) {
				Log.d(DEBUG, "result error", je);
			}

			return result;
		}
	}
}

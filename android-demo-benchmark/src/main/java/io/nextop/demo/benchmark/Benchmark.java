package io.nextop.demo.benchmark;

import android.content.Context;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import org.json.JSONArray;
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
		private final List<Timing> timings;
		private final Map<String, String> context;

		Result() {
			timings = Lists.newArrayList();
			context = Maps.newTreeMap();
		}

		public void addTiming(String id, Map<String, Long> measurements) {
			timings.add(new Timing(id, measurements));
		}

		public void addContext(String name, String value) {
			context.put(name, value);
		}

		public void addContext(Map<String, String> data) {
			context.putAll(data);
		}

		public List<Timing> getTimings() {
			return timings;
		}

		public JSONObject toJSON() {
			JSONObject result = new JSONObject(context);

			try {
				JSONArray timingArr = new JSONArray();
				for (Timing timing: timings) {
					JSONObject timingObj = new JSONObject(timing.measurements());
					timingObj.put("id", timing.id());
					timingArr.put(timingObj);
				}

				result.put("timings", timingArr);

			} catch (JSONException je) {
				Log.d(DEBUG, "result error", je);

			}

			return result;
		}

		static class Timing {
			private final String id;
			private final Map<String, Long> measurements;

			Timing(String timingId, Map<String, Long> timingMeasurements) {
				id = timingId;
				measurements = timingMeasurements;
			}

			public String id() {
				return id;
			}

			public Map<String, Long> measurements() {
				return measurements;
			}
		}
	}
}

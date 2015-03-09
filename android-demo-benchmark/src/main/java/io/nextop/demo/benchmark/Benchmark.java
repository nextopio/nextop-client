package io.nextop.demo.benchmark;


import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class Benchmark<T> {
	static final String LOG_TAG = Benchmark.class.getSimpleName();

	final ImmutableList<T> inputs;
	final Result result;

	private final List<ResultListener> listeners;

	Benchmark(List<T> benchmarkInputs) {
		inputs = ImmutableList.copyOf(benchmarkInputs);
		result = new Result();
		listeners = Lists.newArrayList();
	}

	public void addListener(ResultListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ResultListener listener) {
		listeners.remove(listener);
	}

	public void run(Map<String, String> runContext) {
		before(runContext);
		execute(inputs);
		after(runContext);
	}

	abstract void execute(ImmutableList<T> inputs);

	void before(Map<String, String> runContext) {}

	void after(Map<String, String> runContext) {}

	void record(Measurement first, Measurement ...rest) {
		result.addMeasurement(first, rest);

		if (result.numMeasurements() >= inputs.size()) {
			fireCompleted(result);
		}
	}

	private void fireCompleted(Result result) {
		for (ResultListener listener: listeners) {
			listener.onResult(result);
		}
	}

	interface ResultListener {
		void onResult(Result result);
	}

	static class Result {
		private final Map<String, String> context;
		private final List<Measurement> measurements;

		Result() {
			context = Maps.newTreeMap(Ordering.natural());
			measurements = Lists.newArrayList();
		}

		public void addValue(String name, String value) {
			context.put(name, value);
		}

		public void addValues(Map<String, String> values) {
			context.putAll(values);
		}

		public void addMeasurement(Measurement first, Measurement ...rest) {
			measurements.addAll(Lists.asList(first, rest));
		}

		public List<Measurement> getMeasurements() {
			return measurements;
		}

		public int numMeasurements() {
			return measurements.size();
		}

		public JSONObject toJSONObject() {
			JSONObject json = new JSONObject(context);

			JSONArray measurementJsonArr = new JSONArray();
			for (Measurement measurement : measurements) {
				measurementJsonArr.put(measurement.toJSONObject());
			}

			try {
				json.put("measurements", measurementJsonArr);

			} catch (JSONException je) {
				Log.e(LOG_TAG, "", je);

			}

			return json;
		}

	}

	static class Measurement {
		enum Status {
			SUCCESSFUL,
			FAILED,
			UNKNOWN
		}

		private final String id;
		private final Status status;
		private final Map<String, Long> measurements;

		// TODO(andy) refactor for builder
		Measurement(String measurementId, Status measurementStatus, Map<String, Long> measured) {
			id = measurementId;
			status = measurementStatus;
			measurements = measured;
		}

		public void setValue(String name, Long value) {
			measurements.put(name, value);
		}

		public Long getValue(String name) {
			return measurements.get(name);
		}

		public JSONObject toJSONObject() {
			JSONObject json = new JSONObject();

			try {
				json.put("id", id);
				json.put("status", status.toString());
				json.put("measurements", new JSONObject(measurements));

			} catch (JSONException je) {
				Log.e(LOG_TAG, "", je);

			}

			return json;
		}

		public static Measurement asFailed(String measurementId) {
			return new Measurement(measurementId, Status.FAILED, Collections.EMPTY_MAP);
		}

		public static Measurement asSuccessful(String measurementId, Map<String, Long> measurements) {
			return new Measurement(measurementId, Status.SUCCESSFUL, measurements);
		}

		public static Measurement asSuccessful(String measurementId) {
			return asSuccessful(measurementId, Maps.<String, Long>newHashMap());
		}

	}

}

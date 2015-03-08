package io.nextop.demo.benchmark;


import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class RequestBenchmark {
	static final String LOG_TAG = RequestBenchmark.class.getSimpleName();

	private final Record record;
	private final List<CompletionListener> completionListeners;

	final ImmutableList<URL> requests;

	RequestBenchmark(List<URL> urls) {
		requests = ImmutableList.copyOf(urls);
		record = new Record();
		completionListeners = Lists.newArrayList();
	}

	public void addCompletionListener(CompletionListener listener) {
		completionListeners.add(listener);
	}

	public void removeCompletionListener(CompletionListener listener) {
		completionListeners.remove(listener);
	}

	public void run(String id, Map<String, String> context) {
		before(id, context, requests);
		execute(id, context, requests);
		after(id, context, requests);
	}

	public void run(String id) {
		run(id, Collections.EMPTY_MAP);
	}

	abstract void execute(String id, Map<String, String> context, List<URL> urls);

	void before(String id, Map<String, String> context, List<URL> urls) {
		record.putContextValue("runId", id);
		record.putAllContextValues(context);
	}

	void after(String id, Map<String, String> context, List<URL> urls) {}

	void record(Measurement first, Measurement ...rest) {
		record.addMeasurement(first, rest);

		if (record.size() >= requests.size()) {
			fireCompleted(record);
		}
	}

	void fireCompleted(Record completedRecord) {
		for (CompletionListener listener : completionListeners) {
			listener.onCompleted(completedRecord);
		}
	}

	interface CompletionListener {
		void onCompleted(Record completedRecord);
	}

	static class Record {

		private final Map<String, String> context;
		private final List<Measurement> measurements;

		Record() {
			context = Maps.newTreeMap();
			measurements = Lists.newArrayList();
		}

		public void putContextValue(String name, String value) {
			context.put(name, value);
		}

		public void putAllContextValues(Map<String, String> values) {
			context.putAll(values);
		}

		public void addMeasurement(Measurement first, Measurement ...rest) {
			measurements.addAll(Lists.asList(first, rest));
		}

		public int size() {
			return measurements.size();
		}

		public String toString() {
			return ImmutableMap.of(
				"context", context.toString(),
				"measurements", measurements.toString()
			).toString();
		}

		public JSONObject toJSONObject() {
			JSONObject json = new JSONObject(context);

			try {
				JSONArray measurementJsonArr = new JSONArray();
				for (Measurement measurement: measurements) {
					measurementJsonArr.put(measurement.toJSONObject());
				}

				json.put("measurements", measurementJsonArr);

			} catch (JSONException je) {
				Log.e(LOG_TAG, "", je);

			}

			return json;
		}

	}

	static class Measurement {
		enum Status {
			SUCCESS,
			ERROR,
			UNKNOWN
		}

		private final String id;
		private final Status status;
		private final Map<String, Long> values;

		Measurement(String measureId, Status measureStatus) {
			id = measureId;
			status = measureStatus;
			values = Maps.newTreeMap();
		}

		public void setValue(String name, Long value) {
			values.put(name, value);
		}

		public String toString() {
			return ImmutableMap.of(
				"id", id,
				"values", values.toString()
			).toString();
		}

		public JSONObject toJSONObject() {
			JSONObject json = new JSONObject();
			JSONObject valuesJson = new JSONObject(values);

			try {
				json.put("id", id);
				json.put("status", status.toString());
				json.put("values", valuesJson);

			} catch (JSONException je) {
				Log.e(LOG_TAG, "", je);

			}

			return json;
		}
	}

}

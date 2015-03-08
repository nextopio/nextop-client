package io.nextop.demo.benchmark;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


public class MainActivity extends Activity {
	public static final String DEBUG = MainActivity.class.getSimpleName();

	String signalStrength = "Unknown";
	List<String> benchmarkUrls = Collections.EMPTY_LIST;

	ListView timingsList;
	TimingAdapter timingsAdapter;


//	public void xvolley(View view) {
//		final AtomicInteger count = new AtomicInteger(0);
//		final Timer timer = new Timer();
//
//		timer.schedule(new TimerTask() {
//			@Override
//			public void run() {
//				Log.d(DEBUG, String.format("Run %d", count.get()));
//				volleyBenchmark();
//
//				if (count.incrementAndGet() >= 100) {
//					timer.cancel();
//				}
//			}
//		}, 0, 10000);
//
//	}

	public void volley(View view) {
		URL resultUrl = null;
		try {
			resultUrl = new URL("http://10.1.10.10:9091/result");
		} catch(MalformedURLException je) {}

		UUID runId = UUID.randomUUID();
		RequestBenchmark benchmark = VolleyRequestBenchmark.withURLStrings(this, benchmarkUrls);
		benchmark.addCompletionListener(new BenchmarkReporter(resultUrl));
		benchmark.run(runId.toString(), ImmutableMap.of("signal", signalStrength));
	}

	public void volley_old(View view) {

		final Benchmark benchmark = VolleyBenchmark.using(this, benchmarkUrls);

		new AsyncTask<Void, Void, Benchmark.Result>() {
			@Override
			protected Benchmark.Result doInBackground(Void... params) {
				String runId = UUID.randomUUID().toString();
				ImmutableMap.Builder<String, String> runContext = ImmutableMap.builder();
				runContext.put("signal", signalStrength);

				benchmark.run(runId, runContext.build());
				return benchmark.result();
			}

			@Override
			protected void onPostExecute(Benchmark.Result result) {
				send("volley", result);
				show(result);
			}

		}.execute();
	}

//	public void xnextop(View view) {
//		final AtomicInteger count = new AtomicInteger(0);
//		final Timer timer = new Timer();
//
//		timer.schedule(new TimerTask() {
//			@Override
//			public void run() {
//				Log.d(DEBUG, String.format("Run %d", count.get()));
//				nextopBenchmark();
//
//				if (count.incrementAndGet() >= 100) {
//					timer.cancel();
//				}
//			}
//		}, 0, 10000);
//	}

	public void nextop(View view) {

		new AsyncTask<Context, Void, Benchmark.Result>() {

			@Override
			protected Benchmark.Result doInBackground(Context... params) {
				Benchmark benchmark = NextopBenchmark.using(params[0], benchmarkUrls);

				String runId = UUID.randomUUID().toString();
				ImmutableMap.Builder<String, String> runContext = ImmutableMap.builder();
				runContext.put("signal", signalStrength);

				benchmark.run(runId, runContext.build());

				return benchmark.result();
			}

			@Override
			protected void onPostExecute(Benchmark.Result result) {
				send("nextop", result);
				show(result);
			}

		}.execute(this);

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		timingsList = (ListView) findViewById(R.id.timings);
		timingsAdapter = new TimingAdapter(this);
		timingsList.setAdapter(timingsAdapter);

		loadBenchmarkUrls();

		TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		telephonyManager.listen(
						new PhoneStateListener() {

							public void onSignalStrengthsChanged(SignalStrength strength) {
								super.onSignalStrengthsChanged(strength);
								signalStrength = strength.toString();
							}

						}, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

		final WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		registerReceiver(new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				Log.d(DEBUG, Strings.repeat("-", 80));
				Log.d(DEBUG, wifiManager.getConnectionInfo().toString());
			}
		}, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

	}

	private void loadBenchmarkUrls() {
		try {
			InputStream in = getResources().openRawResource(R.raw.benchmark);
			String str = CharStreams.toString(new InputStreamReader(in, Charsets.UTF_8));

			benchmarkUrls = Lists.newArrayList();
			JSONArray jsonArr = new JSONArray(str);
			for (int i=0; i<jsonArr.length(); i++) {
				benchmarkUrls.add(jsonArr.getString(i));
			}

		} catch(IOException ie) {
			Log.d(DEBUG, "Could not find benchmark.json");
			finish();
		} catch(JSONException je) {
			Log.d(DEBUG, "Could not parse benchmark.json");
			finish();
		}
	}

	private void send(final String name, final Benchmark.Result result) {

		new AsyncTask<Benchmark.Result, Void, Void>() {
			@Override
			protected Void doInBackground(Benchmark.Result... results) {
				String resultUrl = Joiner
								.on('/')
								.skipNulls()
								.join("http://10.1.10.10:9091/result", name);

				try {
					JSONObject json = results[0].toJSON();
					DefaultHttpClient resultClient = new DefaultHttpClient();
					HttpPost post = new HttpPost(resultUrl);
					StringEntity entity = new StringEntity(json.toString());

					post.setEntity(entity);
					post.setHeader("Accept", "application/json");
					post.setHeader("Content-Type", "application/json");

					resultClient.execute(post);

				} catch(Exception e) {
					Log.d(DEBUG, "error", e);
				}

				return null;
			}
		}.execute(result);

	}

	private void send(final Benchmark.Result result) {
		send(null, result);
	}

	private void show(Benchmark.Result result) {
		timingsAdapter.clear();

		for (Benchmark.Result.Timing timing: result.getTimings()) {
			timingsAdapter.add(timing);
		}

		timingsAdapter.notifyDataSetChanged();
	}

}

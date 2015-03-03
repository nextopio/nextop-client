package io.nextop.demo.benchmark;

import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.nextop.Message;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import io.nextop.Route;
import io.nextop.WireValue;
import io.nextop.org.apache.http.client.utils.URIBuilder;
import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;


public class MainAcitivity extends Activity {
	public static final String DEBUG = "benchmark";

	public void onVolley(View view) {
		volley();
	}

	private void volley() {
		final Stopwatch watch = Stopwatch.createUnstarted();
		final Map<String, Long> record = Maps.newTreeMap();

		Log.d(DEBUG, Strings.repeat("-", 80));
		Log.d(DEBUG, "VOLLEY");

		final List<String> requests = ImmutableList.of(
					"http://10.1.10.10:9091/data/1kb",
					"http://10.1.10.10:9091/data/2kb",
					"http://10.1.10.10:9091/data/3kb",
					"http://10.1.10.10:9091/data/4kb",
					"http://10.1.10.10:9091/data/5kb",

					"http://10.1.10.10:9091/data/100kb",
					"http://10.1.10.10:9091/data/1mb",
					"http://10.1.10.10:9091/data/2mb"
		);

		final RequestQueue queue = Volley.newRequestQueue(this);

		final Response.ErrorListener onError = new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError err) {
				Log.d(DEBUG, "error", err);
			}
		};

		watch.start();

		int len = requests.size();
		for (int i=0; i<len; i++) {
			new AsyncTask<String, Void, Void>() {
				@Override
				protected Void doInBackground(String... urls) {
					RequestFuture<String> future = RequestFuture.newFuture();
					StringRequest req = new StringRequest(urls[0], future, onError);
					req.setShouldCache(false);
					queue.add(req);

					try {
						String resp = future.get();
						record.put(String.valueOf(resp.length()), watch.elapsed(TimeUnit.MILLISECONDS));
						Log.d(DEBUG, String.format("elapsed: %d ms, %d", watch.elapsed(TimeUnit.MILLISECONDS), resp.length()));

						if (urls[1] == "true") {
							Log.d(DEBUG, "completed");
							Log.d(DEBUG, record.toString());
							Log.d(DEBUG, String.format("done: %d ms", watch.elapsed(TimeUnit.MILLISECONDS)));
							record.put("completed", watch.elapsed(TimeUnit.MILLISECONDS));

							sendResult("volley", record);
						}

					} catch(Exception e) {}

					return null;
				}
			}.execute(requests.get(i), String.valueOf(i+1 == len));
		}

	}


	public void onNextop(View view) throws InterruptedException {

//		for(int i=0; i<10; i++) {
			nextop();
//			Thread.sleep(5000);
//		}

	}

	private void nextop() {
		final Stopwatch watch = Stopwatch.createUnstarted();
		final Map<String, Long> record = Maps.newTreeMap();

		Log.d(DEBUG, Strings.repeat("-", 80));
		Log.d(DEBUG, "NEXTOP");

		final Nextop nextop = NextopAndroid.getActive(this);

		final List<URI> requestUris = Lists.newArrayList(
						URI.create("http://10.1.10.10:9091/data/1kb"),
						URI.create("http://10.1.10.10:9091/data/2kb"),
						URI.create("http://10.1.10.10:9091/data/3kb"),
						URI.create("http://10.1.10.10:9091/data/4kb"),
						URI.create("http://10.1.10.10:9091/data/5kb"),

						URI.create("http://10.1.10.10:9091/data/100kb"),
						URI.create("http://10.1.10.10:9091/data/1mb"),
						URI.create("http://10.1.10.10:9091/data/2mb")
		);

		final Action1<Message> responseHandler = new Action1<Message>() {
			@Override
			public void call(Message result) {
				String resp = result.getContent().toString();
				record.put(String.valueOf(resp.length()), watch.elapsed(TimeUnit.MILLISECONDS));
				Log.d(DEBUG, String.format("elapsed: %d ms, %d", watch.elapsed(TimeUnit.MILLISECONDS), resp.length()));
			}
		};

		final Action1<Throwable> errorHandler = new Action1<Throwable>() {
			@Override
			public void call(Throwable err) {
				Log.d(DEBUG, "err", err);
			}
		};

		final Action0 completedHandler = new Action0() {
			@Override
			public void call() {
				Log.d(DEBUG, "completed");
				Log.d(DEBUG, record.toString());
				Log.d(DEBUG, String.format("elapsed: %d ms", watch.elapsed(TimeUnit.MILLISECONDS)));
				record.put("completed", watch.elapsed(TimeUnit.MILLISECONDS));
				sendResult("nextop", record);
			}
		};

		watch.start();

	  Message firstMessage = Message.valueOf(Route.Method.GET, requestUris.get(0));
		Observable<Message> messages = nextop.send(firstMessage);
		for (int i=1; i<requestUris.size(); i++) {
			Message req = Message.valueOf(Route.Method.GET, requestUris.get(i));
			messages = messages.concatWith(nextop.send(req));
			nextop.send(req).subscribe(responseHandler, errorHandler, completedHandler);

		}

		messages.subscribe(responseHandler, errorHandler, completedHandler);
	}

	static void sendResult(final String id, final Map<String, Long> result) {
		Log.d(DEBUG, "sendResult" + result.toString());

		new AsyncTask<Map<String, Long>, Void, Void>() {
			@Override
			protected Void doInBackground(Map<String, Long>... params) {

				try {
					JSONObject recordObject = new JSONObject(params[0]);
					recordObject.put("from", (Object) id);
					DefaultHttpClient resultClient = new DefaultHttpClient();
					HttpPost post = new HttpPost("http://10.1.10.10:9091/result");
					StringEntity entity = new StringEntity(recordObject.toString());

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
}

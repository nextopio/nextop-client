package io.nextop.demo.benchmark;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;

import org.apache.http.client.utils.URIUtils;
import org.json.JSONObject;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.nextop.Message;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import io.nextop.Route;
import io.nextop.WireValue;
import io.nextop.org.apache.http.client.utils.URIBuilder;
import rx.functions.Action0;
import rx.functions.Action1;


public class MainAcitivity extends Activity {
	public static final String DEBUG = "benchmark";

	public void volley(View view) {
		final Stopwatch watch = Stopwatch.createUnstarted();

		Log.d(DEBUG, Strings.repeat("-", 80));
		Log.d(DEBUG, "VOLLEY");

		final RequestQueue queue = Volley.newRequestQueue(this);

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

		final Response.Listener<String> onSuccess = new Response.Listener<String>() {
			@Override
			public void onResponse(String data) {
				Log.d(DEBUG, String.format("elapsed: %d ms", watch.elapsed(TimeUnit.MILLISECONDS)));
			}
		};

		final Response.ErrorListener onError = new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError err) {
				Log.d(DEBUG, "error", err);
			}
		};

		watch.start();
		for (final String target: requests) {
			StringRequest req = new StringRequest(Request.Method.GET, target, onSuccess, onError);
			req.setShouldCache(false);
			queue.add(req);
		}
	}

	public void nextop(View view) {
		final Stopwatch watch = Stopwatch.createUnstarted();

		Log.d(DEBUG, Strings.repeat("-", 80));
		Log.d(DEBUG, "NEXTOP");

		final Nextop nextop = NextopAndroid.getActive(this);

		final List<String> requests = ImmutableList.of(
						"GET http://10.1.10.10:9091/data/1kb",
						"GET http://10.1.10.10:9091/data/2kb",
						"GET http://10.1.10.10:9091/data/3kb",
						"GET http://10.1.10.10:9091/data/4kb",
						"GET http://10.1.10.10:9091/data/5kb",
						"GET http://10.1.10.10:9091/data/100kb",
						"GET http://10.1.10.10:9091/data/1mb",
						"GET http://10.1.10.10:9091/data/2mb"
		);

		final List<URI> requestUris = ImmutableList.of(
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
				Log.d(DEBUG, String.format("elapsed: %d ms", watch.elapsed(TimeUnit.MILLISECONDS)));
			}
		};

		final Action1<Throwable> errorHandler = new Action1<Throwable>() {
			@Override
			public void call(Throwable err) {
				Log.d(DEBUG, "err", err);
			}
		};

		watch.start();
		for (final URI target : requestUris) {
			Message req = Message.valueOf(Route.Method.GET, target);
			nextop.send(req).subscribe(responseHandler, errorHandler);
		}
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

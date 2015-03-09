package io.nextop.demo.benchmark;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.net.HttpHeaders;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;


public class BenchmarkReporter implements Benchmark.ResultListener {
	static final String LOG_TAG = BenchmarkReporter.class.getSimpleName();

	private final URL url;

	public BenchmarkReporter(URL reportingUrl) {
		url = reportingUrl;
	}

	@Override
	public void onResult(Benchmark.Result result) {
		new AsyncTask<Benchmark.Result, Void, Void>() {

			@Override
			protected Void doInBackground(Benchmark.Result... results) {
				try {
					post(url, results[0].toJSONObject());
				} catch(IOException ioe) {
					Log.e(LOG_TAG, "", ioe);
				}

				return null;
			}
		}.execute(result);

	}

	private void post(URL resultUrl, JSONObject json) throws IOException {
		DefaultHttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost(resultUrl.toString());
		StringEntity entity = new StringEntity(json.toString());

		post.setEntity(entity);
		post.setHeader(HttpHeaders.ACCEPT, "application/json");
		post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

		client.execute(post);
	}

	public static BenchmarkReporter withStringUrl(String str) {
		URL url = null;

		try {
			url = new URL(str);
		} catch(MalformedURLException mue) {
			Log.e(LOG_TAG, String.format("Will not report. Bad URL: %s", str), mue);
		}

		return new BenchmarkReporter(url);
	}
}

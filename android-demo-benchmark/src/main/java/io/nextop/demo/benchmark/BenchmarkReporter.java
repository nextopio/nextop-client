package io.nextop.demo.benchmark;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.net.HttpHeaders;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;

public class BenchmarkReporter implements RequestBenchmark.CompletionListener {
	static final String LOG_TAG = BenchmarkReporter.class.getSimpleName();

	final URL url;

	BenchmarkReporter(URL resultUrl) {
		url = resultUrl;
	}

	@Override
	public void onCompleted(RequestBenchmark.Record record) {
		Log.d(LOG_TAG, "post to: " + url.toString());

		new AsyncTask<RequestBenchmark.Record, Void, Void>() {
			@Override
			protected Void doInBackground(RequestBenchmark.Record... records) {
				try {
					post(url, records[0].toJSONObject());

				} catch(IOException io) {
					Log.e(LOG_TAG, "", io);
				}

				return null;
			}
		}.execute(record);

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

}

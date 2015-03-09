package io.nextop.demo.benchmark;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.nextop.Message;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import io.nextop.Route;
import rx.functions.Action1;

public class NextopJsonBenchmark extends Benchmark<URL> {
	static final String LOG_TAG = NextopJsonBenchmark.class.getSimpleName();

	private final Context context;

	private Nextop nextop;

	public NextopJsonBenchmark(Context appContext, List<URL> urls) {
		super(urls);
		context = appContext;
	}

	@Override
	void before(Map<String, String> runContext) {
		result.addValues(runContext);
		result.addValue("method", "nextop");

		nextop = NextopAndroid.getActive(context);
	}

	@Override
	void execute(ImmutableList<URL> urls) {
		for (final URL url: urls) {
			final String urlStr = url.toString();
			final Stopwatch watch = Stopwatch.createUnstarted();
			final long startTime = System.currentTimeMillis();

			Action1<Message> nextHandler = new Action1<Message>() {
				@Override
				public void call(Message message) {
					watch.stop();

					Measurement measurement = Measurement.asSuccessful(urlStr);
					measurement.setValue("start", startTime);
					measurement.setValue("elapsed", watch.elapsed(TimeUnit.MILLISECONDS));
					measurement.setValue("len", (long) message.getContent().toString().length());
					record(measurement);
				}
			};

			Action1<Throwable> errorHandler = new Action1<Throwable>() {
				@Override
				public void call(Throwable err) {
					Measurement measurement = Measurement.asFailed(urlStr);
					record(measurement);
				}
			};

			watch.start();

			Message request = Message.valueOf(Route.Method.GET, url);
			nextop.send(request).subscribe(nextHandler, errorHandler);
		}
	}

	public static NextopJsonBenchmark withStringUrls(Context context, List<String> strs) {
		List<URL> urls = Lists.newArrayListWithExpectedSize(strs.size());
		for (String str: strs) {
			try {
				urls.add(new URL(str));

			} catch(MalformedURLException mue) {
				Log.e(LOG_TAG, String.format("Skipping url: %s", str));
				Log.e(LOG_TAG, "", mue);

			}
		}

		return new NextopJsonBenchmark(context, urls);
	}
}

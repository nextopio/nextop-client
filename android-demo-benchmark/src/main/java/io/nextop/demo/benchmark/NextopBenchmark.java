package io.nextop.demo.benchmark;

import android.content.Context;
import android.util.Log;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.nextop.Message;
import io.nextop.Nextop;
import io.nextop.NextopAndroid;
import io.nextop.Route;
import rx.functions.Action1;

public class NextopBenchmark extends Benchmark {
	public static final String DEBUG = NextopBenchmark.class.getSimpleName();

	private final CountDownLatch latch;

	public NextopBenchmark(Context context, List<URL> urls) {
		super(context, urls);

		latch = new CountDownLatch(urls.size());
	}

	public Benchmark.Result result() {
		Uninterruptibles.awaitUninterruptibly(latch, 10, TimeUnit.SECONDS);
		return result;
	}

	public void run(String runId, Map<String, String> runContext) {
		result.addContext("type", "nextop");
		result.addContext("runId", runId);
		result.addContext("timestamp", String.valueOf(System.currentTimeMillis()));
		result.addContext(runContext);

		final Nextop nextop = NextopAndroid.getActive(context);
		for (final URL url: urls) {
			final Stopwatch watch = Stopwatch.createUnstarted();
			final long startTime = System.currentTimeMillis();

			Action1<Message> nextHandler = new Action1<Message>() {
				@Override
				public void call(Message message) {
					latch.countDown();
					watch.stop();

					result.addTiming(url.toString(),
									ImmutableMap.of(
													"start", startTime,
													"elapsed", watch.elapsed(TimeUnit.MILLISECONDS)));
				}
			};

			Action1<Throwable> errorHandler = new Action1<Throwable>() {
				@Override
				public void call(Throwable err) {
					Log.d(DEBUG, "error", err);
					latch.countDown();
				}
			};

			watch.start();

			Message message = Message.valueOf(Route.Method.GET, url);
			nextop.send(message).subscribe(nextHandler, errorHandler);
		}

	}

	public static NextopBenchmark using(Context context, List<String> benchmarkUrls) {
		List<URL> urls = Lists.transform(benchmarkUrls, new Function<String, URL>() {
			@Override
			public URL apply(@Nullable String str) {
				URL url = null;

				try {
					url = new URL(str);
				} catch (MalformedURLException mue) {
					Log.d(DEBUG, String.format("bad url: %s", url), mue);
				}

				return url;
			}
		});

		return new NextopBenchmark(context, urls);
	}
}

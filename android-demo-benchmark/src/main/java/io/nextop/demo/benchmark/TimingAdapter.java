package io.nextop.demo.benchmark;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TimingAdapter extends ArrayAdapter<Benchmark.Result.Timing> {
	static final int LAYOUT_RESOURCE = R.layout.timing_item;

	public TimingAdapter(Context context) {
		super(context, LAYOUT_RESOURCE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View timingItem = convertView;
		TimingParts parts = null;

		if (timingItem == null) {
			Context context = parent.getContext();
			LayoutInflater inflater = LayoutInflater.from(context);
			timingItem = inflater.inflate(LAYOUT_RESOURCE, parent, false);
			timingItem.setTag(parts);
		}

		Benchmark.Result.Timing timing = getItem(position);

		long startTime = timing.measurements().get("start");
		long elapsedTime = timing.measurements().get("elapsed");

		parts = new TimingParts();
		parts.startText = (TextView) timingItem.findViewById(R.id.startText);
		parts.elapsedText = (TextView) timingItem.findViewById(R.id.elapsedText);

		parts.startText.setText(String.valueOf(startTime));
		parts.elapsedText.setText(String.valueOf(elapsedTime));

		return timingItem;
	}

	static class TimingParts {
		TextView startText;
		TextView elapsedText;
	}

}

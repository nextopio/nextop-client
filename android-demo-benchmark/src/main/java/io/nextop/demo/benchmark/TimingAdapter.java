package io.nextop.demo.benchmark;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TimingAdapter extends ArrayAdapter<Benchmark.Measurement> {
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

		Benchmark.Measurement measurement = getItem(position);

		long startTime = measurement.getValue("start");
		long elapsedTime = measurement.getValue("elapsed");

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

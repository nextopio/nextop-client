package io.nextop.demo.benchmark;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BenchmarkMeasurementAdapter extends ArrayAdapter<Benchmark.Measurement> {
	static final int LAYOUT_RESOURCE = R.layout.measurement_item;

	public BenchmarkMeasurementAdapter(Context context) {
		super(context, LAYOUT_RESOURCE);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View measurementItem = convertView;
		MeasurementParts parts = null;

		if (measurementItem == null) {
			Context context = parent.getContext();
			LayoutInflater inflater = LayoutInflater.from(context);
			measurementItem = inflater.inflate(LAYOUT_RESOURCE, parent, false);
			measurementItem.setTag(parts);
		}

		Benchmark.Measurement measurement = getItem(position);

		long startTime = measurement.getValue("start");
		long elapsedTime = measurement.getValue("elapsed");
		long responseLen = measurement.getValue("len");

		parts = new MeasurementParts();
		parts.startText = (TextView) measurementItem.findViewById(R.id.startText);
		parts.elapsedText = (TextView) measurementItem.findViewById(R.id.elapsedText);
		parts.responseLenText = (TextView) measurementItem.findViewById(R.id.responseLenText);

		parts.startText.setText(String.valueOf(startTime));
		parts.elapsedText.setText(String.valueOf(elapsedTime));
		parts.responseLenText.setText(String.valueOf(responseLen));

		return measurementItem;
	}

	static class MeasurementParts {
		TextView startText;
		TextView elapsedText;
		TextView responseLenText;
	}

}

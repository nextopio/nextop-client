package io.nextop.client.view;

import android.content.Context;
import android.util.AttributeSet;

public class UploadImageView extends ImageView {

    public UploadImageView(Context context) {
        super(context);
    }
    public UploadImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public UploadImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public UploadImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    // FIXME set(id) to attach to existing upload (uses echo and status)
    // FIXME use the id only if there is no nurl (otherwise act like a normal image view)

}

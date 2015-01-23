package io.nextop.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.AttributeSet;
import com.google.common.annotations.Beta;
import io.nextop.Id;
import io.nextop.Nextop;
import io.nextop.NextopApplication;
import io.nextop.vm.ImageViewModel;
import rx.Observer;

import javax.annotation.Nullable;
import java.util.Random;


@Beta
public class ImageView extends android.widget.ImageView {

    public ImageView(Context context) {
        super(context);
    }
    public ImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public ImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    // FIXME set ImageLayerConfig
    public void setLayersConfig(Nextop.LayersConfig config) {

    }

    // FIXME setImageViewModel, onAttachToWindow bind, onUnattachFromWindow unbind


    public void clearImage() {

    }

    @Override
    public void setImageURI(Uri uri) {
        setImageUri(uri);
    }

    public void setImageUri(Uri uri) {

    }




    // set to an image in the progress of uploading (the id is the message that is sending the image)
    public void setLocalImage(Id id) {

    }



    // upload example: (see Controllers)
    // - send image via Nextop
    // - setUploadingImage
    // - model gets an update when server receives first layer
    // - imageview already has a local copy, do not load/set into imageview
    // - else, set(server uri)



    // FIXME demo hacks below
    float uploadProgress = 1.f;
    boolean online = true;

    Paint tempPaint = new Paint();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (uploadProgress < 1.f) {
            // if online, filled grey with white pie
            // if offline, outline grey with white pie

            float w = getWidth();
            float h = getHeight();
            float s = 0.2f * Math.min(w, h);

            tempPaint.setColor(Color.argb(128, 64, 64, 64));
            if (online) {
                tempPaint.setStyle(Paint.Style.FILL);

            } else {

                tempPaint.setStyle(Paint.Style.STROKE);
            }
            canvas.drawArc(w / 2.f - s, h / 2.f - s, w / 2.f + s, h / 2.f + s, 360 * uploadProgress, 360 * (1.f - uploadProgress), true, tempPaint);

            if (online) {
                tempPaint.setColor(Color.argb(128, 255, 255, 255));
            } else {
                tempPaint.setColor(Color.argb(64, 255, 255, 255));
            }
            tempPaint.setStyle(Paint.Style.FILL);
            canvas.drawArc(w / 2.f - s, h / 2.f - s, w / 2.f + s, h / 2.f + s, 0.f, 360 * uploadProgress, true, tempPaint);


        }

    }

    public static final class Updater implements Observer<ImageViewModel> {
        private final ImageView imageView;


        // FIXME demo hack
        @Nullable
        Runnable progressUpdater = null;


        public Updater(ImageView imageView) {
            this.imageView = imageView;
        }


        @Override
        public void onNext(final ImageViewModel imageVm) {
            if (null != imageVm.bitmap) {
                imageView.setImageBitmap(imageVm.bitmap);
            } else if (null != imageVm.localId) {
                imageView.setLocalImage(imageVm.localId);
            } else if (null != imageVm.uri) {
                imageView.setImageUri(imageVm.uri);
            } else {
                imageView.clearImage();
            }







            // FIXME demo hacks below

                progressUpdater = new Runnable() {
                    Random r = new Random();
                    @Override
                    public void run() {
                        if (this == progressUpdater) {


                            if (imageVm.uploadProgress < 1.f) {
                                if (((NextopApplication) imageView.getContext().getApplicationContext()).isOnline()) {
                                    imageVm.uploadProgress = Math.min(1.f, imageVm.uploadProgress + r.nextFloat() * 0.003f);
                                    imageView.online = true;
                                } else {
                                    imageView.online = false;
                                }

                                imageView.uploadProgress = imageVm.uploadProgress;
                                imageView.invalidate();

                                imageView.postDelayed(this, 1000 / 60);

                            } else {
                                imageView.uploadProgress = 1.f;
                                imageView.invalidate();
                                progressUpdater = null;
                            }

                        }
                    }
                };
                progressUpdater.run();

        }

        @Override
        public void onCompleted() {
            progressUpdater = null;
        }

        @Override
        public void onError(Throwable e) {
            progressUpdater = null;
        }
    }

}

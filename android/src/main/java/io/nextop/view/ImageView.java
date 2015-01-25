package io.nextop.view;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.util.AttributeSet;
import com.google.common.annotations.Beta;
import io.nextop.*;
import io.nextop.vm.ImageViewModel;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func2;
import rx.internal.util.SubscriptionList;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@Beta
public class ImageView extends android.widget.ImageView {
    @Nullable
    Nextop.LayersConfig layersConfig = null;


    @Nullable
    Source source = null;

    // loading loadSubscriptions FIXME rename to loadSubscriptions
    @Nullable
    SubscriptionList loadSubscriptions = null;

    @Nullable
    Progress progress = null;


    // CONFIG

    /** align visual transitions on these boundaries */
    int transitionQMs = 200;
    int fadeInMs = 200;

    int transferQPx = 48;
    float defaultMaxTransferMultiple = 2.f;
    int defaultMinTransferPx = 48;

    int downProgressTimeoutMs = 2000;


    // DRAW STATE

    Paint tempPaint = new Paint();



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


    private Nextop.LayersConfig createLayersConfig() {
        Nextop.LayersConfig config;
        if (null != layersConfig) {
            config = layersConfig.copy();
        } else {
            config = createDefaultLayersConfig();
        }
        // now compute the target sizes for each bounds

        int w = getWidth();
        int h = getHeight();

        // set the max values depending on the scale type
        int maxW;
        int maxH;
        switch (getScaleType()) {
            case CENTER:
            case CENTER_CROP:
                int s = Math.max(w, h);
                maxW = s;
                maxH = s;
                break;
            case CENTER_INSIDE:
            case FIT_CENTER:
            case FIT_END:
            case FIT_START:
            case FIT_XY:
                maxW = w;
                maxH = h;
                break;
            case MATRIX:
                // use the scale values from the matrix
                float[] mvalues = new float[9];
                getImageMatrix().getValues(mvalues);
                float sx = mvalues[Matrix.MSCALE_X];
                float sy = mvalues[Matrix.MSCALE_Y];
                maxW = Math.round(sx * w);
                maxH = Math.round(sy * h);
                break;
            default:
                throw new IllegalArgumentException();
        }
        maxW = Math.max(defaultMinTransferPx, maxW);
        maxH = Math.max(defaultMinTransferPx, maxH);

        // now quantize the max sizes to hit the same cache values in each band
        // (round up)
        maxW = ((maxW + transferQPx - 1) / transferQPx) * transferQPx;
        maxH = ((maxH + transferQPx - 1) / transferQPx) * transferQPx;

        for (Nextop.LayersConfig.Bound bound : config.receiveBounds) {
            bound.maxWidth = w;
            bound.maxHeight = h;
        }
        return config;
    }
    private Nextop.LayersConfig createDefaultLayersConfig() {
        // 1. set the max transfer size based on the current view size (multiple e.g. 2x)
        // 2. set the min transfer size of all but the last based on a fixed size (e.g. 48px)
        // 3. use two quality steps (0.3 and 1.0)

        int w = getWidth();
        int h = getHeight();

        Nextop.LayersConfig.Bound base = new Nextop.LayersConfig.Bound();
        base.maxTransferWidth = Math.round(defaultMaxTransferMultiple * w);
        base.maxTransferHeight = Math.round(defaultMaxTransferMultiple * h);
        base.minTransferWidth = defaultMinTransferPx;
        base.minTransferHeight = defaultMinTransferPx;

        Nextop.LayersConfig.Bound a = base.copy();
        a.quality = 0.3f;

        Nextop.LayersConfig.Bound b = base.copy();
        b.quality = 1.f;

        return Nextop.LayersConfig.receive(a, b);
    }



    // only the transfer properties are used in Bounds
    // the display properties are set by the view
    public void setLayersConfig(Nextop.LayersConfig layersConfig) {
        this.layersConfig = layersConfig;
        reload();
    }


    /////// SOURCE ///////

    public void clearImage() {
        setSource(null);
    }

    @Override
    public void setImageURI(Uri uri) {
        setImageUri(uri);
    }

    public void setImageUri(Uri uri) {
        setSource(Source.uri(uri));
    }

    // set to an image in the progress of uploading (the localId is the message that is sending the image)
    public void setLocalImage(Id id) {
        setSource(Source.local(id));
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        setSource(Source.memory(bitmap));
    }


    private void setSource(@Nullable Source source) {
        if (!Objects.equals(this.source, source)) {
            reset();
            this.source = source;
            reload();
        }
    }
    private void cancelLoadSubscriptions() {
        if (null != loadSubscriptions) {
            loadSubscriptions.unsubscribe();
            loadSubscriptions = null;
        }
    }
    private void reset() {
        cancelLoadSubscriptions();
        setProgress(null);
        setImageDrawable(null);
    }
    private void reload() {
        cancelLoadSubscriptions();
        setProgress(null);

        if (null != source) {
            switch (source.type) {
                case URI: {
                    loadUri(source.uri);
                    break;
                }
                case LOCAL: {
                    loadLocal(source.localId);
                    break;
                }
                case MEMORY: {
                    loadMemory(source.bitmap);
                    break;
                }
            }
        }
    }
    private void loadUri(Uri uri) {
        assert null == loadSubscriptions;

        @Nullable Nextop nextop = NextopAndroid.getActive(this);
        if (null != nextop) {
            loadSubscriptions = new SubscriptionList();

            Message message = MessageAndroid.valueOf(Nurl.Method.GET, uri);

            Observable<Progress> downProgressSource = Observable.combineLatest(nextop.transferStatus(message.id), nextop.connectionStatus(),
                    new Func2<Nextop.TransferStatus, Nextop.ConnectionStatus, Progress>() {
                        @Override
                        public Progress call(Nextop.TransferStatus transferStatus, Nextop.ConnectionStatus connectionStatus) {
                            return Progress.download(transferStatus.progress, connectionStatus.online);
                        }
                    }).delaySubscription(downProgressTimeoutMs, TimeUnit.MILLISECONDS);
            // cancel this subscription on the first emitted layer (below)
            final Subscription progressSubscription = downProgressSource.subscribe(new ProgressLoader());
            loadSubscriptions.add(progressSubscription);


            LayerLoader loader = new LayerLoader();
            loader.immediate = true;
            loadSubscriptions.add(nextop.send(message,
                    createLayersConfig()).doOnNext(new Action1<Nextop.Layer>() {
                @Override
                public void call(Nextop.Layer layer) {
                    progressSubscription.unsubscribe();
                }
            }).subscribe(loader));
            loader.immediate = false;


        }
    }
    private void loadLocal(Id id) {
        assert null == loadSubscriptions;

        @Nullable Nextop nextop = NextopAndroid.getActive(this);
        if (null != nextop) {
            loadSubscriptions = new SubscriptionList();

            Message message = Message.newBuilder().setNurl(Message.echoNurl(id)).build();


            Observable<Progress> upProgressSource = Observable.combineLatest(nextop.transferStatus(message.id), nextop.connectionStatus(),
                    new Func2<Nextop.TransferStatus, Nextop.ConnectionStatus, Progress>() {
                        @Override
                        public Progress call(Nextop.TransferStatus transferStatus, Nextop.ConnectionStatus connectionStatus) {
                            return Progress.upload(transferStatus.progress, connectionStatus.online);
                        }
                    });
            Subscription progressSubscription = upProgressSource.subscribe(new ProgressLoader());
            loadSubscriptions.add(progressSubscription);


            LayerLoader loader = new LayerLoader();
            loader.immediate = true;
            loadSubscriptions.add(nextop.send(message,
                    createLayersConfig()).subscribe(loader));
            loader.immediate = false;
        }
    }
    private void loadMemory(Bitmap bitmap) {
        setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }


    /////// PROGRESS ///////

    private void setProgress(@Nullable Progress progress) {
        if (!Objects.equals(this.progress, progress)) {
            this.progress = progress;
            invalidate();
        }
    }


    /////// VIEW OVERRIDES ///////

    @Override
    public void setImageMatrix(Matrix matrix) {
        super.setImageMatrix(matrix);
        reload();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        reload();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        reload();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        reset();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (null != progress && progress.progress < 1.f) {
            // TODO
            // if online, filled grey with white pie
            // if offline, outline grey with white pie

            float w = getWidth();
            float h = getHeight();
            float s = 0.2f * Math.min(w, h);

            tempPaint.setColor(Color.argb(128, 64, 64, 64));
            if (progress.active) {
                tempPaint.setStyle(Paint.Style.FILL);
            } else {
                tempPaint.setStyle(Paint.Style.STROKE);
            }
            canvas.drawArc(w / 2.f - s, h / 2.f - s, w / 2.f + s, h / 2.f + s,
                    360 * progress.progress, 360 * (1.f - progress.progress),
                    true, tempPaint);

            if (progress.active) {
                tempPaint.setColor(Color.argb(128, 255, 255, 255));
            } else {
                tempPaint.setColor(Color.argb(64, 255, 255, 255));
            }
            tempPaint.setStyle(Paint.Style.FILL);
            canvas.drawArc(w / 2.f - s, h / 2.f - s, w / 2.f + s, h / 2.f + s,
                    0.f, 360 * progress.progress,
                    true, tempPaint);
        }
    }





    private final class LayerLoader implements Observer<Nextop.Layer> {
        /** if true, the layer should be immediately set into the view (no transition);
         * otherwise, do a fade in on a quantized schedule. */
        boolean immediate = false;
        int count = 0;


        LayerLoader() {
        }


        private void set(Bitmap bitmap) {
            ++count;
            Drawable d;
            if (!immediate && 1 == count) {
                TransitionDrawable td = new TransitionDrawable(new Drawable[]{
                        new ColorDrawable(Color.argb(0, 0, 0, 0)),
                        new BitmapDrawable(getResources(), bitmap)
                });
                td.startTransition(fadeInMs);
                d = td;
            } else {
                d = new BitmapDrawable(getResources(), bitmap);
            }
            setImageDrawable(d);
        }


        @Override
        public void onNext(Nextop.Layer layer) {
            switch (layer.type) {
                case BITMAP:
                    set(layer.bitmap);
                    break;
                default:
                    // ignore
                    break;
            }
        }

        @Override
        public void onCompleted() {
            // Do nothing
        }

        @Override
        public void onError(Throwable e) {
            // TODO
        }
    }

    private final class ProgressLoader implements Observer<Progress> {
        @Override
        public void onNext(Progress progress) {
            setProgress(progress);
        }

        @Override
        public void onCompleted() {
            setProgress(null);
        }

        @Override
        public void onError(Throwable e) {
            // TODO
            setProgress(null);
        }
    }



    private static final class Source {
        static enum Type {
            URI,
            LOCAL,
            MEMORY
        }

        static Source uri(@Nullable Uri uri) {
            if (null != uri) {
                return new Source(Type.URI, uri, null, null);
            } else {
                return null;
            }
        }
        static Source local(@Nullable Id id) {
            if (null != id) {
                return new Source(Type.LOCAL, null, id, null);
            } else {
                return null;
            }
        }
        static Source memory(@Nullable Bitmap bitmap) {
            if (null != bitmap) {
                return new Source(Type.MEMORY, null, null, bitmap);
            } else {
                return null;
            }
        }

        final Type type;
        @Nullable
        final Uri uri;
        @Nullable
        final Id localId;
        @Nullable
        final Bitmap bitmap;

        Source(Type type, Uri uri, Id localId, Bitmap bitmap) {
            this.type = type;
            this.uri = uri;
            this.localId = localId;
            this.bitmap = bitmap;
        }


        @Override
        public int hashCode() {
            int c = type.hashCode();
            c = 31 * c + Objects.hashCode(uri);
            c = 31 * c + Objects.hashCode(localId);
            c = 31 * c + (null != bitmap ? System.identityHashCode(bitmap) : 0);
            return c;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Source)) {
                return false;
            }
            Source b = (Source) o;
            return type.equals(b.type)
                    && Objects.equals(uri, b.uri)
                    && Objects.equals(localId, b.localId)
                    && bitmap == b.bitmap;
        }
    }

    private static final class Progress {
        static enum Type {
            DOWNLOAD,
            UPLOAD
        }


        static Progress download(float progress, boolean active) {
            return create(Type.DOWNLOAD, progress, active);
        }
        static Progress upload(float progress, boolean active) {
            return create(Type.UPLOAD, progress, active);
        }
        static Progress create(Type type, float progress, boolean active) {
            float np = Math.max(0.f, Math.min(1.f, progress));
            // normalize to percent
            np = (int) (np * 100) / 100.f;
            return new Progress(type, np, active);
        }


        final Type type;
        final float progress;
        final boolean active;

        private Progress(Type type, float progress, boolean active) {
            this.type = type;
            this.progress = progress;
            this.active = active;
        }

        @Override
        public int hashCode() {
            int c = type.hashCode();
            c = 31 * c + Float.floatToIntBits(progress);
            c = 31 * c + (active ? 1 : 0);
            return c;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Progress)) {
                return false;
            }
            Progress b = (Progress) o;
            return type.equals(b.type)
                    && progress == b.progress
                    && active == b.active;
        }
    }






    public static final class Updater implements Observer<ImageViewModel> {
        private final ImageView imageView;


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

        }

        @Override
        public void onCompleted() {
            // Do nothing
        }

        @Override
        public void onError(Throwable e) {
            // Do nothing
        }
    }

}

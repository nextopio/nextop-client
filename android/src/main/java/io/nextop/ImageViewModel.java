package io.nextop;

import android.net.Uri;
import com.google.common.base.Objects;
import io.nextop.view.ImageView;
import rx.Subscription;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import javax.annotation.Nullable;

public class ImageViewModel {
    private Source source = new Source(null, null);
    private PublishSubject<Source> sourceSubject = PublishSubject.create();


    public ImageViewModel() {
    }


    public void set(@Nullable Uri uri, @Nullable Id localId) {
        Source s = new Source(uri, localId);
        if (!Objects.equal(source, s)) {
            source = s;
            sourceSubject.onNext(s);
        }
    }


    public Subscription bind(final ImageView imageView) {
        _bind(imageView, source);
        return sourceSubject.subscribe(new Action1<Source>() {
            @Override
            public void call(Source s) {
                _bind(imageView, s);
            }
        });
    }
    private void _bind(ImageView imageView, Source s) {
        if (null != s.uri) {
            imageView.setImage(s.uri);
        } else if (null != s.localId) {
            imageView.setLocalImage(s.localId);
        } else {
            imageView.clearImage();
        }
    }


    private static class Source {
        @Nullable
        final Uri uri;
        @Nullable
        final Id localId;

        private Source(@Nullable Uri uri, @Nullable Id localId) {
            this.uri = uri;
            this.localId = localId;
        }
    }
}

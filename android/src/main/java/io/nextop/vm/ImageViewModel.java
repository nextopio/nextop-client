package io.nextop.vm;

import android.graphics.Bitmap;
import android.net.Uri;
import com.google.common.base.Objects;
import io.nextop.Id;

import javax.annotation.Nullable;

public class ImageViewModel {

    public static ImageViewModel remote(Uri uri) {
        return new ImageViewModel(uri, null, null);
    }
    public static ImageViewModel local(Id localId) {
        return new ImageViewModel(null, localId, null);
    }
    public static ImageViewModel memory(Bitmap bitmap) {
        return new ImageViewModel(null, null, bitmap);
    }

    @Nullable
    public final Uri uri;
    @Nullable
    public final Id localId;
    @Nullable
    public final Bitmap bitmap;


    public ImageViewModel(@Nullable Uri uri, @Nullable Id localId, @Nullable Bitmap bitmap) {
        this.uri = uri;
        this.localId = localId;
        this.bitmap = bitmap;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri, localId, bitmap);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImageViewModel)) {
            return false;
        }
        ImageViewModel b = (ImageViewModel) o;
        return Objects.equal(uri, b.uri)
                && Objects.equal(localId, b.localId)
                && Objects.equal(bitmap, b.bitmap);
    }
}

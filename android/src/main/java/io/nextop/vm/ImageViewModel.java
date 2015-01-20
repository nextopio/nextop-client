package io.nextop.vm;

import android.net.Uri;
import com.google.common.base.Objects;
import io.nextop.Id;

import javax.annotation.Nullable;

public class ImageViewModel {
    @Nullable
    public final Uri uri;
    @Nullable
    public final Id localId;

    public ImageViewModel(@Nullable Uri uri, @Nullable Id localId) {
        this.uri = uri;
        this.localId = localId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri, localId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImageViewModel)) {
            return false;
        }
        ImageViewModel b = (ImageViewModel) o;
        return Objects.equal(uri, b.uri) && Objects.equal(localId, b.localId);
    }
}

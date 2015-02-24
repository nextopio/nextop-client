package io.nextop;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewParent;
import com.google.common.annotations.Beta;

import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;

@Beta
public final class NextopAndroid {

    /** Find the nearest nextop for the view:
     * - tests the view
     * - tests the activity
     * - tests the application
     * - tests down the view hierarchy, for views and fragments (in that order per level).
     * Typically there will be one nextop in the application for the entire app.
     * @see NextopContext */
    @Nullable
    public static Nextop getActive(View view) {
        Nextop nextop;
        nextop = opt(view);
        if (null != nextop) {
            return nextop;
        }
        Activity activity = (Activity) view.getContext();
        nextop = opt(activity);
        if (null != nextop) {
            return nextop;
        }
        nextop = opt(activity.getApplication());
        if (null != nextop) {
            return nextop;
        }
        FragmentManager fm = activity.getFragmentManager();
        for (ViewParent p = view.getParent(); null != p; p = p.getParent()) {
            nextop = opt(p);
            if (null != nextop) {
                return nextop;
            }
            if (p instanceof View) {
                @Nullable Fragment f = fm.findFragmentById(((View) p).getId());
                if (null != f) {
                    nextop = opt(f);
                    if (null != nextop) {
                        return nextop;
                    }
                }
            }
        }
        assert null == nextop;
        return null;
    }

    @Nullable
    public static Nextop getActive(Context context) {
        Nextop nextop;
        nextop = opt(context);
        if (null != nextop) {
            return nextop;
        }
        nextop = opt(context.getApplicationContext());
        if (null != nextop) {
            return nextop;
        }
        assert null == nextop;
        return null;
    }

    @Nullable
    private static Nextop opt(Object obj) {
        if (obj instanceof NextopContext) {
            @Nullable Nextop nextop = ((NextopContext) obj).getNextop();
            if (null != nextop && nextop.isActive()) {
                return nextop;
            }
        }
        return null;
    }


    /////// URI ///////

    public static URI toURI(Uri uri) throws URISyntaxException {
        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            // FIXME try to recover common/bug cases
            throw e;
        }
    }


    private NextopAndroid() {
    }
}

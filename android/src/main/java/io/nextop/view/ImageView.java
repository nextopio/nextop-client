package io.nextop.view;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;
import com.google.common.annotations.Beta;
import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Nextop;
import io.nextop.Nurl;


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
        setImage(uri);
    }

    public void setImage(Uri uri) {

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




}

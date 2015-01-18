package io.nextop.client;

import android.database.Observable;
import android.graphics.Bitmap;
import io.nextop.Message;
import io.nextop.Nurl;

import java.util.List;

public class Nextop {


    public Nextop(String accessKey, String ... grantKeys) {

    }






    Observable<Message> send(io.nextop.Message message) {

        return null;
    }

    Observable<Message> receive(Nurl nurl) {

        return null;
    }


    // send can be GET for image, POST/PUT of new image
    // config controls both up and down, when present
    Observable<ImageLayer> send(Message message, ImageLayersConfig config) {

        return null;
    }

    Observable<ImageLayer> receive(Nurl nurl, ImageLayersConfig config) {

        return null;
    }



    // some bounds might ignored on HTTP (maxBytes, maxMs) - they are passed as pragma
    //
    public static class ImageLayersConfig {
        List<Bounds> sendBounds;
        List<Bounds> receiveBounds;


        static class Bounds {
            // affects url
            int maxCacheWidth;
            // affects url
            int maxCacheHeight;

            // affects url
            int maxBytes;
            // affects url
            int maxMs;


            // does not affect cache url; decode only
            int maxWidth;
            // does not affect cache url; decode only
            int maxHeight;

            boolean isFull() {
                return 0 == maxWidth && 0 == maxHeight;
            }
        }
    }

    public static class ImageLayer {
        Bitmap bitmap;
        // FIXME can this be here?
        ImageLayersConfig.Bounds bounds;
        boolean last;



    }





}

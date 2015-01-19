package io.nextop;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.nextop.client.MessageControlNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import rx.Observable;
import rx.functions.Func1;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Beta
public class Nextop {
    /** The android:name to use in application meta-data to set the access key
     * @see #create(android.content.Context) */
    public static final String M_ACCESS_KEY = "NextopAccessKey";
    /** The android:name to use in application meta-data to set the grant key(s).
     * Can point to a single string or an string array.
     * @see #create(android.content.Context) */
    public static final String M_GRANT_KEYS = "NextopGrantKeys";


    public static Nextop create(Context context) {
        try {
            Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;

            @Nullable String accessKey = metaData.getString(M_ACCESS_KEY);

            @Nullable String[] grantKeys = metaData.getStringArray(M_GRANT_KEYS);
            if (null == grantKeys) {
                @Nullable String oneGrantKey = metaData.getString(M_GRANT_KEYS);
                if (null != oneGrantKey) {
                    grantKeys = new String[]{oneGrantKey};
                }
            }

            return create(Auth.create(accessKey, grantKeys));
        } catch (IllegalArgumentException e) {
            // FIXME log this
            return create((Auth) null);
        } catch (PackageManager.NameNotFoundException e) {
            // FIXME log this
            return create((Auth) null);
        }
    }

    public static Nextop create(String accessKey, String ... grantKeys) {
        return create(Auth.create(accessKey, grantKeys));
    }

    private static Nextop create(@Nullable Auth auth) {
        return new Nextop(auth);
    }

    private static Nextop create(Nextop copy) {
        return new Nextop(copy.auth);
    }



    @Nullable
    protected final Auth auth;


    private Nextop(@Nullable Auth auth) {
        this.auth = auth;

    }


    public Nextop start() {
        if (null != auth) {
            // in this case the access key might still be bad/disabled/unreachable,
            // and the client will fall back
            return Full.start(this);
        } else {
            // in this case the client won't waste time negotiating with the nextop service
            // it will start in fall back
            return Limited.start(this);
        }
    }

    /** Typically this should not be called outside of testing - an app should run indefinitely until terminated by the OS.
     * If called, further calls on this object will result in unspecified behavior.
     * Use the returned object to restart the client. */
    public Nextop stop() {
        return this;
    }

    boolean isActive() {
        return false;
    }


    public Receiver<Message> send(Message message) {
        throw new IllegalStateException("Call on a started nextop.");
    }

    public Receiver<Message> receive(Nurl nurl) {
        throw new IllegalStateException("Call on a started nextop.");
    }


    /////// HTTPCLIENT MIGRATION HELPER ///////

    public HttpResponse execute(HttpUriRequest request) {
        HttpResponse noResponse = null;
        return send(request).in.onErrorReturn(new Func1<Throwable, HttpResponse>() {
            @Override
            public HttpResponse call(Throwable throwable) {
                // FIXME unreachable result
                // FIXME
                return null;
            }
        }).defaultIfEmpty(
        /* FIXME OK result -- no response if using NX protocol (messages don't have to have a response) */noResponse
        ).toBlocking().single();
    }

    public Receiver<HttpResponse> send(HttpUriRequest request) {
        Message message = Message.fromHttpRequest(request);
        return new Receiver<HttpResponse>(message.receiverNurl(),
                send(message).in.map(new Func1<Message, HttpResponse>() {
                    @Override
                    public HttpResponse call(Message message) {
                        return Message.toHttpResponse(message);
                    }
                }));
    }

    public Receiver<Layer> send(HttpUriRequest request, LayersConfig config) {
        return send(Message.fromHttpRequest(request), config);
    }


    /////// IMAGE ///////

    // send can be GET for image, POST/PUT of new image
    // config controls both up and down, when present
    public Receiver<Layer> send(Message message, LayersConfig config) {
        throw new IllegalStateException("Call on a started nextop.");
    }

    public Receiver<Layer> receive(Nurl nurl, LayersConfig config) {
        throw new IllegalStateException("Call on a started nextop.");
    }

    public static final class LayersConfig {
        public static LayersConfig send(Bounds ... bounds) {
            return new LayersConfig(ImmutableList.copyOf(bounds), ImmutableList.<Bounds>of());
        }
        public static LayersConfig receive(Bounds ... bounds) {
            return new LayersConfig(ImmutableList.<Bounds>of(), ImmutableList.copyOf(bounds));
        }


        /** ordered worst to best quality */
        final List<Bounds> sendBounds;
        /** ordered worst to best quality */
        final List<Bounds> receiveBounds;

        LayersConfig(List<Bounds> sendBounds, List<Bounds> receiveBounds) {
            this.sendBounds = sendBounds;
            this.receiveBounds = receiveBounds;
        }


        public LayersConfig andSend(Bounds ... bounds) {
            return new LayersConfig(ImmutableList.copyOf(bounds), receiveBounds);
        }

        public LayersConfig andReceive(Bounds ... bounds) {
            return new LayersConfig(sendBounds, ImmutableList.copyOf(bounds));
        }


        // FIXME
        public static final class Bounds {
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

    public static final class Layer {
        public static enum Type {
            BITMAP,
            UNPARSEABLE
        }


        public static Layer bitmap(Bitmap bitmap, boolean bestQuality) {
            return new Layer(Type.BITMAP, bitmap, null, bestQuality);
        }

        public static Layer unknown(Message message, boolean bestQuality) {
            return new Layer(Type.UNPARSEABLE, null, message, bestQuality);
        }


        public final Type type;
        public final @Nullable Bitmap bitmap;
        public final @Nullable Message message;
        public final boolean bestQuality;

        Layer(Type type, Bitmap bitmap, Message message, boolean bestQuality) {
            this.type = type;
            this.bitmap = bitmap;
            this.message = message;
            this.bestQuality = bestQuality;
        }
    }



    public static final class Receiver<T> {
        public final Nurl nurl;
        public final Observable<T> in;

        Receiver(Nurl nurl, Observable<T> in) {
            this.nurl = nurl;
            this.in = in;
        }

        // return the localId of the outgoing message that this receiver is tied to
        // return null if there is no outgoing message for this nurl
        @Nullable
        public Id getId() {
            return nurl.getLocalId();
        }
    }




    private static abstract class GoNoded extends Nextop {


        protected GoNoded(@Nullable Auth auth) {
            super(auth);

            // FIXME create Nextop nodes, init, and start
        }


        protected abstract MessageControlNode createNode();


        @Override
        public Nextop start() {
            throw new IllegalArgumentException("Already started.");
        }

        @Override
        public Nextop stop() {
            // FIXME stop nodes, close all observers

            return Nextop.create(this);
        }

        @Override
        boolean isActive() {
            return true;
        }


        @Override
        public Receiver<Message> send(Message message) {
            // FIXME
            return null;
        }

        @Override
        public Receiver<Message> receive(Nurl nurl) {
            // FIXME
            return null;
        }

        @Override
        public Receiver<Layer> send(Message message, LayersConfig config) {
            // FIXME
            return null;
        }

        @Override
        public Receiver<Layer> receive(Nurl nurl, LayersConfig config) {
            // FIXME
            return null;
        }
    }


    private static final class Full extends GoNoded {
        static Full start(Nextop copy) {
            return new Full(copy.auth);
        }

        private Full(@Nullable Auth auth) {
            super(auth);

        }

        @Override
        protected MessageControlNode createNode() {
            // FIXME
            return null;
        }
    }

    private static final class Limited extends GoNoded {
        static Limited start(Nextop copy) {
            return new Limited(copy.auth);
        }

        private Limited(@Nullable Auth auth) {
            super(auth);

        }

        @Override
        protected MessageControlNode createNode() {
            // FIXME
            return null;
        }
    }


    /////// AUTH ///////

    private static final class Auth {
        @Nullable
        static Auth create(@Nullable String accessKey, @Nullable String[] grantKeys) {
            if (null != accessKey) {
                Id accessKeyId = Id.valueOf(accessKey);
                Set<Id> grantKeyIds;
                if (null != grantKeys) {
                    grantKeyIds = new HashSet<Id>(grantKeys.length);
                    for (String grantKey : grantKeys) {
                        if (null != grantKey) {
                            grantKeyIds.add(Id.valueOf(grantKey));
                        }
                    }
                } else {
                    grantKeyIds = Collections.emptySet();
                }
                return create(accessKeyId, grantKeyIds);
            } else {
                return null;
            }
        }

        static Auth create(Id accessKeyId, Iterable<Id> grantKeysIds) {
            if (null == accessKeyId) {
                throw new IllegalArgumentException();
            }
            if (null == grantKeysIds) {
                throw new IllegalArgumentException();
            }
            return new Auth(accessKeyId, ImmutableSet.copyOf(grantKeysIds));
        }


        final Id accessKey;
        final Set<Id> grantKeys;

        private Auth(Id accessKey, Set<Id> grantKeys) {
            this.accessKey = accessKey;
            this.grantKeys = grantKeys;
        }
    }
}

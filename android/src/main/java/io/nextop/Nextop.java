package io.nextop;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.nextop.client.HttpNode;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import io.nextop.client.SubjectNode;
import io.nextop.org.apache.http.HttpResponse;
import io.nextop.org.apache.http.client.methods.HttpUriRequest;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

// calls all receives on the MAIN thread
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
            @Nullable Bundle metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;

            if (null != metaData) {
                @Nullable String accessKey = metaData.getString(M_ACCESS_KEY);

                @Nullable String[] grantKeys = metaData.getStringArray(M_GRANT_KEYS);
                if (null == grantKeys) {
                    @Nullable String oneGrantKey = metaData.getString(M_GRANT_KEYS);
                    if (null != oneGrantKey) {
                        grantKeys = new String[]{oneGrantKey};
                    }
                }

                return create(context, Auth.create(accessKey, grantKeys));
            } else {
                return create(context, (Auth) null);
            }
        } catch (IllegalArgumentException e) {
            // FIXME log this
            return create(context, (Auth) null);
        } catch (PackageManager.NameNotFoundException e) {
            // FIXME log this
            return create(context, (Auth) null);
        }
    }

    public static Nextop create(Context context, String accessKey, String ... grantKeys) {
        return create(context, Auth.create(accessKey, grantKeys));
    }

    private static Nextop create(Context context, @Nullable Auth auth) {
        return new Nextop(context, auth);
    }

    private static Nextop create(Context context, Nextop copy) {
        return new Nextop(context, copy.auth);
    }



    protected final Context context;
    @Nullable
    protected final Auth auth;


    private Nextop(Context context, @Nullable Auth auth) {
        this.context = context;
        this.auth = auth;

    }


    @Nullable
    public Auth getAuth() {
        return auth;
    }


    public Nextop start() {
        // FIXME 0.2 see roadmap
//        if (null != auth) {
//            // in this case the access key might still be bad/disabled/unreachable,
//            // and the client will fall back
//            return Full.start(this);
//        } else {
            // in this case the client won't waste time negotiating with the nextop service
            // it will start in fall back
            return Limited.start(this);
//        }
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

    public Receiver<Message> receive(Route route) {
        throw new IllegalStateException("Call on a started nextop.");
    }

    public void cancelSend(Id id) {
        throw new IllegalStateException("Call on a started nextop.");
    }

    /////// HTTPCLIENT MIGRATION HELPER ///////

    public HttpResponse execute(HttpUriRequest request) {
        // FIXME 0.2
        HttpResponse noResponse = null;
        return send(request).onErrorReturn(new Func1<Throwable, HttpResponse>() {
            @Override
            public HttpResponse call(Throwable throwable) {
                // FIXME unreachable result
                // FIXME
                return null;
            }
        }).defaultIfEmpty(
        /* FIXME OK result -- no response if using NX protocol (messages don't have to have a response)
         * FIXME this needs to be on a timeout */noResponse
        ).toBlocking().single();
    }

    public Receiver<HttpResponse> send(HttpUriRequest request) {
        Message message = Message.fromHttpRequest(request);
        return Receiver.create(this, message.inboxRoute(),
                send(message).map(new Func1<Message, HttpResponse>() {
                    @Override
                    public HttpResponse call(Message message) {
                        return Message.toHttpResponse(message);
                    }
                }));
    }

    public Receiver<Layer> send(HttpUriRequest request, @Nullable LayersConfig config) {
        return send(Layer.message(Message.fromHttpRequest(request)), config);
    }


    /////// IMAGE ///////

    // send can be GET for image, POST/PUT of new image
    // config controls both up and down, when present
    public Receiver<Layer> send(Layer layer, @Nullable LayersConfig config) {
        throw new IllegalStateException("Call on a started nextop.");
    }

    public static final class LayersConfig {
        public static LayersConfig send(Bound... bounds) {
            return new LayersConfig(ImmutableList.copyOf(bounds), ImmutableList.<Bound>of());
        }
        public static LayersConfig receive(Bound... bounds) {
            return new LayersConfig(ImmutableList.<Bound>of(), ImmutableList.copyOf(bounds));
        }


        /** ordered worst to best quality */
        public final List<Bound> sendBounds;
        /** ordered worst to best quality */
        public final List<Bound> receiveBounds;

        LayersConfig(List<Bound> sendBounds, List<Bound> receiveBounds) {
            this.sendBounds = sendBounds;
            this.receiveBounds = receiveBounds;
        }


        public LayersConfig andSend(Bound... bounds) {
            return new LayersConfig(ImmutableList.copyOf(bounds), receiveBounds);
        }

        public LayersConfig andReceive(Bound... bounds) {
            return new LayersConfig(sendBounds, ImmutableList.copyOf(bounds));
        }


        public LayersConfig copy() {
            List<Bound> sendBoundsCopy = new ArrayList<Bound>(sendBounds.size());
            List<Bound> receiveBoundsCopy = new ArrayList<Bound>(receiveBounds.size());
            for (Bound sendBound : sendBounds) {
                sendBoundsCopy.add(sendBound.copy());
            }
            for (Bound receiveBound : receiveBounds) {
                receiveBoundsCopy.add(receiveBound.copy());
            }
            return new LayersConfig(ImmutableList.copyOf(sendBoundsCopy), ImmutableList.copyOf(receiveBoundsCopy));
        }


        // once passed off, consider this immutable
        public static final class Bound {
            // TRANSFER

            // affects url
            public int maxTransferWidth = -1;
            // affects url
            public int maxTransferHeight = -1;

            // the layer is ignored if the transferred size is less than this
            public int minTransferWidth = -1;
            // the layer is ignored if the transferred size is less than this
            public int minTransferHeight = -1;

            // affects url
            public float quality = 1.f;

            public Id groupId = Message.DEFAULT_GROUP_ID;
            // affects transmission
            public int groupPriority = Message.DEFAULT_GROUP_PRIORITY;


            // DISPLAY

            // does not affect cache url; decode only
            public int maxWidth = -1;
            // does not affect cache url; decode only
            public int maxHeight = -1;


            public Bound copy() {
                Bound copy = new Bound();
                copy.maxTransferWidth = maxTransferWidth;
                copy.maxTransferHeight = maxTransferHeight;
                copy.minTransferWidth = minTransferWidth;
                copy.minTransferHeight = minTransferHeight;
                copy.quality = quality;
                copy.groupId = groupId;
                copy.groupPriority = groupPriority;
                copy.maxWidth = maxWidth;
                copy.maxHeight = maxHeight;
                return copy;
            }
        }
    }

    public static final class Layer {

        public static Layer message(Message message) {
            return message(message, true);
        }

        public static Layer message(Message message, boolean last) {
            return new Layer(message, null, last);
        }

        public static Layer bitmap(Message message, Bitmap bitmap) {
            return bitmap(message, bitmap, true);
        }

        public static Layer bitmap(Message message, Bitmap bitmap, boolean last) {
            return new Layer(message, bitmap, last);
        }


        public final Message message;
        @Nullable
        public final Bitmap bitmap;
        public final boolean last;

        Layer(Message message, @Nullable Bitmap bitmap, boolean last) {
            if (null == message) {
                throw new IllegalArgumentException();
            }
            this.message = message;
            this.bitmap = bitmap;
            this.last = last;
        }
    }


    /////// CONNECTION STATUS ///////

    public Observable<ConnectionStatus> connectionStatus() {
        return Observable.just(new ConnectionStatus(false));
    }


    public static final class ConnectionStatus {
        public final boolean online;

        ConnectionStatus(boolean online) {
            this.online = online;
        }
    }


    /////// TRANSFER STATUS ///////

    public Observable<TransferStatus> transferStatus(Id id) {
        Message statusMessage = Message.newBuilder().setRoute(Message.statusRoute(id)).build();
        return send(statusMessage).map(new Func1<Message, TransferStatus>() {
            @Override
            public TransferStatus call(Message message) {
                return new TransferStatus(message.parameters.get(Message.P_PROGRESS).asFloat());
            }
        });
    }

    public static final class TransferStatus {
        public final float progress;

        TransferStatus(float progress) {
            this.progress = progress;
        }
    }


    /////// TIME ///////

    private final long millis0 = System.currentTimeMillis();
    private final long nanos0 = System.nanoTime();
    private long headUniqueMillis = 0L;

    // a best-guess at a coordinated time, in millis
    public long millis() {
        return millis0 + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - nanos0);
    }

    // each call guaranteed to be a unique timestamp
    // colliding times are shifted into the future
    public long uniqueMillis() {
        long millis = millis();
        if (millis <= headUniqueMillis) {
            headUniqueMillis += 1;
        } else {
            headUniqueMillis = millis;
        }
        return headUniqueMillis;
    }



    /////// CAMERA ///////

    /* the Nextop instance can manage the camera,
     * which (can be) useful to
     * - align camera performance with network performance
     *   (quality, etc)
     * - keep a single camera across warmed up across the entire app,
     *   which (can) improve start-up times for the camera
     * - to reserve the camera, in your activity/fragment resume, call
     *   {@link #addCameraUser} and in pause call {@link #removeCameraUser},
     *   then (in between) wait for a camera instance with {@link #camera}.
     */

    public void addCameraUser() {
        throw new IllegalStateException("Call on a started nextop.");
    }
    public void removeCameraUser() {
        throw new IllegalStateException("Call on a started nextop.");
    }
    public Observable<CameraAdapter> camera() {
        return Observable.empty();
    }

    public static final class CameraAdapter {
        public final int cameraId;
        public final Camera camera;

        CameraAdapter(int cameraId, Camera camera) {
            this.cameraId = cameraId;
            this.camera = camera;
        }
    }


    private static int getDefaultCameraId() {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                return i;
            }
        }
        return 1 <= numberOfCameras ? 0 : -1;
    }







    public static final class Receiver<T> extends Observable<T> {
        static <T> Receiver<T> create(final Nextop nextop, Route route, Observable<T> in) {
            Map<Source.UnsubscribeBehavior, Observable<T>> ins = new HashMap<Source.UnsubscribeBehavior, Observable<T>>(2);
            Source.UnsubscribeBehavior defaultUnsubscribeBehavior;

            final @Nullable Id id = route.getLocalId();
            if (null != id) {
                ins.put(Source.UnsubscribeBehavior.DETACH, in.share());
                ins.put(Source.UnsubscribeBehavior.CANCEL_SEND, in.doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        nextop.cancelSend(id);
                    }
                }).share());
                defaultUnsubscribeBehavior = Source.UnsubscribeBehavior.CANCEL_SEND;
            } else {
                ins.put(Source.UnsubscribeBehavior.DETACH, in.share());
                defaultUnsubscribeBehavior = Source.UnsubscribeBehavior.DETACH;
            }
            Source source = new Source<T>(ins);
            source.check(defaultUnsubscribeBehavior);
            return new Receiver(route, source, defaultUnsubscribeBehavior);
        }


        public final Route route;
        private final Source<T> source;

        private Receiver(Route route, final Source<T> source, final Source.UnsubscribeBehavior unsubscribeBehavior) {
            super(new OnSubscribe<T>() {
                @Override
                public void call(Subscriber<? super T> subscriber) {
                    source.ins.get(unsubscribeBehavior).subscribe(subscriber);
                }
            });
            this.route = route;
            this.source = source;
        }

        public Receiver<T> cancelSendOnUnsubscribe() {
            source.check(Source.UnsubscribeBehavior.CANCEL_SEND);
            return new Receiver<T>(route, source, Source.UnsubscribeBehavior.CANCEL_SEND);
        }

        public Receiver<T> detachOnUnsubscribe() {
            source.check(Source.UnsubscribeBehavior.DETACH);
            return new Receiver<T>(route, source, Source.UnsubscribeBehavior.DETACH);
        }

        // return the localId of the outgoing message that this receiver is tied to
        // return null if there is no outgoing message for this nurl
        @Nullable
        public Id getId() {
            return route.getLocalId();
        }


        private static class Source<T> {
            static enum UnsubscribeBehavior {
                CANCEL_SEND,
                DETACH
            }

            final Map<UnsubscribeBehavior, Observable<T>> ins;

            Source(Map<UnsubscribeBehavior, Observable<T>> ins) {
                this.ins = ins;
            }

            void check(UnsubscribeBehavior unsubscribeBehavior) {
                if (!ins.containsKey(unsubscribeBehavior)) {
                    throw new UnsupportedOperationException();
                }
            }
        }
    }



    private static class GoNoded extends Nextop {

        // CAMERA

        private int cameraUserCount = 0;
        private int cameraId = -1;
        @Nullable
        private Camera camera = null;
        private boolean cameraConnected = false;
        private BehaviorSubject<CameraAdapter> cameraSubject = BehaviorSubject.create();

        SubjectNode subjectNode;
        MessageControlNode node;



        protected GoNoded(Context context, @Nullable Auth auth, MessageControlNode node) {
            super(context, auth);

            this.node = node;

            subjectNode = new SubjectNode(node);
            MessageControlState mcs = new MessageControlState();
            subjectNode.init(new AndroidMessageContext(mcs));
            subjectNode.start();
        }


        @Override
        public Nextop start() {
            throw new IllegalArgumentException("Already started.");
        }

        @Override
        public Nextop stop() {
            node.stop();
            closeCamera();

            return Nextop.create(context, this);
        }

        @Override
        boolean isActive() {
            return true;
        }


        @Override
        public Receiver<Message> send(Message message) {
            subjectNode.send(message);
            return receive(message.inboxRoute());
        }

        @Override
        public Receiver<Message> receive(Route route) {
            return Receiver.create(this, route, subjectNode.receive(route));
        }

        @Override
        public void cancelSend(Id id) {
            subjectNode.cancelSend(id);
        }

        @Override
        public Receiver<Layer> send(Layer layer, @Nullable LayersConfig config) {
            // FIXME 0.1.1
            // FIXME   send layers should manipulate the route here (base route + parameters per layer)
            // FIXME   this has to be coordinated with the receive/decode step
            // FIXME   get threading right and general correctness
            Message tmessage;
            if (null != layer.bitmap) {
                Bitmap bitmap = layer.bitmap;

                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                byte[] bytes = baos.toByteArray();

                EncodedImage image = new EncodedImage(EncodedImage.Format.JPEG, EncodedImage.Orientation.REAR_FACING,
                        bitmap.getWidth(), bitmap.getHeight(),
                        bytes, 0, bytes.length);

                tmessage = layer.message.buildOn()
                        .setContent(WireValue.of(image))
                        .build();
            } else {
                tmessage = layer.message;
            }

            subjectNode.send(tmessage);
            Route route = tmessage.inboxRoute();
            return Receiver.create(this, route, subjectNode.receive(route).map(new Func1<Message, Layer>() {
                @Override
                public Layer call(Message message) {
                    WireValue content = message.getContent();
                    switch (content.getType()) {
                        case IMAGE:
                            EncodedImage image = content.asImage();

                            Bitmap bitmap = BitmapFactory.decodeByteArray(image.bytes, image.offset, image.length);
                            return Layer.bitmap(message.buildOn().setContent(null).build(),
                                    bitmap);
                        default:
                            return Layer.message(message);
                    }
                }
            }));
        }


        /////// CAMERA ///////


        @Override
        public void addCameraUser() {
            ++cameraUserCount;
            lockCamera();
        }

        @Override
        public void removeCameraUser() {
            if (0 == --cameraUserCount) {
                closeCamera();
            }
        }

        @Override
        public Observable<CameraAdapter> camera() {
            return cameraSubject;
        }


        void lockCamera() {
            openCamera();
            try {
                if (!cameraConnected) {
                    if (cameraId < 0) {
                        cameraId = getDefaultCameraId();
                    }
                    if (null == camera) {
                        if (0 <= cameraId) {
                            try {
                                camera = Camera.open(cameraId);
                                if (null != camera) {
                                    cameraConnected = true;
                                    cameraSubject.onNext(new CameraAdapter(cameraId, camera));
                                }
                            } catch (Exception e) {
                                // e.g. Fail to connect to camera service
                            }
                        }
                    } else {
                        camera.reconnect();
                        cameraConnected = true;
                        cameraSubject.onNext(new CameraAdapter(cameraId, camera));
                    }
                }
            } catch (IOException e) {
                //
            }
        }
        void unlockCamera() {
            if (cameraConnected) {
//            camera.release();
                cameraConnected = false;
                camera.unlock();
                cameraSubject.onCompleted();
                cameraSubject = BehaviorSubject.create();
            }
        }


        void openCamera() {
            if (null == camera) {
                try {
                    camera = Camera.open();
                } catch (Exception e) {
                    // e.g. Fail to connect to camera service

                }
            }
        }

        void closeCamera() {
            unlockCamera();
            if (null != camera) {
                cameraId = -1;
                camera.release();
                camera = null;
            }
        }



        // FIXME 0.1.1
        // FIXME use broadcasts to receive connectivity messages
        // FIXME for now, just poll this:

        // FIXME demo hack
        public boolean isOnline() {
            ConnectivityManager connectivityManager
                    = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

    }

    // FIXME 0.2 see roadmap
//    private static final class Full extends GoNoded {
//        static Full start(Nextop copy) {
//            return new Full(copy.context, copy.auth);
//        }
//
//        private Full(Context context, @Nullable Auth auth) {
//            super(context, auth);
//
//        }
//    }

    private static final class Limited extends GoNoded {
        static Limited start(Nextop copy) {
            return new Limited(copy.context, copy.auth);
        }

        private static MessageControlNode createLimitedNode() {
            // FIXME 0.2 see ClientDemo for where we want to be
            MessageControlNode node = new HttpNode();
            return node;
        }

        private Limited(Context context, @Nullable Auth auth) {
            super(context, auth, createLimitedNode());

        }
    }


    /////// AUTH ///////

    public static final class Auth {
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

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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.nextop.client.MessageContext;
import io.nextop.client.MessageContexts;
import io.nextop.client.MessageControlNode;
import io.nextop.client.MessageControlState;
import io.nextop.client.node.Head;
import io.nextop.client.node.MultiNode;
import io.nextop.client.node.http.HttpNode;
import io.nextop.client.node.nextop.NextopClientWireFactory;
import io.nextop.client.node.nextop.NextopNode;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// FIXME calls all receives on the MAIN thread
// FIXME work out scheduling in general (all signatures should take a scheduler)
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

    @Nullable
    public MessageControlState getMessageControlState() {
        return null;
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


        public static String toCacheKey(String uri, Bound bound) {
            return String.format("%s:%s:%s:%s",
                    0 <= bound.maxTransferWidth ? bound.maxTransferWidth : "",
                    0 <= bound.maxTransferHeight ? bound.maxTransferHeight : "",
                    bound.quality,
                    uri);
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
            // [0, 100]
            public int quality = 100;

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
        BehaviorSubject<ConnectionStatus> subject = BehaviorSubject.create(new ConnectionStatus(true));
        return subject;
    }


    public static final class ConnectionStatus {
        public final boolean online;

        ConnectionStatus(boolean online) {
            this.online = online;
        }
    }


    /////// TRANSFER STATUS ///////

    public Observable<TransferStatus> transferStatus(Id id) {
//        if (null == id) {
//            throw new IllegalArgumentException();
//        }
//        Message statusMessage = Message.newBuilder().setRoute(Message.statusRoute(id)).build();
//        return send(statusMessage).map(new Func1<Message, TransferStatus>() {
//            @Override
//            public TransferStatus call(Message message) {
//                return new TransferStatus(message.parameters.get(Message.P_PROGRESS).asFloat());
//            }
//        });
        throw new IllegalStateException("Call on a started nextop.");
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




    static Receiver.UnsubscribeBehavior defaultUnsubscribeBehavior(Message message) {
        // if the message has no side-effects, it can be canceled when there is no subscriber
        if (Message.isNullipotent(message)) {
            return Receiver.UnsubscribeBehavior.CANCEL_SEND;
        }
        return Receiver.UnsubscribeBehavior.DETACH;
    }



    // FIXME
    // FIXME the CANCEL_SEND unsubscribe behavior as-is is broken
    // FIXME the receiver should re-send the message when re-subscribed, if canceled
    public static final class Receiver<T> extends Observable<T> {
        static <T> Receiver<T> create(final Nextop nextop, Route route, Observable<T> in, UnsubscribeBehavior defaultUnsubscribeBehavior) {
            Map<UnsubscribeBehavior, Observable<T>> ins = new HashMap<UnsubscribeBehavior, Observable<T>>(2);

            final @Nullable Id id = Message.getLocalId(route);
            if (null != id) {
                ins.put(UnsubscribeBehavior.DETACH, in.share());
                ins.put(UnsubscribeBehavior.CANCEL_SEND, in.doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        nextop.cancelSend(id);
                    }
                }).share());
            } else {
                ins.put(UnsubscribeBehavior.DETACH, in.share());
            }
            Source source = new Source<T>(ins);
            source.check(defaultUnsubscribeBehavior);
            return new Receiver(route, source, defaultUnsubscribeBehavior);
        }


        public final Route route;
        private final Source<T> source;

        private Receiver(Route route, final Source<T> source, final UnsubscribeBehavior unsubscribeBehavior) {
            super(new OnSubscribe<T>() {
                @Override
                public void call(Subscriber<? super T> subscriber) {
                    source.ins.get(unsubscribeBehavior).subscribe(subscriber);
                }
            });
            this.route = route;
            this.source = source;
        }

        public Receiver<T> doOnUnsubscribe(UnsubscribeBehavior behavior) {
            source.check(behavior);
            return new Receiver<T>(route, source, behavior);
        }

        // return the localId of the outgoing message that this receiver is tied to
        // return null if there is no outgoing message for this nurl
        @Nullable
        public Id getId() {
            return Message.getLocalId(route);
        }


        public static enum UnsubscribeBehavior {
            CANCEL_SEND,
            DETACH
        }

        private static class Source<T> {
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

        Head head;
        MessageControlState mcs;
        MessageControlNode node;



        protected GoNoded(Context context, @Nullable Auth auth, MessageControlNode node) {
            super(context, auth);

            this.node = node;

            MessageContext messageContext = MessageContexts.create();
            mcs = new MessageControlState(messageContext);
            head = Head.create(messageContext, mcs, node, /* FIXME */ AndroidSchedulers.mainThread());
            head.init(null);
            head.start();
        }


        @Nullable
        public MessageControlState getMessageControlState() {
            return mcs;
        }


        @Override
        public Nextop start() {
            throw new IllegalArgumentException("Already started.");
        }

        @Override
        public Nextop stop() {
            head.stop();
            closeCamera();

            return Nextop.create(context, this);
        }

        @Override
        boolean isActive() {
            return true;
        }


        @Override
        public Receiver<Message> send(Message message) {
            head.send(message);
            return receive(message.inboxRoute());
        }

        @Override
        public Receiver<Message> receive(Route route) {
            return Receiver.create(this, route, head.receive(route), Receiver.UnsubscribeBehavior.DETACH);
        }

        @Override
        public void cancelSend(Id id) {
            head.cancelSend(id);
            // FIXME
            synchronized (cacheMutex) {
                inFlight.inverse().remove(id);
            }
        }





        // FIXME
        // FIXME if these are GETs, do request piggybacking and decoding on multiple threads
        Object cacheMutex = new Object();

        Cache<String, Bitmap> layerCache = CacheBuilder.newBuilder()
                .maximumWeight(100)
                .weigher(new Weigher<String, Bitmap>() {
                    @Override
                    public int weigh(String key, Bitmap value) {
                        // FIXME
                        return 1;
                    }
                }).concurrencyLevel(1
                ).build();

        // FIXME
        BiMap<String, Id> inFlight = HashBiMap.create(32);


        @Override
        public Receiver<Layer> send(Layer layer, @Nullable LayersConfig config) {
            // FIXME 0.1.1
            // FIXME   send layers should manipulate the route here (base route + parameters per layer)
            // FIXME   this has to be coordinated with the receive/decode step
            // FIXME   get threading right and general correctness

            // bounds to use:
            List<LayersConfig.Bound> sendBounds;
            if (null != config) {
                sendBounds = config.sendBounds;
            } else {
                sendBounds = Collections.emptyList();
            }
            if (sendBounds.isEmpty()) {
                sendBounds = Collections.singletonList(new LayersConfig.Bound());
            }

            List<LayersConfig.Bound> receiveBounds;
            if (null != config) {
                receiveBounds = config.receiveBounds;
            } else {
                receiveBounds = Collections.emptyList();
            }
            if (receiveBounds.isEmpty()) {
                receiveBounds = Collections.singletonList(new LayersConfig.Bound());
            }

            // FIXME only for GETs
            // FIXME even on cache hit, still send a HEAD to check on the cache headers?
            final boolean cacheable = true;

            // FIXME start at the last bounds and go down for a cache hit
            final String uri = layer.message.toUriString();
            final String cacheKey = LayersConfig.toCacheKey(uri, sendBounds.get(0));
            @Nullable Bitmap cachedBitmap;
            synchronized (cacheMutex) {
                cachedBitmap = layerCache.getIfPresent(cacheKey);
            }
            if (cacheable && null != cachedBitmap) {
                return Receiver.create(this, layer.message.inboxRoute(), Observable.just(Layer.bitmap(
                        // FIXME set cache headers
                        Message.newBuilder().setRoute(layer.message.inboxRoute()).build(),
                        cachedBitmap)),
                        defaultUnsubscribeBehavior(layer.message));
            }


            // FIXME option to attach
            Route route;
            Receiver.UnsubscribeBehavior defaultUnsubscribeBehavior;
            @Nullable final Id inFlightId;
            synchronized (cacheMutex) {
                inFlightId = inFlight.get(uri);
            }
            if (null != inFlightId) {
                route = Message.inboxRoute(inFlightId);
                // FIXME attach flow and receiver flow needs to be reworked
                defaultUnsubscribeBehavior = Receiver.UnsubscribeBehavior.DETACH;
            } else {
                Message tmessage;
                if (null != layer.bitmap) {
                    Bitmap bitmap = layer.bitmap;

                    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
                    byte[] bytes = baos.toByteArray();

                    EncodedImage image = new EncodedImage(EncodedImage.Format.JPEG, EncodedImage.Orientation.REAR_FACING,
                            bitmap.getWidth(), bitmap.getHeight(),
                            bytes, 0, bytes.length);

                    Message.Builder builder = layer.message.buildOn()
                            .setContent(WireValue.of(image));

                    Message.setLayers(builder, new Message.LayerInfo(Message.LayerInfo.Quality.LOW, EncodedImage.Format.JPEG, 32, 32, null, 0));

                    tmessage = builder.build();
                } else {
                    tmessage = layer.message;
                }


                head.send(tmessage);
                route = tmessage.inboxRoute();
                synchronized (cacheMutex) {
                    inFlight.put(uri, tmessage.id);
                }

                defaultUnsubscribeBehavior = defaultUnsubscribeBehavior(tmessage);
            }

            // FIXME parallel
//            tmessage = tmessage.buildOn().setGroupId(Id.create()).build();


            // FIXME subject node needs to put dispatch on the MAIN thread. get scheduling everywhere fixed
            // FIXME (otherwise could miss the receive)

            Observable<Layer> s = head.receive(route).observeOn(decodeScheduler).map(new Func1<Message, Layer>() {
                @Override
                public Layer call(Message message) {
                    WireValue content = message.getContent();
                    switch (content.getType()) {
                        case IMAGE:
                            EncodedImage image = content.asImage();

                            // FIXME
                            // FIXME fit the scale correctly to the layer
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inSampleSize = 4;

                            Bitmap bitmap = BitmapFactory.decodeByteArray(image.bytes, image.offset, image.length, opts);

                            synchronized (cacheMutex) {
                                // FIXME correct cache key
                                layerCache.put(cacheKey, bitmap);
                                inFlight.remove(uri);
                            }


                            return Layer.bitmap(message.buildOn().setContent(null).build(),
                                    bitmap);
                        default:
                            return Layer.message(message);
                    }
                }
            }).observeOn(AndroidSchedulers.mainThread());


            // FIXME for the cache
            defaultUnsubscribeBehavior = Receiver.UnsubscribeBehavior.DETACH;
            // FIXME for the cache
            s = s.share();
            s.subscribe();


            Receiver<Layer> r = Receiver.create(this, route, s,

                    // FIXME can't cancel send on an in-flight attach
                    // FIXME this whole flow needs to be reworked
                    defaultUnsubscribeBehavior
            );


            return r;
        }

        Executor decodeExecutor = Executors.newFixedThreadPool(4);
        Scheduler decodeScheduler = Schedulers.from(decodeExecutor);

        // FIXME distinct within 1%
        // FIXME timeout if no first emit after 15s
        public Observable<TransferStatus> transferStatus(final Id id) {
            if (null == id) {
                throw new IllegalArgumentException();
            }
            // FIXME scheduler issues
            return mcs.getObservable(id, 30, TimeUnit.SECONDS).subscribeOn(AndroidSchedulers.mainThread()).map(new Func1<MessageControlState.Entry, TransferStatus>() {
                @Override
                public TransferStatus call(MessageControlState.Entry entry) {
                    TransferStatus s = new TransferStatus(entry.outboxTransferProgress, entry.inboxTransferProgress);

                    // FIXME remove
//                    System.out.printf("  transfer status %s %s\n", id, s);

                    return s;
                }
            });
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
            // FIXME 0.2 cache, durability

            // head
            //   ^- multi
            //       ^- nextop
            //       ^- http

            NextopNode nextopNode = new NextopNode();
            nextopNode.setWireFactory(new NextopClientWireFactory(
                    new NextopClientWireFactory.Config(Authority.valueOf(/* FIXME move to config */ "dns.nextop.io"), 2)));

            HttpNode httpNode = new HttpNode();

            MultiNode multiNode = new MultiNode(MultiNode.Downstream.create(nextopNode),
                    MultiNode.Downstream.create(httpNode, MultiNode.Downstream.Support.LOCAL));
            return multiNode;
        }

        private Limited(Context context, @Nullable Auth auth) {
            super(context, auth, createLimitedNode());
        }
    }


    /////// TRANSFER STATUS ///////

    public static final class TransferStatus {
        public final MessageControlState.TransferProgress send;
        public final MessageControlState.TransferProgress receive;

        TransferStatus(MessageControlState.TransferProgress send, MessageControlState.TransferProgress receive) {
            this.send = send;
            this.receive = receive;
        }

        @Override
        public String toString() {
            return String.format("out %s, in %s",
                    send, receive);
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

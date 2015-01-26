package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.Message;
import io.nextop.Nextop;
import io.nextop.WireValue;
import io.nextop.rx.RxManager;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FlipInfoViewModelManager extends RxManager<FlipInfoViewModel> {

    Nextop nextop;


    // CONTROLLER

    void setIntro(final Id flipId, final String intro) {
        update(flipId, new Action1<ManagedState<FlipInfoViewModel>>() {
            @Override
            public void call(ManagedState<FlipInfoViewModel> state) {
                state.m.intro = intro;


                Map<WireValue, WireValue> content = Collections.singletonMap(
                        WireValue.of("intro"),
                        WireValue.of(intro));

                Message update = Message.newBuilder()
                        .setNurl("POST http://demo-flip.nextop.io/flip/$flip-id/info")
                        .set("flip-id", flipId)
                        .setContent(content)
                        .build();
                nextop.send(update);

            }
        });
    }



    // VMM


    @Override
    protected void startUpdates(final ManagedState<FlipInfoViewModel> state) {
        // sync info
        // then subscribe to updates


        Message sync = Message.newBuilder()
                .setNurl("GET http://demo-flip.nextop.io/flip/$flip-id/info")
                .set("flip-id", state.id)
                .build();
        state.subscriptions.add(nextop.send(sync)
                .doOnNext(new Action1<Message>() {
                    @Override
                    public void call(Message message) {

                        set(state.id, message);
                        complete(state.id);
                    }
                }).doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        // start the poller
                        Action0 poller = new Action0() {
                            @Override
                            public void call() {
                                Message poll = Message.newBuilder()
                                        .setNurl("GET http://demo-flip.nextop.io/flip/$flip-id/info")
                                        .set("flip-id", state.id)
                                        .set("after", state.m.getUpdateIndex())
                                        .build();
                                state.subscriptions.add(nextop.send(poll)
                                        .doOnNext(new Action1<Message>() {
                                            @Override
                                            public void call(final Message message) {

                                                set(state.id, message);


                                            }
                                        }).subscribe());

                            }
                        };
                        int timeoutMs = 1000;

                        state.subscriptions.add(AndroidSchedulers.mainThread().createWorker().schedulePeriodically(poller,
                                timeoutMs, timeoutMs, TimeUnit.MILLISECONDS));


                    }
                }).subscribe());

    }


    private void set(Id flipId, Message message) {
        // FIXME parse out intro, most_recent_update_index
    }


    @Override
    protected FlipInfoViewModel create(Id id) {
        return new FlipInfoViewModel(id);
    }
}

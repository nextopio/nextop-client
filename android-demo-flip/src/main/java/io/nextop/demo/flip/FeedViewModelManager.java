package io.nextop.demo.flip;

import io.nextop.*;
import io.nextop.rx.RxManager;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FeedViewModelManager extends RxManager<FeedViewModel, FeedViewModelManager.FeedManagedState> {

    Nextop nextop;


    // CONTROLLER

    void addFlip(Id feedId, final Id flipId) {
        update(feedId, new Action1<FeedManagedState>() {
            @Override
            public void call(FeedManagedState state) {
                state.m.addFirst(new FeedViewModel.FlipState(flipId, Integer.MAX_VALUE));

                Message update = Message.newBuilder()
                        .setNurl("PUT http://demo-flip.nextop.io/flip/$flip-id")
                        .set("flip-id", flipId)
                        .build();
                nextop.send(update);
            }
        });
    }

    void loadBack(final Id feedId) {
        // update: set load back pending; then if not already pending before, load N more ids less than the last id in the view model, update again

        updateComplete(feedId, new Action1<FeedManagedState>() {
            @Override
            public void call(FeedManagedState state) {

                Message message = Message.newBuilder()
                        .setNurl("GET http://demo-flip.nextop.io/feed")
                        .set("before", state.m.getLastUpdateIndex())
                        .build();

                state.subscriptions.add(nextop.send(message)
                        .doOnNext(new Action1<Message>() {
                            @Override
                            public void call(final Message message) {

                                addLast(feedId, message);


                            }
                        }).subscribe());

            }
        });

    }


    // VMM

    @Override
    protected void startUpdates(final FeedManagedState state) {
        // 1. sync the state. on sync, mark it as complete
        // 2. poll for incremental updates. incremental updates take the previous known update id (given from the state sync),
        //    and return a list of changes (id) that should be moved or added to the top
        // 3. changes update vm


        Message sync = Message.newBuilder()
                .setNurl("GET http://demo-flip.nextop.io/feed")
                .build();
        state.subscriptions.add(nextop.send(sync)
                .doOnNext(new Action1<Message>() {
                    @Override
                    public void call(Message message) {

                        addFirst(state.id, message);
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
                                        .setNurl("GET http://demo-flip.nextop.io/feed")
                                        .set("after", state.m.getFirstUpdateIndex())
                                        .build();
                                state.subscriptions.add(nextop.send(poll)
                                        .doOnNext(new Action1<Message>() {
                                            @Override
                                            public void call(final Message message) {

                                                addFirst(state.id, message);


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


    private void addFirst(Id feedId, final Message results) {
        update(feedId, new Action1<FeedManagedState>() {
            @Override
            public void call(FeedManagedState state) {
                for (WireValue value : results.getContent().asList()) {
                    Map<WireValue, WireValue> m = value.asMap();

                    // FIXME asId
                    Id flipId = Id.valueOf(m.get("flip_id").asString());
                    long updateIndex = m.get("most_recent_update_index").asLong();

                    state.m.addFirst(new FeedViewModel.FlipState(flipId, updateIndex));
                }
            }
        });

    }
    private void addLast(Id feedId, final Message results) {
        update(feedId, new Action1<FeedManagedState>() {
            @Override
            public void call(FeedManagedState state) {
                for (WireValue value : results.getContent().asList()) {
                    Map<WireValue, WireValue> m = value.asMap();

                    // FIXME asId
                    Id flipId = Id.valueOf(m.get("flip_id").asString());
                    long updateIndex = m.get("most_recent_update_index").asLong();

                    state.m.addLast(new FeedViewModel.FlipState(flipId, updateIndex));
                }
            }
        });
    }



    @Override
    protected FeedManagedState create(Id id) {
        return new FeedManagedState(new FeedViewModel(id));
    }



    public static final class FeedManagedState extends RxManager.ManagedState<FeedViewModel> {

        public FeedManagedState(FeedViewModel m) {
            super(m);
        }
    }

}

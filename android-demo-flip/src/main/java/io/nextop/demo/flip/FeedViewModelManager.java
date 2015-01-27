package io.nextop.demo.flip;

import io.nextop.*;
import io.nextop.rx.RxManager;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func2;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class FeedViewModelManager extends ThinViewModelManager<FeedViewModel> {


    FeedViewModelManager(Nextop nextop) {
        super(nextop);
    }


    // CONTROLLER

    void addFlip(Id feedId, final Id flipId) {
        Message update = Message.newBuilder()
                .setRoute("PUT http://" + Flip.REMOTE + "/flip/$flip-id")
                .set("flip-id", flipId)
                .build();
        nextop.send(update);

        update(feedId, new Func2<FeedViewModel, RxState, FeedViewModel>() {
            @Override
            public FeedViewModel call(FeedViewModel feedVm, RxState state) {
                feedVm.add(new FeedViewModel.FlipState(flipId, -1L));
                return feedVm;
            }
        });
    }

    void loadBack(final Id feedId) {
        // update: set load back pending; then if not already pending before, load N more ids less than the last id in the view model, update again

        updateComplete(feedId, new Func2<FeedViewModel, RxState, FeedViewModel>() {
            @Override
            public FeedViewModel call(FeedViewModel feedVm, RxState state) {
                Message message = Message.newBuilder()
                        .setRoute("GET http://" + Flip.REMOTE + "/feed")
                        .set("before", feedVm.getMinUpdateIndex())
                        .build();

                state.add(nextop.send(message)
                        .doOnNext(new Action1<Message>() {
                            @Override
                            public void call(final Message message) {

                                add(feedId, message);


                            }
                        })).subscribe();

                return feedVm;
            }
        });

    }


    private void add(final Id feedId, final Message results) {
        update(feedId, new Func2<FeedViewModel, RxState, FeedViewModel>() {
            @Override
            public FeedViewModel call(FeedViewModel feedVm, RxState state) {
                for (WireValue value : results.getContent().asList()) {
                    Map<WireValue, WireValue> m = value.asMap();

                    // FIXME asId
                    Id flipId = Id.valueOf(m.get("flip_id").asString());
                    long updateIndex = m.get("most_recent_update_index").asLong();

                    feedVm.add(new FeedViewModel.FlipState(flipId, updateIndex));
                }
                return feedVm;
            }
        });

    }



    // VMM

    @Override
    protected void startUpdates(final FeedViewModel feedVm, final RxState state) {
        // 1. sync the state. on sync, mark it as complete
        // 2. poll for incremental updates. incremental updates take the previous known update id (given from the state sync),
        //    and return a list of changes (id) that should be moved or added to the top
        // 3. changes update vm


        Message sync = Message.newBuilder()
                .setRoute("GET http://" + Flip.REMOTE + "/feed")
                .build();
        state.add(nextop.send(sync)
                .doOnNext(new Action1<Message>() {
                    @Override
                    public void call(Message message) {

                        add(feedVm.id, message);
                        complete(feedVm.id);
                    }
                }).doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                        // start the poller
                        Action0 poller = new Action0() {
                            @Override
                            public void call() {
                                Message poll = Message.newBuilder()
                                        .setRoute("GET http://" + Flip.REMOTE + "/feed")
                                        .set("after", feedVm.getMaxUpdateIndex())
                                        .build();
                                state.add(nextop.send(poll)
                                        .doOnNext(new Action1<Message>() {
                                            @Override
                                            public void call(final Message message) {
                                                add(feedVm.id, message);
                                            }
                                        })).subscribe();
                            }
                        };
                        state.add(AndroidSchedulers.mainThread().createWorker().schedulePeriodically(poller,
                                pollTimeoutMs, pollTimeoutMs, TimeUnit.MILLISECONDS));
                    }
                })).subscribe();
    }



    @Override
    protected FeedViewModel create(Id id) {
        return new FeedViewModel(id);
    }


}

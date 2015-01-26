package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManaged;

import java.util.ArrayList;
import java.util.List;

public class FeedViewModel extends RxManaged {
    // ids
    // model internally orders by last modified time

    // last update id

    // on change observer

    // FIXME replace FlipIds with flipState, which tracks the most recent update index
    List<FlipState> states = new ArrayList<FlipState>(50);


    public FeedViewModel(Id feedId) {
        super(feedId);
    }


    public int size() {
        return states.size();
    }


    public Id getFlipId(int index) {
        return states.get(index).flipId;
    }



    void addFirst(FlipState state) {
        // TODO use a tree-based sorted list
        removeAll(state.flipId);
        states.add(0, state);
        // TODO assert states descending in update index
    }
    void addLast(FlipState state) {
        // TODO use a tree-based sorted list
        removeAll(state.flipId);
        states.add(state);
        // TODO assert states descending in update index
    }
    private void removeAll(Id flipId) {
        for (int i = states.size() - 1; 0 <= i; --i) {
            FlipState state = states.get(i);
            if (flipId.equals(state.flipId)) {
                states.remove(i);
            }
        }
    }

    long getFirstUpdateIndex() {
        int n = states.size();
        return 0 < n ? states.get(0).updateIndex : 0;
    }

    long getLastUpdateIndex() {
        int n = states.size();
        return 0 < n ? states.get(n - 1).updateIndex : 0;
    }



    static final class FlipState {
        final Id flipId;
        final long updateIndex;

        FlipState(Id flipId, long updateIndex) {
            this.flipId = flipId;
            this.updateIndex = updateIndex;
        }
    }


}

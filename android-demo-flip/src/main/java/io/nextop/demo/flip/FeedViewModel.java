package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManaged;

import javax.annotation.Nullable;
import java.util.*;

public class FeedViewModel extends RxManaged {
    List<FlipState> orderedStates = new ArrayList<FlipState>(8);
    Map<Id, FlipState> states = new HashMap<Id, FlipState>(8);

    long maxUpdateIndex = 0L;
    long minUpdateIndex = 0L;


    public FeedViewModel(Id feedId) {
        super(feedId);
    }


    public int size() {
        return orderedStates.size();
    }


    public Id getFlipId(int index) {
        return orderedStates.get(index).flipId;
    }




    void add(FlipState state) {
        @Nullable FlipState pstate = states.put(state.flipId, state);
        if (null != pstate) {
            orderedStates.remove(pstate);
        }
        orderedStates.add(state);
        Collections.sort(orderedStates, C_BY_UPDATE_INDEX);

        if (0 < state.updateIndex) {
            if (maxUpdateIndex < state.updateIndex) {
                maxUpdateIndex = state.updateIndex;
            } else if (state.updateIndex < minUpdateIndex) {
                minUpdateIndex = state.updateIndex;
            }
        }
    }
    void remove(Id frameId) {
        @Nullable FlipState state = states.remove(frameId);
        if (null != state) {
            orderedStates.remove(state);
        }
    }

    long getMaxUpdateIndex() {
        return maxUpdateIndex;
    }

    long getMinUpdateIndex() {
        return minUpdateIndex;
    }







    static final class FlipState {
        final Id flipId;
        final long updateIndex;

        FlipState(Id flipId, long updateIndex) {
            this.flipId = flipId;
            this.updateIndex = updateIndex;
        }
    }



    private static final Comparator<FlipState> C_BY_UPDATE_INDEX = new Comparator<FlipState>() {
        @Override
        public int compare(FlipState a, FlipState b) {
            // negative (pending) first, then descending
            boolean an = a.updateIndex < 0;
            boolean bn = b.updateIndex < 0;
            if (an && bn) {
                return 0;
            } else if (an) {
                return -1;
            } else if (bn) {
                return 1;
            } else {
                return Long.compare(a.updateIndex, b.updateIndex);
            }
        }
    };

}

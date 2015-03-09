package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManaged;

import javax.annotation.Nullable;
import java.util.*;

public class FlipViewModel extends RxManaged {
    // ids
    // model internally orders by frame number

    // last update id


    List<FrameState> orderedStates = new ArrayList<FrameState>(8);

    Map<Id, FrameState> states = new HashMap<Id, FrameState>(8);

    long maxUpdateIndex = 0;


    // FIXME remove
    int k = 20;


    void add(FrameState state) {
        @Nullable FrameState pstate = states.put(state.frameVm.id, state);
        if (null != pstate) {
            orderedStates.remove(pstate);
        }
        orderedStates.add(state);
        Collections.sort(orderedStates, C_BY_CREATION_TIME);

        if (0 < state.updateIndex) {
            if (maxUpdateIndex < state.updateIndex) {
                maxUpdateIndex = state.updateIndex;
            }
        }
    }
    void remove(Id frameId) {
        @Nullable FrameState state = states.remove(frameId);
        if (null != state) {
            orderedStates.remove(state);
        }
    }


    long getMaxUpdateIndex() {
        return maxUpdateIndex;
    }




    public FlipViewModel(Id flipId) {
        super(flipId);
    }


    public int size() {
        return /* FIXME remove */ k * orderedStates.size();
    }


    public FrameViewModel getFrameVm(int index) {
        return orderedStates.get(index /* FIXME remove */ % orderedStates.size()).frameVm;
    }

    public FrameViewModel getFrameVm(Id frameId) {
        return states.get(frameId).frameVm;
    }


    static final class FrameState {
        final FrameViewModel frameVm;
        final long updateIndex;

        FrameState(FrameViewModel frameVm, long updateIndex) {
            this.frameVm = frameVm;
            this.updateIndex = updateIndex;
        }

    }


    private static final Comparator<FrameState> C_BY_CREATION_TIME = new Comparator<FrameState>() {
        @Override
        public int compare(FrameState lhs, FrameState rhs) {
            // descending
            return Long.compare(rhs.frameVm.creationTime, lhs.frameVm.creationTime);
        }
    };
}

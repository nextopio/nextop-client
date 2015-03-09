package io.nextop.demo.flip;

import com.google.common.primitives.Ints;
import io.nextop.Id;
import io.nextop.rx.RxManaged;

import javax.annotation.Nullable;
import java.util.*;

public class FeedViewModel extends RxManaged {
    List<FlipState> orderedStates = new ArrayList<FlipState>(8);
    Map<Id, FlipState> states = new HashMap<Id, FlipState>(8);

    long maxUpdateIndex = 0L;
    long minUpdateIndex = 0L;

    // FIXME remove
    int k = 20;
    int[][] shuffle = new int[k][];
    {
        Random r = new Random();
        for (int i = 0; i < k; ++i) {
            int n = 100;
            int[] s = new int[n];
            for (int j = 0; j < n; ++j) {
                s[j] = j;
            }
            for (int j = n - 1; 0 < j; --j) {
                int index = r.nextInt(j + 1);
                int t = s[index];
                s[index] = s[j];
                s[j] = t;
            }
            shuffle[i] = s;
        }
    }


    public FeedViewModel(Id feedId) {
        super(feedId);
    }


    public int size() {
        return /* FIXME remove */ k * orderedStates.size();
    }


    public Id getFlipId(int index) {
        // FIXME remove
        int n = orderedStates.size();
        int i = index / n;
        int[] s = shuffle[i];
        int j = index % n;
        if (0 < i && j < s.length) {
            j = s[j];
            while (n <= j) {
                j /= 2;
            }
        }
        return orderedStates.get(j).flipId;
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
                if (a.updateIndex < b.updateIndex) {
                    return 1;
                }
                if (b.updateIndex < a.updateIndex) {
                    return -1;
                }
                return 0;
            }
        }
    };

}

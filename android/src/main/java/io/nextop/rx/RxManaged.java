package io.nextop.rx;

import io.nextop.Id;

public class RxManaged {
    public final Id id;

    public RxManaged(Id id) {
        this.id = id;
    }

    /** Called immediately after the managed object is removed from the manager. */
    public void close() {
    }
}

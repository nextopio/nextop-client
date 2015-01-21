package io.nextop.rx;

import io.nextop.Id;

public abstract class RxManaged {
    public final Id id;

    public RxManaged(Id id) {
        this.id = id;
    }

    // FIXME add dispose method
}

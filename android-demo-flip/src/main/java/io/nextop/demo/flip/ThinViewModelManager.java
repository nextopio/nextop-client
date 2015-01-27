package io.nextop.demo.flip;

import io.nextop.Nextop;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;

public abstract class ThinViewModelManager<M extends RxManaged> extends RxManager<M> {

    final Nextop nextop;
    final int pollTimeoutMs = 1000;

    ThinViewModelManager(Nextop nextop) {
        this.nextop = nextop;
    }

}

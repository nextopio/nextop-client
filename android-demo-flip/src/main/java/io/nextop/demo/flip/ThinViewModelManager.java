package io.nextop.demo.flip;

import io.nextop.Nextop;
import io.nextop.rx.RxManaged;
import io.nextop.rx.RxManager;

import java.util.concurrent.TimeUnit;

public abstract class ThinViewModelManager<M extends RxManaged> extends RxManager<M> {

    final Nextop nextop;
    final int pollTimeoutMs = (int) TimeUnit.SECONDS.toMillis(5);

    ThinViewModelManager(Nextop nextop) {
        this.nextop = nextop;
    }

}

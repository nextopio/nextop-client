package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.vm.ImageViewModel;

import java.util.Random;

public class FlipInfoViewModel extends RxManaged {



    String intro;
    // id, name, top image uri,
    // last modified time
    // last update id
    // boolean uploading


    public FlipInfoViewModel(Id flipId) {
        super(flipId);
    }


}

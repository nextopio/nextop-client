package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManaged;

public class FlipInfoViewModel extends RxManaged {

    int updateIndex = 0;

    String intro;
    // id, name, top image uri,
    // last modified time
    // last update id
    // boolean uploading


    public FlipInfoViewModel(Id flipId) {
        super(flipId);
    }



    int getUpdateIndex() {
        return updateIndex;
    }

}

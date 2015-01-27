package io.nextop.demo.flip;

import io.nextop.Id;
import io.nextop.rx.RxManaged;

public class FlipInfoViewModel extends RxManaged {

    String intro;

    // boolean uploading
    // last update id
    // last modified time
    // id, name, top image uri,
    long updateIndex = 0;


    public FlipInfoViewModel(Id flipId) {
        super(flipId);
    }


    void set(String intro, long updateIndex) {
        this.intro = intro;
        if (0 < updateIndex) {
            this.updateIndex = updateIndex;
        }
    }



    long getUpdateIndex() {
        return updateIndex;
    }

}

package io.nextop.demo;

import io.nextop.Id;
import io.nextop.rx.RxManaged;
import io.nextop.vm.ImageViewModel;

import java.util.Random;

public class FlipInfoViewModel extends RxManaged {



    ImageViewModel imageVm;
    String intro;
    // id, name, top image uri,
    // last modified time
    // last update id
    // boolean uploading

    {
        Random r = new Random();
        int n = 5 + r.nextInt(10);
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; ++i) {
            switch (r.nextInt(4)) {
                case 0:
                    sb.append(' ');
                    break;
                default:
                    sb.append((char) ('a' + r.nextInt(25)));
            }
        }
        intro = sb.toString();
    }


    public FlipInfoViewModel(Id flipId) {
        super(flipId);
    }


}

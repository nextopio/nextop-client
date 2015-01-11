package io.nextop;

import io.nextop.Id;


class Message {
    static enum Type {
        CONTROL,
        DATA
    }

    Id id;
    int priority;

    Nurl nurl;
    // FIXME headers
    // FIXME parameters
    // FIXME body

}

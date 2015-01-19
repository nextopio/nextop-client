package io.nextop;

import com.google.common.annotations.Beta;

import javax.annotation.Nullable;

@Beta
public interface NextopContext {
    @Nullable
    Nextop getNextop();
}

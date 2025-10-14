package com.xzavier0722.mc.plugin.slimefun4.storage.callback;

import com.molean.folia.adapter.SchedulerContext;

public interface IAsyncReadCallback<T> {
    default SchedulerContext getContext() {
        return SchedulerContext.ofGlobal();
    }

    default void onResult(T result) {}

    default void onResultNotFound() {}
}

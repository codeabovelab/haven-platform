package com.codeabovelab.dm.patform.configuration;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

@Repository
public class CachedRepository {

    private boolean result;

    @Cacheable(value = "test_cache", key = "#param1", condition = "#param1>0")
    public boolean loadDataByParams(/*cache key*/ int param1, /*ignored param*/ long param2) {
        return result;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(final boolean globalState) {
        this.result = globalState;
    }
}

package com.codeabovelab.dm.cluman.job;

import lombok.ToString;
import org.springframework.beans.factory.annotation.Autowired;

/**
 */
@JobBean
@ToString
public class UnnecessaryJobBean implements Runnable {
    private String stringParam;
    private int intParam;
    private int max;

    private String ourJobResult;

    @Autowired
    public UnnecessaryJobBean(SampleGlobalBean sampleGlobalBean) {
        System.out.println("CREATE JOB");
    }

    @JobParam
    public void setIntParam(int intParam) {
        this.intParam = intParam;
    }

    @JobParam
    public void setStringParam(String stringParam) {
        this.stringParam = stringParam;
    }

    @JobParam
    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public void run() {
        System.out.println("DO JOB with " + toString());
        this.ourJobResult = stringParam + intParam;
        if(intParam < max) {
            intParam++;
        }
    }

    public int getIntParam() {
        return intParam;
    }

    public String getStringParam() {
        return stringParam;
    }

    @JobParam
    public String getOurJobResult() {
        return ourJobResult;
    }
}

package com.codeabovelab.dm.cluman.cluster.compose;

import org.junit.Test;

import java.io.File;
import java.text.MessageFormat;

import static org.junit.Assert.*;

public class ComposeUtilsTest {

    @Test
    public void test() {
        File file = ComposeUtils.applicationPath("tmp", "testCluster", "testApp", "testFile", true);
        String format = MessageFormat.format("{0}/compose/clusters/{1}/apps/{2}/{3}", "tmp",
                "testCluster", "testApp", "testFile");
        assertEquals(format, file.toString());
    }

}
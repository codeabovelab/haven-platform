package com.codeabovelab.dm.common.utils;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.jar.Manifest;

import static org.junit.Assert.*;

public class AppInfoTest {

    private static final String DATA = "Manifest-Version: 1.0\n" +
      "Implementation-Title: app\n" +
      "Implementation-Version: 1.34-SNAPSHOT\n" +
      "Archiver-Version: Plexus Archiver\n" +
      "Built-By: rad\n" +
      "Start-Class: com.codeabovelab.dm.platform.users.Application\n" +
      "Implementation-Vendor-Id: com.codeabovelab\n" +
      "Spring-Boot-Version: 1.2.1.RELEASE\n" +
      "Created-By: Apache Maven 3.2.1\n" +
      "Build-Jdk: 1.8.0_25\n" +
      "Main-Class: org.springframework.boot.loader.PropertiesLauncher\n";

    @Test
    public void test() throws IOException {
        Manifest manifest = new Manifest(new ByteArrayInputStream(DATA.getBytes()));
        String applicationName = AppInfo.getApplicationName(manifest);
        System.out.println(applicationName);
    }

}
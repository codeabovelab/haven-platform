/*
 * Copyright 2017 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.agent.boot;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.common.collect.ImmutableSet;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;

import static org.junit.Assert.*;

/**
 */
public class CertificateGeneratorTest {
    @Test
    public void constructCert() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        ((Logger)LoggerFactory.getLogger(CertificateGenerator.class)).setLevel(Level.DEBUG);
        File file = new File("/tmp/dm-agent.jks");//Files.createTempFile("dm-agent", ".jks");
        Cert cert = CertificateGenerator.constructCert(file, ImmutableSet.of("test1", "test2"));
        assertNotNull(cert);
    }

}
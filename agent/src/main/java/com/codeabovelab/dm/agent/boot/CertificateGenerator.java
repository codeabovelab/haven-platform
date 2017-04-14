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

import com.codeabovelab.dm.common.utils.OSUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPrivateCrtKey;
import org.bouncycastle.jcajce.provider.asymmetric.rsa.BCRSAPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;


import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

/**
 * Make self signed ssl cert for current ip and names
 */
@Slf4j
public class CertificateGenerator {

    static void gatherNames(Set<String> set, boolean resolve) {
        set.add(OSUtils.getHostName());
        if(resolve) {
            try {
                resolveNames(set);
            } catch (Exception e) {
                log.error("Can not resolve names.", e);
            }
        }
    }

    private static void resolveNames(Set<String> set) throws Exception {
        /*TODO here we add many strange names like
        DNS Name: fe80:0:0:0:4897:56ff:fe28:dc7%veth2eb423a
        DNS Name: fe80:0:0:0:42:eeff:fee6:248b%docker_gwbridge
        DNS Name: fe80:0:0:0:7d86:95fa:f099:95c6%eth0
        DNS Name: 0:0:0:0:0:0:0:1%lo
        we must resolve it correct names or not
        */
        InetAddress localHost = InetAddress.getLocalHost();
        set.add(localHost.getCanonicalHostName());
        set.add(localHost.getHostAddress());
        for(Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces(); nis.hasMoreElements();) {
            NetworkInterface ni = nis.nextElement();
            for(Enumeration<InetAddress> ias = ni.getInetAddresses(); ias.hasMoreElements();) {
                InetAddress ia = ias.nextElement();
                set.add(ia.getCanonicalHostName());
                set.add(ia.getHostAddress());
                set.add(ia.getHostName());
            }
        }
    }

    static Cert constructCert(File keystoreFile, Set<String> names) throws Exception {
        log.debug("Use {} file as keystore.", keystoreFile.getAbsolutePath());
        Cert.Builder cb = Cert.builder();
        // verify: keytool -list -keystore dm-agent.jks
        KeyStore ks = KeyStore.Builder.newInstance("JKS", null, new KeyStore.PasswordProtection(null)).getKeyStore();
        X509CertificateHolder rootCert = createRootCert();
        Certificate jceRootCert = toJava(rootCert);
        String keypass = "1";
        KeyPair keyPair = createKeypair();
        X509CertificateHolder serverCert = createServerCert(keyPair, rootCert, names);
        Certificate jceServerCert = toJava(serverCert);
        ks.setKeyEntry("key", keyPair.getPrivate(), keypass.toCharArray(), new Certificate[]{jceServerCert, jceRootCert});
        String kspass = "1";
        cb.keystorePassword(kspass);
        cb.keyPassword(keypass);
        try(FileOutputStream fos = new FileOutputStream(keystoreFile)) {
            ks.store(fos, kspass.toCharArray());
        }
        cb.keystore(keystoreFile);
        return cb.build();
    }

    private static Certificate toJava(X509CertificateHolder certHolder) throws Exception {
        return new X509CertificateObject(certHolder.toASN1Structure());
    }

    private static X509CertificateHolder createRootCert() throws Exception {
        X500NameBuilder ib = new X500NameBuilder(RFC4519Style.INSTANCE);
        ib.addRDN(RFC4519Style.c, "US");
        ib.addRDN(RFC4519Style.o, "Code Above Lab LLC");
        ib.addRDN(RFC4519Style.l, "<city>");
        ib.addRDN(RFC4519Style.st, "<state>");
        ib.addRDN(PKCSObjectIdentifiers.pkcs_9_at_emailAddress, "hello@codeabovelab.com");
        X500Name issuer = ib.build();
        return createCert(createKeypair(), issuer, issuer, null);
    }

    private static X509CertificateHolder createServerCert(KeyPair keyPair,
                                                          X509CertificateHolder root,
                                                          Collection<String> names) throws Exception {
        X500NameBuilder sb = new X500NameBuilder(RFC4519Style.INSTANCE);
        sb.addRDN(RFC4519Style.name, "localhost");
        return createCert(keyPair, root.getIssuer(), sb.build(), cb -> {
            GeneralNamesBuilder gnb = new GeneralNamesBuilder();
            for(String name: names) {
                gnb.addName(new GeneralName(GeneralName.dNSName, name));
            }
            cb.addExtension(Extension.subjectAlternativeName, true, gnb.build());
        });
    }

    private static X509CertificateHolder createCert(KeyPair keyPair,
                                                    X500Name issuer,
                                                    X500Name subject,
                                                    CertBuilderHandler buildHandler) throws Exception {
        Calendar calendar = Calendar.getInstance();
        Date fromTime = calendar.getTime();
        calendar.add(Calendar.YEAR, 5);
        JcaX509v3CertificateBuilder cb = new JcaX509v3CertificateBuilder(
            issuer,
            BigInteger.valueOf(System.currentTimeMillis()),
            fromTime,
            calendar.getTime(),
            subject,
            keyPair.getPublic()
        );
        if(buildHandler != null) {
            buildHandler.handle(cb);
        }
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder ch = cb.build(signer);
        return ch;
    }

    static KeyPair createKeypair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(1024);
        return kpg.generateKeyPair();
    }
}

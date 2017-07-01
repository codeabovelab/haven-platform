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

package com.codeabovelab.dm.agent.security;

import com.codeabovelab.dm.common.utils.OSUtils;
import com.codeabovelab.dm.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
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
        InetAddress localHost = InetAddress.getLocalHost();
        set.add(localHost.getCanonicalHostName());
        set.add(localHost.getHostAddress());
        for(Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces(); nis.hasMoreElements();) {
            NetworkInterface ni = nis.nextElement();
            for(Enumeration<InetAddress> ias = ni.getInetAddresses(); ias.hasMoreElements();) {
                InetAddress ia = ias.nextElement();
                set.add(strip(ia.getCanonicalHostName()));
                set.add(strip(ia.getHostAddress()));
                set.add(strip(ia.getHostName()));
            }
        }
    }

    private static String strip(String address) {
        return StringUtils.beforeOr(address, '%', address::toString);
    }

    static KeystoreConfig constructCert(X509CertificateHolder rootCert, PrivateKey rootKey, File keystoreFile, Set<String> names) throws Exception {
        log.debug("Create certificate in {} keystore for names: {}", keystoreFile.getAbsolutePath(), names);
        KeystoreConfig.Builder cb = KeystoreConfig.builder();
        // verify: keytool -list -keystore dm-agent.jks
        KeyStore ks = KeyStore.Builder.newInstance("JKS", null, new KeyStore.PasswordProtection(null)).getKeyStore();
        Certificate jceRootCert = toJava(rootCert);
        // we use simple password, because no way to safe store password, and so complexity of password does nothing
        String keypass = "123456";
        String kspass = "123456";
        KeyPair keyPair = createKeypair();
        X509CertificateHolder serverCert = createServerCert(rootKey, rootCert, keyPair, names);
        Certificate jceServerCert = toJava(serverCert);
        ks.setKeyEntry("key", keyPair.getPrivate(), keypass.toCharArray(), new Certificate[]{jceServerCert, jceRootCert});
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

    private static X509CertificateHolder createServerCert(PrivateKey rootKey,
                                                          X509CertificateHolder root,
                                                          KeyPair keyPair,
                                                          Collection<String> names) throws Exception {
        X500NameBuilder sb = new X500NameBuilder(RFC4519Style.INSTANCE);
        sb.addRDN(RFC4519Style.name, "localhost");
        JcaX509v3CertificateBuilder cb = createCert(keyPair, root.getIssuer(), sb.build());
        GeneralNamesBuilder gnb = new GeneralNamesBuilder();
        for (String name : names) {
            gnb.addName(new GeneralName(GeneralName.dNSName, name));
        }
        cb.addExtension(Extension.subjectAlternativeName, true, gnb.build());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(rootKey);
        return cb.build(signer);
    }

    static JcaX509v3CertificateBuilder createCert(KeyPair keyPair,
                                                    X500Name issuer,
                                                    X500Name subject) {
        Calendar calendar = Calendar.getInstance();
        Date fromTime = calendar.getTime();
        calendar.add(Calendar.YEAR, 5);
        return new JcaX509v3CertificateBuilder(
            issuer,
            BigInteger.valueOf(System.currentTimeMillis()),
            fromTime,
            calendar.getTime(),
            subject,
            keyPair.getPublic()
        );
    }

    static KeyPair createKeypair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }
}

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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509v1CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.X509CertificateObject;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.*;

/**
 * Make self signed ssl cert for current ip and names
 */
@Component
@Slf4j
public class CertificateGenerator implements ApplicationListener<ApplicationPreparedEvent> {

    @Value("${dm.agent.server.names:}")
    private List<String> preconfiguredNames;
    @Value("${dm.agent.server.resolve}")
    private boolean resolve;
    private Cert cert;

    @PostConstruct
    public void init() {
        Set<String> names = new HashSet<>();
        gatherNames(names);
        try {
            this.cert = constructCert(names);
        } catch (Exception e) {
            log.error("Can not generate cert.", e);
        }
    }

    private void gatherNames(Set<String> set) {
        if(this.preconfiguredNames != null) {
            for(String name: this.preconfiguredNames) {
                if(StringUtils.hasText(name)) {
                    set.add(name);
                }
            }
        }
        set.add(OSUtils.getHostName());
        if(resolve) {
            try {
                resolveNames(set);
            } catch (Exception e) {
                log.error("Can not resolve names.", e);
            }
        }
    }

    private void resolveNames(Set<String> set) throws Exception {
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

    static Cert constructCert(Set<String> names) throws Exception {
        Cert.Builder cb = Cert.builder();
        // verify: keytool -list -keystore dm-agent.jks
        File keystore = new File("dm-agent.jks");
        KeyStore ks = KeyStore.Builder.newInstance("JKS", null, new KeyStore.PasswordProtection(null)).getKeyStore();
        ks.setCertificateEntry("cert", createCert());
        try(FileOutputStream fos = new FileOutputStream(keystore)) {
            ks.store(fos, new char[0]);
        }
        cb.keysore(keystore);
        return cb.build();
    }

    private static Certificate createCert() throws Exception {
        AsymmetricCipherKeyPair keyPair = createKeypair();
        Calendar calendar = Calendar.getInstance();
        Date fromTime = calendar.getTime();
        calendar.add(Calendar.YEAR, 5);
        X509v1CertificateBuilder cb = new BcX509v1CertificateBuilder(
            new X500Name("CN=Test Root Certificate"),
            BigInteger.valueOf(1),
            fromTime,
            calendar.getTime(),
            new X500Name("CN=Test Root Certificate"),
            keyPair.getPublic()
        );
        AlgorithmIdentifier sigAlg = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA1withRSA");
        AlgorithmIdentifier digAlg = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlg);
        ContentSigner signer = new BcRSAContentSignerBuilder(sigAlg, digAlg).build(keyPair.getPrivate());
        X509CertificateHolder ch = cb.build(signer);
        return new X509CertificateObject(ch.toASN1Structure());
    }

    static AsymmetricCipherKeyPair createKeypair() throws Exception {
        RSAKeyPairGenerator kg = new RSAKeyPairGenerator();
        //below values need verification
        RSAKeyGenerationParameters genParam = new RSAKeyGenerationParameters(
          BigInteger.valueOf(3), new SecureRandom(), 1024, 25);
        kg.init(genParam);
        return kg.generateKeyPair();
    }

    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event) {
        if(cert == null) {
            return;
        }
        ConfigurableListableBeanFactory bf = event.getApplicationContext().getBeanFactory();
        SslServletContainerCustomizer bean = new SslServletContainerCustomizer(cert);
        bf.autowireBean(bean);
        bf.registerSingleton("dmAgentSslConfigurer", bean);
    }
}

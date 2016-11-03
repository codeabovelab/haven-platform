//package com.codeabovelab.dm.balancer.web;
//
//import org.apache.catalina.connector.Connector;
//import org.apache.coyote.http11.Http11Nio2Protocol;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
//import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.io.File;
//
///**
// * Configures servlet container
// *
// */
//@Configuration
//public class ServletContainerConfiguration {
//
//
//    @Value("${https.keystore:}")
//    private String keystoreFile;
//    @Value("${https.keystore.password}")
//    private String keystorePass;
//    @Value("${https.port:8001}")
//    private int tlsPort;
//
//    @Value("${dm.ui.admin.password}")
//    private String adminPassword;
//
//    @Value("${security.basic.enabled:false}")
//    private boolean basicAuthEnable;
//
//    /**
//     * ads SSL connector to Tomcat
//     * @return
//     */
//    @Bean
//    public EmbeddedServletContainerFactory servletContainer() {
//        TomcatEmbeddedServletContainerFactory tomcat = new TomcatEmbeddedServletContainerFactory();
//        if(!keystoreFile.isEmpty()) {
//            tomcat.addAdditionalTomcatConnectors(createSslConnector());
//        }
//        return tomcat;
//    }
//
//    /**
//     * Configures ssl connector
//     * @return
//     */
//    Connector createSslConnector() {
//        final String absoluteKeystoreFile = new File(keystoreFile).getAbsolutePath();
//
//        Connector connector = new Connector("org.apache.coyote.http11.Http11Nio2Protocol");
//        connector.setPort(tlsPort);
//        connector.setSecure(true);
//        connector.setScheme("https");
//
//        Http11Nio2Protocol proto = (Http11Nio2Protocol) connector.getProtocolHandler();
//        proto.setSSLEnabled(true);
//        proto.setKeystoreFile(absoluteKeystoreFile);
//        proto.setKeystorePass(keystorePass);
//        proto.setKeystoreType("PKCS12");
//        proto.setSslProtocol("TLSv1.2");
//        proto.setKeyAlias("tomcat");
//        return connector;
//    }
//
//}

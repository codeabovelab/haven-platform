///*
// * Copyright 2016 Code Above Lab LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.codeabovelab.dm.mail.template;
//
//import com.codeabovelab.dm.mail.dto.*;
//import org.apache.commons.io.output.StringBuilderWriter;
//import org.apache.velocity.VelocityContext;
//import org.apache.velocity.app.VelocityEngine;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
//import org.springframework.stereotype.Component;
//import org.springframework.util.MimeType;
//
//import java.io.IOException;
//import java.io.Reader;
//import java.io.StringReader;
//import java.util.*;
//import java.util.function.Function;
//
///**
// */
//@ConditionalOnBean(type = "org.apache.velocity.app.VelocityEngine")
//@Component
//public class VelocityMailTemplateEngine implements MailTemplateEngine {
//
//    /**
//     * Id of this engine
//     */
//    public static final String ENGINE_ID = "velocity";
//    private final VelocityEngine velocityEngine;
//    private static final Set<String> SET = Collections.singleton(ENGINE_ID);
//
//
//
//    @Autowired
//    public VelocityMailTemplateEngine(VelocityEngine velocityEngine) {
//        this.velocityEngine = velocityEngine;
//    }
//
//    @Override
//    public Set<String> getProvidedEngines() {
//        return SET;
//    }
//
//    @Override
//    public MailMessage create(MailTemplate mailTemplate, MailSource source) {
//        MailPartTemplate bodySource = mailTemplate.getBodySource();
//        final MimeType mime = bodySource.getMime();
//        MailMessageImpl.Builder msgBuilder = MailMessageImpl.builder();
//        try(Reader reader = new StringReader(bodySource.getData())) {
//            String text = evaluate(source, reader);
//            msgBuilder.setBody(new MailTextBody(text, mime));
//        } catch (IOException e) {
//            throw new RuntimeException("On " + source.getTemplateUri());
//        }
//        MailHeadImpl.Builder headBuilder = MailHeadImpl.builder();
//        headBuilder.from(mailTemplate.getHeadSource(), new Interceptor(source));
//        msgBuilder.setHead(headBuilder.build());
//        return msgBuilder.build();
//    }
//
//    private String evaluate(MailSource source, Reader reader) {
//        StringBuilderWriter writer = new StringBuilderWriter();
//        VelocityContext context = new VelocityContext();
//        final Map<String, Object> variables = source.getVariables();
//        fillContext(context, variables);
//        String templateUri = source.getTemplateUri();
//        if(templateUri == null) {
//            // velocity does not allow null template name
//            templateUri = "<unknown template>";
//        }
//        velocityEngine.evaluate(context, writer, templateUri, reader);
//        return writer.toString();
//    }
//
//    private void fillContext(VelocityContext context, Map<String, Object> variables) {
//        for(Map.Entry<String, Object> e: variables.entrySet()) {
//            context.put(e.getKey(), e.getValue());
//        }
//    }
//
//    private class Interceptor implements Function<Object, Object> {
//        private final MailSource source;
//
//        public Interceptor(MailSource source) {
//            this.source = source;
//        }
//
//        @Override
//        @SuppressWarnings("unchecked")
//        public Object apply(Object arg) {
//            return MailTemplateUtils.process(this::evalHeadAttr, arg);
//        }
//
//
//        private String evalHeadAttr(Object item) {
//            return evaluate(this.source, new StringReader((String) item));
//        }
//    }
//}

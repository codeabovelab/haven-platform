/*
 * Copyright 2016 Code Above Lab LLC
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

package com.codeabovelab.dm.mail.template;

import com.codeabovelab.dm.mail.dto.*;
import com.google.common.collect.ImmutableSet;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.*;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Template engine based on Spring Expression Language. <p/>
 * For expression use follow syntax: '${spel}'. Also see: http://docs.spring.io/spring/docs/current/spring-framework-reference/html/expressions.html
 */
@Component
public class SpelMailTemplateEngine implements MailTemplateEngine {
    private static final ParserContext PC = new ParserContext() {
        @Override
        public boolean isTemplate() {
            return true;
        }

        @Override
        public String getExpressionPrefix() {
            return "${";
        }

        @Override
        public String getExpressionSuffix() {
            return "}";
        }
    };
    private static final Set<String> SET = ImmutableSet.of("spel");
    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Override
    public Set<String> getProvidedEngines() {
        return SET;
    }

    @Override
    public MailMessage create(MailTemplate mailTemplate, MailSource source) {
        MailMessageImpl.Builder b = MailMessageImpl.builder();
        final StandardEvaluationContext ctx = new StandardEvaluationContext(source.getVariables());
        ctx.addPropertyAccessor(new MapAccessor());
        UnaryOperator<Object> processor = (o) -> MailTemplateUtils.process((s) -> evaluate(ctx, s), o);
        b.setHead(MailHeadImpl.builder().from(mailTemplate.getHeadSource(), processor).build());
        MailPartTemplate bs = mailTemplate.getBodySource();
        String bodyText = (String) evaluate(ctx, bs.getData());
        b.setBody(new MailTextBody(bodyText, bs.getMime()));
        return b.build();
    }

    private Object evaluate(StandardEvaluationContext ctx, Object src) {
        if(src == null || !(src instanceof CharSequence)) {
            return src;
        }
        Expression expression = parser.parseExpression(src.toString(), PC);
        Object value = expression.getValue(ctx);
        return String.valueOf(value);
    }
}

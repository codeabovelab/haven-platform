/*
 * Copyright 2016 Code Above Lab LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.codeabovelab.dm.cluman.ui;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.handler.AbstractUrlHandlerMapping;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * Based on WebMvcAutoConfiguration.WelcomePageHandlerMapping
 */
final class WelcomePageHandlerMapping extends AbstractUrlHandlerMapping {

    // target page, it can be changed in future, or configured by property
    private final String target = "/index.html";

    WelcomePageHandlerMapping(Collection<String> paths) {
        ParameterizableViewController controller = new ParameterizableViewController();
        controller.setViewName("forward:" + target);
        paths.forEach(path -> registerHandler(path, controller));
        //we handle only non handled resources, but resource handler (which has LOWEST_PRECEDENCE - 1) handle all
        setOrder(LOWEST_PRECEDENCE - 10);
    }

    @Override
    public Object getHandlerInternal(HttpServletRequest request) throws Exception {
        String req = request.getRequestURI();
        //this prevent recursion and unnecessary mapping
        if(target.equals(req)) {
            return null;
        }
        List<MediaType> mediaTypes = MediaType
          .parseMediaTypes(request.getHeader(HttpHeaders.ACCEPT));
        for (MediaType mediaType : mediaTypes) {
            if (mediaType.includes(MediaType.TEXT_HTML)) {
                return super.getHandlerInternal(request);
            }
        }
        return null;
    }

}

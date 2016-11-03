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

package com.codeabovelab.dm.cluman.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller of UI entry point
 */
@RequestMapping("/ui")
@Controller
public class UiEntryPointController {

    @RequestMapping({"/", ""})
    public String main() {
        return "/ui/main.html";
    }

    @RequestMapping({"/info/"})
    public String info() {
        return "/ui/info.html";
    }

    @RequestMapping({"/nodes/"})
    public String nodes() {
        return "/ui/nodes.html";
    }

    @RequestMapping({"/login/"})
    public String login() {
        return "/ui/login.html";
    }

    @RequestMapping({"/stomp/"})
    public String stomp() {
        return "/ui/stomp.html";
    }
}

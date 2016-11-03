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

package com.codeabovelab.dm.cluman.ui.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 */
@Data
public class UiNodeGroup {
    private String name;
    /**
     * SpEL string which applied to images. It evaluated over object with 'tag(name)' and 'label(key, val)' functions,
     * also it has 'r(regexp)' function which can combined with other, like: <code>'spel:tag(r(".*_dev")) or label("dev", "true")'</code>.
     */
    @ApiModelProperty("SpEL string which applied to images. It evaluated over object with 'tag(name)' and 'label(key, val)' functions,\n" +
      "also it has 'r(regexp)' function which can combined with other, like: <code>'spel:tag(r(\".*_dev\")) or label(\"dev\", \"true\")'</code>.")
    private String filter;
}

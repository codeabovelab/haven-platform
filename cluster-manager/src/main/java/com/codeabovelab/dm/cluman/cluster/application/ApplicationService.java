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

package com.codeabovelab.dm.cluman.cluster.application;

import com.codeabovelab.dm.cluman.cluster.compose.model.ComposeArg;
import com.codeabovelab.dm.cluman.model.Application;
import com.codeabovelab.dm.cluman.model.ApplicationSource;

import java.io.File;
import java.util.List;

public interface ApplicationService {

    String APP_LABEL = "appLabel";

    List<Application> getApplications(String cluster);

    //TODO move long term commands to jobs
    void startApplication(String cluster, String id) throws Exception;

    CreateApplicationResult deployCompose(ComposeArg composeArg) throws Exception;

    void stopApplication(String cluster, String id);

    Application getApplication(String cluster, String id) ;

    ApplicationSource getSource(String cluster, String id);

    File getInitComposeFile(String cluster, String appId);

    void addApplication(Application application);

    void removeApplication(String cluster, String id);
}

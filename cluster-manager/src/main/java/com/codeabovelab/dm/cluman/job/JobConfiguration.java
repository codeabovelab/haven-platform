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

package com.codeabovelab.dm.cluman.job;

import com.codeabovelab.dm.common.mb.Subscriptions;
import lombok.Data;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Map;

/**
 */
@Configuration
@EnableConfigurationProperties(JobConfiguration.JobsManagerConfiguration.class)
@ComponentScan(basePackageClasses = JobsManagerImpl.class)
public class JobConfiguration {

    @ConfigurationProperties("dm.job")
    @Data
    public static class JobsManagerConfiguration {
        private Map<String, JobParameters.Builder> predefined;
        /**
         * Max number of concurrently executed scheduled jobs
         */
        private int schedulerPoolSize = 100;
        /**
         * Max number of concurrently executed jobs
         */
        private int executorPoolSize = 100;
        /**
         * Time which executed job remaining in system.
         * @see java.time.Duration#parse(CharSequence)
         */
        private String executedJobLifetime = "P1D";
    }


    @Bean(name = JobEvent.BUS)
    Subscriptions<JobEvent> jobEventSubscriptions(JobsManager jobsManager) {
        return jobsManager.getSubscriptions();
    }

    @Bean
    @Scope(JobScopeSupport.SCOPE_NAME)
    JobContext jobContext(JobScopeSupport jobScopeSupport) {
        return jobScopeSupport.getContext();
    }

    @Configuration
    public static class PredefinedJobsLoader implements ApplicationListener<ApplicationReadyEvent> {

        @Autowired
        private JobsManagerConfiguration config;

        @Autowired
        private JobsManager jobsManager;


        private void loadJobs() {
            Map<String, JobParameters.Builder> scheduled = config.getPredefined();
            if(scheduled != null) {
                for(Map.Entry<String, JobParameters.Builder> e: scheduled.entrySet()) {
                    JobParameters.Builder b = e.getValue();
                    b.setType(e.getKey());
                    JobParameters params = b.build();
                    try {
                        // we can run job or schedule it, behaviour depends from params.schedule field
                        JobInstance instance = jobsManager.create(params);
                        instance.start();
                    } catch(Exception ex) {
                        LoggerFactory.getLogger(PredefinedJobsLoader.class)
                          .error("Can not create predefined job: {}", params, ex);
                    }
                }
            }
        }

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            loadJobs();
        }
    }

}

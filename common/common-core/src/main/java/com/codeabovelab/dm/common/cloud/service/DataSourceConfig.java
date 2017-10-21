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

package com.codeabovelab.dm.common.cloud.service;

import com.codeabovelab.dm.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

import java.util.List;

/**
 * Creates and fills DataSourceProperties from Service Discovery
 * If discoveryClient and cloud.mysql.key property are not null tries to get connection params from SD
 * cloud.mysql.key have to contains db name information:  mysql:userDB
 *
 */
@Cloud
@Configuration
@AllArgsConstructor
public class DataSourceConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataSourceConfig.class);

    public static final String MYSQL_SERVICE_ID = "cloud.mysql.key";
    private static final String JDBC_PREFIX = "jdbc:";
    private static final String JDBC_URL_TYPE = "mysql:loadbalance";
    private static final Logger LOG = LoggerFactory.getLogger(DataSourceConfig.class);

    private final DiscoveryClient discoveryClient;

    private final LoadBalancerClient loadBalancerClient;
    private final Environment environment;

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setInitialize(false);
        // dbName.serviceName example: user-db.mysql (we must )
        final String serviceId = environment.getProperty(MYSQL_SERVICE_ID);
        if (discoveryClient != null && serviceId != null) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (!instances.isEmpty()) {
                properties.setUrl(getJdbcUrl(instances, fetchDBName(serviceId)));
            }  else {
                LOGGER.warn("there is no services with id {} in service discovery", serviceId);
            }
        }
        return properties;
    }

    private String fetchDBName(String property) {
        // internally spring use eureka VIP as service id (instead appName), so potentially we can have problems with invalid hostnames
        String dbname = StringUtils.before(property, '.');
        Assert.isTrue(dbname != null, MYSQL_SERVICE_ID + "property doesn't match pattern dbname.dbserver, property is '" + property + "'");
        // many db does not allow '-', but allow '_' which is forbidden in hostnames
        dbname = dbname.replace('-', '_');
        return dbname;
    }

    public String getJdbcUrl(List<ServiceInstance> instances, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(JDBC_PREFIX).append(JDBC_URL_TYPE).append("://");
        int i = 0;
        for(ServiceInstance instance: instances) {
            if(i > 0) {
                sb.append(',');
            }
            sb.append(instance.getHost());
            addPort(sb, instance.getPort());
            i++;
            LOGGER.info("registered mysql from cloud {}:{}", instance.getHost(), instance.getPort());
        }
        sb.append('/').append(path);
        String url = sb.toString();
        LOG.info("Database url:{}", url);
        return url;
    }

    private void addPort(StringBuilder sb, int port) {
        if (port > 0) {
            sb.append(':').append(port);
        }
    }

}

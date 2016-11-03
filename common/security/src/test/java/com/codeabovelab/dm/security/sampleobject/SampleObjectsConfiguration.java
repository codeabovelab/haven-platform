package com.codeabovelab.dm.security.sampleobject;

import org.flywaydb.core.Flyway;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 */
@Configuration
@Import(SampleObjectsConfiguration.LifecycleListener.class)
public class SampleObjectsConfiguration extends HibernateJpaAutoConfiguration  {

    @Configuration
    public static class LifecycleListener {
        @Autowired(required = false)
        private DataSource dataSource;

        @Bean
        Flyway flyway() {
            Flyway flyway = new Flyway();
            flyway.setDataSource(dataSource);
            flyway.setLocations("classpath:db/test_migration", "classpath:db/migration");
            flyway.setInitOnMigrate(true);
            return flyway;
        }
    }


    @Autowired
    Flyway flyway;

    @PostConstruct
    private void postConstruct() {
        flyway.migrate();
    }

    @PreDestroy
    private void preDestroy() {
        flyway.clean();
    }
}

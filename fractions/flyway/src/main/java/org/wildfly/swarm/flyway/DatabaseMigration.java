package org.wildfly.swarm.flyway;

import java.util.logging.Logger;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

public class DatabaseMigration implements Integrator {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigration.class.getName());
    //@Inject
    //private Instance<FlywayFraction> flywayFractionInstance;
    @Override
    public void integrate(Metadata configuration, SessionFactoryImplementor sessionFactory,
                          SessionFactoryServiceRegistry serviceRegistry) {

        LOGGER.info("Migrating");
        final Flyway flyway = new Flyway();

        DataSource dataSource = (DataSource) sessionFactory.getProperties().get("hibernate.connection.datasource");

        flyway.setDataSource(dataSource);


        flyway.configure(System.getProperties());
        flyway.migrate();

    }

    @Override
    public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
        // do nothing
    }
}

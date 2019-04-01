/*
 * Copyright 2016 Red Hat, Inc, and individual contributors.
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
package org.wildfly.swarm.flyway.runtime;

import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;
import org.wildfly.swarm.config.datasources.DataSource;
import org.wildfly.swarm.datasources.DatasourcesFraction;
import org.wildfly.swarm.flyway.FlywayFraction;
import org.wildfly.swarm.spi.api.DeploymentProcessor;
import org.wildfly.swarm.spi.runtime.annotations.DeploymentScoped;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@DeploymentScoped
public class FlywayMigrationInstaller implements DeploymentProcessor {

    private static final Logger logger = Logger.getLogger(FlywayMigrationInstaller.class);
    @Inject
    private Instance<FlywayFraction> flywayFractionInstance;

    @Inject
    private Instance<DatasourcesFraction> dsFractionInstance;

    public FlywayMigrationInstaller() {
    }

    @Override
    public void process() throws NamingException {
        FlywayFraction flywayFraction = flywayFractionInstance.get();
        Flyway flyway = new Flyway();

        if (flywayFraction.usePrimaryDataSource()) {
            DataSource primaryDatasource = getPrimaryDatasource();
            if (primaryDatasource == null) {
                logger.warn("Flyway fraction is installed but no valid configuration is provided");
            }
            javax.sql.DataSource dataSource = (javax.sql.DataSource) new InitialContext().lookup(primaryDatasource.jndiName());
            flyway.setDataSource(dataSource);
        } else {
            flyway.setDataSource(flywayFraction.jdbcUrl(), flywayFraction.jdbcUser(), flywayFraction.jdbcPassword());

        }
        flyway.configure(System.getProperties());
        flyway.migrate();
        logger.info("Done");
    }

    private DataSource getPrimaryDatasource() {
        if (!dsFractionInstance.isUnsatisfied()) {
            List<DataSource> dataSources = dsFractionInstance.get().subresources().dataSources();
            if (dataSources.size() > 0) {
                return dataSources.get(0);
            }
        }
        return null;
    }

}

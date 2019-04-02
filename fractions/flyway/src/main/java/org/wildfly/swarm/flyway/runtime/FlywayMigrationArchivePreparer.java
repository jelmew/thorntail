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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.wildfly.swarm.bootstrap.util.TempFileManager;
import org.wildfly.swarm.config.datasources.DataSource;
import org.wildfly.swarm.datasources.DatasourcesFraction;
import org.wildfly.swarm.flyway.DatabaseMigration;
import org.wildfly.swarm.flyway.FlywayFraction;
import org.wildfly.swarm.spi.api.DeploymentProcessor;
import org.wildfly.swarm.spi.runtime.annotations.DeploymentScoped;
import org.wildfly.swarm.undertow.WARArchive;

/**
 * @author <a href="mailto:ggastald@redhat.com">George Gastaldi</a>
 */
@DeploymentScoped
public class FlywayMigrationArchivePreparer implements DeploymentProcessor {

    private static final Logger logger = Logger.getLogger(FlywayMigrationArchivePreparer.class.getName());
    public static final String INTEGRATOR = "services/org.hibernate.integrator.spi.Integrator";
    private final Archive<?> archive;

    @Inject
    private Instance<DatasourcesFraction> dsFractionInstance;

    @Inject
    private Instance<FlywayFraction> flywayFractionInstance;

    @Inject
    public FlywayMigrationArchivePreparer(Archive archive) {
        this.archive = archive;
    }

    @Override
    public void process() throws IOException {
        logger.info("Adding integrator to war");
        if (archive.getName().endsWith(".war")) {

            File dir = TempFileManager.INSTANCE.newTempDirectory("thorntail-flyway-config", ".d");
            ByteArrayAsset asset = new ByteArrayAsset("org.wildfly.swarm.flyway.deployment.DatabaseMigration".getBytes());
            WARArchive warArchive = archive.as(WARArchive.class);
           // warArchive.add(asset, "services/org.hibernate.integrator.spi.Integrator");
            logger.info("Adding class");
             warArchive = warArchive.addClass(DatabaseMigration.class);
            InputStream resourceAsStream = getClass().getClassLoader()
                                                     .getResourceAsStream(INTEGRATOR);

            logger.info("Adding services file");
            String IntegratorString = convertStreamToString(resourceAsStream);
            StringAsset stringAsset = new StringAsset(IntegratorString);
            warArchive = warArchive.addAsManifestResource(stringAsset,INTEGRATOR);
            //warArchive.addAsResource(new File(dir + INTEGRATOR), INTEGRATOR);
            logger.info("Added to war");

            //logger.info(warArchive.contains(INTEGRATOR));
            logger.info(warArchive.getContent());
        }
    }

    private String getDatasourceNameJndi() {
        String jndiName = "java:jboss/datasources/ExampleDS";
        if (!dsFractionInstance.isUnsatisfied()) {
            List<DataSource> dataSources = dsFractionInstance.get().subresources().dataSources();
            if (dataSources.size() > 0) {
                jndiName = dataSources.get(0).jndiName();
            }
        }
        return jndiName;
    }
    static String convertStreamToString(java.io.InputStream is) {
    java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
}

}

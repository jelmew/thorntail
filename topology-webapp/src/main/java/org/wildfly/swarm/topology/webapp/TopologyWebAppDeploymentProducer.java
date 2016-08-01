package org.wildfly.swarm.topology.webapp;

import java.util.Map;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.wildfly.swarm.spi.api.annotations.ConfigurationValue;
import org.wildfly.swarm.topology.TopologyArchive;
import org.wildfly.swarm.undertow.WARArchive;

/**
 * @author Bob McWhirter
 */
@Singleton
public class TopologyWebAppDeploymentProducer {

    @Inject
    private TopologyWebAppFraction fraction;

    @Inject
    @ConfigurationValue("swarm.topology.context.path")
    private String contextPath;

    @Produces @Dependent()
    Archive deployment() {

        String context = TopologyWebAppFraction.DEFAULT_CONTEXT;
        if (this.contextPath != null) {
            context = this.contextPath;
        }

        if (fraction.exposeTopologyEndpoint()) {
            WARArchive war = ShrinkWrap.create(WARArchive.class, "topology-webapp.war");
            war.addAsWebInfResource(new StringAsset(getWebXml(fraction)), "web.xml");
            war.addClass(TopologySSEServlet.class);
            war.addModule("swarm.application");
            war.addModule("org.wildfly.swarm.topology");
            war.addAsWebResource(new ClassLoaderAsset("topology.js", this.getClass().getClassLoader()), "topology.js");
            war.setContextRoot(context);
            war.as(TopologyArchive.class);
            return war;
        }
        return null;
    }

    protected String getWebXml(TopologyWebAppFraction fraction) {
        String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\"" +
                "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" +
                "    xsi:schemaLocation=\"http://java.sun.com/xml/ns/javaee" +
                "                        http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd\"" +
                "    version=\"3.0\">";

        Map<String, String> proxiedServiceMappings = fraction.proxiedServiceMappings();
        for (String serviceName : proxiedServiceMappings.keySet()) {
            String contextPath = proxiedServiceMappings.get(serviceName);
            webXml += "    <context-param>" +
                    "        <param-name>" + serviceName + "-proxy</param-name>" +
                    "        <param-value>" + contextPath + "</param-value>" +
                    "    </context-param>";
        }

        webXml += "</web-app>";
        return webXml;
    }
}

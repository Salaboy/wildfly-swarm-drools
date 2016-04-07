/**
 * Copyright 2015-2016 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.swarm.kie.server.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.wildfly.swarm.bootstrap.util.TempFileManager;
import org.wildfly.swarm.config.security.Flag;
import org.wildfly.swarm.config.security.security_domain.authentication.LoginModule;
import org.wildfly.swarm.jaxrs.JAXRSArchive;
import org.wildfly.swarm.kie.server.KieServerFraction;
import org.wildfly.swarm.spi.api.ArtifactLookup;
import org.wildfly.swarm.spi.runtime.AbstractServerConfiguration;

/**
 * @author Salaboy
 */
public class KieServerConfiguration extends AbstractServerConfiguration<KieServerFraction> {

    private static String configFolder = System.getProperty("org.kie.server.swarm.web.conf");

    public KieServerConfiguration() {
        super(KieServerFraction.class);
    }

    @Override
    public KieServerFraction defaultFraction() {
        return new KieServerFraction();
    }

    @Override
    public List<Archive> getImplicitDeployments(KieServerFraction fraction, ArtifactLookup lookup) throws Exception {
        

        if (System.getProperty("org.kie.server.swarm.web.conf") == null) {
            try {
                //Path dir = Files.createTempDirectory("swarm-keycloak-config");
                File dir = TempFileManager.INSTANCE.newTempDirectory("swarm-kie-web-config", ".d");
                System.setProperty("org.kie.server.swarm.conf", dir.getAbsolutePath());
                Files.copy(getClass().getClassLoader().getResourceAsStream("config/web/web.xml"),
                        dir.toPath().resolve("web.xml"),
                        StandardCopyOption.REPLACE_EXISTING);
                Files.copy(getClass().getClassLoader().getResourceAsStream("config/web/jboss-web.xml"),
                        dir.toPath().resolve("jboss-web.xml"),
                        StandardCopyOption.REPLACE_EXISTING);
                configFolder = dir.toPath().toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("\tConfiguration folder is " + configFolder);
        
        JAXRSArchive deployment = ShrinkWrap.create(JAXRSArchive.class, "kie-server.war");
        deployment.addAllDependencies();

        deployment.addAsWebInfResource(new File(configFolder + "/web.xml"), "web.xml");
        deployment.addAsWebInfResource(new File(configFolder + "/jboss-web.xml"), "jboss-web.xml");
        List<Archive> archives = new ArrayList<>();
        archives.add(deployment);
        return archives;
    }

}

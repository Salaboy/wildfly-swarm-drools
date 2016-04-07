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
package org.wildfly.swarm.kie.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.ReleaseId;
import org.kie.server.services.impl.storage.KieServerState;
import org.kie.server.services.impl.storage.file.KieServerStateFileRepository;
import org.wildfly.swarm.bootstrap.util.TempFileManager;
import org.wildfly.swarm.config.security.Flag;
import org.wildfly.swarm.config.security.SecurityDomain;
import org.wildfly.swarm.config.security.security_domain.ClassicAuthentication;
import org.wildfly.swarm.config.security.security_domain.authentication.LoginModule;
import org.wildfly.swarm.security.SecurityFraction;
import org.wildfly.swarm.spi.api.Fraction;

/**
 * @author Salaboy
 */
public class KieServerFraction implements Fraction {
    
    private static String configFolder = System.getProperty("org.kie.server.swarm.security.conf");

    public KieServerFraction() {
    }

    @Override
    public void postInitialize(Fraction.PostInitContext initContext) {
        
        
        if (System.getProperty("org.kie.server.swarm.security.conf") == null) {
            try {
                //Path dir = Files.createTempDirectory("swarm-keycloak-config");
                File dir = TempFileManager.INSTANCE.newTempDirectory("swarm-kie-security-config", ".d");
                System.setProperty("org.kie.server.swarm.conf", dir.getAbsolutePath());
                Files.copy(getClass().getClassLoader().getResourceAsStream("config/security/application-users.properties"),
                           dir.toPath().resolve("application-users.properties"),
                           StandardCopyOption.REPLACE_EXISTING);
                Files.copy(getClass().getClassLoader().getResourceAsStream("config/security/application-roles.properties"),
                           dir.toPath().resolve("application-roles.properties"),
                           StandardCopyOption.REPLACE_EXISTING);
                configFolder = dir.toPath().toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        
        System.out.println("\tConfiguration folder is " + configFolder);
        LoginModule<?> loginModule = new LoginModule<>("UsersRoles");
        loginModule.flag(Flag.REQUIRED)
        .code("UsersRoles")
        .moduleOption("usersProperties", configFolder + "/application-users.properties")
        .moduleOption("rolesProperties", configFolder + "/application-roles.properties");
        
        SecurityDomain<?> security = new SecurityDomain<>("other-drools")
                .classicAuthentication(new ClassicAuthentication<>()
                        .loginModule(loginModule)); 
        
        SecurityFraction securityFraction = (SecurityFraction) initContext.fraction("security");
        securityFraction.securityDomain(security);
        
//        container.fraction(new SecurityFraction().securityDomain(security));
        
        
        
        

//        if (System.getProperty("jboss.server.config.dir") == null) {
//            try {
//                //Path dir = Files.createTempDirectory("swarm-keycloak-config");
//                File dir = TempFileManager.INSTANCE.newTempDirectory("swarm-keycloak-config", ".d");
//                System.setProperty("jboss.server.config.dir", dir.getAbsolutePath());
//                Files.copy(getClass().getClassLoader().getResourceAsStream("keycloak-server.json"),
//                           dir.toPath().resolve("keycloak-server.json"),
//                           StandardCopyOption.REPLACE_EXISTING);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }


//        DatasourcesFraction datasources = (DatasourcesFraction) initContext.fraction("datasources");
//
//        if (datasources.subresources().dataSource("KeycloakDS") == null) {
//            if (datasources.subresources().jdbcDriver("h2") == null) {
//                datasources.jdbcDriver("h2", (driver) -> {
//                    driver.driverModuleName("com.h2database.h2");
//                    driver.moduleSlot("main");
//                    driver.xaDatasourceClass("org.h2.jdbcx.JdbcDataSource");
//                });
//            }
//
//            datasources.dataSource("KeycloakDS", (ds) -> {
//                ds.jndiName("java:jboss/datasources/KeycloakDS");
//                ds.useJavaContext(true);
//                ds.connectionUrl("jdbc:h2:${wildfly.swarm.keycloak.server.db:./keycloak};AUTO_SERVER=TRUE");
//                ds.driverName("h2");
//                ds.userName("sa");
//                ds.password("sa");
//            });
//        }
    }
    
    
    protected static void installKJars(String[] args) {
        
        if (args == null || args.length == 0) {
            return;
        }        
        String serverId = System.getProperty(KieServerConstants.KIE_SERVER_ID);
        String controller = System.getProperty(KieServerConstants.KIE_SERVER_CONTROLLER);
        
        if ( controller != null) {
            System.out.println("Controller is configured ("+controller+") - no local kjars can be installed");
            return;
        }
        
        // proceed only when kie server id is given and there is no controller
        if (serverId != null) {
            KieServerStateFileRepository repository = new KieServerStateFileRepository();
            KieServerState currentState = repository.load(serverId);
            
            Set<KieContainerResource> containers = new HashSet<KieContainerResource>();
            

            for (String gav : args) {
                String[] gavElements = gav.split(":");
                ReleaseId releaseId = new ReleaseId(gavElements[0], gavElements[1], gavElements[2]);
                
                
                KieContainerResource container = new KieContainerResource(releaseId.getArtifactId(), releaseId, KieContainerStatus.STARTED);
                containers.add(container);
            }
            
            currentState.setContainers(containers);
            
            repository.store(serverId, currentState);
        }
    }
    

}

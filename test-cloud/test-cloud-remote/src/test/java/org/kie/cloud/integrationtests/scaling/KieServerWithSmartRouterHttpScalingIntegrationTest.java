/*
 * Copyright 2017 JBoss by Red Hat.
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
package org.kie.cloud.integrationtests.scaling;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.kie.cloud.api.DeploymentScenarioBuilderFactory;
import org.kie.cloud.api.scenario.ClusteredWorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenario;
import org.kie.cloud.common.provider.KieServerClientProvider;
import org.kie.cloud.common.provider.KieServerControllerClientProvider;
import org.kie.cloud.common.provider.SmartRouterAdminClientProvider;
import org.kie.cloud.tests.common.AbstractMethodIsolatedCloudIntegrationTest;
import org.kie.cloud.integrationtests.Kjar;
import org.kie.cloud.integrationtests.category.ApbNotSupported;
import org.kie.cloud.integrationtests.util.SmartRouterUtils;
import org.kie.cloud.integrationtests.util.WorkbenchUtils;
import org.kie.cloud.maven.MavenDeployer;
import org.kie.cloud.maven.constants.MavenConstants;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieContainerStatus;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.controller.api.model.runtime.ServerInstanceKey;
import org.kie.server.controller.api.model.spec.ContainerSpec;
import org.kie.server.controller.client.KieServerControllerClient;
import org.kie.server.integrationtests.router.client.KieServerRouterClient;
import org.kie.server.router.Configuration;

@Category(ApbNotSupported.class) // Because DroolsServerFilterClasses not supported yet
public class KieServerWithSmartRouterHttpScalingIntegrationTest extends AbstractMethodIsolatedCloudIntegrationTest<ClusteredWorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenario> {
    private static final String SMART_ROUTER_ID = "test-kie-router";

    private KieServerControllerClient kieControllerClient;
    private KieServicesClient kieServerClient;
    private KieServerRouterClient smartRouterAdminClient;

    @Override
    protected ClusteredWorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenario createDeploymentScenario(DeploymentScenarioBuilderFactory deploymentScenarioFactory) {
        return deploymentScenarioFactory.getClusteredWorkbenchRuntimeSmartRouterTwoKieServersTwoDatabasesScenarioBuilder()
                .withExternalMavenRepo(MavenConstants.getMavenRepoUrl(), MavenConstants.getMavenRepoUser(), MavenConstants.getMavenRepoPassword())
                .withSmartRouterId(SMART_ROUTER_ID)
                .build();
    }

    @BeforeClass
    public static void buildKjar() {
        MavenDeployer.buildAndDeployMavenProject(ClassLoader.class.getResource("/kjars-sources/definition-project-snapshot").getFile());
    }

    @Before
    public void setUp() {
        kieControllerClient = KieServerControllerClientProvider.getKieServerControllerClient(deploymentScenario.getWorkbenchRuntimeDeployment());
        kieServerClient = KieServerClientProvider.getKieServerClient(deploymentScenario.getKieServerOneDeployment());
        smartRouterAdminClient = SmartRouterAdminClientProvider.getSmartRouterClient(deploymentScenario.getSmartRouterDeployment());
    }

    @Test
    public void testConnectionBetweenDeployables() {
        String kieServerId = getKieServerId(kieServerClient);

        verifyServerTemplateContainsKieServers(kieServerId, 1);
        verifyServerTemplateContainsKieServers(SMART_ROUTER_ID, 1);

        deployAndStartContainer();

        verifyContainerIsDeployed(kieServerClient, CONTAINER_ID);
        verifyServerTemplateContainsContainer(kieServerId, CONTAINER_ID);
        verifyServerTemplateContainsContainer(SMART_ROUTER_ID, CONTAINER_ID);
        verifySmartRouterContainsKieServers(kieServerId, 1);

        scaleKieServerTo(3);

        // In case of WebSockets (default option) all Kie servers with same URL are registered under one server template instance
        verifyServerTemplateContainsKieServers(kieServerId, 1);
        verifySmartRouterContainsKieServers(kieServerId, 3);

        scaleKieServerTo(1);

        verifyServerTemplateContainsKieServers(kieServerId, 1);
        verifySmartRouterContainsKieServers(kieServerId, 1);
    }

    private String getKieServerId(KieServicesClient kieServerClient) {
        ServiceResponse<KieServerInfo> serverInfo = kieServerClient.getServerInfo();
        assertThat(serverInfo.getType()).isEqualTo(ResponseType.SUCCESS);

        return serverInfo.getResult().getServerId();
    }

    private void verifyContainerIsDeployed(KieServicesClient kieServerClient, String containerId) {
        ServiceResponse<KieContainerResourceList> containers = kieServerClient.listContainers();
        assertThat(containers.getType()).isEqualTo(ResponseType.SUCCESS);

        List<KieContainerResource> containerList = containers.getResult().getContainers();
        assertThat(containerList).hasSize(1);
        assertThat(containerList.get(0).getContainerId()).isEqualTo(containerId);
        assertThat(containerList.get(0).getStatus()).isEqualTo(KieContainerStatus.STARTED);
    }

    private void verifyServerTemplateContainsContainer(String serverTemplate, String containerId) {
        Collection<ContainerSpec> containersSpec = kieControllerClient.getServerTemplate(serverTemplate).getContainersSpec();
        assertThat(containersSpec).hasSize(1);
        assertThat(containersSpec.iterator().next().getId()).isEqualTo(containerId);
    }

    private void verifyServerTemplateContainsKieServers(String serverTemplate, int numberOfKieServers) {
        Collection<ServerInstanceKey> kieServers = kieControllerClient.getServerTemplate(serverTemplate).getServerInstanceKeys();
        assertThat(kieServers).hasSize(numberOfKieServers);
    }

    private void verifySmartRouterContainsKieServers(String kieServerId, int numberOfKieServers) {
        Configuration routerConfig = smartRouterAdminClient.getRouterConfig();

        assertThat(routerConfig.getHostsPerServer()).containsKey(kieServerId);
        assertThat(routerConfig.getHostsPerServer().get(kieServerId)).hasSize(numberOfKieServers);
        assertThat(routerConfig.getHostsPerContainer()).containsKey(CONTAINER_ID);
        assertThat(routerConfig.getHostsPerContainer().get(CONTAINER_ID)).hasSize(numberOfKieServers);
        assertThat(routerConfig.getContainerInfosPerContainer()).containsKey(CONTAINER_ID);
    }

    private void deployAndStartContainer() {
        KieServerInfo serverInfo = kieServerClient.getServerInfo().getResult();
        WorkbenchUtils.saveContainerSpec(kieControllerClient, serverInfo.getServerId(), serverInfo.getName(), CONTAINER_ID, CONTAINER_ALIAS, Kjar.DEFINITION_SNAPSHOT, KieContainerStatus.STARTED);
        // Wait until container is started in Kie server
        KieServerClientProvider.waitForContainerStart(deploymentScenario.getKieServerOneDeployment(), CONTAINER_ID);
        // Wait until container is registered in Smart router
        SmartRouterUtils.waitForContainerStart(smartRouterAdminClient, CONTAINER_ID);
        // Wait until container is registered in Workbench under Smart router server template
        // (containers are registered asynchronously in a batch).
        WorkbenchUtils.waitForContainerRegistration(kieControllerClient, SMART_ROUTER_ID, CONTAINER_ID);
    }

    private void scaleKieServerTo(int count) {
        deploymentScenario.getKieServerOneDeployment().scale(count);
        deploymentScenario.getKieServerOneDeployment().waitForScale();
    }
}

/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.cloud.openshift.operator.scenario;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cz.xtf.openshift.OpenShiftBinaryClient;
import cz.xtf.wait.SimpleWaiter;
import org.kie.cloud.api.deployment.ControllerDeployment;
import org.kie.cloud.api.deployment.Deployment;
import org.kie.cloud.api.deployment.KieServerDeployment;
import org.kie.cloud.api.deployment.SmartRouterDeployment;
import org.kie.cloud.api.deployment.WorkbenchDeployment;
import org.kie.cloud.api.scenario.WorkbenchKieServerScenario;
import org.kie.cloud.common.provider.KieServerControllerClientProvider;
import org.kie.cloud.openshift.deployment.KieServerDeploymentImpl;
import org.kie.cloud.openshift.deployment.WorkbenchDeploymentImpl;
import org.kie.cloud.openshift.operator.resources.OpenShiftResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkbenchKieServerScenarioImpl extends OpenShiftOperatorScenario implements WorkbenchKieServerScenario {

    private WorkbenchDeploymentImpl workbenchDeployment;
    private KieServerDeploymentImpl kieServerDeployment;

    private static final Logger logger = LoggerFactory.getLogger(WorkbenchKieServerScenarioImpl.class);

    @Override
    public void deploy() {
        super.deploy();

        // deploy application
        logger.info("Creating application from " + OpenShiftResource.WORKBENCH_KIE_SERVER.getResourceUrl().toString());
        OpenShiftBinaryClient.getInstance().executeCommand("Deployment failed.", "create", "-f", OpenShiftResource.WORKBENCH_KIE_SERVER.getResourceUrl().toString());

        // Wait until deployment is created
        // Temporary, replace by something better.
        try {
            new SimpleWaiter(() -> project.getOpenShiftUtil().getServices().size() == 2).timeout(TimeUnit.MINUTES, 1).execute();
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout while deploying application.", e);
        }

        // Usernames/passwords currently hardcoded
        workbenchDeployment = new WorkbenchDeploymentImpl(project);
        workbenchDeployment.setUsername("adminUser");
        workbenchDeployment.setPassword("RedHat");

        kieServerDeployment = new KieServerDeploymentImpl(project);
        kieServerDeployment.setUsername("executionUser");
        kieServerDeployment.setPassword("RedHat");
        kieServerDeployment.setServiceSuffix("-0");

        logger.info("Waiting for Workbench deployment to become ready.");
        workbenchDeployment.waitForScale();

        logger.info("Waiting for Kie server deployment to become ready.");
        kieServerDeployment.waitForScale();

        logger.info("Waiting for Kie server to register itself to the Workbench.");
        KieServerControllerClientProvider.waitForServerTemplateCreation(workbenchDeployment, 1);

        logNodeNameOfAllInstances();

        // Used to track persistent volume content due to issues with volume cleanup
        storeProjectInfoToPersistentVolume(workbenchDeployment, "/opt/eap/standalone/data/kie");
    }

    @Override
    public WorkbenchDeployment getWorkbenchDeployment() {
        return workbenchDeployment;
    }

    @Override
    public KieServerDeployment getKieServerDeployment() {
        return kieServerDeployment;
    }

    @Override
    public List<Deployment> getDeployments() {
        List<Deployment> deployments = new ArrayList<Deployment>(Arrays.asList(workbenchDeployment, kieServerDeployment));
        deployments.removeAll(Collections.singleton(null));
        return deployments;
    }

    private void storeProjectInfoToPersistentVolume(Deployment deployment, String persistentVolumeMountPath) {
        String storeCommand = "echo \"Project " + projectName + ", time " + Instant.now() + "\" > " + persistentVolumeMountPath + "/info.txt";
        workbenchDeployment.getInstances().get(0).runCommand("/bin/bash", "-c", storeCommand);
    }

    @Override
    public List<WorkbenchDeployment> getWorkbenchDeployments() {
        return Arrays.asList(workbenchDeployment);
    }

    @Override
    public List<KieServerDeployment> getKieServerDeployments() {
        return Arrays.asList(kieServerDeployment);
    }

    @Override
    public List<SmartRouterDeployment> getSmartRouterDeployments() {
        return Collections.emptyList();
    }

    @Override
    public List<ControllerDeployment> getControllerDeployments() {
        return Collections.emptyList();
    }
}

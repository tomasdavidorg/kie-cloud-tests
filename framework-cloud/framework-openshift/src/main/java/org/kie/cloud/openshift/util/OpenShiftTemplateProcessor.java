/*
 * Copyright 2018 JBoss by Red Hat.
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

package org.kie.cloud.openshift.util;

import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.openshift.client.OpenShiftConfig;
import io.fabric8.openshift.client.OpenShiftConfigBuilder;
import org.kie.cloud.openshift.template.OpenShiftTemplate;

/**
 * Utility class used for various operations against OpenShift template.
 */
public class OpenShiftTemplateProcessor {

    private static OpenShiftClient openShiftClient;

    static {
        OpenShiftConfig openShiftConfig = new OpenShiftConfigBuilder()
                .withDisableApiGroupCheck(true)
                .build();
        openShiftClient = new DefaultOpenShiftClient(openShiftConfig);
    }

    /**
     * @param openShiftTemplate OpenShift template to be processed.
     * @param parameterName Name of template parameter.
     * @return Value of parameter from OpenShift template.
     */
    public static String getParameterValue(OpenShiftTemplate openShiftTemplate, String parameterName) {
        Template template = openShiftClient.templates().load(openShiftTemplate.getTemplateUrl()).get();
        Parameter parameter = template.getParameters().stream().filter(p -> p.getName().equals(parameterName))
                                                               .findAny()
                                                               .orElseThrow(() -> new RuntimeException("Parameter " + parameterName + " not found."));
        return parameter.getValue();
    }
}

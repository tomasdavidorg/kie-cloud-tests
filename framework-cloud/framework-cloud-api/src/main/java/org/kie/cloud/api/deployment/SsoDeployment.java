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
package org.kie.cloud.api.deployment;

import java.net.URL;
import java.util.Optional;

/**
 * SSO deplyoment representation in cloud.
 */
public interface SsoDeployment extends Deployment {

    /**
     * Get URL for SSO service (deployment).
     *
     * @return SSO URL
     */
    URL getUrl();

    /**
     * Get HTTP URL for SSO service (deployment).
     *
     * @return SSO URL
     */
    Optional<URL> getInsecureUrl();

    /**
     * Get HTTPS URL for SSO service (deployment).
     *
     * @return SSO URL
     */
    Optional<URL> getSecureUrl();

    /**
     * Get SSO user name.
     *
     * @return SSO user name
     */
    String getUsername();

    /**
     * Get SSO user password.
     *
     * @return SSO user password
     */
    String getPassword();
}

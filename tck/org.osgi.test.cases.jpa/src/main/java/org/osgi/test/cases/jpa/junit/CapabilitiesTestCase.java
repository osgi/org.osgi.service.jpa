/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.test.cases.jpa.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.osgi.framework.Version.parseVersion;
import static org.osgi.namespace.contract.ContractNamespace.CONTRACT_NAMESPACE;
import static org.osgi.namespace.extender.ExtenderNamespace.EXTENDER_NAMESPACE;
import static org.osgi.resource.Namespace.CAPABILITY_USES_DIRECTIVE;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.contract.ContractNamespace;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @version $Rev$ $Date$
 */
public class CapabilitiesTestCase extends JPATestSupport {



	public static final long SERVICE_WAIT_TIME = 5000;

	/**
	 * See JPA Service Spec 127.7.1
	 * 
	 * @throws Exception
	 */
	@Test
	public void testExtenderCapability() throws Exception {

		boolean found = false;

		outer: for (Bundle b : context.getBundles()) {

			BundleWiring wiring = b.adapt(BundleWiring.class);

			if (wiring == null)
				continue;

			for (BundleCapability bc : wiring
					.getCapabilities(EXTENDER_NAMESPACE)) {
				Map<String,Object> attributes = bc.getAttributes();
				if ("osgi.jpa".equals(attributes.get(EXTENDER_NAMESPACE))
						&& parseVersion("1.1").equals(attributes.get(
								ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE))) {

					String uses = bc.getDirectives()
							.get(CAPABILITY_USES_DIRECTIVE);

					if (uses != null) {
						Set<String> packages = new HashSet<>(
								Arrays.asList(uses.split("\\s*,\\s*")));
						if (packages.contains("javax.persistence")
								&& packages.contains("org.osgi.service.jpa")) {
							found = true;
							break outer;
						}
					}
				}
			}
		}

		assertThat(found).as("No suitable extender capability found").isTrue();
	}

	/**
	 * See JPA Service Spec 127.7.2
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testContractCapability() throws Exception {

		boolean found = false;

		outer: for (Bundle b : context.getBundles()) {

			BundleWiring wiring = b.adapt(BundleWiring.class);

			if (wiring == null)
				continue;

			for (BundleCapability bc : wiring
					.getCapabilities(CONTRACT_NAMESPACE)) {
				Map<String,Object> attributes = bc.getAttributes();
				if ("JavaJPA".equals(attributes.get(CONTRACT_NAMESPACE))) {
					Object o = attributes.get(
							ContractNamespace.CAPABILITY_VERSION_ATTRIBUTE);

					Version[] versions;
					if (o instanceof Version) {
						versions = new Version[] {
								(Version) o
						};
					} else if (o instanceof Version[]) {
						versions = (Version[]) o;
					} else if (o instanceof List) {
						versions = ((List<Version>) o).toArray(new Version[0]);
					} else {
						versions = new Version[0];
					}

					for (Version v : versions) {
						if (parseVersion("2.1").equals(v)) {
							String uses = bc.getDirectives()
									.get(CAPABILITY_USES_DIRECTIVE);

							if (uses != null) {
								Set<String> packages = new HashSet<>(
										Arrays.asList(uses.split("\\s*,\\s*")));
								if (packages.contains("javax.persistence")
										&& packages.contains(
												"javax.persistence.criteria")
										&& packages.contains(
												"javax.persistence.metamodel")
										&& packages.contains(
												"javax.persistence.spi")) {
									found = true;
									break outer;
								}
							}
						}
					}
				}
			}
		}

		assertThat(found).as("No contract capability found").isTrue();
	}

	/**
	 * See JPA Service Spec 127.7.1
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRequireContractCapability() throws Exception {
		Bundle ctExtenderCap = null;
		Bundle persistenceBundle = null;
		try {
			ctExtenderCap = installer.installBundle(context.getBundle().getEntry("ctExtenderCapProvidingBundle.jar"), true);
			persistenceBundle = installer.installBundle(context.getBundle().getEntry("extenderCapRequiringBundle.jar"), true);
			waitForService(EntityManagerFactoryBuilder.class, true);
		} finally {
			if (persistenceBundle != null) {
				persistenceBundle.uninstall();
			}
			if (ctExtenderCap != null) {
				ctExtenderCap.uninstall();
			}
		}
	}

	/**
	 * See JPA Service Spec 127.7.1
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRequireDifferentContractCapability() throws Exception {
		Bundle ctExtenderCap = null;
		Bundle persistenceBundle = null;
		try {
			ctExtenderCap = installer.installBundle(context.getBundle().getEntry("ctExtenderCapProvidingBundle.jar"), true);
			persistenceBundle = installer.installBundle(context.getBundle().getEntry("ctExtenderCapRequiringBundle.jar"), true);
			waitForService(EntityManagerFactoryBuilder.class, false);
		} finally {
			if (persistenceBundle != null) {
				persistenceBundle.uninstall();
			}
			if (ctExtenderCap != null) {
				ctExtenderCap.uninstall();
			}
		}
	}

	public <T> void waitForService(Class<T> cls, boolean expected) {
		ServiceTracker<T,T> tracker = new ServiceTracker<>(context,
				cls.getName(), null);
		tracker.open();
		Object service = null;
		try {
			service = tracker.waitForService(SERVICE_WAIT_TIME);
		} catch (InterruptedException intEx) {
			// service will be null
		}
		tracker.close();

		if (expected) {
			assertThat(service).as("Service for " + cls.getName()
					+ " was not registered after waiting " + SERVICE_WAIT_TIME
					+ " milliseconds").isNotNull();
		} else {
			assertThat(service).as("Service for " + cls.getName()
							+ " was registered despite not being expected").isNull();
		}
	}
}

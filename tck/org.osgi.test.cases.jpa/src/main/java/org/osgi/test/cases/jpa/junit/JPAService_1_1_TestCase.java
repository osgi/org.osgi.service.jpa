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
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @version $Rev$ $Date$
 */
public class JPAService_1_1_TestCase extends JPATestSupport {



	public static final long SERVICE_WAIT_TIME = 5000;

	/**
	 * See JPA Service Spec 127.3.4.1
	 * <p>
	 * Tests that the EntityManagerFactoryBuilder supports the standard JPA
	 * property {@code javax.persistence.dataSource} as defined in section 9.7
	 * of the JPA 2.1 specification. The value of this property is an instance
	 * of {@link javax.sql.DataSource} (not a string). The JPA Service
	 * implementation must handle this property and use the provided DataSource
	 * to configure the persistence unit, even if the underlying Persistence
	 * Provider does not natively support it as a map entry.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUseStandardDataSourceConfig() throws Exception {

		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBuilderBundle.jar"), true);
		EntityManagerFactoryBuilder emfBuilder = null;
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactoryBuilder.class, true);
			emfBuilder = getService(EntityManagerFactoryBuilder.class,
					"(osgi.unit.name=emfBuilderTestUnit)");
			assertThat(emfBuilder).as("Unable to retrieve the specified EntityManagerFactoryBuilder").isNotNull();

			DataSourceFactory dsf = getService(DataSourceFactory.class);

			Properties props = new Properties();
			props.put(DataSourceFactory.JDBC_URL, "jdbc:h2:mem:");
			DataSource ds = dsf.createDataSource(props);

			Map<String,Object> emfProps = new HashMap<>();
			emfProps.put("javax.persistence.dataSource", ds);
			emf = emfBuilder.createEntityManagerFactory(emfProps);

			EntityManager em = emf.createEntityManager();
			try {
				assertThat(em
						.createNativeQuery("SELECT X FROM SYSTEM_RANGE(1, 1);")
						.getSingleResult()).hasToString("1");
			} finally {
				em.close();
			}
		} catch (java.lang.IllegalArgumentException ex) {
			fail("Unknown properties should be ignored and not result in an IllegalArgumentException.");
		} finally {
			if (emf != null) {
				emf.close();
			}
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			persistenceBundle.uninstall();
		}
	}

	/**
	 * See JPA Service Spec 127.4.7
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateEMFProperties() throws Exception {
		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBuilderBundle.jar"), true);
		EntityManagerFactoryBuilder emfBuilder = null;
		try {
			waitForService(EntityManagerFactoryBuilder.class, true);
			emfBuilder = getService(EntityManagerFactoryBuilder.class,
					"(osgi.unit.name=emfBuilderTestUnit)");
			assertThat(emfBuilder).as("Unable to retrieve the specified EntityManagerFactoryBuilder").isNotNull();

			assertThat(context
							.getServiceReference(EntityManagerFactory.class)).as("There should not be an EntityManagerFactory").isNull();

			Map<String,Object> props = new HashMap<>();
			props.put("javax.persistence.jdbc.driver", "org.h2.Driver");
			props.put("say.hello", "Hello!");
			props.put("javax.persistence.jdbc.password", "Secret");

			emfBuilder.createEntityManagerFactory(props);

			waitForService(EntityManagerFactory.class, true);
			EntityManagerFactory emfService = getService(
					EntityManagerFactory.class);

			ServiceReference< ? > serviceRef = getServiceReference(emfService);
			assertThat(serviceRef.getProperty("say.hello")).isEqualTo("Hello!");
			assertThat(Arrays.asList(serviceRef.getPropertyKeys())
					.contains("javax.persistence.jdbc.password")).isFalse();

			props.put("say.hello", "overridden");
			emfBuilder.createEntityManagerFactory(props);

			assertThat(serviceRef.getBundle()).isNull();
			ungetService(emfService);

			emfService = getService(EntityManagerFactory.class);
			serviceRef = getServiceReference(emfService);
			assertThat(serviceRef.getProperty("say.hello")).isEqualTo("overridden");

		} catch (java.lang.IllegalArgumentException ex) {
			fail("Unknown properties should be ignored and not result in an IllegalArgumentException.");
		} finally {
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			persistenceBundle.uninstall();
		}
	}
	
	/**
	 * See JPA Service Spec 127.3.4.1
	 * 
	 * @throws Exception
	 */
	@Test
	public void testClosingEntityManagerFactoryService() throws Exception {

		// Install the bundles necessary for this test
				Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBuilderBundle.jar"), true);
				EntityManagerFactoryBuilder emfBuilder = null;
				try {
					waitForService(EntityManagerFactoryBuilder.class, true);
					emfBuilder = getService(EntityManagerFactoryBuilder.class,
							"(osgi.unit.name=emfBuilderTestUnit)");
					assertThat(emfBuilder).as("Unable to retrieve the specified EntityManagerFactoryBuilder").isNotNull();

					assertThat(context
									.getServiceReference(EntityManagerFactory.class)).as("There should not be an EntityManagerFactory").isNull();

					Map<String,Object> props = new HashMap<>();
					props.put("javax.persistence.jdbc.driver", "org.h2.Driver");

					EntityManagerFactory emfFromBuilder = emfBuilder.createEntityManagerFactory(props);

					waitForService(EntityManagerFactory.class, true);
					EntityManagerFactory emfService = getService(
							EntityManagerFactory.class);

					// Test the service is not closeable
					emfService.close();
					
					assertThat(emfService.isOpen()).isTrue();
					assertThat(emfFromBuilder.isOpen()).isTrue();
					
					ServiceReference< ? > serviceRef = getServiceReference(emfService);

					ServiceReference< ? >[] serviceReferences = context.getServiceReferences(
							EntityManagerFactory.class.getName(), 
							"(service.id=" + serviceRef.getProperty("service.id")  + ")");

			assertThat(serviceReferences).isNotNull();
					assertThat(serviceReferences).hasSize(1);
					assertThat(serviceReferences[0]).isEqualTo(serviceRef);
					
			// Now close the builder
			emfFromBuilder.close();

			assertThat(emfService.isOpen()).isFalse();
			assertThat(emfFromBuilder.isOpen()).isFalse();

			assertThat(serviceAvailable(EntityManagerFactory.class)).isFalse();

		} catch (Exception ex) {
			fail("An exception occurredException.", ex);
				} finally {
					if (emfBuilder != null) {
						ungetService(emfBuilder);
					}
					persistenceBundle.uninstall();
				}
	}

	/**
	 * See JPA Service Spec 127.3.4
	 * 
	 * @throws Exception
	 */
	@Test
	public void testReportsCorrectPersistenceProviderDetails()
			throws Exception {

		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBuilderBundle.jar"), true);
		EntityManagerFactoryBuilder emfBuilder = null;
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactoryBuilder.class, true);
			emfBuilder = getService(EntityManagerFactoryBuilder.class,
					"(osgi.unit.name=emfBuilderTestUnit)");
			assertThat(emfBuilder).as("Unable to retrieve the specified EntityManagerFactoryBuilder").isNotNull();

			String provider = String
					.valueOf(getServiceReference(emfBuilder).getProperty(
							EntityManagerFactoryBuilder.JPA_UNIT_PROVIDER));

			assertThat(emfBuilder.getPersistenceProviderName()).isEqualTo(provider);

			Bundle providerBundle = emfBuilder.getPersistenceProviderBundle();
			assertThat(providerBundle).as("EntityManagerFactoryBuilder returned null for PersistenceProviderBundle").isNotNull();

			boolean found = false;
			for (ServiceReference< ? > ref : providerBundle
					.getRegisteredServices()) {
				if (provider.equals(
						ref.getProperty("javax.persistence.provider"))) {
					found = true;
					break;
				}
			}

			if (!found) {
				try {
					providerBundle.loadClass(provider);
					found = true;
				} catch (ClassNotFoundException cnfe) {}
			}

			assertThat(found).as("The provider bundle returned by the EntityManagerFactoryBuilder did not advertise a PersistenceProvider service, nor was it able to load the provider class").isTrue();

		} catch (java.lang.IllegalArgumentException ex) {
			fail("Unknown properties should be ignored and not result in an IllegalArgumentException.");
		} finally {
			if (emf != null) {
				emf.close();
			}
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			persistenceBundle.uninstall();
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

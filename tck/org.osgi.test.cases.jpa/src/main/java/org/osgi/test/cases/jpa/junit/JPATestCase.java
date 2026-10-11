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
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.spi.PersistenceProvider;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.jpa.EntityManagerFactoryBuilder;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 *
 * @version $Rev$ $Date$
 */
public class JPATestCase extends JPATestSupport {



    public static final long SERVICE_WAIT_TIME = 5000;

	@Test
	public void testPersistenceClass() throws Exception {
		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("staticAccessBundle.jar"), true);
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactory.class);
			emf = Persistence.createEntityManagerFactory("staticAccessTestUnit");
			assertThat(emf).as("Unable to create the specified EntityManagerFactory").isNotNull();
		} finally {
			if (emf != null) {
				emf.close();
			}
			persistenceBundle.uninstall();
		}

	}

	@Test
	public void testPersistenceClassWithMap() throws Exception {
		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("staticAccessWithMapBundle.jar"), true);
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactoryBuilder.class);
			DataSourceFactory dsf = getService(DataSourceFactory.class);
			ServiceReference< ? > dsfRef = getServiceReference(dsf);
			assertThat(dsfRef).as("Unable to retrieve a reference for the DataSourceFactory service").isNotNull();
			Map<String,Object> props = new HashMap<>();
			props.put("javax.persistence.jdbc.driver", dsfRef.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS));
			emf = Persistence.createEntityManagerFactory("staticAccessWithMapTestUnit", props);
			assertThat(emf).as("Unable to create the specified EntityManagerFactory").isNotNull();
		} finally {
			if (emf != null) {
				emf.close();
			}
			persistenceBundle.uninstall();
		}
	}

	@Test
	public void testEntityManagerFactory() throws Exception {
		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBundle.jar"), true);
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactory.class);
			emf = getService(EntityManagerFactory.class, "(osgi.unit.name=emfTestUnit)");
			assertThat(emf).as("Unable to retrieve the specified EntityManagerFactory").isNotNull();
		} finally {
			if (emf != null) {
				emf.close();
				ungetService(emf);
			}
			persistenceBundle.uninstall();
		}

	}

	@Test
	public void testEntityManagerFactoryWithIncompletePersistenceUnit() throws Exception {
		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("incompletePersistenceUnitBundle.jar"), true);
		try {
			waitForService(EntityManagerFactoryBuilder.class);
			ServiceReference< ? >[] emfRefs = context.getServiceReferences(
					EntityManagerFactory.class.getName(),
					"(osgi.unit.name=incompleteTestUnit)");
			assertThat(emfRefs).as("There should be no EntityManagerFactory registered since this persistence unit is incomplete").isNull();
		} finally {
			persistenceBundle.uninstall();
		}
	}

	@Test
	public void testEntityManagerFactoryBuilder() throws Exception {
		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBuilderBundle.jar"), true);
		EntityManagerFactoryBuilder emfBuilder = null;
		@SuppressWarnings("unused")
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactoryBuilder.class);
			emfBuilder = getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=emfBuilderTestUnit)");
			assertThat(emfBuilder).as("Unable to retrieve the specified EntityManagerFactoryBuilder").isNotNull();
			DataSourceFactory dsf = getService(DataSourceFactory.class);
			ServiceReference< ? > dsfRef = getServiceReference(dsf);
			assertThat(dsfRef).as("Unable to retrieve a reference for the DataSourceFactory service").isNotNull();
			Map<String,Object> props = new HashMap<>();
			props.put("javax.persistence.jdbc.driver", dsfRef.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS));
			props.put("fake.property", "fake property value");
			emf = emfBuilder.createEntityManagerFactory(props);
		} catch (java.lang.IllegalArgumentException ex) {
			fail("Unknown properties should be ignored and not result in an IllegalArgumentException.");
		} finally {
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			persistenceBundle.uninstall();
		}
	}

	@Test
	public void testEntityManagerFactoryBuilderRebinding() throws Exception {
		// Install the bundle necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBuilderRebindingBundle.jar"), true);
		EntityManagerFactoryBuilder emfBuilder = null;
		@SuppressWarnings("unused")
		EntityManagerFactory emf1 = null;
		@SuppressWarnings("unused")
		EntityManagerFactory emf2 = null;
		try {
			waitForService(EntityManagerFactoryBuilder.class);
			emfBuilder = getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=emfBuilderRebindingTestUnit)");
			assertThat(emfBuilder).as("Unable to retrieve the specified EntityManagerFactroyBuilder").isNotNull();
			DataSourceFactory dsf = getService(DataSourceFactory.class);
			ServiceReference< ? > dsfRef = getServiceReference(dsf);
			assertThat(dsfRef).as("Unable to retrieve a reference for the DataSourceFactory service").isNotNull();
			Map<String,Object> props1 = new HashMap<>();
			props1.put("javax.persistence.jdbc.driver", dsfRef.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS));
			emf1 = emfBuilder.createEntityManagerFactory(props1);
			Map<String,Object> props2 = new HashMap<>();
			props2.put("javax.persistence.jdbc.driver", "fake.driver.class");
			emf2 = emfBuilder.createEntityManagerFactory(props2);
		} catch (java.lang.IllegalArgumentException ex) {
			System.out.println("java.lang.IllegalArgumentException caught in testEntityManagerFactoryBuilderRebinding: SUCCESS");
			return;
		} finally {
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			persistenceBundle.uninstall();
		}
		failException("testEntityManagerFactoryBuilderRebinding failed", java.lang.IllegalArgumentException.class);
	}

	@Test
	public void testEntityManagerFactoryRebindingWithBuilder() throws Exception {
		// Install the bundle necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfRebindingWithBuilderBundle.jar"), true);
		EntityManagerFactoryBuilder emfBuilder = null;
		EntityManagerFactory emf1 = null;
		EntityManagerFactory emf2 = null;
		try {
			waitForService(EntityManagerFactory.class);
			waitForService(EntityManagerFactoryBuilder.class);
			emf1 = getService(EntityManagerFactory.class, "(osgi.unit.name=emfRebindingWithBuilderTestUnit)");
			assertThat(emf1).as("Unable to retrieve the specified EntityManagerFactory").isNotNull();
			assertThat(emf1.isOpen()).isTrue();

			emfBuilder = getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=emfRebindingWithBuilderTestUnit)");
			Map<String,Object> props = new HashMap<>();
			props.put("javax.persistence.jdbc.driver", "fake.driver.class");
			emf2 = emfBuilder.createEntityManagerFactory(props);
		} catch (java.lang.IllegalArgumentException ex) {
			System.out.println("java.lang.IllegalArgumentException caught in testEntityManagerFactoryRebindingWithBuilder: SUCCESS");
			return;
		} finally {
			if (emf1 != null) {
				emf1.close();
				ungetService(emf1);
			}
			if (emf2 != null) {
				emf2.close();
			}
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			persistenceBundle.uninstall();
		}
		failException("testEntityManagerFactoryRebindingWithBuilder failed", java.lang.IllegalArgumentException.class);

	}

	@Test
	public void testConfigPropertiesWithEntityManagerFactoryBuilder() throws Exception {
		// Install the bundle necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("configPropertiesBundle.jar"), true);
		EntityManagerFactoryBuilder emfBuilder = null;
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactoryBuilder.class);
			emfBuilder = getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=configPropertiesTestUnit)");
			assertThat(emfBuilder).as("Unable to retrieve the specified EntityManagerFactoryBuilder.").isNotNull();
			DataSourceFactory dsf = getService(DataSourceFactory.class);
			ServiceReference< ? > dsfRef = getServiceReference(dsf);
			assertThat(dsfRef).as("Unable to retrieve a reference for the DataSourceFactory service").isNotNull();
			Map<String,Object> props = new HashMap<>();
			props.put("javax.persistence.jdbc.driver", dsfRef.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_CLASS));
			props.put("javax.persistence.jdbc.password", "configPassword");
			emf = emfBuilder.createEntityManagerFactory(props);
		} finally {
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			if (emf != null) {
				emf.close();
			}
			persistenceBundle.uninstall();
		}
	}


	@Test
	public void testDataSourceFactoryUnregistration() throws Exception {
		// Install the bundle needed for this test.
		Bundle emfBundle = installer.installBundle(context.getBundle().getEntry("dsfEMFBundle.jar"), true);
		Bundle dsfBundle = null;
		EntityManagerFactoryBuilder emfBuilder = null;
		try {
			waitForService(EntityManagerFactory.class);
			DataSourceFactory dsf = getService(DataSourceFactory.class);
			ServiceReference< ? > dsfRef = getServiceReference(dsf);
			ungetService(dsf);
			dsfBundle = dsfRef.getBundle();
			dsfBundle.stop();
			// Make sure the entityManagerFactory is no longer available.
			ServiceReference< ? >[] emfRef = context.getServiceReferences(
					EntityManagerFactory.class.getName(),
					"(osgi.unit.name=dsfEMFTestUnit)");
			assertThat(emfRef).as("There should be no entityManagerFactory service registered for this persistence unit").isNull();
			// The emfBuilder service should not have been unregistered as its lifecycle is supposed to be independent of the dsf
			emfBuilder = getService(EntityManagerFactoryBuilder.class, "(osgi.unit.name=dsfEMFTestUnit)");
			assertThat(emfBuilder).isNotNull();
		} finally {
			if (emfBuilder != null) {
				ungetService(emfBuilder);
			}
			dsfBundle.start();
			emfBundle.uninstall();
		}
	}

	@Test
	public void testPersistenceBundleStopping() throws Exception {
		// Install the bundles necessary for this test
		Bundle persistenceBundle = installer.installBundle(context.getBundle().getEntry("emfBundle.jar"), true);
		EntityManagerFactory emf = null;
		try {
			waitForService(EntityManagerFactory.class);
			emf = getService(EntityManagerFactory.class, "(osgi.unit.name=emfTestUnit)");
			assertThat(emf).as("Unable to retrieve the specified EntityManagerFactory").isNotNull();
			persistenceBundle.uninstall();
			try {
				if (emf.isOpen()) {
					fail("The EntityManagerFactory should have been closed when the persistence bundle was uninstalled");
				}
			} catch (NullPointerException npe) {
				// Do nothing.  An NPE is expected if the bundle performs all the appropriate steps when stopping.
			}
		} finally {
			if (persistenceBundle.getState() != Bundle.UNINSTALLED) {
				persistenceBundle.uninstall();
			}
		}
		// Reinstall the bundle and go through the motions again to make sure nothing that conflicts was left behind
		Bundle reinstalledBundle = installer.installBundle(context.getBundle().getEntry("emfBundle.jar"), true);
		emf = null;
		try {
			waitForService(EntityManagerFactory.class);
			emf = getService(EntityManagerFactory.class, "(osgi.unit.name=emfTestUnit)");
			assertThat(emf).as("Unable to retrieve the specified EntityManagerFactory").isNotNull();
		} finally {
			reinstalledBundle.uninstall();
		}

	}

	@Test
	public void testPersistenceProviderRegistration() throws Exception {
		// Registration of PersistenceProvider is optional per §127.7.1
		// (managed mode, "should" requirement). Skip if not registered.
		ServiceReference<PersistenceProvider> providerRef = context
				.getServiceReference(PersistenceProvider.class);
		assumeThat(providerRef).isNotNull();
		// If registered, the javax.persistence.provider property is
		// required per §127.7.1.
		String javaxPersistenceProvider = (String) providerRef
				.getProperty("javax.persistence.provider");
		assertThat(javaxPersistenceProvider).as("The javax.persistence.provider service property must be "
						+ "registered alongside the PersistenceProvider "
						+ "service (§127.7.1 Managed Model)").isNotNull();
	}

	public <S> S waitForService(Class<S> cls) {
		ServiceTracker<S,S> tracker = new ServiceTracker<>(context, cls,
				null);
        tracker.open();
		S service = null;
		try {
			service = tracker.waitForService(SERVICE_WAIT_TIME);
		}
		catch (InterruptedException intEx) {
			// service will be null
		}
        tracker.close();
        assertThat(service).as("Service for " + cls.getName() + " was not registered after waiting " +
            SERVICE_WAIT_TIME + " milliseconds").isNotNull();
		return service;
    }
}

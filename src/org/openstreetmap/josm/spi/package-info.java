// License: GPL. For details, see LICENSE file.

/**
 * This package contains services.
 * <ul>
 * <li>Abstract service interfaces and
 * <li>manager classes to install a service provider and give access to the
 * functionality of the service.
 * </ul>
 * Client code will only depend on the classes from the spi package, but not on
 * implementations of the service interface (service providers) which are found
 * elsewhere.
 * <p>
 * The concept is similar to Java Service Provider Interfaces (hence the name),
 * except the service providers are registered directly with a method call instead
 * of using {@link java.util.ServiceLoader} to discover the providers on classpath.
 */
package org.openstreetmap.josm.spi;

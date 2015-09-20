/*
 * Copyright © 1996-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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
 */

package com.globalmentor.marmot;

import java.net.URI;

import org.urframework.URFResource;

import com.globalmentor.config.Concerns;

import static org.urframework.URF.*;

/**
 * Constant values and utilities used by Marmot.
 * @author Garret Wilson
 */
public class Marmot {

	static {
		setDefaultConfiguration(new DefaultMarmotConfiguration()); //configure the default Marmot configuration to return a default Marmot resource cache
	}

	/**
	 * Returns the default Marmot configuration.
	 * <p>
	 * This method is the preferred approach for determining the default Marmot configuration, as it ensures a default configuration has been installed.
	 * </p>
	 * @return The default Marmot configuration.
	 * @see Concerns#getDefaultConcern(Class)
	 */
	public static MarmotConfiguration getDefaultConfiguration() {
		return Concerns.getDefaultConcern(MarmotConfiguration.class);
	}

	/**
	 * Sets the default Marmot configuration.
	 * @param configuration The configuration to set.
	 * @return The previous configuration, or <code>null</code> if there was no previous configuration.
	 * @throws NullPointerException if the given configuration is <code>null</code>.
	 * @see Concerns#registerDefaultConcern(Class)
	 */
	public static MarmotConfiguration setDefaultConfiguration(final MarmotConfiguration marmotConfiguration) {
		return Concerns.registerDefaultConcern(MarmotConfiguration.class, marmotConfiguration);
	}

	/**
	 * Returns the configured Marmot configuration for the current context.
	 * <p>
	 * This method is the preferred approach for determining the Marmot configuration, as it ensures a default configuration has been installed.
	 * </p>
	 * @return The configured Marmot configuration for the current context.
	 * @see Concerns#getConcern(Class)
	 */
	public static MarmotConfiguration getConfiguration() {
		return Concerns.getConcern(MarmotConfiguration.class);
	}

	/** @return The cache configured for use by Marmot for the appropriate configuration. */
	public static MarmotResourceCache<?> getResourceCache() {
		return getConfiguration().getResourceCache();
	}

	//predefined users
	/** The principal wildcard character, '*'. */
	//TODO del	public static final char WILDCARD_PRINCIPAL_CHAR='*';
	/** The predefined "all principals at localhost" <code>mailto</code> URI. */
	//TODO del	public static final URI ALL_LOCALHOST_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), LOCALHOST_DOMAIN);
	/** The predefined "all principals" <code>mailto</code> URI. */
	//TODO del	public static final URI ALL_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), String.valueOf(WILDCARD_PRINCIPAL_CHAR));

	/** An ID token to use for identifying the Marmot framework. */
	public static final String ID = "marmot";

	/**
	 * The URI to the Marmot namespace.
	 */
	public static final URI NAMESPACE_URI = URI.create("http://globalmentor.com/marmot/");

	//Marmot properties
	/** Provides an annotation of the resource. */
	public static final URI ANNOTATION_PROPERTY_URI = createResourceURI(NAMESPACE_URI, "annotation");

	/**
	 * Returns the annotations of the resource.
	 * @param resource The resource the property of which should be located.
	 * @return The annotation values of the property.
	 * @see #ANNOTATION_PROPERTY_URI
	 */
	public static Iterable<Annotation> getAnnotations(final URFResource resource) {
		return getAnnotations(resource, Annotation.class);
	}

	/**
	 * Returns the annotations of the resource.
	 * @param <A> The type of annotation to be returned.
	 * @param resource The resource the property of which should be located.
	 * @return The annotation values of the property.
	 * @see #ANNOTATION_PROPERTY_URI
	 */
	public static <A extends Annotation> Iterable<A> getAnnotations(final URFResource resource, final Class<A> annotationClass) {
		return resource.getPropertyValues(ANNOTATION_PROPERTY_URI, annotationClass);
	}

	/**
	 * Adds an annotation to the resource.
	 * @param resource The resource of which the property should be set.
	 * @param value The property value to set.
	 * @see #ANNOTATION_PROPERTY_URI
	 */
	public static void addAnnotation(final URFResource resource, final Annotation value) {
		resource.addPropertyValue(ANNOTATION_PROPERTY_URI, value);
	}

}

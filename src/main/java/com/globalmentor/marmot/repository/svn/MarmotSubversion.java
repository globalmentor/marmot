/*
 * Copyright © 2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.svn;

import static com.globalmentor.java.Characters.*;
import static com.globalmentor.marmot.repository.svn.Subversion.*;
import static com.globalmentor.net.URIs.*;

import java.net.URI;

import com.globalmentor.marmot.Marmot;
import com.globalmentor.net.URIs;

/**
 * Constants and utilities for working with Subversion on Marmot.
 * 
 * @author Garret Wilson
 * 
 */
public class MarmotSubversion {

	/** The obsolete prefix for Marmot properties: <code>marmot:</code>. */
	@Deprecated
	public static final String OBSOLETE_PROPERTY_PREFIX = Marmot.ID + PROPERTY_NAMESPACE_SEPARATOR;

	/** The prefix for Marmot properties: <code>marmot-</code>. */
	public static final String PROPERTY_PREFIX = Marmot.ID + HYPHEN_MINUS_CHAR;

	/**
	 * Determines the a property name to represent an URF property by encoded the URF property URI to be a simple local name. The name is created by
	 * simple-encoding the URI and using {@value Marmot#ID} as the namespace.
	 * @param propertyURI The URI of the URF property to represent.
	 * @return A property name to use in representing an URF property with the given URF property URI.
	 * @throws NullPointerException if the given property URI is <code>null</code>.
	 * @see #decodePropertyURIPropertyName(String)
	 * @see URIs#plainEncode(URI)
	 */
	public static String encodePropertyURIPropertyName(final URI propertyURI) {
		return PROPERTY_PREFIX + plainEncode(propertyURI);
	}

	/**
	 * Determines the URI of the URF property to represent the given Marmot property local name. The property must start with the {@value #PROPERTY_PREFIX}
	 * prefix.
	 * <p>
	 * This implementation for the time being also supports decoding obsolete property forms in the {@value Marmot#ID} namespace.
	 * </p>
	 * @param propertyName The name of the property, which is assumed to be in the {@value Marmot#ID} namespace.
	 * @return The URI of the URF property to represent the given property name.
	 * @throws NullPointerException if the given property name is <code>null</code>.
	 * @throws IllegalArgumentException if the given property name does not start with the {@value #PROPERTY_PREFIX} prefix or is not a valid plain-encoded URI.
	 * @see #PROPERTY_PREFIX
	 * @see #encodePropertyURIPropertyName(URI)
	 * @see URIs#plainDecode(CharSequence)
	 */
	public static URI decodePropertyURIPropertyName(final String propertyName) {
		if(Marmot.ID.equals(getPropertyNamespace(propertyName))) { //if this property is in the Marmot namespace TODO delete when legacy properties converted
			return plainDecode(getPropertyLocalName(propertyName)); //perform legacy decoding
		}
		if(propertyName.startsWith(PROPERTY_PREFIX)) { //if the property starts with "marmot-"
			return plainDecode(propertyName.substring(PROPERTY_PREFIX.length()));
		}
		throw new IllegalArgumentException("The property name " + propertyName + " does not represent a Marmot Subversion property.");
	}

}

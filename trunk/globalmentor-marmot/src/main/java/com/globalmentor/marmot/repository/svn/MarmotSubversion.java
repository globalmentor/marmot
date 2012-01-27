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

import static com.globalmentor.apache.subversion.Subversion.*;
import static com.globalmentor.java.Characters.*;
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
public class MarmotSubversion
{

	/** The obsolete property for synchronizing last-modified times. */
	@Deprecated
	public final static String OBSOLETE_SYNC_WEBDAV_GET_LAST_MODIFIED_PROPERTY_NAME = "syncWebDAVGetLastModified";

	/** The obsolete character used to escape URIs to encode them as property names in another namespace. */
	@Deprecated
	private final static char OBSOLETE_PROPERTY_NAME_URI_ESCAPE_CHAR = MIDDLE_DOT_CHAR;

	/**
	 * Decodes a property URI from an obsolete property name encoding
	 * @param propertyName The name of the Subversion property.
	 * @return The URI of the URF property to represent the given property local name.
	 * @throws IllegalArgumentException if the given local name has no valid absolute URF property URI encoded in it.
	 */
	@Deprecated
	public static URI decodeObsoletePropertyURILocalName(final String propertyName)
	{
		final String urfPRopertyURI = decode(propertyName, OBSOLETE_PROPERTY_NAME_URI_ESCAPE_CHAR); //the URF property URI may be encoded as the local name of the custom property
		return checkAbsolute(URI.create(urfPRopertyURI)); //create an URF property URI from the decoded local name and make sure it is absolute
	}

	/**
	 * Determines the a property name to represent an URF property by encoded the URF property URI to be a simple local name. The name is created by
	 * simple-encoding the URI and using {@value Marmot#ID} as the namespace.
	 * @param propertyURI The URI of the URF property to represent.
	 * @return A property name to use in representing an URF property with the given URF property URI.
	 * @throws NullPointerException if the given property URI is <code>null</code>.
	 * @see #decodePropertyURIPropertyName(String)
	 * @see URIs#plainEncode(URI)
	 */
	public static String encodePropertyURIPropertyName(final URI propertyURI)
	{
		return Marmot.ID + PROPERTY_NAMESPACE_SEPARATOR + plainEncode(propertyURI);
	}

	/**
	 * Determines the URI of the URF property to represent the given Marmot property local name. The property is assumed to be in the {@value Marmot#ID}
	 * namespace.
	 * <p>
	 * This implementation for the time being also supports decoding obsolete property forms if the property is not in the {@value Marmot#ID} namespace.
	 * </p>
	 * @param propertyName The name of the property, which is assumed to be in the {@value Marmot#ID} namespace.
	 * @return The URI of the URF property to represent the given property name.
	 * @throws NullPointerException if the given property name is <code>null</code>.
	 * @throws IllegalArgumentException if the given property name is not in the {@value Marmot#ID} namespace or is not a valid plain-encoded URI.
	 * @see #encodePropertyURIPropertyName(URI)
	 * @see URIs#plainDecode(String)
	 * @see #decodeObsoletePropertyURILocalName(String)
	 */
	public static URI decodePropertyURIPropertyName(final String propertyName)
	{
		if(!Marmot.ID.equals(getPropertyNamespace(propertyName))) //if this property is not in the Marmot namespace
		{
			return decodeObsoletePropertyURILocalName(propertyName); //perform legacy decoding
		}
		return plainDecode(getPropertyLocalName(propertyName));
	}

}

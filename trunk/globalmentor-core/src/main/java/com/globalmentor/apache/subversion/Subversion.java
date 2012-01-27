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

package com.globalmentor.apache.subversion;

import java.util.regex.Pattern;

/**
 * Constants and utilities for working with Apache Subversion.
 * 
 * @author Garret Wilson
 * 
 * @see <a href="http://subversion.apache.org/">Apache Subversion</a>
 */
public class Subversion
{

	/** The character used for separating the namespace ID from the rest of a Subversion property name. */
	public final static char PROPERTY_NAMESPACE_SEPARATOR = ':';

	/**
	 * The pattern describing a valid Subversion property name as per <code>svn_prop_name_is_valid()</code>: (alpha/colon/underscore) character followed by any
	 * number of (alpha/digit/minus/dot/colon/underscore) characters.
	 */
	public final Pattern PROPERTY_NAME_PATTERN = Pattern.compile("^[\\p{Alpha}:_][\\p{Alpha}\\p{Digit}\\-.:_]*$");

	/**
	 * Determines the namespace of the Subversion property. The namespace is the part before the namespace separator, if any.
	 * @param propertyName The full property name.
	 * @return The namespace of the Subversion property, or <code>null</code> if no namespace is present.
	 * @throws NullPointerException if the given property name is <code>null</code>.
	 */
	public static String getPropertyNamespace(final String propertyName)
	{
		final int namespaceSeparatorIndex = propertyName.indexOf(PROPERTY_NAMESPACE_SEPARATOR);
		return namespaceSeparatorIndex >= 0 ? propertyName.substring(0, namespaceSeparatorIndex) : null;
	}

	/**
	 * Determines the local name of the Subversion property. The local name is the part after the namespace, or the entire property name if no namespace is
	 * present.
	 * @param propertyName The full property name.
	 * @return The local name of the Subversion property.
	 * @throws NullPointerException if the given property name is <code>null</code>.
	 */
	public static String getPropertyLocalName(final String propertyName)
	{
		final int namespaceSeparatorIndex = propertyName.indexOf(PROPERTY_NAMESPACE_SEPARATOR);
		return namespaceSeparatorIndex >= 0 ? propertyName.substring(namespaceSeparatorIndex + 1) : propertyName;
	}
}

/*
 * Copyright © 1996-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.security;

import java.net.URI;

import org.urframework.select.*;

import static com.globalmentor.java.Objects.*;
import static com.globalmentor.marmot.security.MarmotSecurity.*;
import static org.urframework.select.Select.*;

/**
 * Specifies a rule for access to a resource.
 * @author Garret Wilson
 */
public class AccessRule extends AbstractMarmotSecurityResource {

	/** Default constructor. */
	public AccessRule() {
		this(null); //construct the class with no reference URI
	}

	/**
	 * Reference URI constructor.
	 * @param referenceURI The reference URI for the new resource.
	 */
	public AccessRule(final URI referenceURI) {
		super(referenceURI); //construct the parent class
	}

	/**
	 * Returns this rule's selector, or <code>null</code> if this rule has no selector property or the value is not a {@link Selector}.
	 * @return This rule's selector, or <code>null</code> if this rule has no selector property or the value is not a {@link Selector}.
	 * @see Select#SELECTOR_PROPERTY_URI
	 */
	public Selector getSelector() {
		return asInstance(getPropertyValue(SELECTOR_PROPERTY_URI), Selector.class); //return the select.selector value
	}

	/**
	 * Sets this rule's selector.
	 * @param selector This rule's selector, or <code>null</code> if this rule should have no selector property.
	 */
	public void setSelector(final Selector selector) {
		setPropertyValue(SELECTOR_PROPERTY_URI, selector); //set the select.select property
	}

	/**
	 * Returns this rule's access level, or <code>null</code> if this rule has no access level property or the value is not a {@link AccessLevel}.
	 * @return This rule's access level, or <code>null</code> if this rule has no access level property or the value is not a {@link AccessLevel}.
	 * @see MarmotSecurity#ACCESS_LEVEL_PROPERTY_URI
	 */
	public AccessLevel getAccessLevel() {
		return asInstance(getPropertyValue(ACCESS_LEVEL_PROPERTY_URI), AccessLevel.class); //return the security.accessLevel value
	}

	/**
	 * Sets this rule's access level.
	 * @param accessLevel This rule's access level, or <code>null</code> if this rule should have no access level property.
	 * @see MarmotSecurity#ACCESS_LEVEL_PROPERTY_URI
	 */
	public void setAccessLevel(final AccessLevel accessLevel) {
		setPropertyValue(ACCESS_LEVEL_PROPERTY_URI, accessLevel); //set the security.accessLevel property
	}

	/**
	 * Sets this rule's access level based upon an access level type.
	 * @param accessLevelType This rule's access level, or <code>null</code> if this rule should have no <code>marmot:accessLevel</code> property.
	 * @return The access level value used.
	 */
	public AccessLevel setAccessLevel(final AccessLevelType accessLevelType) {
		final AccessLevel accessLevel = accessLevelType != null ? createAccessLevel(accessLevelType) : null; //create the appropriate type of access level
		setAccessLevel(accessLevel); //set the access level
		return accessLevel; //return the access level we used
	}

	/**
	 * Creates an access level based upon an access level type.
	 * @param accessLevelType The access level type for which an access level should be created.
	 * @return A new access level for the given access level type.
	 * @throws NullPointerException if the given access level type is <code>null</code>.
	 */
	public static AccessLevel createAccessLevel(final AccessLevelType accessLevelType) {
		switch(accessLevelType) { //see which access level type this is, and return the correct access level instance
			case INHERITED:
				return new InheritedAccessLevel();
			case CUSTOM:
				return new CustomAccessLevel();
			case PRIVATE:
				return new PrivateAccessLevel();
			case STEALTH:
				return new StealthAccessLevel();
			case PREVIEW:
				return new PreviewAccessLevel();
			case USE:
				return new UseAccessLevel();
			case RETRIEVE:
				return new RetrieveAccessLevel();
			case EDIT:
				return new EditAccessLevel();
			case FULL:
				return new FullAccessLevel();
			default:
				throw new AssertionError("Unrecognized access level type: " + accessLevelType);
		}
	}

}
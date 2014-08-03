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

package com.globalmentor.marmot;

import java.net.URI;
import java.util.Set;

import com.globalmentor.marmot.security.*;

import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**
 * An abstract implementation of an access level.
 * @author Garret Wilson
 */
public abstract class AbstractAccessLevel extends AbstractMarmotSecurityResource implements AccessLevel {

	/** The access level type this access level represents. */
	private final AccessLevelType accessLevelType;

	/** The access level type this access level represents. */
	public AccessLevelType getAccessLevelType() {
		return accessLevelType;
	}

	/** Default constructor. */
	public AbstractAccessLevel() {
		this(null); //construct the class with no URI
	}

	/**
	 * URI constructor.
	 * @param uri The URI for the new resource.
	 * @throws IllegalArgumentException if this class does not correspond to an existing {@link AccessLevelType}.
	 */
	public AbstractAccessLevel(final URI uri) {
		super(uri); //construct the parent class
		this.accessLevelType = AccessLevelType.getAccessLevelType(getTypeURI()); //determine the access level type
	}

	/**
	 * Returns this access level's allowed permissions.
	 * @return This access level's allowed permissions.
	 * @see MarmotSecurity#ALLOW_PROPERTY_URI
	 */
	public Iterable<Permission> getAllows() {
		return getPropertyValues(ALLOW_PROPERTY_URI, Permission.class); //return the allow values
	}

	/**
	 * Adds a particular permission as allowed.
	 * @param permission The permission to be allowed.
	 * @see MarmotSecurity#ALLOW_PROPERTY_URI
	 */
	public void addAllow(final Permission permission) {
		addPropertyValue(ALLOW_PROPERTY_URI, permission); //add the allow property value
	}

	/**
	 * Adds a particular permission as allowed.
	 * @param permissionType The type of permission to be allowed.
	 * @see MarmotSecurity#ALLOW_PROPERTY_URI
	 */
	public void addAllow(final PermissionType permissionType) {
		addAllow(createPermission(permissionType)); //create a permission and allow it
	}

	/**
	 * Sets the allowed permissions to those specified.
	 * @param permissions The permissions that should be allowed.
	 * @see MarmotSecurity#ALLOW_PROPERTY_URI
	 */
	public void setAllowedPermissions(final Set<Permission> permissions) {
		removePropertyValues(ALLOW_PROPERTY_URI); //remove all the allow properties
		for(final Permission permission : permissions) { //for each permission
			addAllow(permission); //allow this permission
		}
	}

	/**
	 * Sets the allowed permissions to those specified.
	 * @param permissionTypes The permission types that should be allowed.
	 * @see MarmotSecurity#ALLOW_PROPERTY_URI
	 */
	public void setAllowedPermissionTypes(final Set<PermissionType> permissionTypes) {
		removePropertyValues(ALLOW_PROPERTY_URI); //remove all the allow properties
		for(final PermissionType permissionType : permissionTypes) { //for each permission
			addAllow(createPermission(permissionType)); //create a permission and allow it
		}
	}

	/**
	 * Returns this access level's denied permissions.
	 * @return This access level's denied permissions.
	 * @see MarmotSecurity#DENY_PROPERTY_URI
	 */
	public Iterable<Permission> getDenies() {
		return getPropertyValues(DENY_PROPERTY_URI, Permission.class); //return the deny values
	}

	/**
	 * Adds a particular permission as denied.
	 * @param permission The permission to be denied.
	 * @see MarmotSecurity#DENY_PROPERTY_URI
	 */
	public void addDeny(final Permission permission) {
		addPropertyValue(DENY_PROPERTY_URI, permission); //add the deny property value
	}

	/**
	 * Adds a particular permission as denied.
	 * @param permissionType The type of permission to be denied.
	 * @see MarmotSecurity#DENY_PROPERTY_URI
	 */
	public void addDeny(final PermissionType permissionType) {
		addDeny(createPermission(permissionType)); //create a permission and deny it
	}

	/**
	 * Sets the denied permissions to those specified.
	 * @param permissions The permissions that should be denied.
	 * @see MarmotSecurity#DENY_PROPERTY_URI
	 */
	public void setDeniedPermissions(final Set<Permission> permissions) {
		removePropertyValues(DENY_PROPERTY_URI); //remove all the deny properties
		for(final Permission permission : permissions) { //for each permission
			addDeny(permission); //deny this permission
		}
	}

	/**
	 * Sets the denied permissions to those specified.
	 * @param permissionTypes The permission types that should be denied.
	 * @see MarmotSecurity#DENY_PROPERTY_URI
	 */
	public void setDeniedPermissionTypes(final Set<PermissionType> permissionTypes) {
		removePropertyValues(DENY_PROPERTY_URI); //remove all the deny properties
		for(final PermissionType permissionType : permissionTypes) { //for each permission
			addDeny(createPermission(permissionType)); //create a permission and deny it
		}
	}

	/**
	 * Creates an permission based upon an permission type.
	 * @param permissionType The permission type for which an permission should be created.
	 * @return A new permission for the given access type.
	 * @throws NullPointerException if the given access type is <code>null</code>.
	 */
	public static Permission createPermission(final PermissionType permissionType) {
		switch(permissionType) { //see which permission type this is, and return the correct permission instance
			case DISCOVER:
				return new DiscoverPermission();
			case BROWSE:
				return new BrowsePermission();
			case ANNOTATE:
				return new AnnotatePermission();
			case PREVIEW:
				return new PreviewPermission();
			case UTILIZE:
				return new ExecutePermission();
			case READ:
				return new ReadPermission();
			case MODIFY_PROPERTIES:
				return new ModifyPropertiesPermission();
			case MODIFY_SECURITY:
				return new ModifySecurityPermission();
			case RENAME:
				return new RenamePermission();
			case ADD:
				return new AddPermission();
			case SUBTRACT:
				return new SubtractPermission();
			case DELETE:
				return new DeletePermission();
			default:
				throw new AssertionError("Unrecognized access level type: " + permissionType);
		}
	}

}
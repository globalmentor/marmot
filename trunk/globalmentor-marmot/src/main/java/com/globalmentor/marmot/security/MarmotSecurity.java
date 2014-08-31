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

import java.net.*;

import static org.urframework.URF.*;

/**
 * Constant values and utilities for Marmot security.
 * @author Garret Wilson
 */
public class MarmotSecurity {

	/** The URI to the Marmot security namespace. */
	public static final URI MARMOT_SECURITY_NAMESPACE_URI = URI.create("http://globalmentor.com/marmot/security/");

	//classes
	/** A rule specifying access permissions for zero or more principals. */
	public static final URI ACCESS_RULE_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "AccessRule");

	//properties
	/** Specifies the access rules and permissions. */
	public static final URI ACCESS_PROPERTY_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "access");
	/** Specifies the level of access. */
	public static final URI ACCESS_LEVEL_PROPERTY_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "accessLevel");
	/** Specifies the list of access rules. */
	public static final URI ACCESS_RULES_PROPERTY_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "accessRules");
	/** Specifies a principal by URI. Used in various contexts. */
	public static final URI PRINCIPAL_PROPERTY_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "principal");
	/** Specifies the rules list of an access specification. */
	public static final URI RULES_PROPERTY_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "rules");
	/** Specifies resource security. */
	//TODO del if not needed	public static final URI SECURITY_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "security");
	//access levels
	/** Predefined access level type specifying inherited access. */
	public static final URI INHERITED_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "InheritedAccessLevel");
	/** Predefined access level type allowing custom access. */
	public static final URI CUSTOM_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "CustomAccessLevel");
	/** Predefined access level type allowing no access. */
	public static final URI PRIVATE_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "PrivateAccessLevel");
	/** Predefined access level type allowing preview and utilize permissions without discovery. */
	public static final URI STEALTH_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "StealthAccessLevel");
	/** Predefined access level type allowing read and utilize permissions of only a subset of the resource contents. */
	public static final URI PREVIEW_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "PreviewAccessLevel");
	/** Predefined access level type preview and utilize permissions but not read permissions. */
	public static final URI USE_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "UseAccessLevel");
	/** Predefined access level type allowing discover and read permissions. */
	public static final URI RETRIEVE_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "RetrieveAccessLevel");
	/** Predefined access level type allowing discover, read, and write permissions. */
	public static final URI EDIT_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "EditAccessLevel");
	/** Predefined access level type allowing discover, read, write, and delete permissions. */
	public static final URI FULL_ACCESS_LEVEL_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "FullAccessLevel");
	//permission operators
	/** Allows a permission. */
	public static final URI ALLOW_PROPERTY_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "allow");
	/** Denies a permission. */
	public static final URI DENY_PROPERTY_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "deny");
	//permissions
	/** The principal may detect that the resource exists, such as when the resource is listed in the contents of a parent collection. */
	public static final URI DISCOVER_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "DiscoverPermission");
	/** The principal may view general information about the resource. */
	public static final URI BROWSE_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "BrowsePermission");
	/** The principal may add annotations to the resource. */
	public static final URI ANNOTATE_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "AnnotatePermission");
	/** The principal may add annotations to the resource.. */
	public static final URI PREVIEW_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "PreviewPermission");
	/** The principal may make use of the resource, such as executing the resource, but may not necessarily have access to the literal source contents. */
	public static final URI UTILIZE_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "UtilizePermission");
	/** The principal may read the literal contents of the resource, including child listings for collections. */
	public static final URI READ_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "ReadPermission");
	/** The principal may change, add, and remove resource properties. */
	public static final URI MODIFY_PROPERTIES_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "ModifyPropertiesPermission");
	/** The principal may change the permissions describing the security of the resource, including how other principals access the resource. */
	public static final URI MODIFY_SECURITY_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "ModifySecurityPermission");
	/** The principal may rename the resource. */
	public static final URI RENAME_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "RenamePermission");
	/** The principal may add to the contents of the resource, including adding children in collections, but may not be able to remove contents. */
	public static final URI ADD_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "AddPermission");
	/** The principal may subtract from the contents of the resource, including removing children in collections, but may not be able to add contents. */
	public static final URI SUBTRACT_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "SubtractPermission");
	/** The principal may remove the resource, including collections. */
	public static final URI DELETE_PERMISSION_CLASS_URI = createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "DeletePermission");

}

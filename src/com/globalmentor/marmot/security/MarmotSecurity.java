package com.globalmentor.marmot.security;

import java.net.*;

import static com.garretwilson.urf.URF.*;

/**Constant values and utilities for Marmot security.
@author Garret Wilson
*/
public class MarmotSecurity
{

	/**The URI to the Marmot security namespace.*/
	public final static URI MARMOT_SECURITY_NAMESPACE_URI=URI.create("http://globalmentor.com/marmot/security");

		//classes
	/**A rule specifying access permissions for zero or more principals.*/
	public final static URI ACCESS_RULE_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "AccessRule");

		//properties
	/**Specifies the access rules and permissions.*/
	public final static URI ACCESS_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "access");
	/**Specifies the level of access.*/
	public final static URI ACCESS_LEVEL_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "accessLevel");
	/**Specifies the list of access rules.*/
	public final static URI ACCESS_RULES_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "accessRules");
	/**Specifies a principal by URI. Used in various contexts.*/
	public final static URI PRINCIPAL_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "principal");
	/**Specifies the rules list of an access specification.*/
	public final static URI RULES_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "rules");
	/**Specifies resource security.*/
	public final static URI SECURITY_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "security");
				//access levels
	/**Predefined access level type specifying inherited access.*/
	public final static URI INHERITED_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "InheritedAccessLevel");
	/**Predefined access level type allowing custom access.*/
	public final static URI CUSTOM_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "CustomAccessLevel");
	/**Predefined access level type allowing no access.*/
	public final static URI PRIVATE_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "PrivateAccessLevel");
	/**Predefined access level type allowing preview and execute permissions without discovery.*/
	public final static URI STEALTH_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "StealthAccessLevel");
	/**Predefined access level type allowing read and execute permissions of only a subset of the resource contents.*/
	public final static URI PREVIEW_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "PreviewAccessLevel");
	/**Predefined access level type preview and execute permissions but not read permissions.*/
	public final static URI USE_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "UseAccessLevel");
	/**Predefined access level type allowing discover and read permissions.*/
	public final static URI RETRIEVE_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "RetrieveAccessLevel");
	/**Predefined access level type allowing discover, read, and write permissions.*/
	public final static URI EDIT_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "EditAccessLevel");
	/**Predefined access level type allowing discover, read, write, and delete permissions.*/
	public final static URI FULL_ACCESS_LEVEL_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "FullAccessLevel");
			//permission operators
	/**Allows a permission.*/
	public final static URI ALLOW_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "allow");
	/**Denies a permission.*/
	public final static URI DENY_PROPERTY_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "deny");
			//permissions
	/**The principal may detect that the resource exists, such as when the resource is listed in the contents of a parent collection.*/
	public final static URI DISCOVER_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "DiscoverPermission");
	/**The principal may view general information about the resource.*/
	public final static URI BROWSE_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "BrowsePermission");
	/**The principal may add annotations to the resource.*/
	public final static URI ANNOTATE_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "AnnotatePermission");
	/**The principal may add annotations to the resource..*/
	public final static URI PREVIEW_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "PreviewPermission");
	/**The principal may add annotations to the resource.*/
	public final static URI EXECUTE_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "ExecutePermission");
	/**The principal may read the literal contents of the resource, including child listings for collections.*/
	public final static URI READ_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "ReadPermission");
	/**The principal may change, add, and remove resource properties.*/
	public final static URI MODIFY_PROPERTIES_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "ModifyPropertiesPermission");
	/**The principal may change the permissions describing the security of the resource, including how other principals access the resource.*/
	public final static URI MODIFY_SECURITY_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "ModifySecurityPermission");
	/**The principal may rename the resource.*/
	public final static URI RENAME_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "RenamePermission");
	/**The principal may add to the contents of the resource, including adding children in collections, but may not be able to remove contents.*/
	public final static URI ADD_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "AddPermission");
	/**The principal may subtract from the contents of the resource, including removing children in collections, but may not be able to add contents.*/
	public final static URI SUBTRACT_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "SubtractPermission");
	/**The principal may remove the resource, including collections.*/
	public final static URI DELETE_PERMISSION_CLASS_URI=createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, "DeletePermission");

}

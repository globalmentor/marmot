package com.globalmentor.marmot;

import java.net.*;

import static com.garretwilson.net.URIConstants.*;
import static com.garretwilson.net.URIUtilities.*;

import static com.garretwilson.text.CharacterConstants.*;

/**Constant values used by Marmot.
@author Garret Wilson
*/
public class MarmotConstants
{

	/**The application URI.*/
	public final static URI MARMOT_URI=URI.create("urn:x-globalmentor:software:/marmot");

	/**The application title.*/
	public final static String MARMOT_TITLE="Marmot"+TRADE_MARK_SIGN_CHAR;

	/**The application copyright.*/
	public final static String MARMOT_COPYRIGHT="Copyright "+COPYRIGHT_SIGN+" 2003-2007 GlobalMentor, Inc. All Rights Reserved.";	//G**i18n

	/**The version of the application.*/
	public final static String VERSION="Beta Version 0.5 build 2007-04-02";

	/**The expiration date of the application.*/
//TODO del	public final static Calendar EXPIRATION=new GregorianCalendar(2006, GregorianCalendar.JANUARY, 1);

		//predefined users
	/**The principal wildcard character, '*'.*/
	public final static char WILDCARD_PRINCIPAL_CHAR='*';
	/**The predefined "all principals at localhost" <code>mailto</code> URI.*/
	public final static URI ALL_LOCALHOST_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), LOCALHOST_DOMAIN);
	/**The predefined "all principals" <code>mailto</code> URI.*/
	public final static URI ALL_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), String.valueOf(WILDCARD_PRINCIPAL_CHAR));

		//XML/RDF constants

	/**The recommended prefix to the Marmot  namespace.*/
	public final static String MARMOT_NAMESPACE_PREFIX="marmot";

	/**The URI to the Marmot namespace.*/
	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.com/namespaces/marmot#");
	
		//Marmot property names
	/**Specifies the access rules and permissions.*/
	public final static String ACCESS_PROPERTY_NAME="access";
	/**Specifies the level of access.*/
	public final static String ACCESS_LEVEL_PROPERTY_NAME="accessLevel";
	/**Specifies the list of access rules.*/
	public final static String ACCESS_RULES_PROPERTY_NAME="accessRules";
	/**Specifies a principal by URI. Used in various contexts. The local name of <code>marmot:principal</code>.*/
	public final static String PRINCIPAL_PROPERTY_NAME="principal";
	/**Specifies the rules list of an access specification. The local name of <code>marmot:rules</code>.*/
	public final static String RULES_PROPERTY_NAME="rules";
	/**Specifies resource security.*/
	public final static String SECURITY_PROPERTY_NAME="security";
	/**Specifies the selector to select one or more resources.*/
	public final static String SELECT_PROPERTY_NAME="select";

		//Marmot type names
	/**A rule specifying access permissions for zero or more principals.*/
	public final static String ACCESS_RULE_TYPE_NAME="AccessRule";
	/**A class of principals including any principal.*/
//TODO del	public final static String ANY_PRINCIPAL_TYPE_NAME="AnyPrincipal";

			//selector types
	/**A selector selecting a resource by a property value.*/
	public final static String PROPERTY_SELECTOR_TYPE_NAME="PropertySelector";
	/**A selector selecting the union of other selectors.*/
	public final static String UNION_SELECTOR_TYPE_NAME="UnionSelector";
	/**A selector selecting all resources.*/
	public final static String UNIVERSAL_SELECTOR_TYPE_NAME="UniversalSelector";
	/**A selector selecting a resource by its URI.*/
	public final static String URI_SELECTOR_TYPE_NAME="URISelector";
			//selector properties
	/**Specifies the URI of a URI selector.*/
	public final static String SELECT_URI_PROPERTY_NAME="selectURI";
	/**Specifies the property of a property selector.*/
	public final static String SELECT_PROPERTY_PROPERTY_NAME="selectProperty";
	/**Specifies the value of a property selector.*/
	public final static String SELECT_VALUE_PROPERTY_NAME="selectValue";
			
				//access levels
	/**Predefined access level type specifying inherited access.*/
	public final static String INHERITED_ACCESS_LEVEL_TYPE_NAME="InheritedAccessLevel";
	/**Predefined access level type allowing custom access.*/
	public final static String CUSTOM_ACCESS_LEVEL_TYPE_NAME="CustomAccessLevel";
	/**Predefined access level type allowing no access.*/
	public final static String PRIVATE_ACCESS_LEVEL_TYPE_NAME="PrivateAccessLevel";
	/**Predefined access level type allowing preview and execute permissions without discovery.*/
	public final static String STEALTH_ACCESS_LEVEL_TYPE_NAME="StealthAccessLevel";
	/**Predefined access level type allowing read and execute permissions of only a subset of the resource contents.*/
	public final static String PREVIEW_ACCESS_LEVEL_TYPE_NAME="PreviewAccessLevel";
	/**Predefined access level type preview and execute permissions but not read permissions.*/
	public final static String USE_ACCESS_LEVEL_TYPE_NAME="UseAccessLevel";
	/**Predefined access level type allowing discover and read permissions.*/
	public final static String RETRIEVE_ACCESS_LEVEL_TYPE_NAME="RetrieveAccessLevel";
	/**Predefined access level type allowing discover, read, and write permissions.*/
	public final static String EDIT_ACCESS_LEVEL_TYPE_NAME="EditAccessLevel";
	/**Predefined access level type allowing discover, read, write, and delete permissions.*/
	public final static String FULL_ACCESS_LEVEL_TYPE_NAME="FullAccessLevel";

			//permission operators
	/**Allows a permission.*/
	public final static String ALLOW_PROPERTY_NAME="allow";
	/**Denies a permission.*/
	public final static String DENY_PROPERTY_NAME="deny";
	
			//permissions
	/**The principal may detect that the resource exists, such as when the resource is listed in the contents of a parent collection. The local name of <code>marmot:DiscoverPermission</code>.*/
	public final static String DISCOVER_PERMISSION_TYPE_NAME="DiscoverPermission";
	/**The principal may view general information about the resource. The local name of <code>marmot:BrowsePermission</code>.*/
	public final static String BROWSE_PERMISSION_TYPE_NAME="BrowsePermission";
	/**The principal may add annotations to the resource. The local name of <code>marmot:AnnotatePermission</code>.*/
	public final static String ANNOTATE_PERMISSION_TYPE_NAME="AnnotatePermission";
	/**The principal may add annotations to the resource. The local name of <code>marmot:PreviewPermission</code>.*/
	public final static String PREVIEW_PERMISSION_TYPE_NAME="PreviewPermission";
	/**The principal may add annotations to the resource. The local name of <code>marmot:ExecutePermission</code>.*/
	public final static String EXECUTE_PERMISSION_TYPE_NAME="ExecutePermission";
	/**The principal may read the literal contents of the resource, including child listings for collections. The local name of <code>marmot:ReadPermission</code>.*/
	public final static String READ_PERMISSION_TYPE_NAME="ReadPermission";
	/**The principal may change, add, and remove resource properties. The local name of <code>marmot:ModifyPropertiesPermission</code>.*/
	public final static String MODIFY_PROPERTIES_PERMISSION_TYPE_NAME="ModifyPropertiesPermission";
	/**The principal may change the permissions describing the security of the resource, including how other principals access the resource. The local name of <code>marmot:ModifyAccessPermission</code>.*/
	public final static String MODIFY_SECURITY_PERMISSION_TYPE_NAME="ModifySecurityPermission";
	/**The principal may rename the resource. The local name of <code>marmot:RenamePermission</code>.*/
	public final static String RENAME_PERMISSION_TYPE_NAME="RenamePermission";
	/**The principal may add to the contents of the resource, including adding children in collections, but may not be able to remove contents. The local name of <code>marmot:AddPermission</code>.*/
	public final static String ADD_PERMISSION_TYPE_NAME="AddPermission";
	/**The principal may subtract from the contents of the resource, including removing children in collections, but may not be able to add contents. The local name of <code>marmot:SubtractPermission</code>.*/
	public final static String SUBTRACT_PERMISSION_TYPE_NAME="SubtractPermission";
	/**The principal may remove the resource, including collections. The local name of <code>marmot:DeletePermission</code>.*/
	public final static String DELETE_PERMISSION_TYPE_NAME="DeletePermission";

		//installation constants

	/**The property for the example directory, if given by the launcher.*/
	public final static String EXAMPLE_DIR_PROPERTY="com.globalmentor.marmot.example.dir";

}

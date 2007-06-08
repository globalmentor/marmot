package com.globalmentor.marmot;

import java.net.*;

import static com.garretwilson.net.URIConstants.*;
import static com.garretwilson.net.URIUtilities.*;

import static com.garretwilson.rdf.RDFUtilities.*;
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
	/**Specifies the access rules and permissions. The local name of <code>marmot:access</code>.*/
	public final static String ACCESS_PROPERTY_NAME="access";
	/**Specifies the rules list of an access specification. The local name of <code>marmot:rules</code>.*/
	public final static String RULES_PROPERTY_NAME="rules";
	/**Specifies a principal by URI. Used in various contexts. The local name of <code>marmot:principal</code>.*/
	public final static String PRINCIPAL_PROPERTY_NAME="principal";

		//Marmot type names
	/**A rule specifying access permissions for zero or more principals. The local name of <code>marmot:AccessRule</code>.*/
	public final static String ACCESS_RULE_TYPE_NAME="AccessRule";
				//access types
	/**Customizable access for specific principals. The local name of <code>marmot:CustomAccess</code>.*/
	public final static String CUSTOM_ACCESS_TYPE_NAME="CustomAccess";
	/**Predefined access type allowing no principals access. The local name of <code>marmot:PrivateAccess</code>.*/
	public final static String PRIVATE_ACCESS_TYPE_NAME="PrivateAccess";
	/**Predefined access type allowing all principals read permission. The local name of <code>marmot:PublicStealthAccess</code>.*/
	public final static String PUBLIC_STEALTH_ACCESS_TYPE_NAME="PublicStealthAccess";
	/**Predefined access type allowing all principals discover and read permissions. The local name of <code>marmot:PublicViewAccess</code>.*/
	public final static String PUBLIC_VIEW_ACCESS_TYPE_NAME="PublicViewAccess";
	/**Predefined access type allowing all principals discover, read, and write permissions. The local name of <code>marmot:PublicEditAccess</code>.*/
	public final static String PUBLIC_EDIT_ACCESS_TYPE_NAME="PublicEditAccess";
	/**Predefined access type allowing all principals discover, read, write, and delete permissions. The local name of <code>marmot:PublicAccess</code>.*/
	public final static String PUBLIC_ACCESS_TYPE_NAME="PublicAccess";

			//operators
	/**Allow a permission. The local name of <code>marmot:allow</code>.*/
	public final static String ALLOW_TYPE_NAME="allow";
	/**Allow all permissions except a permission. The local name of <code>marmot:allowExcept</code>.*/
	public final static String ALLOW_EXCEPT_TYPE_NAME="allowExcept";
	/**Deny a permission. The local name of <code>marmot:deny</code>.*/
	public final static String DENY_TYPE_NAME="deny";
	/**Deny all permissions except a permission. The local name of <code>marmot:denyExcept</code>.*/
	public final static String DENY_EXCEPT_TYPE_NAME="denyExcept";
	
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
	/**The principal may set the literal contents of the resource, including adding and removing children in collections, but may not delete the resource. The local name of <code>marmot:WritePermission</code>.*/
	public final static String WRITE_PERMISSION_TYPE_NAME="WritePermission";
	/**The principal may change, add, and remove resource properties. The local name of <code>marmot:ModifyPropertiesPermission</code>.*/
	public final static String MODIFY_PROPERTIES_PERMISSION_TYPE_NAME="ModifyPropertiesPermission";
	/**The principal may change the permissions describing how other principals access the resource. The local name of <code>marmot:ModifyAccessPermission</code>.*/
	public final static String MODIFY_ACCESS_PERMISSION_TYPE_NAME="ModifyAccessPermission";
	/**The principal may rename the resource. The local name of <code>marmot:RenamePermission</code>.*/
	public final static String RENAME_PERMISSION_TYPE_NAME="RenamePermission";
	/**The principal may remove the resource, including collections. The local name of <code>marmot:DeletePermission</code>.*/
	public final static String DELETE_PERMISSION_TYPE_NAME="DeletePermission";

		//installation constants

	/**The property for the example directory, if given by the launcher.*/
	public final static String EXAMPLE_DIR_PROPERTY="com.globalmentor.marmot.example.dir";

}

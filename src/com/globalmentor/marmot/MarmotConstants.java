package com.globalmentor.marmot;

import java.net.*;

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
	public final static String MARMOT_COPYRIGHT="Copyright "+COPYRIGHT_SIGN+" 2003-2005 GlobalMentor, Inc. All Rights Reserved.";	//G**i18n

	/**The version of the application.*/
	public final static String VERSION="Beta Version 0.42 build 20050129";

	/**The expiration date of the application.*/
//TODO del	public final static Calendar EXPIRATION=new GregorianCalendar(2006, GregorianCalendar.JANUARY, 1);

		//XML/RDF constants

	/**The recommended prefix to the Marmot vocabulary namespace.*/
	public final static String MARMOT_NAMESPACE_PREFIX="marmot";

	/**The URI to the Marmot namespace.*/
	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.com/namespaces/marmot#");

	
		//Marmot property names
	/**Specifies a permission of a resource. The local name of <code>marmot:permission</code>.*/
	public final static String PERMISSION_PROPERTY_NAME="permission";
	/**Specifies a principal by URI. Used in various contexts. The local name of <code>marmot:principal</code>.*/
	public final static String PRINCIPAL_PROPERTY_NAME="principal";

		//Marmot type names
	/**The user may detect that the resource exists, such as when the resource is listed in the contents of a parent collection. The local name of <code>marmot:DiscoverPermission</code>.*/
	public final static String DISCOVER_PERMISSION_TYPE_NAME="DiscoverPermission";
	/**The user may detect that the resource exists, such as when the resource is listed in the contents of a parent collection. The local name of <code>marmot:DiscoverPermission</code>.*/
	public final static URI DISCOVER_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, DISCOVER_PERMISSION_TYPE_NAME);

	/**The user may read the literal contents of the resource, including child listings for collections. The local name of <code>marmot:ReadPermission</code>.*/
	public final static String READ_PERMISSION_TYPE_NAME="ReadPermission";
	/**The user may read the literal contents of the resource, including child listings for collections. The local name of <code>marmot:ReadPermission</code>.*/
	public final static URI READ_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, READ_PERMISSION_TYPE_NAME);

	/**The user may set the literal contents of the resource, including adding and removing children in collections, but may not delete the resource. The local name of <code>marmot:WritePermission</code>.*/
	public final static String WRITE_PERMISSION_TYPE_NAME="WritePermission";
	/**The user may set the literal contents of the resource, including adding and removing children in collections, but may not delete the resource. The local name of <code>marmot:WritePermission</code>.*/
	public final static URI WRITE_PERMISSION_TYPE_URI=createReferenceURI(MARMOT_NAMESPACE_URI, WRITE_PERMISSION_TYPE_NAME);

		//installation constants

	/**The property for the example directory, if given by the launcher.*/
	public final static String EXAMPLE_DIR_PROPERTY="com.globalmentor.marmot.example.dir";

}

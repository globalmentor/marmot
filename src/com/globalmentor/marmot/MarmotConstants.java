package com.globalmentor.marmot;

import java.net.*;
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

		//Marmot ontology property names
	/**The icon of a resource. The local name of marmot:icon.*/
/*TODO del; moved to XPackage
	public final static String ICON_PROPERTY_NAME="icon";
*/
	
		//installation constants
		
	/**The property for the Marmot install directory, if given by the launcher.*/
//G***del	public final static String INSTALL_DIR="com.globalmentor.marmot.install.dir";

	/**The property for the example directory, if given by the launcher.*/
	public final static String EXAMPLE_DIR_PROPERTY="com.globalmentor.marmot.example.dir";

}

package com.globalmentor.marmot;

import java.io.*;
import java.net.*;
import java.util.*;
import com.garretwilson.lang.*;
import com.garretwilson.text.*;
import com.garretwilson.util.*;

/**Constant values used by Marmot.
@author Garret Wilson
*/
public interface MarmotConstants extends CharacterConstants
{

	/**The application URI.*/
	public final static URI MARMOT_URI=URI.create("http://globalmentor.org/software/marmot");

	/**The application title.*/
	public final static String MARMOT_TITLE="Marmot"+TRADE_MARK_SIGN_CHAR;

	/**The application copyright.*/
	public final static String MARMOT_COPYRIGHT="Copyright "+COPYRIGHT_SIGN+" 2003 GlobalMentor, Inc. All Rights Reserved.";	//G**i18n

	/**Whether this is a demo build.*/
	public final static boolean IS_DEMO=false;

	/**Whether this is a release build.*/
	public final static boolean IS_RELEASE=true;

	/**The version of the application.*/
	public final static String VERSION="Beta Version 0.2 build 20030417";

	/**The build number of the application.*/
//G***fix	public final static String BUILD="20020531";

	/**The expiration date of the application.*/
	public final static Calendar EXPIRATION=CalendarUtilities.createCalendar(2003, Calendar.MAY, 15);

		//XML/RDF constants

	/**The recommended prefix to the Marmot vocabulary namespace.*/
	public final static String MARMOT_NAMESPACE_PREFIX="marmot";

	/**The URI to the Marmot namespace.*/
	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.org/namespaces/2003/marmot#");

		//configuration file constants

	/**The name of the configuration directory.*/
	public final static String CONFIGURATION_DIRECTORY_NAME=".marmot";
	
	/**The configuration directory.*/
	public final static File CONFIGURATION_DIRECTORY=new File(System.getProperty(SystemConstants.USER_HOME), CONFIGURATION_DIRECTORY_NAME);

	/**The filename of the configuration file.*/
	public final static String CONFIGURATION_FILENAME="configuration.rdf";

	/**The file in which the the configuration information are stored.*/
	public final static File CONFIGURATION_FILE=new File(CONFIGURATION_DIRECTORY, CONFIGURATION_FILENAME);

		//installation constnats
		
	/**The property for the Marmot install directory, if given by the launcher.*/
//G***del	public final static String INSTALL_DIR="com.globalmentor.marmot.install.dir";

	/**The property for the example directory, if given by the launcher.*/
	public final static String EXAMPLE_DIR_PROPERTY="com.globalmentor.marmot.example.dir";

}

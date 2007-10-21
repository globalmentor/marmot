package com.globalmentor.marmot;

import java.net.URI;

/**Constant values and utilities used by Marmot.
@author Garret Wilson
*/
public class Marmot
{

		//predefined users
	/**The principal wildcard character, '*'.*/
//TODO del	public final static char WILDCARD_PRINCIPAL_CHAR='*';
	/**The predefined "all principals at localhost" <code>mailto</code> URI.*/
//TODO del	public final static URI ALL_LOCALHOST_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), LOCALHOST_DOMAIN);
	/**The predefined "all principals" <code>mailto</code> URI.*/
//TODO del	public final static URI ALL_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), String.valueOf(WILDCARD_PRINCIPAL_CHAR));

	/**The URI to the Marmot namespace.*/
//TODO del	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.com/marmot");

	
	/**The URI to the Marmot namespace.*/
	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.com/namespaces/marmot#");	//TODO del; used by old code

	/**A resource that is a collection of other resources.*/
	public final static String COLLECTION_CLASS_NAME="Collection";
}

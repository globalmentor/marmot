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


import static com.globalmentor.urf.URF.*;

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

	
	/**The URI of the Marmot RDF namespace.*/
	public final static URI MARMOT_RDF_NAMESPACE_URI=URI.create("http://globalmentor.com/namespaces/marmot#");	//TODO del; used by old code

	/**A resource that is a collection of other resources.*/
	public final static String COLLECTION_CLASS_NAME="Collection";	//TODO del; used by old code

}

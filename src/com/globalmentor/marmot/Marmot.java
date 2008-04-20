package com.globalmentor.marmot;

import java.net.URI;

import com.globalmentor.urf.URFResource;

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

	/**The URI of the Marmot namespace.*/
	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.com/marmot");

		//properties
	/**Specifies a template of resource by its URI, which may be a path URI relative to the repository.*/
	public final static URI TEMPLATE_URI_PROPERTY_URI=createResourceURI(MARMOT_NAMESPACE_URI, "templateURI");

	/**Returns the template URI of the resource
	@param resource The resource the property of which should be located.
	@return The URI value of the property, or <code>null</code> if there is no such property or the property value is not a URI.
	@see #TEMPLATE_URI_PROPERTY_URI
	*/
	public static URI getTemplateURI(final URFResource resource)
	{
		return asURI(resource.getPropertyValue(TEMPLATE_URI_PROPERTY_URI));
	}

	/**Sets the template URI of the resource.
	@param resource The resource of which the property should be set.
	@param value The property value to set.
	@see #TEMPLATE_URI_PROPERTY_URI
	*/
	public static void setTemplateURI(final URFResource resource, final URI value)
	{
		resource.setPropertyValue(TEMPLATE_URI_PROPERTY_URI, value);
	}

}

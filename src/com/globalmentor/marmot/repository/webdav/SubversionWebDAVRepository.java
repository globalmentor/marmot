package com.globalmentor.marmot.repository.webdav;

import java.net.*;
import java.util.Set;

import static com.garretwilson.net.URIs.*;

import com.garretwilson.net.URIConstants;
import com.garretwilson.net.http.*;
import com.garretwilson.net.http.webdav.*;
import static com.garretwilson.net.http.webdav.SubversionWebDAV.*;
import static com.garretwilson.text.Characters.*;

/**Subversion repository accessed via WebDAV with autoversioning turned on.
<p>Autoversioning must be turned on in Apaching using <code>SVNAutoversioning on</code>.</p>
<p>This version stores URF properties by using a URI that is the concatenation of {@value SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI} and the encoded version of the URF property URI,
using {@value #XML_NAME_URI_ESCAPE_CHAR} as the escape character. The standard URI escape character, {@value URIConstants#ESCAPE_CHAR}, is not a valid name character, so
{@value #XML_NAME_URI_ESCAPE_CHAR}, which conveniently is not a valid URI character, is used instead.</p>
@author Garret Wilson
*/
public class SubversionWebDAVRepository extends WebDAVRepository
{

	/**The Subversion custom property namespace converted to a string for quick string comparisons.*/
	protected final static String SUBVERSION_CUSTOM_NAMESPACE=SUBVERSION_CUSTOM_NAMESPACE_URI.toString();

	/**The character used to escape URIs to encode them as XML names.*/
	protected final static char XML_NAME_URI_ESCAPE_CHAR=MIDDLE_DOT_CHAR;

	/**Repository URI contructor using the default HTTP client.
	The given repository URI should end in a slash.
	@param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	*/
	public SubversionWebDAVRepository(final URI repositoryURI)
	{
		this(repositoryURI, HTTPClient.getInstance());	//construct the class using the default HTTP client		
	}
	
	/**Repository URI and HTTP client contructor.
	The given repository URI should end in a slash.
	@param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	@param httpClient The HTTP client used to create a connection to this resource.	
	*/
	public SubversionWebDAVRepository(final URI repositoryURI, final HTTPClient httpClient)
	{
		this(repositoryURI, repositoryURI, httpClient);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI contructor using the default HTTP client.
	The given private repository URI should end in a slash.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	*/
	public SubversionWebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		this(publicRepositoryURI, privateRepositoryURI, HTTPClient.getInstance());	//construct the class using the default HTTP client				
	}

	/**Public repository URI, private repository URI, and HTTP client contructor.
	The given private repository URI should end in a slash.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	@param httpClient The HTTP client used to create a connection to this resource.	
	*/
	public SubversionWebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final HTTPClient httpClient)
	{
		super(publicRepositoryURI, privateRepositoryURI, httpClient);	//construct the parent class
		final Set<String> ignoredWebDAVNamespaces=getIgnoredWebDAVNamespaces();	//get the map of ignored WebDAV namespaces
		ignoredWebDAVNamespaces.add(SUBVERSION_DAV_NAMESPACE_URI.toString());	//by default ignore the Subversion DAV namespace
		ignoredWebDAVNamespaces.add(SUBVERSION_CUSTOM_NAMESPACE);	//by default ignore the Subversion custom namespace
	}

	/**Determines the WebDAV property name to represent an URF property.
	This version uses the encoded URF property URI as the local name of the {@value SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI} namespace.
	The standard URI escape character, {@value URIConstants#ESCAPE_CHAR}, is not a valid name character, so
	{@value #XML_NAME_URI_ESCAPE_CHAR}, which conveniently is not a valid URI character, is used instead.
	@param urfPropertyURI The URI of the URF property to represent.
	@return A WebDAV property name to use in representing an URF property with the given URF property URI.
	@see SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI
	@see #XML_NAME_URI_ESCAPE_CHAR
	*/
	protected WebDAVPropertyName getWebDAVPropertyName(final URI urfPropertyURI)
	{
		return new WebDAVPropertyName(SUBVERSION_CUSTOM_NAMESPACE, encodeURI(urfPropertyURI, XML_NAME_URI_ESCAPE_CHAR));	//create and return a new WebDAV property name in the Subversion custom property namespace
	}

	/**Determines the URF property to represent the given WebDAV property if possible.
	If the WebDAV property has a local name of {@value SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI}, the decoded form of its local name, if an absolute URI, will be used as the URF property URI.
	The standard URI escape character, {@value URIConstants#ESCAPE_CHAR}, is not a valid name character, so
	{@value #XML_NAME_URI_ESCAPE_CHAR}, which conveniently is not a valid URI character, is used instead.
	Otherwise, this method delegates to the super version.
	@param webdavPropertyName The name of the WebDAV property.
	@return The URI of the URF property to represent the given WebDAV property, or <code>null</code> if the given WebDAV property cannot be represented in URF.
	@see SubversionWebDAV#SUBVERSION_CUSTOM_NAMESPACE_URI
	@see #XML_NAME_URI_ESCAPE_CHAR
	*/
	protected URI getURFPropertyURI(final WebDAVPropertyName webdavPropertyName)
	{
		if(SUBVERSION_CUSTOM_NAMESPACE.equals(webdavPropertyName.getNamespace()))	//if this is the Subversion custom property namespace
		{
			try
			{
				final String urfPRopertyURI=uriDecode(webdavPropertyName.getLocalName(), XML_NAME_URI_ESCAPE_CHAR);	//the URF property URI may be encoded as the local name of the Subversion custom property
				final URI urfPropertyURI=URI.create(urfPRopertyURI);	//create an URF property URI from the decoded local name, if we can
				if(urfPropertyURI.isAbsolute())	//only absolute URIs could have been URF property URIs
				{
					return urfPropertyURI;	//return the URF property URI we determined
				}
			}
			catch(final IllegalArgumentException illegalArgumentException)	//if the Subversion custom property local name wasn't an encoded URI, ignore the error and use the property normally
			{
			}
		}
		return super.getURFPropertyURI(webdavPropertyName);	//if this doesn't appear to be an URF property, treat the property as a normal WebDAV property
	}

}

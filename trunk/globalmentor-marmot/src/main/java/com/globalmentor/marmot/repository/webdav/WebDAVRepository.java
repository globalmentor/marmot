/*
 * Copyright © 1996-2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.webdav;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.*;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import static com.globalmentor.java.Objects.*;
import static com.globalmentor.text.xml.XML.*;
import static com.globalmentor.urf.URF.*;
import static com.globalmentor.urf.content.Content.*;
import static com.globalmentor.urf.dcmi.DCMI.*;

import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.model.Locales.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.net.http.webdav.WebDAV.*;

import com.globalmentor.collections.CollectionMap;
import com.globalmentor.collections.HashSetHashMap;
import com.globalmentor.event.ProgressListener;
import com.globalmentor.io.*;
import com.globalmentor.log.Log;
import com.globalmentor.marmot.Marmot;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.model.NameValuePair;
import com.globalmentor.net.*;
import com.globalmentor.net.http.*;
import com.globalmentor.net.http.webdav.*;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.*;
import com.globalmentor.util.*;

import org.w3c.dom.*;

/**
 * Repository accessed via WebDAV.
 * <p>
 * This repository recognizes the URF type <code>urf.List</code> and creates a collection for each such resource.
 * </p>
 * <p>
 * URF properties are stored as normal WebDAV properties, except that the value is a TURF interchange document beginning with the TURF signature
 * {@value TURF#SIGNATURE}, and within the instance are one or more URF resources values of the property are stored. If an URF property has no namespace, a
 * WebDAV property name is formed using the URF property URI as the namespace and the string {@value #URF_TOKEN_LOCAL_NAME} as a local name, because WebDAV
 * requires that each property have a separate namespace and local name.
 * </p>
 * <p>
 * This implementation requires exact URIs and does not follow HTTP redirects. Any redirection responses are interpreted as indicating that the resource does
 * not exist.
 * </p>
 * <p>
 * This implementation has a race condition for adding new property values for properties that already exist using
 * {@link #addResourceProperties(URI, URFProperty...)} and {@link #alterResourceProperties(URI, URFResourceAlteration)} in that simultaneous additions could
 * clobber all the additions but the last one.
 * </p>
 * @author Garret Wilson
 */
public class WebDAVRepository extends AbstractHierarchicalSourceRepository
{

	/** The URI to the Marmot WebDAV repository namespace. */
	public final static URI MARMOT_WEBDAV_REPOSITORY_NAMESPACE_URI = Marmot.MARMOT_NAMESPACE_URI.resolve("repository/webdav/");

	/**
	 * The server's last modified time, using RFC 1123 format, reported by the {@value WebDAV#GET_LAST_MODIFIED_PROPERTY_NAME} property when the
	 * {@link Content#MODIFIED_PROPERTY_URI} property was written. This is stored as a duplicate time value so that it can be detected if any other software
	 * modified the file without Marmot's knowledge. If the value of this property matches the {@value WebDAV#GET_LAST_MODIFIED_PROPERTY_NAME} property, it will
	 * be assumed that {@value Content#MODIFIED_PROPERTY_URI} stores the correct last modified time set by this repository.
	 */
	protected final static WebDAVPropertyName SYNC_WEBDAV_GET_LAST_MODIFIED_PROPERTY_NAME = new WebDAVPropertyName(MARMOT_WEBDAV_REPOSITORY_NAMESPACE_URI,
			"syncWebDAVGetLastModified");

	/**
	 * Determines the WebDAV property name to represent the synchronization WebDAV get last modified property.
	 * @return The WebDAV property name to use in representing the synchronization WebDAV get last modified property, or <code>null</code> if no synchronization
	 *         last modified property should be used.
	 */
	protected WebDAVPropertyName getSyncWebDAVGetLastModifiedWebDAVPropertyName()
	{
		return SYNC_WEBDAV_GET_LAST_MODIFIED_PROPERTY_NAME;
	}

	//TODO the current technique of erasing the password after each call may become obsolete when the HTTP client supports persistent connections

	/** The extension used for directories to hold resource children. */
	//TODO move if needed	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/** The name of the WebDAV property that holds the description of a resource. */
	//TODO del if not needed	private final static WebDAVPropertyName RESOURCE_DESCRIPTION_PROPERTY_NAME=new WebDAVPropertyName(MARMOT_NAMESPACE_URI.toString(), "description");

	/**
	 * Determines the WebDAV property that holds the description of a resource. This version returns {@value #RESOURCE_DESCRIPTION_PROPERTY_NAME}.
	 * @return The WebDAV property to hold a resource description.
	 */
	/*TODO del
		protected WebDAVPropertyName getResourceDescriptionPropertyName()
		{
			return RESOURCE_DESCRIPTION_PROPERTY_NAME;
		}
	*/

	/** The WebDAV namespaces that are not automatically added as URF properties, although some properties in these namespaces may be used. */
	private final Set<String> ignoredWebDAVNamespaces = new HashSet<String>();

	/**
	 * Returns the WebDAV namespaces that are not automatically added as URF properties. Some properties in these namespaces may be used. The returned map is not
	 * thread-safe; it can be used for reading, but should not be modified after repository construction.
	 * @return The WebDAV namespaces that are not automatically added as URF properties.
	 */
	protected Set<String> getIgnoredWebDAVNamespaces()
	{
		return ignoredWebDAVNamespaces;
	}

	/** The WebDAV properties that are not automatically added as URF properties, although some of these properties may be used. */
	//TODO del if not needed	private final Set<String> ignoredWebDAVProperties=new HashSet<String>();

	/**
	 * Returns the WebDAV properties that are not automatically added as URF properties. Some of these properties may be used. The returned map is not
	 * thread-safe; it can be used for reading, but should not be modified after repository construction.
	 * @return The WebDAV properties that are not automatically added as URF properties.
	 */
	//TODO del if not needed		protected Set<String> getIgnoredWebDAVProperties() {return ignoredWebDAVProperties;}

	/**
	 * Default constructor with no root URI defined. The root URI must be defined before the repository is opened.
	 */
	public WebDAVRepository()
	{
		this(null);
	}

	/**
	 * Repository URI constructor using the default HTTP client. The given repository URI should end in a slash.
	 * @param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 */
	public WebDAVRepository(final URI repositoryURI)
	{
		this(repositoryURI, HTTPClient.getInstance()); //construct the class using the default HTTP client		
	}

	/**
	 * Repository URI and HTTP client constructor. The given repository URI should end in a slash.
	 * @param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 * @param httpClient The HTTP client used to create a connection to this resource.
	 */
	public WebDAVRepository(final URI repositoryURI, final HTTPClient httpClient)
	{
		this(repositoryURI, repositoryURI, httpClient); //use the same repository URI as the public and private namespaces
	}

	/**
	 * Public repository URI and private repository URI constructor using the default HTTP client. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 */
	public WebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		this(publicRepositoryURI, privateRepositoryURI, HTTPClient.getInstance()); //construct the class using the default HTTP client				
	}

	/**
	 * Public repository URI, private repository URI, and HTTP client constructor. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	 * @param httpClient The HTTP client used to create a connection to this resource.
	 */
	public WebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final HTTPClient httpClient)
	{
		super(publicRepositoryURI, privateRepositoryURI); //construct the parent class
		this.httpClient = httpClient; //save the HTTP client
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO = (URFResourceTURFIO<URFResource>)getDescriptionIO(); //get the description I/O
		urfResourceDescriptionIO.setBOMWritten(false); //turn off BOM generation
		urfResourceDescriptionIO.setFormatted(false); //turn off formatting
		getIgnoredWebDAVNamespaces().add(WEBDAV_NAMESPACE.toString()); //we only access WebDAV-specific properties explictly
		getIgnoredWebDAVNamespaces().add(MARMOT_WEBDAV_REPOSITORY_NAMESPACE_URI.toString()); //we only access Marmot WebDAV repository properties explictly
		getIgnoredWebDAVNamespaces().add(ApacheWebDAV.APACHE_WEBDAV_PROPERTY_NAMESPACE_URI.toString()); //by default ignore properties in the Apache WebDAV namespace
		getIgnoredWebDAVNamespaces().add(SRTWebDAV.SRT_WEBDAV_PROPERTY_NAMESPACE_URI.toString()); //ignore South River Tech properties, which duplicate URF properties, unless we ask for them specifically to guess at a the last modified time
		getIgnoredWebDAVNamespaces().add(MicrosoftWebDAV.MICROSOFT_WEBDAV_PROPERTY_NAMESPACE_URI.toString()); //ignore Microsoft properties placed by Windows Vista; although these properties might have been useful, they forego millisecond precision by using RFC 1123 instead of RFC 3339
	}

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace.
	 * @param publicRepositoryURI The public URI identifying the location of the new repository.
	 * @param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	 * @throws NullPointerException if the given public repository URI and/or private repository URI is <code>null</code>.
	 */
	protected Repository createSubrepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		final WebDAVRepository repository = new WebDAVRepository(publicRepositoryURI, privateRepositoryURI, getHTTPClient()); //create a new repository
		repository.setUsername(getUsername()); //transfer authentication info
		repository.setPassword(getPassword()); //transfer authentication info
		return repository; //return the new repository
	}

	/** A token local name for WebDAV for use with URF properties that have no local name. */
	private final static String URF_TOKEN_LOCAL_NAME = "urfTokenLocalName";

	/**
	 * Determines the WebDAV property name to represent an URF property.
	 * <p>
	 * If an URF property has no namespace, this implementation forms a WebDAV property name using the URF property URI as the namespace and the string
	 * {@value #URF_TOKEN_LOCAL_NAME} as a local name, because WebDAV requires that each property have a separate namespace and local name.
	 * </p>
	 * @param urfPropertyURI The URI of the URF property to represent.
	 * @return A WebDAV property name to use in representing an URF property with the given URF property URI.
	 * @see #URF_TOKEN_LOCAL_NAME
	 */
	protected WebDAVPropertyName createWebDAVPropertyName(final URI urfPropertyURI)
	{
		final String webDAVPropertyNamespace;
		final String webDAVPropertyLocalName;
		final String rawFragment = urfPropertyURI.getRawFragment(); //get the raw fragment of the URF property URI
		if(rawFragment != null) //if the URI has a fragment
		{
			final String urfPropertyURIString = urfPropertyURI.toString(); //get the string representation of the URF property URI
			assert urfPropertyURIString.endsWith(rawFragment);
			webDAVPropertyNamespace = urfPropertyURIString.substring(0, urfPropertyURIString.length() - rawFragment.length()); //remove the raw fragment, but leave the fragment identifier on the namespaces
			webDAVPropertyLocalName = rawFragment; //the raw fragment itself is the WebDAV local name
		}
		else
		//check for a path-based namespace
		{
			final URI urfPropertyNamespaceURI = getNamespaceURI(urfPropertyURI); //get the normal URF namespace for path-based namespaces
			if(urfPropertyNamespaceURI != null) //if there is an URF namespace
			{
				webDAVPropertyNamespace = urfPropertyNamespaceURI.toString(); //the WebDAV namespace is the string form of the URF namespace URI
				webDAVPropertyLocalName = getRawName(urfPropertyURI); //the raw name of the URF property URI is the WebDAV property local name
			}
			else
			//if there is no URF namespace
			{
				webDAVPropertyNamespace = urfPropertyURI.toString(); //use the string form of the property URI as the namespace
				webDAVPropertyLocalName = URF_TOKEN_LOCAL_NAME; //create a fake local name; we have to have some WebDAV property to correspond to the URF property, and there are no other options at this point				
			}
		}
		return new WebDAVPropertyName(webDAVPropertyNamespace, webDAVPropertyLocalName); //create and return a new WebDAV property name from the components we determined
	}

	/**
	 * Creates a WebDAV property and value to represent the given URF property and value(s).
	 * @param resourceURI The URI of the resource.
	 * @param properties The URF properties to represent as values for a WebDAV property.
	 * @return A WebDAV property and value representing the given URF property.
	 * @throws NullPointerException if the given properties is <code>null</code>.
	 * @throws IllegalArgumentException if no properties are given.
	 * @throws IllegalArgumentException if all of the properties do not have the same property URI.
	 * @throws IOException if there is an error creating the WebDAV property and value.
	 * @see #createWebDAVPropertyName(URI)
	 */
	protected WebDAVProperty createWebDAVProperty(final URI resourceURI, final Iterable<URFProperty> properties) throws IOException
	{
		final NameValuePair<URI, String> webdavPropertyStringValue = encodePropertiesTextValue(resourceURI, properties); //encode the properties into a single value
		final WebDAVPropertyName webdavPropertyName = createWebDAVPropertyName(webdavPropertyStringValue.getName()); //create a WebDAV property name from the URF property URI
		final WebDAVPropertyValue webdavPropertyValue = new WebDAVLiteralPropertyValue(webdavPropertyStringValue.getValue()); //create a WebDAV literal property value from the determined string
		return new WebDAVProperty(webdavPropertyName, webdavPropertyValue); //create and return a WebDAV property and value
	}

	/**
	 * Determines the URF property to represent the given WebDAV property if possible.
	 * <p>
	 * If the WebDAV property has a local name of {@value #URF_TOKEN_LOCAL_NAME}, the URI of its namespace will be used as the URF property URI.
	 * </p>
	 * <p>
	 * This method returns <code>null</code> for all WebDAV properties in the {@value WebDAV#WEBDAV_NAMESPACE} namespace.
	 * </p>
	 * @param webdavPropertyName The name of the WebDAV property.
	 * @return The URI of the URF property to represent the given WebDAV property, or <code>null</code> if the given WebDAV property cannot be represented in URF.
	 * @see #URF_TOKEN_LOCAL_NAME
	 */
	protected URI getURFPropertyURI(final WebDAVPropertyName webdavPropertyName)
	{
		final String webdavPropertyNamespace = webdavPropertyName.getNamespace(); //get the property namespace
		if(WEBDAV_NAMESPACE.equals(webdavPropertyNamespace)) //ignore the WebDAV namespace
		{
			return null; //the WebDAV namespace isn't a valid URI, anyway
		}
		try
		{
			final URI webdavPropertyNamespaceURI = new URI(webdavPropertyNamespace); //get the property namespace URI
			final String webdavPropertyLocalName = webdavPropertyName.getLocalName(); //get the property local name
			//if the local name is just a token local name, the namespace is the real URF property URI
			return URF_TOKEN_LOCAL_NAME.equals(webdavPropertyLocalName) ? webdavPropertyNamespaceURI : webdavPropertyName.getURI();
		}
		catch(final URISyntaxException uriSyntaxException) //if the namespace is not a valid URI, this is not a valid URF property
		{
			return null; //there is no way to represent this property in URF
		}
	}

	/** The HTTP client used to create a connection to this resource. */
	private final HTTPClient httpClient;

	/** @return The HTTP client used to create a connection to this resource. */
	protected HTTPClient getHTTPClient()
	{
		return httpClient;
	}

	/** The username to use in accessing the repository, or <code>null</code> if no username is specified. */
	private String username = null;

	/** @return The username to use in accessing the repository, or <code>null</code> if no username is specified. */
	public String getUsername()
	{
		return username;
	}

	/**
	 * Sets the username to use in accessing the repository.
	 * @param username The username to use in accessing the repository, or <code>null</code> if no username is specified.
	 */
	public void setUsername(final String username)
	{
		this.username = username;
	}

	/** The password to use in accessing the repository, or <code>null</code> if no password is specified. */
	private char[] password = null;

	/** @return The username to use in accessing the repository, or <code>null</code> if no password is specified. */
	public char[] getPassword()
	{
		return password;
	}

	/**
	 * Sets the password to use in accessing the repository.
	 * @param password The password to use in accessing the repository, or <code>null</code> if no password is specified.
	 */
	public void setPassword(final char[] password)
	{
		this.password = password;
	}

	/**
	 * Returns whatever password authentication should be used when communicating with a resource.
	 * @return A password authentication object with the repository's username and password, or <code>null</code> if no username and password are specified.
	 * @see #getUsername()
	 * @see #getPassword()
	 */
	protected PasswordAuthentication getPasswordAuthentication()
	{
		final String username = getUsername(); //get the username
		final char[] password = getPassword(); //get the password
		return username != null && password != null ? new PasswordAuthentication(username, password) : null; //return new password authentication if this information is available
	}

	/** {@inheritDoc} This implementation returns <code>false</code> for all resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected boolean resourceExistsImpl(URI resourceURI) throws ResourceIOException
	{
		final URI privateResourceURI = getSourceResourceURI(resourceURI); //get the resource URI in the private space
		if(!isSourceResourceVisible(privateResourceURI)) //if this resource should not be public
		{
			return false; //ignore this resource
		}
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(privateResourceURI, getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			return webdavResource.exists(); //see if the WebDAV resource exists		
		}
		catch(final HTTPRedirectException httpRedirectException) //if the WebDAV resource tries to redirect us somewhere else
		{
			return false; //consider this to indicate that the resource, as identified by the resource URI, does not exist
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource getResourceDescriptionImpl(final URI resourceURI) throws ResourceIOException
	{
		final URF urf = createURF(); //create a new URF data model
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			final Map<WebDAVPropertyName, WebDAVProperty> properties = webdavResource.propFind(); //get the properties of this resource
			return createResourceDescription(urf, resourceURI, properties); //create a resource from this URI and property list
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		catch(final DataException dataException) //if the data wasn't correct
		{
			throw toResourceIOException(resourceURI, dataException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} For collections, this implementation retrieves the content of the {@value #COLLECTION_CONTENT_NAME} file, if any. */
	@Override
	protected InputStream getResourceInputStreamImpl(final URI resourceURI) throws ResourceIOException
	{
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			if(isCollectionURI(resourceURI)) //if the resource is a collection
			{
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				final WebDAVResource contentWebDAVResource = new WebDAVResource(getSourceResourceURI(contentURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource for special collection content resource TODO cache these resources, maybe
				if(contentWebDAVResource.exists()) //if there is a special collection content resource
				{
					return contentWebDAVResource.getInputStream(); //return an input stream to the collection content resource
				}
				else
				//if there is no collection content resource
				{
					if(!webdavResource.exists()) //if the content resource doesn't exist because the collection itself doesn't exist
					{
						throw new HTTPNotFoundException("Collection resource " + webdavResource.getURI() + " does not exist.");
					}
					return new ByteArrayInputStream(NO_BYTES); //return an input stream to an empty byte array
				}
			}
			else
			//if the resource is not a collection
			{
				return webdavResource.getInputStream(); //return an input stream to the resource
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} For collections, this implementation stores the content in the {@value #COLLECTION_CONTENT_NAME} file. */
	@Override
	protected OutputStream getResourceOutputStreamImpl(final URI resourceURI, final URFDateTime newContentModified) throws ResourceIOException
	{
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource TODO cache these resources, maybe
			if(!webdavResource.exists()) //if the resource doesn't already exist
			{
				throw new ResourceNotFoundException(resourceURI, "Cannot open output stream to non-existent resource " + resourceURI);
			}
			final WebDAVResource contentWebDAVResource; //determine the WebDAV resource for accessing the content file
			if(isCollectionURI(resourceURI)) //if the resource is a collection
			{
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				contentWebDAVResource = new WebDAVResource(getSourceResourceURI(contentURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource for special collection content resource
			}
			else
			//if the resource is not a collection
			{
				contentWebDAVResource = webdavResource; //use the normal WebDAV resource
			}
			OutputStream outputStream = contentWebDAVResource.getOutputStream(); //get an output stream to the content WebDAV resource
			if(newContentModified != null) //if we should update the content modified datetime
			{
				final URFResourceAlteration resourceAlteration = DefaultURFResourceAlteration.createSetPropertiesAlteration(new DefaultURFProperty(
						Content.MODIFIED_PROPERTY_URI, newContentModified)); //create a resource alteration for setting the content modified property
				outputStream = new DescriptionWriterOutputStreamDecorator(outputStream, resourceURI, resourceAlteration, webdavResource, passwordAuthentication); //wrap the output stream in a decorator that will update the WebDAV properties after the contents are stored; this method will erase the provided password, if any, after it completes the resource property updates
			}
			return outputStream; //return the output stream we created
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(newContentModified == null && passwordAuthentication != null) //if we didn't do a delayed write we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} This implementation ignores child resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected boolean hasChildrenImpl(final URI resourceURI) throws ResourceIOException
	{
		final URI privateResourceURI = getSourceResourceURI(resourceURI); //get the URI of the resource in the private namespace
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(privateResourceURI, getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			final List<NameValuePair<URI, Map<WebDAVPropertyName, WebDAVProperty>>> propertyMaps = webdavResource.propFind(Depth.ONE); //get the properties of the resources one level down
			for(final NameValuePair<URI, Map<WebDAVPropertyName, WebDAVProperty>> propertyMap : propertyMaps) //look at each property map
			{
				final URI childResourcePrivateURI = propertyMap.getName(); //get the private URI of the child resource this property list represents
				if(isSourceResourceVisible(childResourcePrivateURI) && !privateResourceURI.equals(childResourcePrivateURI)) //if the associated child resource is public and the property list is *not* for this resource
				{
					return true; //this resource has children
				}
			}
			return false; //no properties could be found for any children
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} This implementation does not include child resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	public List<URFResource> getChildResourceDescriptionsImpl(final URI resourceURI, final ResourceFilter resourceFilter, final int depth)
			throws ResourceIOException
	{
		if(depth != 0) //a depth of zero means don't get child resources
		{
			final URI privateResourceURI = getSourceResourceURI(resourceURI); //get the URI of the resource in the private namespace
			final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
			try
			{
				final WebDAVResource webdavResource = new WebDAVResource(privateResourceURI, getHTTPClient(), passwordAuthentication); //create a WebDAV resource
				final Depth webdavDepth; //we'll get the depth based upon the value passed
				try
				{
					webdavDepth = depth == INFINITE_DEPTH ? Depth.INFINITY : Depth.values()[depth]; //get the depth based upon the value passed
				}
				catch(final IndexOutOfBoundsException indexOutOfBoundsException) //if an illegal depth was passed
				{
					throw new IllegalArgumentException(Integer.toString(depth)); //TODO later convert the depth by using infinity and checking the result
				}
				final URF urf = createURF(); //create a new URF data model
				final List<NameValuePair<URI, Map<WebDAVPropertyName, WebDAVProperty>>> propertyMaps = webdavResource.propFind(webdavDepth); //get the properties of the resources
				final List<URFResource> childResourceList = new ArrayList<URFResource>(propertyMaps.size()); //create a list of child resources no larger than the number of WebDAV resource property maps
				for(final NameValuePair<URI, Map<WebDAVPropertyName, WebDAVProperty>> propertyMap : propertyMaps) //look at each property map
				{
					final URI childResourcePrivateURI = propertyMap.getName(); //get the private URI of the child resource this property list represents
					if(isSourceResourceVisible(childResourcePrivateURI) && !privateResourceURI.equals(childResourcePrivateURI)) //if the associated child resource is visible and the property list is *not* for this resource
					{
						final URI childResourcePublicURI = getRepositoryResourceURI(childResourcePrivateURI); //get the public URI of this child resource
						if(getSubrepository(childResourcePublicURI) == this) //if this child wouldn't be located in a subrepository (i.e. ignore resources obscured by subrepositories)
						{
							if(resourceFilter == null || resourceFilter.isPass(childResourcePublicURI)) //if we should include this resource based upon its URI
							{
								final URFResource childResourceDescription = createResourceDescription(urf, childResourcePublicURI, propertyMap.getValue()); //create a resource from this URI and property lists
								if(resourceFilter == null || resourceFilter.isPass(childResourceDescription)) //if we should include this resource based upon its description
								{
									childResourceList.add(childResourceDescription); //add this child resource description to our list
								}
							}
						}
					}
				}
				//aggregate any mapped subrepositories
				for(final Repository childSubrepository : getChildSubrepositories(resourceURI)) //see if any subrepositories are mapped as children of this repository
				{
					final URI childSubrepositoryURI = childSubrepository.getRootURI(); //get the URI of the subrepository
					childResourceList.add(childSubrepository.getResourceDescription(childSubrepositoryURI)); //get a description of the subrepository root resource
					if(depth == INFINITE_DEPTH || depth > 0) //if we should get child resources lower in the hierarchy
					{
						childResourceList.addAll(childSubrepository.getChildResourceDescriptions(childSubrepositoryURI, resourceFilter, depth == INFINITE_DEPTH ? depth
								: depth - 1)); //get descriptions of subrepository children
					}
				}

				//TODO do the special Marmot thing about checking for special Marmot directories

				//TODO fix				Collections.sort(resourceList);	//sort the resource by URI
				return childResourceList; //return the list of resources we constructed
			}
			catch(final IOException ioException) //if an I/O exception occurs
			{
				throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
			}
			catch(final DataException dataException) //if the data wasn't correct
			{
				throw toResourceIOException(resourceURI, dataException); //translate the exception to a resource I/O exception and throw that
			}
			finally
			{
				if(passwordAuthentication != null) //if we used password authentication
				{
					fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
				}
			}
		}
		else
		//if a depth of zero was requested
		{
			return emptyList(); //return an empty list
		}
	}

	/** {@inheritDoc} This implementation updates the resource description after its contents are stored.. */
	@Override
	protected OutputStream createResourceImpl(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			final WebDAVResource contentWebDAVResource; //determine the WebDAV resource for accessing the content file
			if(isCollectionURI(resourceURI)) //if this is a collection
			{
				webdavResource.mkCol(); //create the collection
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				contentWebDAVResource = new WebDAVResource(getSourceResourceURI(contentURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource for special collection content resource
			}
			else
			//if this is not a collection
			{
				contentWebDAVResource = webdavResource; //use the normal WebDAV resource
			}
			final OutputStream outputStream = contentWebDAVResource.getOutputStream(); //get an output stream to the content WebDAV resource
			return new DescriptionWriterOutputStreamDecorator(outputStream, resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription),
					webdavResource, passwordAuthentication); //wrap the output stream in a decorator that will update the WebDAV properties after the contents are stored; this method will erase the provided password, if any, after it completes the resource property updates
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource createResourceImpl(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents)
			throws ResourceIOException
	{
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			final WebDAVResource contentWebDAVResource; //determine the WebDAV resource for accessing the content file
			if(isCollectionURI(resourceURI)) //if this is a collection
			{
				webdavResource.mkCol(); //create the collection
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				contentWebDAVResource = new WebDAVResource(getSourceResourceURI(contentURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource for special collection content resource
			}
			else
			//if this is not a collection
			{
				contentWebDAVResource = webdavResource; //use the normal WebDAV resource
			}
			if(resourceContents.length > 0 || !isCollectionURI(resourceURI)) //don't write empty content for a new collection
			{
				contentWebDAVResource.put(resourceContents); //create the content WebDAV resource with the given contents
			}
			return alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), webdavResource); //set the properties using the WebDAV resource object
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		catch(final DataException dataException) //if the data wasn't correct
		{
			throw toResourceIOException(resourceURI, dataException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/** {@inheritDoc} This implementation ignores requests to delete all resource for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected void deleteResourceImpl(final URI resourceURI) throws ResourceIOException
	{
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final URI sourceResourceURI = getSourceResourceURI(resourceURI);
			if(isSourceResourceVisible(sourceResourceURI)) //if this is a visible resource
			{
				final WebDAVResource webdavResource = new WebDAVResource(sourceResourceURI, getHTTPClient(), passwordAuthentication); //create a WebDAV resource
				webdavResource.delete(); //delete the resource		
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**
	 * Determines the WebDAV property names of an existing list of WebDAV properties that correspond to a set of URF property URIs. This is useful for deleting
	 * existing URF properties.
	 * @param webdavProperties The existing WebDAV properties.
	 * @param propertyURIs The set of URF property URIs to match.
	 * @return The set of names of WebDAV properties that match the given URF property URIs.
	 */
	/*TODO del if not needed
		protected Set<WebDAVPropertyName> getWebDAVPropertyNames(final List<WebDAVProperty> webdavProperties, final Set<URI> urfPropertyURIs)
		{
			final Set<WebDAVPropertyName> webdavPropertyNames = new HashSet<WebDAVPropertyName>(); //create a set to find out how many matching WebDAV properties there are
			for(final WebDAVProperty webdavProperty : webdavProperties) //look at each of the WebDAV properties
			{
				final WebDAVPropertyName webdavPropertyName = webdavProperty.getName(); //get the property namespace
				final URI urfPropertyURI = getURFPropertyURI(webdavPropertyName); //get the URI of the corresponding URF property, if any
				if(urfPropertyURI != null && urfPropertyURIs.contains(urfPropertyURI)) //if there is a valid URF property and the property is one of the URF properties in question
				{
					webdavPropertyNames.add(webdavPropertyName); //this WebDAV property matches
				}
			}
			return webdavPropertyNames; //return the WebDAV property names we collected		
		}
	*/

	/**
	 * {@inheritDoc} This implementation does not support removing specific properties by value.
	 * @throws UnsupportedOperationException if a property is requested to be removed by value.
	 * @throws UnsupportedOperationException if a property is requested to be added without the property URI first being removed (i.e. a property addition
	 *              instead of a property setting).
	 */
	@Override
	protected URFResource alterResourcePropertiesImpl(final URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException
	{
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			return alterResourceProperties(resourceURI, resourceAlteration, webdavResource); //alter the properties of the resource
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		catch(final DataException dataException) //if the data wasn't correct
		{
			throw toResourceIOException(resourceURI, dataException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**
	 * Alters properties of a given resource. This implementation does not support removing specific properties by value.
	 * <p>
	 * This implementation has a race condition for adding new property values for properties that already exist in that simultaneous additions could clobber all
	 * the additions but the last one.
	 * </p>
	 * <p>
	 * If the {@link Content#MODIFIED_PROPERTY_URI} property is being set/added, the property returned by
	 * {@link #getSyncWebDAVGetLastModifiedWebDAVPropertyName()}, if any, will be updated with the current value of the
	 * {@link WebDAV#GET_LAST_MODIFIED_PROPERTY_NAME} property.
	 * </p>
	 * @param resourceURI The reference URI of the resource.
	 * @param resourceAlteration The specification of the alterations to be performed on the resource.
	 * @param webdavResource The WebDAV resource to be modified.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI, resource alteration, and/or WebDAV resource is <code>null</code>.
	 * @throws IOException if the resource properties could not be altered.
	 * @throws DataException if some data we retrieved was not as expected.
	 * @throws UnsupportedOperationException if a property is requested to be removed by value.
	 */
	protected URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration, final WebDAVResource webdavResource)
			throws IOException, DataException
	{
		if(!resourceAlteration.getPropertyRemovals().isEmpty()) //if there are properties to be removed by value
		{
			throw new UnsupportedOperationException("This implementation does not support removing properties by value.");
		}
		final Set<URI> livePropertyURIs = getLivePropertyURIs(); //get the set of live properties
		final Set<URI> propertyURIRemovals = new HashSet<URI>(resourceAlteration.getPropertyURIRemovals()); //get the property URI removals, which we'll optimize based upon the property settings
		Map<WebDAVPropertyName, WebDAVProperty> properties = null; //we'll only retrieve properties if needed
		URFResource resourceDescription = null; //we'll only retrieve the existing resource description if needed
		boolean hasContentModifiedAddition = false; //we'll determine if the content modified properties is being added
		//determine the URF properties that should be added for each URF property
		final CollectionMap<URI, URFProperty, Set<URFProperty>> urfPropertyURIPropertyAdditions = new HashSetHashMap<URI, URFProperty>(); //create a map of sets of properties to add, keyed to their property URIs, so that we can find multiple property values for a single property if present
		for(final URFProperty propertyAddition : resourceAlteration.getPropertyAdditions()) //look at all the property additions
		{
			final URI propertyURI = propertyAddition.getPropertyURI(); //get the URI of the URF property
			if(!hasContentModifiedAddition && propertyURI.equals(MODIFIED_PROPERTY_URI)) //if we're setting the content modified property, later we'll need to write the synchronization last modified property
			{
				hasContentModifiedAddition = true;
			}
			if(!livePropertyURIs.contains(propertyURI)) //if this is not a live property
			{
				if(!resourceAlteration.getPropertyURIRemovals().contains(propertyURI)) //if a property addition was requested instead of a property setting (i.e. without first removing all the URI properties), we'll need to first gather the existing properties
				{
					if(resourceDescription == null) //if we don't yet have a description for the resource
					{
						final URF urf = createURF(); //create a new URF data model
						final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
						try
						{
							properties = webdavResource.propFind(); //get the properties of this resource
							resourceDescription = createResourceDescription(urf, resourceURI, properties); //create a resource from this URI and property list
						}
						finally
						{
							if(passwordAuthentication != null) //if we used password authentication
							{
								fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
							}
						}
					}
					for(final URFProperty existingProperty : resourceDescription.getProperties(propertyURI)) //gather the existing properties; we'll have to combine them all into one WebDAV property
					{
						urfPropertyURIPropertyAdditions.addItem(propertyURI, existingProperty); //indicate that this is another URF property to add for this property URI
					}
				}
				urfPropertyURIPropertyAdditions.addItem(propertyURI, propertyAddition); //indicate that this is another URF property to add/set for this property URI
				propertyURIRemovals.remove(propertyURI); //indicate that we don't have to  remove this property, because it will be removed by setting it
			}
		}
		WebDAVPropertyValue syncWebDAVGetLastContentModified = null;
		if(hasContentModifiedAddition) //if we're setting the content modified property, write the synchronization last modified property
		{
			if(resourceDescription == null) //if we don't yet have a description for the resource
			{
				final URF urf = createURF(); //create a new URF data model
				final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
				try
				{
					properties = webdavResource.propFind(); //get the properties of this resource
					resourceDescription = createResourceDescription(urf, resourceURI, properties); //create a resource from this URI and property list
				}
				finally
				{
					if(passwordAuthentication != null) //if we used password authentication
					{
						fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
					}
				}
			}
			final WebDAVProperty syncWebDAVGetLastContentModifiedProperty = properties.get(GET_LAST_MODIFIED_PROPERTY_NAME); //get the last WebDAV getlastmodified we know of
			if(syncWebDAVGetLastContentModifiedProperty != null) //if we know of a getlastmodified property
			{
				syncWebDAVGetLastContentModified = syncWebDAVGetLastContentModifiedProperty.getValue(); //get the value
			}
		}
		//convert the URF property additions to WebDAV properties
		final Set<WebDAVProperty> setWebDAVProperties = new HashSet<WebDAVProperty>(); //keep track of which WebDAV properties to set based upon the URF properties to add
		for(final Map.Entry<URI, Set<URFProperty>> urfPropertyURIPropertyAdditionEntries : urfPropertyURIPropertyAdditions.entrySet())
		{
			final Set<URFProperty> urfPropertyAdditions = urfPropertyURIPropertyAdditionEntries.getValue(); //get the URF properties to add
			final WebDAVProperty webdavProperty = createWebDAVProperty(resourceURI, urfPropertyAdditions); //create a WebDAV property and value for these URF property
			setWebDAVProperties.add(webdavProperty); //add this WebDAV property to the set of properties to set
		}
		if(syncWebDAVGetLastContentModified != null && getSyncWebDAVGetLastModifiedWebDAVPropertyName() != null) //if we have a synchronize property to write
		{
			setWebDAVProperties.add(new WebDAVProperty(getSyncWebDAVGetLastModifiedWebDAVPropertyName(), syncWebDAVGetLastContentModified)); //we'll update the last time we knew of a getlastmodified
		}
		//determine the WebDAV properties to remove
		final Set<WebDAVPropertyName> removeWebDAVPropertyNames = new HashSet<WebDAVPropertyName>(); //keep track of which WebDAV property names to remove
		for(final URI propertyURIRemoval : propertyURIRemovals) //look at all the property removals left after removing that which are irrelevant
		{
			final WebDAVPropertyName webdavPropertyName = createWebDAVPropertyName(propertyURIRemoval); //create a WebDAV property name from the URF property URI
			removeWebDAVPropertyNames.add(webdavPropertyName); //add this WebDAV property name to the set of property names to remove
		}
		//ignore removal of live properties
		for(final URI livePropertyURI : livePropertyURIs) //look at all live properties
		{
			final WebDAVPropertyName liveWebDAVPropertyName = createWebDAVPropertyName(livePropertyURI); //create a WebDAV property name for the live property URI
			removeWebDAVPropertyNames.remove(liveWebDAVPropertyName); //don't remove this property after all, as it's a live property
		}
		webdavResource.propPatch(removeWebDAVPropertyNames, setWebDAVProperties); //remove and set WebDAV properties
		final Map<WebDAVPropertyName, WebDAVProperty> newProperties = webdavResource.propFind(); //get the properties of this resource
		return createResourceDescription(createURF(), resourceURI, newProperties); //create a resource from this URI and property list
	}

	/**
	 * {@inheritDoc} This implementation throws a {@link ResourceNotFoundException} for all resource for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>false</code>.
	 */
	@Override
	protected void copyResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException
	{
		if(!isSourceResourceVisible(getSourceResourceURI(resourceURI))) //if this is not a visible resource
		{
			throw new ResourceNotFoundException(resourceURI);
		}
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			webdavResource.copy(getSourceResourceURI(destinationURI), overwrite); //copy the resource with an infinite depth, overwriting the destination resource only if requested
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**
	 * {@inheritDoc} This implementation throws a {@link ResourceNotFoundException} for all resource for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>false</code>.
	 */
	@Override
	protected void moveResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException
	{
		if(!isSourceResourceVisible(getSourceResourceURI(resourceURI))) //if this is not a visible resource
		{
			throw new ResourceNotFoundException(resourceURI);
		}
		final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
		try
		{
			final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
			webdavResource.move(getSourceResourceURI(destinationURI), overwrite); //move the resource with an infinite depth, overwriting the destination resource only if requested
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication != null) //if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**
	 * Creates a resource to represent this map of properties.
	 * @param urf The URF data model to use when creating this resource.
	 * @param resourceURI The URI of the resource being described.
	 * @param properties The map of WebDAV properties.
	 * @return A resource representing the given WebDAV properties.
	 * @throws NullPointerException if one or more of the provided properties has a value of <code>null</code>.
	 * @throws IOException if the resource description could not load addition needed data.
	 * @throws DataException if the data was not what was expected.
	 */
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, final Map<WebDAVPropertyName, WebDAVProperty> properties)
			throws IOException, DataException
	{
		final URFResource resource = urf.locateResource(resourceURI); //create a resource to represent the WebDAV property list
		final Set<String> ignoredWebDAVNamespaces = getIgnoredWebDAVNamespaces(); //get the map of ignored WebDAV namespaces
		boolean isCollection = false; //we'll detect if this is a collection base upon the properties TODO update to check to make sure collections have collection URIs and vice-versa
		final URFIO<URFResource> descriptionIO = getDescriptionIO(); //get I/O for the description

		final WebDAVProperty webdavDisplayNameProperty = properties.get(DISPLAY_NAME_PROPERTY_NAME); //D:displayname
		if(webdavDisplayNameProperty != null)
		{
			final WebDAVPropertyValue propertyValue = webdavDisplayNameProperty.getValue(); //get the value of the property
			if(propertyValue != null)
			{
				final String displayName = propertyValue.getText().trim(); //get the display name TODO just trim control characters (e.g. CR, LF), as we want users to be able to add whitespace
				resource.setLabel(displayName); //set the label as the display name of the WebDAV resource; this will get overridden by any custom names later			
			}
		}
		final WebDAVProperty webdavResourceTypeProperty = properties.get(RESOURCE_TYPE_PROPERTY_NAME); //D:resourcetype
		if(webdavResourceTypeProperty != null)
		{
			final WebDAVPropertyValue propertyValue = webdavResourceTypeProperty.getValue(); //get the value of the property
			if(propertyValue instanceof WebDAVDocumentFragmentPropertyValue) //if the WebDAV property represents a document fragment
			{
				final List<Element> valueElements = getChildElements(((WebDAVDocumentFragmentPropertyValue)propertyValue).getDocumentFragment()); //get the child elements of the document fragment
				if(valueElements.size() == 1 && COLLECTION_TYPE.equals(createQualifiedName(valueElements.get(0)).getURI())) //if there is one child element with a reference URI of D:collection
				{
					isCollection = true; //show that this is a collection
				}
			}
		}
		final WebDAVProperty webdavContentLanguageProperty = properties.get(GET_CONTENT_LANGUAGE_PROPERTY_NAME); //D:getcontentlanguage
		if(webdavContentLanguageProperty != null)
		{
			final WebDAVPropertyValue propertyValue = webdavContentLanguageProperty.getValue(); //get the value of the property
			if(propertyValue != null)
			{
				final Locale contentLanguage = createLocale(propertyValue.getText().trim()); //get the content language string and create a locale from it
				setLanguage(resource, contentLanguage); //set the dc:language to the language of the WebDAV resource			
			}
		}

		Map<WebDAVPropertyName, WebDAVProperty> contentProperties = properties; //if we have a special collection content resource, we'll use the properties from that
		if(isCollection) //if this is a collection
		{
			if(isCollectionURI(resourceURI))
			{
				final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
				try
				{
					final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
					final WebDAVResource contentWebDAVResource = new WebDAVResource(getSourceResourceURI(contentURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource for special collection content resource TODO cache these resources, maybe
					if(contentWebDAVResource.exists()) //if there is a special collection content resource
					{
						contentProperties = contentWebDAVResource.propFind(); //get the properties of the content file TODO only ask for the appropriate property if we can
					}
				}
				finally
				{
					if(passwordAuthentication != null) //if we used password authentication
					{
						fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
					}
				}
			}
		}
		final WebDAVProperty webdavContentLengthProperty = contentProperties.get(GET_CONTENT_LENGTH_PROPERTY_NAME); //get the D:getcontentlength from the content properties
		long contentLength = isCollection ? 0 : -1; //determine the content length; default to a content length of zero for collections
		if(webdavContentLengthProperty != null) //if we know a content length property
		{
			final WebDAVPropertyValue propertyValue = webdavContentLengthProperty.getValue(); //get the value of the property
			if(propertyValue != null)
			{
				final String contentLengthString = propertyValue.getText().trim(); //get the content length string
				try
				{
					contentLength = Long.parseLong(contentLengthString); //parse the content length
				}
				catch(final NumberFormatException numberFormatException) //if the content length is not a valid value
				{
					throw new DataException("Illegal WebDAV " + GET_CONTENT_LENGTH_PROPERTY_NAME.getLocalName() + " value: " + contentLengthString, numberFormatException);
				}
			}
		}
		if(contentLength >= 0) //if we know a valid content length
		{
			setContentLength(resource, contentLength); //set the content length to whatever we determined
		}
		if(isCollection) //if this is a collection
		{
			resource.removePropertyValues(Content.TYPE_PROPERTY_URI); //remove any content type properties (Apache mod_dav adds a "httpd/unix-directory" pseudo MIME type for collections, for example)
		}
		for(final WebDAVProperty webdavProperty : properties.values()) //now process the  set URF properties so that they will override the other properties
		{
			final WebDAVPropertyName propertyName = webdavProperty.getName(); //get the property name
			final String propertyNamespace = propertyName.getNamespace(); //get the string version of the property namespace
			final String propertyLocalName = propertyName.getLocalName(); //get the local name of the property
			final WebDAVPropertyValue propertyValue = webdavProperty.getValue(); //get the value of the property
			if(!ignoredWebDAVNamespaces.contains(propertyNamespace)) //if this is not a namespace to ignore
			{
				//Log.trace("looking at non-WebDAV property", propertyName);
				final URI urfPropertyURI = getURFPropertyURI(propertyName); //get the URI of the corresponding URF property, if any
				//Log.trace("URF property URI", urfPropertyURI);
				if(urfPropertyURI != null && propertyValue != null) //if there is a corresponding URF property and there is an actual value specified (URF does not define a null value) TODO fix null
				{

					if(propertyValue instanceof WebDAVLiteralPropertyValue) //if the value is a literal (we don't yet support complex WebDAV properties)
					{
						final String propertyTextValue = ((WebDAVLiteralPropertyValue)propertyValue).getText(); //get the text value of the property
						try
						{
							decodePropertiesTextValue(resource, urfPropertyURI, propertyTextValue); //decode the properties from the single text value and update the resource
						}
						catch(final IllegalArgumentException illegalArgumentException) //if the property text value wasn't encoded properly
						{
							//TODO fix								throw new DataException(ioException);
							Log.warn("Error parsing resource; removing", resourceURI, "property", urfPropertyURI, "with value", propertyTextValue, illegalArgumentException);
							//TODO eventually leave the bad property; for now, it's probably an anomaly from older development versions, so remove it
							final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //get authentication, if any
							try
							{
								final WebDAVResource webdavResource = new WebDAVResource(getSourceResourceURI(resourceURI), getHTTPClient(), passwordAuthentication); //create a WebDAV resource
								webdavResource.removeProperties(propertyName); //remove the bad properties
							}
							catch(final IOException ioException2) //if an I/O exception occurs
							{
								Log.error(ioException2); //just log the error; we shouldn't bring the application down over this
							}
							finally
							{
								if(passwordAuthentication != null) //if we used password authentication
								{
									fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
								}
							}

						}
					}
				}
			}
		}
		URFDateTime created = getCreated(resource); //try to determine the creation date and time; the stored creation time will always trump everything else
		/*TODO del automatic content.created update 
				if(created==null)	//if no creation time is specified
				{
					final WebDAVProperty srtCreationTimeProperty=properties.get(SRTWebDAV.DEPRECATED_CREATION_TIME_PROPERTY_NAME);	//check for the SRT creation date which, if set by WebDrive, will trump the WebDAV creation date
					if(srtCreationTimeProperty!=null)
					{
						final WebDAVPropertyValue propertyValue=srtCreationTimeProperty.getValue();	//get the value of the property
						if(propertyValue!=null)
						{
							final String creationDateString=propertyValue.getText();	//get the D:creationdate string
							try
							{
								created=URFDateTime.valueOfTimestamp(creationDateString);	//parse the creation date
							}
							catch(final IllegalArgumentException illegalArgumentException)	//if the SRT creation date does not have the correct syntax, just log a warning; the SRT properties shouldn't break things if they aren't correct
							{
								Log.warn("Invalid "+CREATION_DATE_PROPERTY_NAME.getLocalName()+" value: "+creationDateString, illegalArgumentException);
							}
						}
					}
					if(created==null)	//if there was no SRT creation date, use the WebDAV creation date
					{
						final WebDAVProperty webdavCreationDateProperty=properties.get(CREATION_DATE_PROPERTY_NAME);	//D:creationdate
						if(webdavCreationDateProperty!=null)
						{
							final WebDAVPropertyValue propertyValue=webdavCreationDateProperty.getValue();	//get the value of the property
							if(propertyValue!=null)
							{
								final String creationDateString=propertyValue.getText();
								try
								{
									created=URFDateTime.valueOfTimestamp(creationDateString);	//parse the creation date; the WebDAV D:creationdate property uses the RFC 3339 Internet timestamp ISO 8601 profile
								}
								catch(final IllegalArgumentException illegalArgumentException)	//if the creation date does not have the correct syntax
								{
									throw new DataException("Illegal WebDAV "+CREATION_DATE_PROPERTY_NAME.getLocalName()+" value: "+creationDateString, illegalArgumentException);
								}
							}
						}
					}
					if(created!=null)	//if we know the created date and time
					{
						setCreated(resource, created);	//set the created date time
					}
				}
		*/
		URFDateTime webdavGetLastModified = null; //try to determine the modified date and time as WebDAV reports it
		final WebDAVProperty webdavLastModifiedProperty = contentProperties.get(GET_LAST_MODIFIED_PROPERTY_NAME); //D:getlastmodified
		if(webdavLastModifiedProperty != null)
		{
			final WebDAVPropertyValue propertyValue = webdavLastModifiedProperty.getValue(); //get the value of the property
			if(propertyValue != null)
			{
				final String lastModifiedDateString = propertyValue.getText().trim(); //get the last modified date string
				try
				{
					final DateFormat httpDateFormat = new HTTPDateFormat(HTTPDateFormat.Style.RFC1123); //create an HTTP date formatter; the WebDAV D:getlastmodified property prefers the RFC 1123 style, as does HTTP
					webdavGetLastModified = new URFDateTime(httpDateFormat.parse(lastModifiedDateString)); //parse the last modified date
				}
				catch(final java.text.ParseException parseException) //if the last modified time is not the correct type
				{
					throw new DataException("Illegal WebDAV " + GET_LAST_MODIFIED_PROPERTY_NAME.getLocalName() + " value: " + lastModifiedDateString, parseException);
				}
			}
		}
		URFDateTime modified = getModified(resource); //try to determine the modified date and time; the stored modified time will always trump everything else, unless the time is out of sync
		if(modified != null && getSyncWebDAVGetLastModifiedWebDAVPropertyName() != null) //if we have an explicit modified date and time, verify that the file hasn't changed externally to Marmot
		{
			final WebDAVProperty syncWebDAVGetLastModifiedProperty = contentProperties.get(getSyncWebDAVGetLastModifiedWebDAVPropertyName()); //check sync modified time to see if we should use our modified value
			if(syncWebDAVGetLastModifiedProperty != null)
			{
				final WebDAVPropertyValue syncWebDAVGetLastModifiedPropertyValue = syncWebDAVGetLastModifiedProperty.getValue(); //get the value of the property
				if(syncWebDAVGetLastModifiedPropertyValue != null)
				{
					final String syncLastModifiedDateString = syncWebDAVGetLastModifiedPropertyValue.getText().trim(); //get the last modified date string
					try
					{
						final DateFormat httpDateFormat = new HTTPDateFormat(HTTPDateFormat.Style.RFC1123); //create an HTTP date formatter; the WebDAV D:getlastmodified property prefers the RFC 1123 style, as does HTTP
						final Date syncLastModifiedDate = httpDateFormat.parse(syncLastModifiedDateString); //parse the last modified date
						if(syncLastModifiedDate.compareTo(webdavGetLastModified) < 0) //if the sync record of the last modified time is less than what it is now
						{
							modified = null; //don't use the explicit modified date and time; the resource must have been modified since we modified it
						}
					}
					catch(final java.text.ParseException parseException) //if the last modified time is not the correct type
					{
						throw new DataException("Illegal WebDAV " + GET_LAST_MODIFIED_PROPERTY_NAME.getLocalName() + " value: " + syncLastModifiedDateString,
								parseException);
					}
				}
			}
		}
		if(modified == null) //if no modification date is indicated
		{
			if(webdavGetLastModified != null) //if there is a WebDAV last modified time
			{
				modified = webdavGetLastModified; //use the WebDAV last modified time unless we need to modify it
			}
			URFDateTime srtModified = null; //try to parse the SRT modified date
			final WebDAVProperty srtModifiedTimeProperty = contentProperties.get(SRTWebDAV.DEPRECATED_MODIFIED_TIME_PROPERTY_NAME); //check for the SRT modification date which may override the modified date return by WebDrive
			if(srtModifiedTimeProperty != null)
			{
				final WebDAVPropertyValue srtModifiedTimePropertyValue = srtModifiedTimeProperty.getValue(); //get the value of the property
				if(srtModifiedTimePropertyValue != null)
				{
					final String srtModifiedTimeString = srtModifiedTimePropertyValue.getText();
					try
					{
						srtModified = URFDateTime.valueOfTimestamp(srtModifiedTimeString); //parse the SRT modified date
						if(modified != null) //if there is a WebDAV modified time, make sure we should override it
						{
							final WebDAVProperty srtTimestampProperty = contentProperties.get(SRTWebDAV.DEPRECATED_TIMESTAMP_PROPERTY_NAME); //check for the SRT timestamp to see if we should use the SRT modified value
							if(srtTimestampProperty != null)
							{
								final WebDAVPropertyValue srtTimestampPropertyValue = srtTimestampProperty.getValue(); //get the value of the property
								if(srtTimestampPropertyValue != null)
								{
									final String srtTimestampString = srtTimestampPropertyValue.getText();
									try
									{
										final URFDateTime srtTimestamp = URFDateTime.valueOfTimestamp(srtTimestampString); //parse the SRT timestamp
										if(srtTimestamp.compareTo(modified) < 0) //if the SRT record of the last modified time is less than what it is now
										{
											srtModified = null; //don't use the SRT modified date and time; the resource must have been modified since WebDrive modified it
										}
									}
									catch(final IllegalArgumentException illegalArgumentException) //if the SRT timestamp does not have the correct syntax, just log a warning; the SRT properties shouldn't break things if they aren't correct
									{
										Log.warn("Invalid " + SRTWebDAV.DEPRECATED_TIMESTAMP_PROPERTY_NAME.getLocalName() + " value: " + srtTimestampString,
												illegalArgumentException);
									}
								}
							}
						}
					}
					catch(final IllegalArgumentException illegalArgumentException) //if the SRT modification date does not have the correct syntax, just log a warning; the SRT properties shouldn't break things if they aren't correct
					{
						Log.warn("Invalid " + SRTWebDAV.DEPRECATED_MODIFIED_TIME_PROPERTY_NAME.getLocalName() + " value: " + srtModifiedTimeString,
								illegalArgumentException);
					}
				}
			}
			if(srtModified != null) //if the SRT modified time should override the WebDAV modified time, if any
			{
				modified = srtModified; //override the WebDAV modified time, if any
			}
			if(modified != null) //if we know the last modified time
			{
				setModified(resource, modified); //set the modified date time
			}
		}

		/*TODO del
				if(!isCollection)	//if this is not a collection, try to get the content type of the resource if it wasn't specified already
				{
					updateContentType(resource);	//update the content type information based upon the repository defaults
				}
		*/
		//TODO fix filename encoding/decoding---there's no way to know what operating system the server is using

		//TODO encode in UTF-8
		//		Log.trace("ready to return resource description:", URF.toString(resource));
		return resource; //return the resource that represents the file
	}

	/**
	 * {@inheritDoc} This version makes the following translations:
	 * <dl>
	 * <dt>{@link HTTPForbiddenException}</dt>
	 * <dd>{@link ResourceForbiddenException}</dd>
	 * <dt>{@link HTTPNotFoundException}</dt>
	 * <dd>{@link ResourceNotFoundException}</dd>
	 * <dt>{@link HTTPRedirectException}</dt>
	 * <dd>{@link ResourceNotFoundException}</dd>
	 * <dt>{@link HTTPPreconditionFailedException}</dt>
	 * <dd>{@link ResourceStateException}</dd>
	 * </dl>
	 */
	@Override
	protected ResourceIOException toResourceIOException(final URI resourceURI, final Throwable throwable)
	{
		if(throwable instanceof HTTPForbiddenException)
		{
			return new ResourceForbiddenException(resourceURI, throwable);
		}
		else if(throwable instanceof HTTPNotFoundException)
		{
			return new ResourceNotFoundException(resourceURI, throwable);
		}
		else if(throwable instanceof HTTPRedirectException)
		{
			return new ResourceNotFoundException(resourceURI, throwable);
		}
		else if(throwable instanceof HTTPPreconditionFailedException)
		{
			return new ResourceStateException(resourceURI, throwable);
		}
		else
		//if this is not one of our specially-handled exceptions
		{
			return super.toResourceIOException(resourceURI, throwable); //convert the exception normally
		}
	}

	/** {@inheritDoc} This version calls clears and releases the password, if any. */
	@Override
	public synchronized void dispose()
	{
		try
		{
			super.dispose();
		}
		finally
		{
			if(password != null) //if we have a password
			{
				fill(password, (char)0); //erase the password from memory as a security measure
				password = null; //release the password
			}
		}
	}

	/**
	 * Creates an output stream that updates the properties of a WebDAV resource after its contents are stored. The password of the given password authentication
	 * object, if any, will be erased after updating the properties of the WebDAV resource.
	 * @author Garret Wilson
	 */
	protected class DescriptionWriterOutputStreamDecorator extends OutputStreamDecorator<OutputStream>
	{
		/** The URI of the resource. */
		private final URI resourceURI;

		/** @protected The URI of the resource. */
		public URI getResourceURI()
		{
			return resourceURI;
		}

		/** The specification of the alterations to be performed on the resource. */
		private final URFResourceAlteration resourceAlteration;

		/** @return The specification of the alterations to be performed on the resource. */
		protected URFResourceAlteration getResourceAlteration()
		{
			return resourceAlteration;
		}

		/** The WebDAV resource for updating the WebDAV properties. */
		private final WebDAVResource webdavResource;

		/** @return The WebDAV resource for updating the WebDAV properties. */
		protected WebDAVResource getWebDAVResource()
		{
			return webdavResource;
		}

		/** The password authentication being used, or <code>null</code> if no password authentication is given. */
		private final PasswordAuthentication passwordAuthentication;

		/** @return The password authentication being used, or <code>null</code> if no password authentication is given. */
		protected PasswordAuthentication getPasswordAuthentication()
		{
			return passwordAuthentication;
		}

		/**
		 * Decorates the given output stream.
		 * @param outputStream The output stream to decorate
		 * @param resourceURI The URI of the resource.
		 * @param resourceAlteration The specification of the alterations to be performed on the resource.
		 * @param webdavResource The WebDAV resource for updating the WebDAV properties.
		 * @param passwordAuthentication The password authentication being used, or <code>null</code> if no password authentication is given.
		 * @throws NullPointerException if the given output stream, resourceURI, resource alteration, and/or WebDAV resource is <code>null</code>.
		 */
		public DescriptionWriterOutputStreamDecorator(final OutputStream outputStream, final URI resourceURI, final URFResourceAlteration resourceAlteration,
				final WebDAVResource webdavResource, final PasswordAuthentication passwordAuthentication)
		{
			super(outputStream); //construct the parent class
			this.resourceURI = checkInstance(resourceURI, "Resource URI cannot be null.");
			this.resourceAlteration = checkInstance(resourceAlteration, "Resource alteration cannot be null.");
			this.webdavResource = checkInstance(webdavResource, "WebDAV resource cannot be null.");
			this.passwordAuthentication = passwordAuthentication;
		}

		/**
		 * Called after the stream is successfully closed. This version updates the WebDAV properties to reflect the given resource description.
		 * @throws ResourceIOException if an I/O error occurs.
		 */
		protected void afterClose() throws ResourceIOException
		{
			try
			{
				alterResourceProperties(getResourceURI(), getResourceAlteration(), getWebDAVResource()); //alter the properties using the WebDAV resource object
			}
			catch(final IOException ioException) //if an I/O exception occurs
			{
				throw toResourceIOException(getResourceURI(), ioException); //translate the exception to a resource I/O exception and throw that
			}
			catch(final DataException dataException) //if the data wasn't correct
			{
				throw toResourceIOException(getResourceURI(), dataException); //translate the exception to a resource I/O exception and throw that
			}
			finally
			{
				final PasswordAuthentication passwordAuthentication = getPasswordAuthentication(); //see if we were given password authentication information
				if(passwordAuthentication != null) //if we used password authentication
				{
					fill(passwordAuthentication.getPassword(), (char)0); //always erase the password from memory as a security measure when we're done with the authentication object
				}
			}
		}

	}

}

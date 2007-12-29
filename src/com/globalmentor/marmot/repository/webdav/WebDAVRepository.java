package com.globalmentor.marmot.repository.webdav;

import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.util.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;

import javax.mail.internet.*;

import com.garretwilson.io.*;

import static com.garretwilson.lang.ObjectUtilities.*;

import com.garretwilson.lang.Strings;
import com.garretwilson.net.*;
import static com.garretwilson.net.URIs.*;
import com.garretwilson.net.http.*;
import com.garretwilson.net.http.webdav.*;
import static com.garretwilson.net.http.webdav.ApacheWebDAV.*;
import static com.garretwilson.net.http.webdav.WebDAVConstants.*;
import static com.garretwilson.text.xml.XMLUtilities.*;
import static com.garretwilson.text.CharacterEncodingConstants.*;
import com.garretwilson.urf.*;

import static com.garretwilson.urf.TURF.*;
import static com.garretwilson.urf.URF.*;
import com.garretwilson.urf.content.*;
import static com.garretwilson.urf.content.Content.*;
import static com.garretwilson.urf.dcmi.DCMI.*;
import com.garretwilson.util.*;
import static com.garretwilson.util.LocaleUtilities.*;

import com.globalmentor.marmot.repository.AbstractRepository;

import org.w3c.dom.*;

/**Repository accessed via WebDAV.
<p>This repository recognizes the URF type <code>urf.List</code> and creates a collection for each such resource.</p>
<p>URF properties are stored as normal WebDAV properties, except that the value is a TURF interchange document beginning with the TURF signature {@value TURF#TURF_SIGNATURE},
	and within the instance are one or more URF resources values of the property are stored.
	If an URF property has no namespace, a WebDAV property name is formed using the URF property URI as the namespace and the string {@value #URF_TOKEN_LOCAL_NAME}
	as a local name, because WebDAV requires that each property have a separate namespace and local name.</p>
@author Garret Wilson
*/
public class WebDAVRepository extends AbstractRepository
{

		//TODO the current technique of erasing the password after each call may become obsolete when the HTTP client supports persistent connections
	
	/**The extension used for directories to hold resource children.*/
//TODO move if needed	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/**The name of the WebDAV property that holds the description of a resource.*/
//TODO del if not needed	private final static WebDAVPropertyName RESOURCE_DESCRIPTION_PROPERTY_NAME=new WebDAVPropertyName(MARMOT_NAMESPACE_URI.toString(), "description");

	/**Determines the WebDAV property that holds the description of a resource.
	This version returns {@value #RESOURCE_DESCRIPTION_PROPERTY_NAME}.
	@return The WebDAV property to hold a resource description.
	*/
/*TODO del
	protected WebDAVPropertyName getResourceDescriptionPropertyName()
	{
		return RESOURCE_DESCRIPTION_PROPERTY_NAME;
	}
*/

	/**The WebDAV namespaces that are not automatically added as URF properties, although some properties in these namespaces may be explicitly used.*/
	private final Set<String> ignoredWebDAVNamespaces=new HashSet<String>();

		/**Returns the WebDAV namespaces that are not automatically added as URF properties.
		Some properties in these namespaces may be explicitly used.
		The returned map is not thread-safe; it can be used for reading, but should not be modified after repository construction.
		@return The WebDAV namespaces that are not automatically added as URF properties.
		*/
		protected Set<String> getIgnoredWebDAVNamespaces() {return ignoredWebDAVNamespaces;}

	/**Repository URI contructor using the default HTTP client.
	The given repository URI should end in a slash.
	@param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	*/
	public WebDAVRepository(final URI repositoryURI)
	{
		this(repositoryURI, HTTPClient.getInstance());	//construct the class using the default HTTP client		
	}
	
	/**Repository URI and HTTP client contructor.
	The given repository URI should end in a slash.
	@param repositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	@param httpClient The HTTP client used to create a connection to this resource.	
	*/
	public WebDAVRepository(final URI repositoryURI, final HTTPClient httpClient)
	{
		this(repositoryURI, repositoryURI, httpClient);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI contructor using the default HTTP client.
	The given private repository URI should end in a slash.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	*/
	public WebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		this(publicRepositoryURI, privateRepositoryURI, HTTPClient.getInstance());	//construct the class using the default HTTP client				
	}

	/**Public repository URI, private repository URI, and HTTP client contructor.
	The given private repository URI should end in a slash.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The WebDAV URI identifying the base URI of the WebDAV repository.
	@param httpClient The HTTP client used to create a connection to this resource.	
	*/
	public WebDAVRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final HTTPClient httpClient)
	{
		super(publicRepositoryURI, privateRepositoryURI);	//construct the parent class
		this.httpClient=httpClient;	//save the HTTP client
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO=(URFResourceTURFIO<URFResource>)getDescriptionIO();	//get the description I/O
		urfResourceDescriptionIO.setBOMWritten(false);	//turn off BOM generation
		urfResourceDescriptionIO.setFormatted(false);	//turn off formatting
		getIgnoredWebDAVNamespaces().add(APACHE_WEBDAV_PROPERTY_NAMESPACE_URI.toString());	//by default ignore properties in the Apache WebDAV namespace
	}

	/**A token local name for WebDAV for use with URF properties that have no local name.*/
	private final static String URF_TOKEN_LOCAL_NAME="urfTokenLocalName";
	
	/**Determines the WebDAV property name to represent an URF property.
	<p>If an URF property has no namespace, this implementation forms a WebDAV property name using the URF property URI as the namespace and the string {@value #URF_TOKEN_LOCAL_NAME}
	as a local name, because WebDAV requires that each property have a separate namespace and local name.</p>
	@param urfPropertyURI The URI of the URF property to represent.
	@return A WebDAV property name to use in representing an URF property with the given URF property URI.
	@see #URF_TOKEN_LOCAL_NAME
	*/
	protected WebDAVPropertyName getWebDAVPropertyName(final URI urfPropertyURI)
	{
		final String webDAVPropertyNamespace;
		final String webDAVPropertyLocalName;
		final String rawFragment=urfPropertyURI.getRawFragment();	//get the raw fragment of the URF property URI
		if(rawFragment!=null)	//if the URI has a fragment
		{
			final String urfPropertyURIString=urfPropertyURI.toString();	//get the string representation of the URF property URI
			assert urfPropertyURIString.endsWith(rawFragment);
			webDAVPropertyNamespace=urfPropertyURIString.substring(0, urfPropertyURIString.length()-rawFragment.length());	//remove the raw fragment, but leave the fragment identifier on the namespaces
			webDAVPropertyLocalName=rawFragment;	//the raw fragment itself is the WebDAV local name
		}
		else	//check for a path-based namespace
		{
			final URI urfPropertyNamespaceURI=getNamespaceURI(urfPropertyURI);	//get the normal URF namespace for path-based namespaces
			if(urfPropertyNamespaceURI!=null)	//if there is an URF namespace
			{
				webDAVPropertyNamespace=urfPropertyNamespaceURI.toString();	//the WebDAV namespace is the string form of the URF namespace URI
				webDAVPropertyLocalName=getRawName(urfPropertyURI);	//the raw name of the URF property URI is the WebDAV property local name
			}
			else	//if there is no URF namespace
			{
				webDAVPropertyNamespace=urfPropertyURI.toString();	//use the string form of the property URI as the namespace
				webDAVPropertyLocalName=URF_TOKEN_LOCAL_NAME;	//create a fake local name; we have to have some WebDAV property to correspond to the URF property, and there are no other options at this point				
			}
		}
		return new WebDAVPropertyName(webDAVPropertyNamespace, webDAVPropertyLocalName);	//create and return a new WebDAV property name from the components we determined
	}

	/**Determines the URF property to represent the given WebDAV property if possible.
	<p>If the WebDAV property has a local name of {@value #URF_TOKEN_LOCAL_NAME}, the URI of its namespace will be used as the URF property URI.</p>
	<p>This method returns <code>null</code> for all WebDAV properties in the {@value WebDAVConstants#WEBDAV_NAMESPACE} namespace.</p> 
	@param webdavPropertyName The name of the WebDAV property.
	@return The URI of the URF property to represent the given WebDAV property, or <code>null</code> if the given WebDAV property cannot be represented in URF.
	@see #URF_TOKEN_LOCAL_NAME
	*/
	protected URI getURFPropertyURI(final WebDAVPropertyName webdavPropertyName)
	{
		final String webdavPropertyNamespace=webdavPropertyName.getNamespace();	//get the property namespace
		if(WEBDAV_NAMESPACE.equals(webdavPropertyNamespace))	//ignore the WebDAV namespace
		{
			return null;	//the WebDAV namespace isn't a valid URI, anyway
		}
		try
		{
			final URI webdavPropertyNamespaceURI=new URI(webdavPropertyNamespace);	//get the property namespace URI
			final String webdavPropertyLocalName=webdavPropertyName.getLocalName();	//get the property local name
				//if the local name is just a token local name, the namespace is the real URF property URI
//TODO del when works			return URF_TOKEN_LOCAL_NAME.equals(webdavPropertyLocalName) ? webdavPropertyNamespaceURI : createResourceURI(webdavPropertyNamespaceURI, webdavPropertyLocalName);
			return URF_TOKEN_LOCAL_NAME.equals(webdavPropertyLocalName) ? webdavPropertyNamespaceURI : webdavPropertyName.getURI();
		}
		catch(final URISyntaxException uriSyntaxException)	//if the namespace is not a valid URI, this is not a valid URF property
		{
			return null;	//there is no way to represent this property in URF
		}
	}

	/**The HTTP client used to create a connection to this resource.*/
	private final HTTPClient httpClient;

		/**@return The HTTP client used to create a connection to this resource.*/
		protected HTTPClient getHTTPClient() {return httpClient;}

	/**The username to use in accessing the repository, or <code>null</code> if no username is specified.*/
	private String username=null;

		/**@return The username to use in accessing the repository, or <code>null</code> if no username is specified.*/
		public String getUsername() {return username;}

		/**Sets the username to use in accessing the repository.
		@param username The username to use in accessing the repository, or <code>null</code> if no username is specified.
		*/
		public void setUsername(final String username) {this.username=username;}

	/**The password to use in accessing the repository, or <code>null</code> if no password is specified.*/
	private char[] password=null;

		/**@return The username to use in accessing the repository, or <code>null</code> if no password is specified.*/
		public char[] getPassword() {return password;}

		/**Sets the password to use in accessing the repository.
		@param password The password to use in accessing the repository, or <code>null</code> if no password is specified.
		*/
		public void setPassword(final char[] password) {this.password=password;}

	/**Returns whatever password authentication should be used when communicating with a resource.
	@return A password authentication object with the repository's username and password, or <code>null</code> if no username and password are specified.
	@see #getUsername()
	@see #getPassword()
	*/
	protected PasswordAuthentication getPasswordAuthentication()
	{
		final String username=getUsername();	//get the username
		final char[] password=getPassword();	//get the password
		return username!=null && password!=null ? new PasswordAuthentication(username, password) : null;	//return new password authentication if this information is available
	}
	
	/**Gets an input stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getResourceInputStream(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return webdavResource.getInputStream();	//return an input stream to the resource
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}
	
	/**Gets an output stream to the contents of the resource specified by the given URI.
	An error is generated if the resource does not exist.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource TODO cache these resources, maybe
			if(!webdavResource.exists())	//if the resource doesn't already exist
			{
				throw new ResourceNotFoundException(resourceURI, "Cannot open output stream to non-existent resource "+resourceURI);
			}
			return webdavResource.getOutputStream();	//return an output stream to the resource
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}
	
	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URFResource getResourceDescription(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final URF urf=createURF();	//create a new URF data model
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final List<WebDAVProperty> propertyList=webdavResource.propFind();	//get the properties of this resource
			return createResourceDescription(urf, resourceURI, propertyList);	//create a resource from this URI and property list
		}
		catch(final DataException dataException)	//if the data wasn't correct
		{
			throw createResourceIOException(resourceURI, dataException);	//translate the exception to a resource I/O exception and throw that
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Determines if the resource at the given URI exists.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean resourceExists(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return webdavResource.exists();	//see if the WebDAV resource exists		
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Determines if the resource at a given URI is a collection.
	This is a convenience method to quickly determine if a resource exists at the given URI
	and retrieving that resource would result in a resource of type <code>file:Folder</code>.
	@param resourceURI The URI of the requested resource.
	@return <code>true</code> if the resource is a collection, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isCollection(final URI resourceURI) throws ResourceIOException
  {
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return webdavResource.isCollection();	//see if the WebDAV resource is a collection		
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
  }

	/**Determines whether the resource represented by the given URI has children.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final URI privateResourceURI=getPrivateURI(resourceURI);	//get the URI of the resource in the private namespace
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(privateResourceURI, getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final List<NameValuePair<URI, List<WebDAVProperty>>> propertyLists=webdavResource.propFind(Depth.ONE);	//get the properties of the resources one level down
			for(final NameValuePair<URI, List<WebDAVProperty>> propertyList:propertyLists)	//look at each property list
			{
				if(!privateResourceURI.equals(propertyList.getName()))	//if this property list is *not* for this resource
				{
					return true;	//this resource has children
				}
			}
			return false;	//no properties could be found for any children
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resources descriptions directly under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		if(depth!=0)	//a depth of zero means don't get child resources
		{
			final URI privateResourceURI=getPrivateURI(resourceURI);	//get the URI of the resource in the private namespace
			final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
			try
			{
				final WebDAVResource webdavResource=new WebDAVResource(privateResourceURI, getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
				final Depth webdavDepth;	//we'll get the depth based upon the value passed
				try
				{
					webdavDepth=depth==-1 ? Depth.INFINITY : Depth.values()[depth];	//get the depth based upon the value passed
				}
				catch(final IndexOutOfBoundsException indexOutOfBoundsException)	//if an illegal depth was passed
				{
					throw new IllegalArgumentException(Integer.toString(depth));	//TODO later convert the depth by using infinity and checking the result
				}
				final URF urf=createURF();	//create a new URF data model
				final List<NameValuePair<URI, List<WebDAVProperty>>> propertyLists=webdavResource.propFind(webdavDepth);	//get the properties of the resources
				final List<URFResource> childResourceList=new ArrayList<URFResource>(propertyLists.size());	//create a list of child resources no larger than the number of WebDAV resource property lists
				for(final NameValuePair<URI, List<WebDAVProperty>> propertyList:propertyLists)	//look at each property list
				{
					final URI childResourcePrivateURI=propertyList.getName();
					if(!privateResourceURI.equals(childResourcePrivateURI))	//if this property list is *not* for this resource
					{
						childResourceList.add(createResourceDescription(urf, getPublicURI(childResourcePrivateURI), propertyList.getValue()));	//create a resource from this URI and property lists
					}
				}
	//TODO do the special Marmot thing about checking for special Marmot directories
				
	//TODO fix				Collections.sort(resourceList);	//sort the resource by URI
				return childResourceList;	//return the list of resources we constructed
			}
			catch(final DataException dataException)	//if the data wasn't correct
			{
				throw createResourceIOException(resourceURI, dataException);	//translate the exception to a resource I/O exception and throw that
			}
			catch(final IOException ioException)	//if an I/O exception occurs
			{
				throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
			}
			finally
			{
				if(passwordAuthentication!=null)	//if we used password authentication
				{
					fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
				}
			}
		}
		else	//if a depth of zero was requested
		{
			return emptyList();	//return an empty list
		}
	}

	/**Creates a new resource with the given description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
	If a resource with no contents is desired, {@link #createResource(URI, URFResource, byte[])} with zero bytes is better suited for this task.
	This implementation updates the resource description after its contents are stored.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final OutputStream outputStream=webdavResource.getOutputStream();	//get an output stream to the WebDAV resource
			return new DescriptionWriterOutputStreamDecorator(outputStream, resourceURI, resourceDescription, webdavResource, passwordAuthentication);	//wrap the output stream in a decorator that will update the WebDAV properties after the contents are stored; this method will erase the provided password, if any, after it completes the resource property updates
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Creates a new resource with the given description and contents.
	If a resource already exists at the given URI it will be replaced.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.put(resourceContents);	//create a WebDAV resource with the guven contents
			return getResourceDescription(resourceURI);	//return a description of the new resource
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Creates a collection in the repository.
	@param collectionURI The URI of the collection to be created.
	@return A description of the collection that was created.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error creating the collection.
	*/
	public URFResource createCollection(final URI collectionURI) throws ResourceIOException
	{
		checkResourceURI(collectionURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(collectionURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.mkCol();	//create the collection
			return getResourceDescription(collectionURI);	//return a description of the new collection
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(collectionURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Sets the properties of a resource based upon the given description.
	This version delegates to {@link #setResourceProperties(URI, URFResource, WebDAVResource)}.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource setResourceProperties(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return setResourceProperties(resourceURI, resourceDescription, webdavResource);	//set the properties using the WebDAV resource object
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Sets the properties of a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified.
	@param resourceURI The reference URI of the resource.
	@param properties The properties to set.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or properties is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource setResourceProperties(final URI resourceURI, final URFProperty... properties) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final Set<URI> newPropertyURISet=new HashSet<URI>();	//create a set to find out which properties we will be setting
		for(final URFProperty property:properties)	//look at each property
		{
			newPropertyURISet.add(property.getPropertyURI());	//add this property URI to our set
		}
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final URFIO<URFResource> descriptionIO=getDescriptionIO();	//get I/O for the description
			final Set<WebDAVProperty> setProperties=new HashSet<WebDAVProperty>();	//create a set of properties to set
			for(final URFProperty property:properties)	//for each property to set
			{
				final URI propertyURI=property.getPropertyURI();	//get the URI of the URF property
				final WebDAVPropertyName webdavPropertyName=getWebDAVPropertyName(propertyURI);	//create a WebDAV property name from the URF property URI
				final URFResource propertyDescription=new DefaultURFResource(resourceURI);	//create a new resource description just for this property
				propertyDescription.addProperty(property);	//add this property to the resource
				final String webdavPropertyStringValue=Strings.write(resourceURI, propertyDescription, descriptionIO, UTF_8);	//write the description to a string, using the resource URI as the base URI
				final WebDAVPropertyValue webdavPropertyValue=new WebDAVLiteralPropertyValue(webdavPropertyStringValue);	//create a WebDAV literal property value from the determined string
				setProperties.add(new WebDAVProperty(webdavPropertyName, webdavPropertyValue));	//add the WebDAV property with the its value
			}
			webdavResource.setProperties(setProperties);	//patch the properties of the resource
			return getResourceDescription(resourceURI);	//retrieve and return a new description of the resource
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}		
	}

	/**Removes properties from a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified. 
	@param resourceURI The reference URI of the resource.
	@param propertyURIs The properties to remove.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or property URIs is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource removeResourceProperties(final URI resourceURI, final URI... propertyURIs) throws ResourceIOException
	{
		final Set<URI> propertyURISet=new HashSet<URI>();	//create a set to find out which properties we will be setting
		addAll(propertyURISet, propertyURIs);	//create a set of properties to remove
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final List<WebDAVProperty> oldPropertyList=webdavResource.propFind();	//get the original properties of this resource
			final Set<WebDAVPropertyName> removePropertyNames=getWebDAVPropertyNames(oldPropertyList, propertyURISet);	//get the WebDAV property names that will need to be removed
			webdavResource.removeProperties(removePropertyNames);	//remove the designated properties
			return getResourceDescription(resourceURI);	//retrieve and return a new description of the resource
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}		
	}

	/**Determines the WebDAV property names of an existing list of WebDAV properties that correspond to a set of URF property URIs.
	This is useful for deleting existing URF properties.
	@param webdavProperties The existing WebDAV properties.
	@param propertyURIs The set of URF property URIs to match.
	@return The set of names of WebDAV properties that match the given URF property URIs.
	*/
	protected Set<WebDAVPropertyName> getWebDAVPropertyNames(final List<WebDAVProperty> webdavProperties, final Set<URI> urfPropertyURIs)
	{
		final Set<WebDAVPropertyName> webdavPropertyNames=new HashSet<WebDAVPropertyName>();	//create a set to find out how many matching WebDAV properties there are
		for(final WebDAVProperty webdavProperty:webdavProperties)	//look at each of the WebDAV properties
		{
			final WebDAVPropertyName webdavPropertyName=webdavProperty.getName();	//get the property namespace
			final URI urfPropertyURI=getURFPropertyURI(webdavPropertyName);	//get the URI of the corresponding URF property, if any
			if(urfPropertyURI!=null && urfPropertyURIs.contains(urfPropertyURI))	//if there is a valid URF property and the property is one of the URF properties in question
			{
				webdavPropertyNames.add(webdavPropertyName);	//this WebDAV property matches
			}
		}
		return webdavPropertyNames;	//return the WebDAV property names we collected		
	}

	/**Sets the properties of a resource based upon the given description.
	This implementation only supports the {@value Content#MO} property, ignoring all other properties.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@param webdavResource The WebDAV resource for setting the resource WebDAV properties
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	protected URFResource setResourceProperties(final URI resourceURI, final URFResource resourceDescription, final WebDAVResource webdavResource) throws ResourceIOException
	{
		final URFDateTime modifiedDateTime=getModified(resourceDescription);	//get the modified date time designation, if there is one
		if(modifiedDateTime!=null)	//if there is a modified date time designated
		{
/*TODO fix
			final String modifiedTimeString=new W3CDateFormat(W3CDateFormat.Style.DATE_TIME).format(modifiedTime);	//create a string with the modified time TODO eventually just copy over all RDF properties
			final Element element=createPropertyupdateDocument(createWebDAVDocumentBuilder().getDOMImplementation()).getDocumentElement();	//create a propertyupdate document (it doesn't really matter what type of document we create)	//TODO check DOM exceptions here
			appendText(element, modifiedTimeString);	//append the modified time to the document element
			final DocumentFragment modifiedTimeDocumentFragment=extractChildren(element);	//extract the children into a document element
			
			
			webdavResource.propPatch(asList(new WebDAVProperty(new QualifiedName(FILE_ONTOLOGY_NAMESPACE_URI, FILE_ONTOLOGY_NAMESPACE_PREFIX, MODIFIED_TIME_PROPERTY_NAME), )));
			//TODO make a general method for property patching, maybe; update AbstractWebDAVServlet to store properties
*/
			
//TODO fix			resourceFile.setLastModified(modifiedTime.getTime());	//update the last modified time TODO does this work for directories? should we check?
		}
		return getResourceDescription(resourceURI);	//return the new resource description
	}

	/**Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destionation only if requested. 
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.copy(getPrivateURI(destinationURI), overwrite);	//copy the resource with an infinite depth, overwriting the destination resource only if requested
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Deletes a resource.
	@param resourceURI The reference URI of the resource to delete.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if the resource could not be deleted.
	*/
	public void deleteResource(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		if(resourceURI.normalize().equals(getPublicRepositoryURI()))	//if they try to delete the root URI
		{
			throw new IllegalArgumentException("Cannot delete repository base URI "+resourceURI);
		}
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.delete();	//delete the resource		
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}

	/**Moves a resource to another URI in this repository, overwriting any resource at the destionation only if requested.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		if(resourceURI.normalize().equals(getPublicRepositoryURI()))	//if they try to move the root URI
		{
			throw new IllegalArgumentException("Cannot move repository base URI "+resourceURI);
		}
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.move(getPrivateURI(destinationURI), overwrite);	//move the resource with an infinite depth, overwriting the destination resource only if requested
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		finally
		{
			if(passwordAuthentication!=null)	//if we used password authentication
			{
				fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
			}
		}
	}
	
	/**Creates a resource to represent this list of properties.
	@param urf The URF data model to use when creating this resource.
	@param referenceURI The reference URI of the property to create.
	@param propertyList The list of property qualified names paired with WebDAV property values.
	@return A resource representing the given WebDAV property list.
	@exception NullPointerException if one or more of the provided properties has a value of <code>null</code>.
	@exception ResourceIOException if there was an error creating a description from the given information.
	*/
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, List<WebDAVProperty> propertyList) throws DataException	
	{
		final URFResource resource=urf.locateResource(resourceURI);	//create a resource to represent the WebDAV property list
/*TODO fix
		final RK resourceKit=getResourceKitManager().getResourceKit(this, resource);	//get a resource kit for this resource
		if(resourceKit!=null)	//if we found a resource kit for this resource
		{
			resourceKit.initialize(this, rdf, resource);	//initialize the resource
		}
*/
		final Set<String> ignoredWebDAVNamespaces=getIgnoredWebDAVNamespaces();	//get the map of ignored WebDAV namespaces
			//create a label G***maybe only do this if the resource kit has not added a label
		boolean isCollection=false;	//we'll detect if this is a collection base upon the properties
		final URFIO<URFResource> descriptionIO=getDescriptionIO();	//get I/O for the description
		for(final WebDAVProperty webdavProperty:propertyList)	//look at each WebDAV property
		{
			final WebDAVPropertyName propertyName=webdavProperty.getName();	//get the property name
			final String propertyNamespace=propertyName.getNamespace();	//get the string version of the property namespace
			final String propertyLocalName=propertyName.getLocalName();	//get the local name of the property
			final WebDAVPropertyValue propertyValue=webdavProperty.getValue();	//get the value of the property
			if(WEBDAV_NAMESPACE.equals(propertyNamespace))	//if this property is in the WebDAV namespace
			{
				if(DISPLAY_NAME_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:displayname
				{
					final String displayName=propertyValue.getText().trim();	//get the display name TODO just trim control characters (e.g. CR, LF), as we want users to be able to add whitespace
					resource.setLabel(displayName);	//set the label as the display name of the WebDAV resource			
				}				
				else if(CREATION_DATE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:creationdate
				{
					final String creationDateString=propertyValue.getText();	//get the D:creationdate string
					try
					{
						final URFDateTime creationDate=URFDateTime.valueOfTimestamp(creationDateString);	//parse the creation date; the WebDAV D:creationdate property uses the RFC 3339 Internet timestamp ISO 8601 profile
						setCreated(resource, creationDate);	//set the created date time as the creation date of the WebDAV resource			
					}
					catch(final IllegalArgumentException illegalArgumentException)	//if the creation date does not have the correct syntax
					{
						throw new DataException("Illegal WebDAV "+CREATION_DATE_PROPERTY_NAME+" value: "+creationDateString, illegalArgumentException);
					}
				}				
				else if(RESOURCE_TYPE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:resourcetype
				{
					if(propertyValue instanceof WebDAVDocumentFragmentPropertyValue)	//if the WebDAV property represents a document fragment
					{
						final List<Element> valueElements=getChildElements(((WebDAVDocumentFragmentPropertyValue)propertyValue).getDocumentFragment());	//get the child elements of the document fragment
						if(valueElements.size()==1 && COLLECTION_TYPE.equals(createQualifiedName(valueElements.get(0)).getURI()))	//if there is one child element with a reference URI of D:collection
						{
							isCollection=true;	//show that this is a collection
							resource.addTypeURI(LIST_CLASS_URI);	//add the urf:List type to indicate that this resource is a folder
						}
					}
				}
				else if(GET_CONTENT_LANGUAGE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getcontentlanguage
				{
					final Locale contentLanguage=createLocale(propertyValue.getText().trim());	//get the content language string and create a locale from it
					setLanguage(resource, contentLanguage);	//set the dc:language to the language of the WebDAV resource			
				}				
				else if(GET_CONTENT_LENGTH_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getcontentlength
				{
					final String contentLengthString=propertyValue.getText().trim();	//get the content length string
					try
					{
						final long contentLength=Long.parseLong(contentLengthString);	//parse the content length
						setContentLength(resource, contentLength);	//set the content length to  the content length of the WebDAV resource			
					}
					catch(final NumberFormatException numberFormatException)	//if the content length is not a valid value
					{
						throw new DataException("Illegal WebDAV "+GET_CONTENT_LENGTH_PROPERTY_NAME+" value: "+contentLengthString, numberFormatException);
					}
				}				
				else if(GET_CONTENT_TYPE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getcontenttype
				{
					final String contentTypeString=propertyValue.getText().trim();	//get the content type string
					try
					{
						final ContentType contentType=new ContentType(contentTypeString);	//create a content type object from the text of the element
						if(contentType!=null)	//if we know the content type
						{
							setContentType(resource, contentType);	//set the content type property
						}
					}
					catch(final javax.mail.internet.ParseException parseException)	//if the content type is not a correct MIME type
					{
						throw new DataException("Illegal WebDAV "+GET_CONTENT_TYPE_PROPERTY_NAME+" value: "+contentTypeString, parseException);
					}
				}
				else if(GET_LAST_MODIFIED_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getlastmodified
				{
					final String lastModifiedDateString=propertyValue.getText().trim();	//get the last modified date string
					try
					{
						final DateFormat httpDateFormat=new HTTPDateFormat(HTTPDateFormat.Style.RFC1123);	//create an HTTP date formatter; the WebDAV D:getlastmodified property prefers the RFC 1123 style, as does HTTP
						final URFDateTime lastModifiedDate=new URFDateTime(httpDateFormat.parse(lastModifiedDateString));	//parse the date
						setModified(resource, lastModifiedDate);	//set the modified date time as the last modified date of the WebDAV resource			
					}
					catch(final java.text.ParseException parseException)	//if the last modified time is not the correct type
					{
						throw new DataException("Illegal WebDAV "+GET_LAST_MODIFIED_PROPERTY_NAME+" value: "+lastModifiedDateString, parseException);
					}
				}				
			}
			if(!ignoredWebDAVNamespaces.contains(propertyNamespace))	//if this is not a namespace to ignore
			{
//Debug.trace("looking at non-WebDAV property", propertyName);
				final URI urfPropertyURI=getURFPropertyURI(propertyName);	//get the URI of the corresponding URF property, if any
//Debug.trace("URF property URI", urfPropertyURI);
				if(urfPropertyURI!=null && propertyValue!=null)	//if there is a corresponding URF property and there is an actual value specified (URF does not define a null value) TODO fix null
				{

					if(propertyValue instanceof WebDAVLiteralPropertyValue)	//if the value is a literal (we don't yet support complex WebDAV properties)
					{
						final String propertyTextValue=((WebDAVLiteralPropertyValue)propertyValue).getText();	//get the text value of the property
						if(propertyTextValue.startsWith(TURF_SIGNATURE))	//if this property value is stored in TURF
						{
							try
							{
								final URFResource propertyDescription=descriptionIO.read(urf, new ByteArrayInputStream(propertyTextValue.getBytes(UTF_8)), resourceURI);	//read a description of the property
								for(final URFProperty property:propertyDescription.getProperties(urfPropertyURI))	//for each read property that we expect in the description
								{
									resource.addProperty(property);	//add this property to the description
								}
							}
							catch(final IOException ioException)	//TODO improve; comment
							{
//TODO fix								throw new DataException(ioException);
								Debug.warn("Error parsing property", urfPropertyURI, "with value", propertyTextValue, ioException);
							}
						}
						else	//if this is a normal string property
						{
							resource.addPropertyValue(urfPropertyURI, propertyTextValue);	//add the string value to the resource
						}
					}
				}
			}
		}
		if(isCollection)	//if this is a collection
		{
			resource.removeProperties(Content.TYPE_PROPERTY_URI);	//remove any content type properties (Apache mod_dav adds a "httpd/unix-directory" pseudo MIME type for collections, for example)
		}
		else	//if this is not a collection, try to get the content type of the resource if it wasn't specified already
		{
				//try to find a content type if none was specified
			if(getContentType(resource)==null)	//if no content was determined
			{
				final ContentType contentType=getExtensionContentType(getNameExtension(URIs.getName(resourceURI)));	//get the registered content type for the resource's extension
				if(contentType!=null)	//if there is a registered content type for the resource's extension
				{
					setContentType(resource, contentType);	//set the content type property
				}
			}
		}
			//TODO fix filename encoding/decoding---there's no way to know what operating system the server is using
		
			//TODO encode in UTF-8
//		Debug.trace("ready to return resource description:", URF.toString(resource));
		return resource;	//return the resource that respresents the file
	}

	/**Translates the given error specific to this repository type into a resource I/O exception.
	This version makes the following translations:
	<dl>
		<dt>{@link HTTPForbiddenException}</dt> <dd>{@link ResourceForbiddenException}</dd>
		<dt>{@link HTTPNotFoundException}</dt> <dd>{@link ResourceNotFoundException}</dd>
		<dt>{@link HTTPMovedTemporarilyException}</dt> <dd>{@link ResourceMovedTemporarilyException}</dd>
		<dt>{@link HTTPMovedPermanentlyException}</dt> <dd>{@link ResourceMovedPermanentlyException}</dd>
		<dt>{@link HTTPPreconditionFailedException}</dt> <dd>{@link ResourceStateException}</dd>
	</dl>
	@param resourceURI The URI of the resource to which the exception is related.
	@param throwable The error which should be translated to a resource I/O exception.
	@return A resource I/O exception based upon the given throwable.
	*/
	protected ResourceIOException createResourceIOException(final URI resourceURI, final Throwable throwable) 
	{
		if(throwable instanceof HTTPForbiddenException)
		{
			return new ResourceForbiddenException(resourceURI, throwable);
		}
		else if(throwable instanceof HTTPNotFoundException)
		{
			return new ResourceNotFoundException(resourceURI, throwable);
		}
			//TODO check to see if the getPublicURI() throws an exception; the new location is somewhat out of our control, and may not be in the private repository namespace
		else if(throwable instanceof HTTPMovedTemporarilyException)
		{
			return new ResourceMovedTemporarilyException(resourceURI, getPublicURI(((HTTPMovedTemporarilyException)throwable).getLocation()), throwable);	//get the new location and translate it into the public repository namespace, if possible
		}
		else if(throwable instanceof HTTPMovedPermanentlyException)
		{
			return new ResourceMovedPermanentlyException(resourceURI, getPublicURI(((HTTPMovedPermanentlyException)throwable).getLocation()), throwable);	//get the new location and translate it into the public repository namespace, if possible
		}
		else if(throwable instanceof HTTPPreconditionFailedException)
		{
			return new ResourceStateException(resourceURI, throwable);
		}
		else	//if this is not one of our specially-handled exceptions
		{
			return super.createResourceIOException(resourceURI, throwable);	//convert the exception normally
		}
	}

	/**Creates an output stream that updates the properties of a WebDAV resource after its contents are stored.
	The password of the given password authentication object, if any, will be erased after updating the properties of the WebDAV resource.
	@author Garret Wilson
	*/
	protected class DescriptionWriterOutputStreamDecorator extends OutputStreamDecorator<OutputStream>
	{
		/**The reference URI to use to identify the resource.*/
		private final URI resourceURI;

			/**@protected The reference URI to use to identify the resource.*/
			public URI getResourceURI() {return resourceURI;}
	
		/**The description of the resource to store as WebDAV properties.*/
		private final URFResource resourceDescription;

			/**@return The description of the resource to store as WebDAV properties.*/
			protected URFResource getResourceDescription() {return resourceDescription;}

		/**The WebDAV resource for updating the WebDAV properties.*/
		private final WebDAVResource webdavResource;

			/**@return The WebDAV resource for updating the WebDAV properties.*/
			protected WebDAVResource getWebDAVResource() {return webdavResource;}

		/**The password authentication being used, or <code>null</code> if no password authentication is given.*/
		private final PasswordAuthentication passwordAuthentication;

			/**@return The password authentication being used, or <code>null</code> if no password authentication is given.*/
			protected PasswordAuthentication getPasswordAuthentication() {return passwordAuthentication;}

		/**Decorates the given output stream.
		@param outputStream The output stream to decorate
		@param resourceURI The reference URI to use to identify the resource.
		@param resourceDescription The description of the resource to store as WebDAV properties.
		@param webdavResource The WebDAV resource for updating the WebDAV properties.
		@param passwordAuthentication The password authentication being used, or <code>null</code> if no password authentication is given.
		@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
		*/
		public DescriptionWriterOutputStreamDecorator(final OutputStream outputStream, final URI resourceURI, final URFResource resourceDescription, final WebDAVResource webdavResource, final PasswordAuthentication passwordAuthentication)
		{
			super(outputStream);	//construct the parent class
			this.resourceURI=checkInstance(resourceURI, "Resource URI cannot be null.");
			this.resourceDescription=checkInstance(resourceDescription, "Resource description cannot be null.");
			this.webdavResource=checkInstance(webdavResource, "WebDAV resource cannot be null.");
			this.passwordAuthentication=passwordAuthentication;
		}
	
	  /**Called after the stream is successfully closed.
		This version updates the WebDAV properties to reflect the given resource description.
		@exception ResourceIOException if an I/O error occurs.
		*/
	  protected void afterClose() throws ResourceIOException
	  {
	  	try
	  	{
	  		setResourceProperties(getResourceURI(), getResourceDescription(), getWebDAVResource());	//set the properties using the WebDAV resource object
			}
			finally
			{
				final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//see if we were given password authentication information
				if(passwordAuthentication!=null)	//if we used password authentication
				{
					fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
				}
			}	  		
	  }

	}

}

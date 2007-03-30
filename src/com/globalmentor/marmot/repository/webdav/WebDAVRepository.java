package com.globalmentor.marmot.repository.webdav;

import java.io.*;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.util.*;
import static java.util.Arrays.*;

import static java.util.Collections.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.*;

import org.w3c.dom.*;

import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.net.URIUtilities.*;
import static com.garretwilson.net.http.webdav.ApacheWebDAVConstants.*;
import static com.garretwilson.net.http.webdav.WebDAVConstants.*;
import static com.garretwilson.net.http.webdav.WebDAVUtilities.*;
import static com.garretwilson.rdf.dublincore.DCUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;
import static com.garretwilson.rdf.xpackage.FileOntologyUtilities.*;
import static com.garretwilson.rdf.xpackage.MIMEOntologyUtilities.*;

import com.garretwilson.io.FileUtilities;
import com.garretwilson.io.OutputStreamDecorator;
import com.garretwilson.net.Resource;
import com.garretwilson.net.ResourceIOException;
import com.garretwilson.net.ResourceMovedPermanentlyException;
import com.garretwilson.net.ResourceMovedTemporarilyException;
import com.garretwilson.net.ResourceNotFoundException;
import com.garretwilson.net.http.*;
import com.garretwilson.net.http.webdav.*;
import com.garretwilson.rdf.*;

import static com.garretwilson.rdf.RDFUtilities.*;
import static com.garretwilson.rdf.rdfs.RDFSUtilities.*;
import static com.garretwilson.text.xml.XMLUtilities.*;

import com.garretwilson.text.W3CDateFormat;
import com.garretwilson.util.Debug;
import static com.garretwilson.util.LocaleUtilities.*;
import com.garretwilson.util.NameValuePair;
import com.globalmentor.marmot.repository.AbstractRepository;

/**Repository accessed via WebDAV.
<p>This repository recognizes the XPackage resource type <code>file:folder</code> and creates a collection for each such resource.</p>
<p>RDF properties are stored as WebDAV properties with a local name in the form <code>rdf_<var>propertyName</var>[.<var>uniqueNumber</var>.]</code>
(e.g. <code>rdf_lastName</code> or <code>rdf_middleName.27.</code>), where <var>uniqueNumber</var> is an optional number unique among all other RDF properties of the WebDAV resource with the same property URI.
This allows multiple RDF properties to be stored as WebDAV properties, which only allow single instances of property declarations.
If there exists only one RDF property with a given property URI, the property name should forsake the unique identifier for readability purposes.</p>
<p>Plain literal RDF property values with no <code>xml:lang</code> specification can and should be stored as text node(s) under the WebDAV property XML element.
The content of all RDF properties may, and all RDF properties besides literal RDF property values must, consist of the serialization of the RDF property XML element and its children.</p>
@author Garret Wilson
*/
public class WebDAVRepository extends AbstractRepository
{

		//TODO the current technique of erasing the password after each call may become obsolete when the HTTP client supports persistent connections
	
	/**The URI represting the XPackage file:folder type.*/	//TODO check; use static imports 
//TODO move if needed	protected final static URI FILE_FOLDER_TYPE_URI=RDFUtilities.createReferenceURI(FileOntologyConstants.FILE_ONTOLOGY_NAMESPACE_URI, FileOntologyConstants.FOLDER_TYPE_NAME);	//TODO promote to parent file-based class		

	/**The extension used for directories to hold resource children.*/
//TODO move if needed	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/**The prefix used to identify WebDAV properties representing RDF properties.*/
//TODO del if not needed	public final static String WEBDAV_RDF_PROPERTY_LOCAL_NAME_PREFIX="rdf_";

	/**The delimiter used to separate the optional trailing uniqueness number when storing RDF properties as WebDAV properties.*/
	public final static String WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER=".";

	/**The regular expression pattern to match RDF properties stored as WebDAV properties.
	The first matching group, which will always be present, will identify the original RDF property name.
	The second matching group, if present, will identify the uniqueness number string.
	*/
//TODO del	public final static Pattern WEBDAV_RDF_PROPERTY_LOCAL_NAME_PATTERN=Pattern.compile(WEBDAV_RDF_PROPERTY_LOCAL_NAME_PREFIX+"(.+)(?:\\"+WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER+"(\\d+)\\"+WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER+")?");
	public final static Pattern WEBDAV_RDF_PROPERTY_LOCAL_NAME_PATTERN=Pattern.compile("(.+)(?:\\"+WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER+"(\\d+)\\"+WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER+")?");

	/**Creates a name of a WebDAV property to represent an RDF property based upon the RDF property URI.
	An RDF propertyURI with a property count of zero will result in a WebDAV property name witho no uniqueness identifier.
	@param rdfPropertyURI The URI of the RDF property to represent.
	@param propertyCount The number of WebDAV properties representing RDF properties with URI that have already been used, greater than zero. 
	@return A WebDAV property name to use in representing an RDF property with the given RDF property URI.
	@exception IllegalArgumentException if the given property count is less than zero.
	*/
	protected static WebDAVPropertyName createWebDAVRDFPropertyName(final URI rdfPropertyURI, final long propertyCount)
	{
		if(propertyCount<0)	//if the property count is less than zero
		{
			throw new IllegalArgumentException("Property count cannot be less than zero.");
		}
		final String webDAVPropertyNamespace=getNamespaceURI(rdfPropertyURI).toString();	//get the namespace to use, which is the same as the RDF namespace URI
		final String rdfPropertyLocalName=getLocalName(rdfPropertyURI);	//get the RDF property local name
//TODO del if not needed		final StringBuilder webDAVPropertyLocalNameStringBuilder=new StringBuilder(WEBDAV_RDF_PROPERTY_LOCAL_NAME_PREFIX);	//create a string builder for creating the WebDAV local name, initializing it with the WebDAV RDF property prefix
		final StringBuilder webDAVPropertyLocalNameStringBuilder=new StringBuilder();	//create a string builder for creating the WebDAV local name
		webDAVPropertyLocalNameStringBuilder.append(rdfPropertyLocalName);	//append the RDF local name
		if(propertyCount>0)	//if there is nonzero property count
		{
			webDAVPropertyLocalNameStringBuilder.append(WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER).append(Long.toString(propertyCount)).append(WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER);	//append ".propertyCount."
		}
		return new WebDAVPropertyName(webDAVPropertyNamespace, webDAVPropertyLocalNameStringBuilder.toString());	//create and return an appropriate WebDAV property name for the RDF property
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
	
	/**Default constructor with no settings.
	Settings must be configured before repository is opened.
	*/
	public WebDAVRepository()
	{
		this.httpClient=HTTPClient.getInstance();	//TODO improve; consolidate constructors; create HTTPClient constructor
	}

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
	}
	
	/**Gets an input stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
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
	@exception IllegalStateException if the repository is not open for access.
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
	@exception IllegalStateException if the repository is not open for access.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public RDFResource getResourceDescription(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final RDF rdf=new RDF();	//G***use a common RDF data model
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final List<WebDAVProperty> propertyList=webdavResource.propFind();	//get the properties of this resource
			return createResourceDescription(rdf, resourceURI, propertyList);	//create a resource from this URI and property list
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
	@exception IllegalStateException if the repository is not open for access.
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
	@exception IllegalStateException if the repository is not open for access.
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
	@exception IllegalStateException if the repository is not open for access.
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
	@exception IllegalStateException if the repository is not open for access.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws ResourceIOException
	{
//	TODO del Debug.traceStack("!!!!!!!!getting child resource descriptions for resource", resourceURI);
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
				final RDF rdf=new RDF();	//create a new RDF data model
				final List<NameValuePair<URI, List<WebDAVProperty>>> propertyLists=webdavResource.propFind(webdavDepth);	//get the properties of the resources
				final List<RDFResource> childResourceList=new ArrayList<RDFResource>(propertyLists.size());	//create a list of child resources no larger than the number of WebDAV resource property lists
	//		TODO del Debug.trace("looking at children");
				for(final NameValuePair<URI, List<WebDAVProperty>> propertyList:propertyLists)	//look at each property list
				{
					final URI childResourcePrivateURI=propertyList.getName();
	//			TODO del Debug.trace("looking at child", childResourceURI);
					if(!privateResourceURI.equals(childResourcePrivateURI))	//if this property list is *not* for this resource
					{
	//				TODO del Debug.trace("creating resource for child", childResourceURI);
						childResourceList.add(createResourceDescription(rdf, getPublicURI(childResourcePrivateURI), propertyList.getValue()));	//create a resource from this URI and property lists
					}
				}
	//TODO do the special Marmot thing about checking for special Marmot directories
				
	//TODO fix				Collections.sort(resourceList);	//sort the resource by URI
				return childResourceList;	//return the list of resources we constructed
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
	If a resource with no contents is desired, {@link #createResource(URI, RDFResource, byte[])} with zero bytes is better suited for this task.
	This implementation updates the resource description after its contents are stored.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception ResourceIOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI, final RDFResource resourceDescription) throws ResourceIOException
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
	@exception IllegalStateException if the repository is not open for access.
	@exception ResourceIOException if the resource could not be created.
	*/
	public RDFResource createResource(final URI resourceURI, final RDFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException
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
	@exception IllegalStateException if the repository is not open for access.
	@exception ResourceIOException if there is an error creating the collection.
	*/
	public RDFResource createCollection(final URI collectionURI) throws ResourceIOException
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
	This version delegates to {@link #setResourceProperties(URI, RDFResource, WebDAVResource)}.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public RDFResource setResourceProperties(final URI resourceURI, final RDFResource resourceDescription) throws ResourceIOException
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
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public RDFResource setResourceProperties(final URI resourceURI, final RDFPropertyValuePair... properties) throws ResourceIOException
	{
		final Set<URI> newPropertyURISet=new HashSet<URI>();	//create a set to find out which properties we will be setting
		for(final RDFPropertyValuePair property:properties)	//look at each property
		{
			newPropertyURISet.add(property.getName().getReferenceURI());	//add this property URI to our set
		}
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final List<WebDAVProperty> oldPropertyList=webdavResource.propFind();	//get the original properties of this resource
			final Set<WebDAVPropertyName> removePropertyNames=getWebDAVPropertyNames(oldPropertyList, newPropertyURISet);	//get the WebDAV property names that will need to be removed
			final List<URI> newPropertyURIList=new ArrayList<URI>(newPropertyURISet);	//create a list of the distinct new properties to set
			final int[] propertyCounts=new int[newPropertyURIList.size()];	//keep track of how many of each property is added
			fill(propertyCounts, 0);	//fill the new property counts array with zero; when we get above zero we'll use this value when we create the the WebDAV property name
			final RDFXMLGenerator rdfXMLGenerator=new RDFXMLGenerator();	//create an RDF XML generator
			rdfXMLGenerator.setLiteralAttributeSerialization(false);	//serialize all literal property values as child elements rather than attributes
			rdfXMLGenerator.setCompactRDFListSerialization(true);	//serialize all RDF lists in short form
				//TODO decide what to do to take care of typed RDF literal values and xml:lang specifications
			final Document rdfDocument=rdfXMLGenerator.createDocument(new RDF(), WebDAVXMLGenerator.createWebDAVDocumentBuilder().getDOMImplementation());	//create a new XML document for RDF
			final Element resourceElement=rdfXMLGenerator.createResourceElement(rdfDocument, new DefaultRDFResource());	//create a dummy RDF resource to which we'll add property child elements
			final Set<WebDAVProperty> setProperties=new HashSet<WebDAVProperty>();	//create a set of properties to set
			for(final RDFPropertyValuePair rdfProperty:properties)	//for each property to set
			{
				rdfXMLGenerator.reset();	//reset the RDF XML generator so that any previously serialized RDF resources (which were property values, for example) won't be serialized by reference
				final URI rdfPropertyURI=rdfProperty.getName().getReferenceURI();	//get the URI of the RDF property
				final int propertyIndex=newPropertyURIList.indexOf(rdfPropertyURI);	//get the index of this property so we can determine the count
				assert propertyIndex>=0 : "Known RDF property unexpectedly not found in list.";
				final int propertyCount=propertyCounts[propertyIndex]++;	//find the current property count of this property, and update the property count for next time
				final WebDAVPropertyName webdavPropertyName=createWebDAVRDFPropertyName(rdfPropertyURI, propertyCount);	//create a WebDAV property name from the RDF property URI
				final WebDAVPropertyValue webdavPropertyValue;	//we'll determine the WebDAV property value to use
				final RDFObject rdfPropertyValue=rdfProperty.getPropertyValue();	//get the value of the property
				if(rdfPropertyValue instanceof RDFPlainLiteral && ((RDFPlainLiteral)rdfPropertyValue).getLanguage()==null)	//if this is a plain literal with no language specified
				{
					webdavPropertyValue=new WebDAVLiteralPropertyValue(((RDFPlainLiteral)rdfPropertyValue).getLexicalForm());	//create a WebDAV literal property value with the RDF plain literal value lexical form
				}
				else	//if this is not an RDF plain literal, or if there is a language specified
				{
					final Element propertyElement=rdfXMLGenerator.addProperty(rdfDocument, resourceElement, rdfProperty);	//add this property as an RDF child element
					assert propertyElement!=null : "Property element missing, even though output should not be compact.";
					final DocumentFragment propertyValueDocumentFragment=extractNode(propertyElement);	//extract the entire property element into a document fragment
					webdavPropertyValue=new WebDAVDocumentFragmentPropertyValue(propertyValueDocumentFragment);	//create a WebDAV property value from a document fragment
				}
				setProperties.add(new WebDAVProperty(webdavPropertyName, webdavPropertyValue));	//add the property with the document fragment as its value
				removePropertyNames.remove(webdavPropertyName);	//since we'll be explicitly setting this WebDAV property name, it's redundance and inefficient to explicitly remove it first, so remove it from the remove set, if it's there
			}
			webdavResource.propPatch(removePropertyNames, setProperties);	//patch the properties of the resource
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
	public RDFResource removeResourceProperties(final URI resourceURI, final URI... propertyURIs) throws ResourceIOException
	{
		final Set<URI> propertyURISet=new HashSet<URI>();	//create a set to find out which properties we will be setting
		addAll(propertyURISet, propertyURIs);	//create a set of properties to remove
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final List<WebDAVProperty> oldPropertyList=webdavResource.propFind();	//get the original properties of this resource
			final Set<WebDAVPropertyName> removePropertyNames=getWebDAVPropertyNames(oldPropertyList, propertyURISet);	//get the WebDAV property names that will need to be removed
			webdavResource.propPatch(removePropertyNames, (Set<WebDAVProperty>)EMPTY_SET);	//patch the properties of the resource; remove the designated properties, but don't add any new ones TODO fix; why can't we use emptySet()?
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

	/**Determines the WebDAV property names of an existing list of WebDAV properties that correspond to a set of RDF property URIs.
	This is useful for deleting existing RDF properties, for example; a single RDF property may exist under several WebDAV property names, as WebDAV doesn't allow repeated properties.
	@param webdavProperties The existing WebDAV properties.
	@param propertyURIs The set of RDF property URIs to match.
	@return The set of names of WebDAV properties that match the given RDF property URIs.
	*/
	protected Set<WebDAVPropertyName> getWebDAVPropertyNames(final List<WebDAVProperty> webdavProperties, final Set<URI> rdfPropertyURIs)
	{
		final Set<WebDAVPropertyName> webdavPropertyNames=new HashSet<WebDAVPropertyName>();	//create a set to find out how many matching WebDAV properties there are
		for(final WebDAVProperty webdavProperty:webdavProperties)	//look at each of the WebDAV properties
		{
			final WebDAVPropertyName webdavPropertyName=webdavProperty.getName();	//get the property name
//TODO del Debug.trace("looking at old WebDAV property name", oldWebDAVPropertyName);
			final String webdavPropertyNamespace=webdavPropertyName.getNamespace();	//get the property namespace
			if(!WEBDAV_NAMESPACE.equals(webdavPropertyNamespace))	//ignore the WebDAV namespace properties TODO make sure the new RDF property isn't something that should be converted into a WebDAV property
			{
				final String webdavPropertyLocalName=webdavPropertyName.getLocalName();	//get the property local name
			//TODO del Debug.trace("looking at old WebDAV property local name", oldWebDAVPropertyLocalName);
				final Matcher webdavRDFPropertyMatcher=WEBDAV_RDF_PROPERTY_LOCAL_NAME_PATTERN.matcher(webdavPropertyLocalName);	//see if this local name looks like an RDF property local name
				if(webdavRDFPropertyMatcher.matches())	//if this local name could be representing an RDF object
				{
				//TODO del Debug.trace("this is an RDF property; checking RDF property local name", webdavRDFPropertyMatcher.group(1));
					final URI rdfPropertyURI=WebDAVPropertyName.createPropertyURI(webdavPropertyNamespace, webdavRDFPropertyMatcher.group(1));	//create what should be the RDF property URI by using the same namespace URI but the RDF version of the local name
				//TODO del 	Debug.trace("this is the existing RDF property in WebDAV:", rdfPropertyURI);
					if(rdfPropertyURIs.contains(rdfPropertyURI))	//if this property is one of the new RDF properties in question
					{
					//TODO del 	Debug.trace("we'll have to remove", oldWebDAVPropertyName);
						webdavPropertyNames.add(webdavPropertyName);	//this WebDAV property matches
					}
				}
			}
		}
		return webdavPropertyNames;	//return the WebDAV property names we collected		
	}


	/**Sets the properties of a resource based upon the given description.
	This implementation only supports the {@value FileOntologyConstants#MODIFIED_TIME_PROPERTY_URI} property, ignoring all other properties.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@param webdavResource The WebDAV resource for setting the resource WebDAV properties
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	protected RDFResource setResourceProperties(final URI resourceURI, final RDFResource resourceDescription, final WebDAVResource webdavResource) throws ResourceIOException
	{
		final Date modifiedTime=getModifiedTime(resourceDescription);	//get the modified time designation, if there is one
		if(modifiedTime!=null)	//if there is a modified time designated
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

	/**Creates an infinitely deep copy of a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception ResourceIOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.copy(getPrivateURI(destinationURI));	//copy the resource with an infinite depth, overwriting the destination resource if one exists
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
	@exception IllegalStateException if the repository is not open for access.
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

	/**Moves a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException
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
			webdavResource.move(getPrivateURI(destinationURI));	//move the resource with an infinite depth, overwriting the destination resource if one exists
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
	@param rdf The RDF data model to use when creating this resource.
	@param referenceURI The reference URI of the property to create.
	@param propertyList The list of property qualified names paired with WebDAV property values.
	@return A resource representing the given WebDAV property list.
	@exception NullPointerException if one or more of the provided properties has a value of <code>null</code>.
	*/
	protected RDFResource createResourceDescription(final RDF rdf, final URI resourceURI, List<WebDAVProperty> propertyList)	
	{
		final RDFResource resource=rdf.locateResource(resourceURI);	//create a resource to represent the WebDAV property list
/*TODO fix
		final RK resourceKit=getResourceKitManager().getResourceKit(this, resource);	//get a resource kit for this resource
		if(resourceKit!=null)	//if we found a resource kit for this resource
		{
			resourceKit.initialize(this, rdf, resource);	//initialize the resource
		}
*/
			//create a label G***maybe only do this if the resource kit has not added a label
		final String filename=getFileName(resourceURI);	//get the filename
		boolean isCollection=false;	//we'll detect if this is a collection base upon the properties
		final RDFXMLProcessor rdfXMLProcessor=new RDFXMLProcessor(rdf);	//create a new processor for analyzing any RDF contained in RDF property values, using the existing RDF instance
//TODO del; not needed with Apache mod_dav patch		rdfXMLProcessor.setRDFAttributeNamespaceRequirement(RDFXMLProcessor.NamespaceRequirement.ANY);	//recognize RDF attributes in any namespace to compensate for buggy WebDAV implementations such as Apache httpd 2.2.3 mod_dav (we should have serialized all literal property values as child elements rather than attributes, so this shouldn't hurt us)
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
					setLabel(resource, displayName);	//set the label as the display name of the WebDAV resource			
				}				
				else if(CREATION_DATE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:creationdate
				{
					try
					{
							//TODO fix; RFC 2518 23.2 Appendix 2 says that ISO 8601 can have an optional fractional part of seconds, but Apache mod_dav doesn't seem to do that; make sure this date formatter will handle it if any server tries to send back fractions of a second
						final DateFormat iso8601DateFormat=new W3CDateFormat(W3CDateFormat.Style.DATE_HOURS_MINUTES_SECONDS);	//create an ISO 8601 date formatter; the WebDAV D:creationdate property prefers the ISO 8601 style
						final Date creationDate=iso8601DateFormat.parse(propertyValue.getText().trim());	//parse the date
						setCreatedTime(resource, creationDate);	//set the created time as the creation date of the WebDAV resource			
					}
					catch(final java.text.ParseException parseException)	//if the creation date is not the correct type
					{
						Debug.warn(parseException);	//TODO improve error handling
					}
				}				
				else if(RESOURCE_TYPE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:resourcetype
				{
					if(propertyValue instanceof WebDAVDocumentFragmentPropertyValue)	//if the WebDAV property represents a document fragment
					{
						final List<Element> valueElements=getChildElements(((WebDAVDocumentFragmentPropertyValue)propertyValue).getDocumentFragment());	//get the child elements of the document fragment
						if(valueElements.size()==1 && COLLECTION_TYPE.equals(createQualifiedName(valueElements.get(0)).getReferenceURI()))	//if there is one child element with a reference URI of D:collection
						{
							isCollection=true;	//show that this is a collection
							addType(resource, FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME);	//add the file:folder type to indicate that this resource is a folder
						}
					}
				}
				else if(GET_CONTENT_LANGUAGE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getcontentlanguage
				{
					final Locale contentLanguage=createLocale(propertyValue.getText().trim());	//get the content language string and create a locale from it
					setLanguage(resource, contentLanguage);	//set the dc:languate as the content length of the WebDAV resource			
				}				
				else if(GET_CONTENT_LENGTH_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getcontentlength
				{
					try
					{
						final long contentLength=Long.parseLong(propertyValue.getText().trim());	//parse the content length
						setSize(resource, contentLength);	//set the size as the content length of the WebDAV resource			
					}
					catch(final NumberFormatException numberFormatException)	//if the content length is not a valid value
					{
						Debug.warn(numberFormatException);	//TODO improve error handling
					}
				}				
				else if(GET_CONTENT_TYPE_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getcontenttype
				{
					try
					{
						final ContentType contentType=new ContentType(propertyValue.getText().trim());	//create a content type object from the text of the element TODO check for errors
						if(contentType!=null)	//if we know the content type
						{
							setContentType(resource, contentType);	//set the content type property
						}
					}
					catch(final javax.mail.internet.ParseException parseException)	//if the content type is not a correct MIME type
					{
						Debug.warn(parseException);	//TODO improve error handling
					}
				}
				else if(GET_LAST_MODIFIED_PROPERTY_NAME.equals(propertyLocalName) && propertyValue!=null)	//D:getlastmodified
				{
					try
					{
						final DateFormat httpDateFormat=new HTTPDateFormat(HTTPDateFormat.Style.RFC1123);	//create an HTTP date formatter; the WebDAV D:getlastmodified property prefers the RFC 1123 style, as does HTTP
						final Date lastModifiedDate=httpDateFormat.parse(propertyValue.getText().trim());	//parse the date
						setModifiedTime(resource, lastModifiedDate);	//set the modified time as the last modified date of the WebDAV resource			
					}
					catch(final java.text.ParseException parseException)	//if the last modified time is not the correct type
					{
						Debug.warn(parseException);	//TODO improve error handling
					}
				}				
			}
			if(APACHE_WEBDAV_PROPERTY_NAMESPACE_URI.toString().equals(propertyNamespace))	//if this property is in the Apache WebDAV namespace
			{
				//ignore Apache WebDAV properties
			}
			else	//for non-WebDAV properties
			{
				final Matcher webdavRDFPropertyMatcher=WEBDAV_RDF_PROPERTY_LOCAL_NAME_PATTERN.matcher(propertyLocalName);	//see if this local name could be an RDF property local name
				if(webdavRDFPropertyMatcher.matches() && propertyValue!=null)	//if this local name is representing an RDF object, and there is an actual value specified (RDF does not define a null value)
				{
					final URI rdfPropertyNamespaceURI;	//the URI of the RDF property namespace
					try
					{
						rdfPropertyNamespaceURI=URI.create(propertyNamespace);	//convert the property namespace to an RDF property URI
					}
					catch(final IllegalArgumentException illegalArgumentException)	//if we can't turn the WebDAV namespace into an RDF namespace, we must have been wrong about the property being an RDF property to begin with
					{
						continue;	//skip processing of this resource
					}					
					final String rdfPropertyLocalName=webdavRDFPropertyMatcher.group(1);	//get the local name to use for the RDF property
					final RDFObject rdfPropertyValue;	//we'll determine the RDF property value to use
					if(propertyValue instanceof WebDAVLiteralPropertyValue)	//if the value is a literal, we'll use a plain literal RDF value with no language specified
					{
						resource.addProperty(rdfPropertyNamespaceURI, rdfPropertyLocalName, new RDFPlainLiteral(((WebDAVLiteralPropertyValue)propertyValue).getText()));	//an an RDF plain literal property value to represent the value text
					}
					else if(propertyValue instanceof WebDAVDocumentFragmentPropertyValue)	//if the value is a document fragment
					{
						final DocumentFragment rdfPropertyValueDocumentFragment=((WebDAVDocumentFragmentPropertyValue)propertyValue).getDocumentFragment();	//get the document fragment representing the RDF property value
						if(getChildNodeNot(rdfPropertyValueDocumentFragment, Node.TEXT_NODE)==null)	//if all child nodes are text nodes
						{
							resource.addProperty(rdfPropertyNamespaceURI, rdfPropertyLocalName, new RDFPlainLiteral(getText(rdfPropertyValueDocumentFragment)));	//an an RDF plain literal property value to represent the value text of the document fragment
						}
						else	//if there is a document fragment that has non-text children
						{
							try
							{
								final List<Element> childElements=getChildElements(rdfPropertyValueDocumentFragment);	//get the child elements of the document fragment
								final Element propertyElement=childElements.size()==1 ? childElements.get(0) : null;	//get the first (and only) child element, but only if there's a single child element
								if(propertyElement==null || !propertyNamespace.equals(propertyElement.getNamespaceURI()) || !propertyLocalName.equals(propertyElement.getLocalName()))	//if there isn't a single child element, and it doesn't have the same namespace and local name as the WebDAV property
								{
									continue;	//skip processing of this resource; it's not an RDF property
								}
								final Object rdfPropertyValueObject=rdfXMLProcessor.processProperty(resource, propertyElement, 0);	//process the contents of the RDF property XML element
									//TODO incorporate the following logic into a RDFXMLProcessor and make these individual methods protected again 
								rdfXMLProcessor.createResources();	//create all proxied resources in the statements we gathered
								rdfXMLProcessor.processStatements();	//process all the RDF statements and assign resources to properties as needed; the subject resource should now have the property and value added
								rdfXMLProcessor.reset();	//reset the RDF XML processor
								rdfXMLProcessor.clearStatements();	//clear all the RDF statements so that that we won't re-create property/value settings the next time we use the processor
							}
							catch(final URISyntaxException uriSyntaxException)	//if we run into an incorrect URI
							{
								Debug.warn(uriSyntaxException);	//log the error but keep processing the other properties
								continue;	//skip processing of this resource
							}
						}
					}
					else	//if we don't recognize the type of WebDAV property value
					{
						throw new AssertionError("Unrecognized WebDAV property value type: "+propertyValue);
					}
				}
				
			}
		}
		
			//TODO fix filename encoding/decoding---there's no way to know what operating system the server is using
		
			//TODO encode in UTF-8
		return resource;	//return the resource that respresents the file
	}

	/**Translates the given error specific to this repository type into a resource I/O exception.
	This version makes the following translations:
	<dl>
		<dt>{@link HTTPNotFoundException}</dt> <dd>{@link ResourceNotFoundException}</dd>
		<dt>{@link HTTPMovedTemporarilyException}</dt> <dd>{@link ResourceMovedTemporarilyException}</dd>
		<dt>{@link HTTPMovedPermanentlyException}</dt> <dd>{@link ResourceMovedPermanentlyException}</dd>
	</dl>
	@param resourceURI The URI of the resource to which the exception is related.
	@param throwable The error which should be translated to a resource I/O exception.
	@return A resource I/O exception based upon the given throwable.
	*/
	protected ResourceIOException createResourceIOException(final URI resourceURI, final Throwable throwable) 
	{
		if(throwable instanceof HTTPNotFoundException)
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
		else	//if this is not one of our specially-handled exceptions
		{
			return super.createResourceIOException(resourceURI, throwable);	//convert the exceptoin normally
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
		private final RDFResource resourceDescription;

			/**@return The description of the resource to store as WebDAV properties.*/
			protected RDFResource getResourceDescription() {return resourceDescription;}

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
		public DescriptionWriterOutputStreamDecorator(final OutputStream outputStream, final URI resourceURI, final RDFResource resourceDescription, final WebDAVResource webdavResource, final PasswordAuthentication passwordAuthentication)
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

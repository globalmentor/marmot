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
import static com.garretwilson.net.http.webdav.WebDAVConstants.*;
import static com.garretwilson.net.http.webdav.WebDAVUtilities.*;
import static com.garretwilson.rdf.dublincore.DCUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;
import static com.garretwilson.rdf.xpackage.FileOntologyUtilities.*;
import static com.garretwilson.rdf.xpackage.MIMEOntologyUtilities.*;

import com.garretwilson.io.FileUtilities;
import com.garretwilson.io.OutputStreamDecorator;
import com.garretwilson.model.Resource;
import com.garretwilson.net.http.*;
import com.garretwilson.net.http.webdav.*;
import com.garretwilson.rdf.DefaultRDFResource;
import com.garretwilson.rdf.RDF;
import com.garretwilson.rdf.RDFLiteral;
import com.garretwilson.rdf.RDFObject;
import com.garretwilson.rdf.RDFPlainLiteral;
import com.garretwilson.rdf.RDFPropertyValuePair;
import com.garretwilson.rdf.RDFResource;
import com.garretwilson.rdf.RDFUtilities;
import com.garretwilson.rdf.RDFXMLGenerator;
import com.garretwilson.rdf.RDFXMLProcessor;

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
This allows multiple RDF properties to be stored as WebDAV properties, which only allow single instances of property declarations.</p>
If there exists only one RDF property with a given property URI, the property name should forsake the unique identifier for readability purposes.   
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
	public final static String WEBDAV_RDF_PROPERTY_LOCAL_NAME_PREFIX="rdf_";

	/**The delimiter used to separate the optional trailing uniqueness number when storing RDF properties as WebDAV properties.*/
	public final static String WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER=".";

	/**The regular expression pattern to match RDF properties stored as WebDAV properties.
	The first matching group, which will always be present, will identify the original RDF property name.
	The second matching group, if present, will identify the uniqueness number string.
	*/
	public final static Pattern WEBDAV_RDF_PROPERTY_LOCAL_NAME_PATTERN=Pattern.compile(WEBDAV_RDF_PROPERTY_LOCAL_NAME_PREFIX+"(.+)(?:\\"+WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER+"(\\d+)\\"+WEBDAV_RDF_PROPERTY_LOCAL_NAME_UNIQUENESS_DELIMITER+")?");

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
		final StringBuilder webDAVPropertyLocalNameStringBuilder=new StringBuilder(WEBDAV_RDF_PROPERTY_LOCAL_NAME_PREFIX);	//create a string builder for creating the WebDAV local name, initializing it with the WebDAV RDF property prefix
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
	@exception IOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getResourceInputStream(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return webdavResource.getInputStream();	//return an input stream to the resource
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
	@exception IOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource TODO cache these resources, maybe
			if(!webdavResource.exists())	//if the resource doesn't already exist
			{
				throw new HTTPNotFoundException("Cannot open output stream to non-existent resource "+resourceURI);
			}
			return webdavResource.getOutputStream();	//return an output stream to the resource
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
	@exception IOException if there is an error accessing the repository.
	*/
	public RDFResource getResourceDescription(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		if(getReferenceURI().equals(resourceURI))	//if this is the URI of the repository
		{
			return this;	//return the repository itself TODO fix; this is not appropriate for WebDAV, and probably not appropriate for file repositories, either
		}
		else	//if this is some other URI
		{
//TODO del Debug.traceStack("!!!!!!!!getting resource description for resource", resourceURI);
			final RDF rdf=new RDF();	//G***use a common RDF data model
			final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
			try
			{
				final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
				final List<WebDAVProperty> propertyList=webdavResource.propFind();	//get the properties of this resource
				return createResourceDescription(rdf, resourceURI, propertyList);	//create a resource from this URI and property list
			}
			finally
			{
				if(passwordAuthentication!=null)	//if we used password authentication
				{
					fill(passwordAuthentication.getPassword(), (char)0);	//always erase the password from memory as a security measure when we're done with the authentication object
				}
			}
		}
	}

	/**Determines if the resource at the given URI exists.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean resourceExists(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return webdavResource.exists();	//see if the WebDAV resource exists		
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
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean isCollection(final URI resourceURI) throws IOException
  {
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return webdavResource.isCollection();	//see if the WebDAV resource is a collection		
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
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(final URI resourceURI) throws IOException
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
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws IOException
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
	@exception IOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI, final RDFResource resourceDescription) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
		final OutputStream outputStream=webdavResource.getOutputStream();	//get an output stream to the WebDAV resource
		return new DescriptionWriterOutputStreamDecorator(outputStream, resourceURI, resourceDescription, webdavResource, passwordAuthentication);	//wrap the output stream in a decorator that will update the WebDAV properties after the contents are stored; this method will erase the provided password, if any, after it completes the resource property updates
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
	@exception IOException if the resource could not be created.
	*/
	public RDFResource createResource(final URI resourceURI, final RDFResource resourceDescription, final byte[] resourceContents) throws IOException
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
	@exception IOException if there is an error creating the collection.
	*/
	public RDFResource createCollection(final URI collectionURI) throws IOException
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
	@exception IOException Thrown if the resource properties could not be updated.
	*/
	public RDFResource setResourceProperties(final URI resourceURI, final RDFResource resourceDescription) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			return setResourceProperties(resourceURI, resourceDescription, webdavResource);	//set the properties using the WebDAV resource object
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
	@exception IOException Thrown if the resource properties could not be updated.
	*/
	public RDFResource setResourceProperties(final URI resourceURI, final RDFPropertyValuePair... properties) throws IOException
	{
Debug.trace("ready to set properties, count", properties.length);
		final Set<URI> newPropertyURISet=new HashSet<URI>();	//create a set to find out which properties we will be setting
		for(final RDFPropertyValuePair property:properties)	//look at each property
		{
			newPropertyURISet.add(property.getName().getReferenceURI());	//add this property URI to our set
		}
Debug.trace("we have unique properties to set, count:", newPropertyURISet.size());
		final RDF rdf=new RDF();	//TODO use a common RDF data model; del if not needed here
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			final List<WebDAVProperty> oldPropertyList=webdavResource.propFind();	//get the original properties of this resource
Debug.trace("found existing properties, count:", oldPropertyList.size());
				//determine if there are existing RDF properties that need to be removed
Debug.trace("ready to remove existing matching RDF properties");
			final Set<WebDAVPropertyName> removePropertyNameSet=new HashSet<WebDAVPropertyName>();	//create a set to find out how many properties we should remove
			for(final WebDAVProperty oldWebDAVProperty:oldPropertyList)	//look at each of the existing WebDAV properties
			{
				final WebDAVPropertyName oldWebDAVPropertyName=oldWebDAVProperty.getName();	//get the property name
Debug.trace("looking at old WebDAV property name", oldWebDAVPropertyName);
				final String oldWebDAVPropertyLocalName=oldWebDAVPropertyName.getLocalName();	//get the property local name
Debug.trace("looking at old WebDAV property local name", oldWebDAVPropertyLocalName);
				final Matcher webdavRDFPropertyMatcher=WEBDAV_RDF_PROPERTY_LOCAL_NAME_PATTERN.matcher(oldWebDAVPropertyLocalName);	//see if this local name looks like an RDF property local name
				if(webdavRDFPropertyMatcher.matches())	//if this local name is representing an RDF object
				{
Debug.trace("this is an RDF property; checking RDF property local name", webdavRDFPropertyMatcher.group(1));
					final String oldWebDAVPropertyNamespace=oldWebDAVPropertyName.getNamespace();	//get the property namespace
					try
					{
						final URI rdfPropertyURI=createReferenceURI(URI.create(oldWebDAVPropertyNamespace), webdavRDFPropertyMatcher.group(1));	//create what should be the RDF property URI by using the same namespace URI but the RDF version of the local name
	Debug.trace("this is the existing RDF property in WebDAV:", rdfPropertyURI);
						if(newPropertyURISet.contains(rdfPropertyURI))	//if this property is one of the new RDF properties we'll be setting
						{
	Debug.trace("we'll have to remove", oldWebDAVPropertyName);
							removePropertyNameSet.add(oldWebDAVPropertyName);	//we'll be removing this existing property
						}
					}
					catch(final IllegalArgumentException illegalArgumentException)	//if we can't turn the WebDAV namespace into an RDF namespace, we must have been wrong about the property being an RDF property to begin with (this is very, very unlikely)
					{
						Debug.warn(illegalArgumentException);						
					}
				}
			}
			final List<WebDAVProperty> propPatchProperties=new ArrayList<WebDAVProperty>();	//create a new list of properties to remove and to add
			for(final WebDAVPropertyName removePropertyName:removePropertyNameSet)	//for each name of a WebDAV property to remove
			{
				propPatchProperties.add(new WebDAVProperty(removePropertyName, null));	//add the property with a null value
			}
			final List<URI> newPropertyURIList=new ArrayList<URI>(newPropertyURISet);	//create a list of the distinct new properties to set
			final int[] propertyCounts=new int[newPropertyURIList.size()];	//keep track of how many of each property is added
			fill(propertyCounts, 0);	//fill the new property counts array with zero; when we get above zero we'll use this value when we create the the WebDAV property name
			final RDFXMLGenerator rdfXMLGenerator=new RDFXMLGenerator();	//create an RDF XML generator
			rdfXMLGenerator.setLiteralAttributeSerialization(false);	//serialize all literal property values as child elements rather than attributes
			rdfXMLGenerator.setCompactRDFListSerialization(false);	//serialize all RDF lists in long form
				//TODO decide what to do to take care of typed RDF literal values and xml:lang specifications
			final Document rdfDocument=rdfXMLGenerator.createDocument(new RDF(), WebDAVXMLGenerator.createWebDAVDocumentBuilder().getDOMImplementation());	//create a new XML document for RDF
			final Element resourceElement=rdfXMLGenerator.createResourceElement(rdfDocument, new DefaultRDFResource());	//create a dummy RDF resource to which we'll add property child elements
			for(final RDFPropertyValuePair rdfProperty:properties)	//for each property to set
			{
				rdfXMLGenerator.reset();	//reset the RDF XML generator so that any previously serialized RDF resources (which were property values, for example) won't be serialized by reference
				final URI rdfPropertyURI=rdfProperty.getName().getReferenceURI();	//get the URI of the RDF property
				final int propertyIndex=newPropertyURIList.indexOf(rdfPropertyURI);	//get the index of this property so we can determine the count
				assert propertyIndex>=0 : "Known RDF property unexpectedly not found in list.";
				final int propertyCount=propertyCounts[propertyIndex]++;	//find the current property count of this property, and update the property count for next time
				final WebDAVPropertyName webdavPropertyName=createWebDAVRDFPropertyName(rdfPropertyURI, propertyCount);	//create a WebDAV property name from the RDF property URI
				final Element propertyElement=rdfXMLGenerator.addProperty(rdfDocument, resourceElement, rdfProperty);	//add this property as an RDF child element
				assert propertyElement!=null : "Property element missing, even though output should not be compact.";
				final DocumentFragment propertyValueDocumentFragment=extractChildren(propertyElement);	//extract the children of the property element into a document fragment
				propPatchProperties.add(new WebDAVProperty(webdavPropertyName, new WebDAVDocumentFragmentPropertyValue(propertyValueDocumentFragment)));	//add the property with the document fragment as its value				
			}
			webdavResource.propPatch(propPatchProperties);	//patch the properties of the resource
			return getResourceDescription(resourceURI);	//retrieve and return a new description of the resource
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
	This implementation only supports the {@value FileOntologyConstants#MODIFIED_TIME_PROPERTY_URI} property, ignoring all other properties.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@param webdavResource The WebDAV resource for setting the resource WebDAV properties
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IOException Thrown if the resource properties could not be updated.
	*/
	protected RDFResource setResourceProperties(final URI resourceURI, final RDFResource resourceDescription, final WebDAVResource webdavResource) throws IOException
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
	@exception IOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.copy(getPrivateURI(destinationURI));	//copy the resource with an infinite depth, overwriting the destination resource if one exists
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
	@exception IOException if the resource could not be deleted.
	*/
	public void deleteResource(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.delete();	//delete the resource		
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
	@exception IOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		final PasswordAuthentication passwordAuthentication=getPasswordAuthentication();	//get authentication, if any
		try
		{
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient(), passwordAuthentication);	//create a WebDAV resource
			webdavResource.move(getPrivateURI(destinationURI));	//move the resource with an infinite depth, overwriting the destination resource if one exists
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
		for(final WebDAVProperty webdavProperty:propertyList)	//look at each WebDAV property
		{
			final WebDAVPropertyName propertyName=webdavProperty.getName();	//get the property name
			final String propertyNamespace=propertyName.getNamespace();	//get the string version of the property namespace
			final String propertyLocalName=propertyName.getLocalName();	//get the local name of the property
			final WebDAVPropertyValue propertyValue=webdavProperty.getValue();	//get the value of the property

			
			final Matcher webdavRDFPropertyMatcher=WEBDAV_RDF_PROPERTY_LOCAL_NAME_PATTERN.matcher(propertyLocalName);	//see if this local name looks like an RDF property local name
			if(webdavRDFPropertyMatcher.matches())	//if this local name is representing an RDF object
			{
				final RDFObject rdfPropertyValue;	//we'll determine the RDF property value to use
				if(propertyValue instanceof WebDAVLiteralPropertyValue)	//if the value is a literal
				{
					rdfPropertyValue=new RDFPlainLiteral(((WebDAVLiteralPropertyValue)propertyValue).getText());	//create an RDF plain literal to represent the value text
				}
				else if(propertyValue instanceof WebDAVDocumentFragmentPropertyValue)	//if the value is a document fragment
				{
					try
					{
						final DocumentFragment rdfPropertyValueDocumentFragment=((WebDAVDocumentFragmentPropertyValue)propertyValue).getDocumentFragment();	//get the document fragment representing the RDF property value
						final Object rdfPropertyValueObject=rdfXMLProcessor.processPropertyValueContents(rdfPropertyValueDocumentFragment);	//process the contents of the document fragment as an RDF property value
						if(rdfPropertyValueObject instanceof RDFLiteral)	//if the property object is already an RDF literal
						{
							rdfPropertyValue=(RDFLiteral)rdfPropertyValueObject;	//use the RDF literal value as is
						}
						else if(rdfPropertyValueObject instanceof Resource)	//if the property value is a resource (which may be a proxy)
						{
							rdfPropertyValue=rdfXMLProcessor.createResources((Resource)rdfPropertyValueObject);	//create any RDF resources from resource proxies and use the RDF property that was produced						
						}
						else	//if the property value is neither an RDF literal or a resource
						{
							throw new AssertionError("Unrecognized property value type: "+rdfPropertyValueObject);
						}
							//TODO incorporate the following logic into a RDFXMLProcessor and make these individual methods protected again 
						rdfXMLProcessor.processStatements();	//process all the RDF statements and assign resources to properties as needed
						rdfXMLProcessor.reset();	//reset the RDF XML processor
						rdfXMLProcessor.clearStatements();	//clear all the RDF statements so that that we won't re-create property/value settings the next time we use the processor
					}
					catch(final URISyntaxException uriSyntaxException)	//if we run into an incorrect URI
					{
						Debug.warn(uriSyntaxException);	//log the error but keep processing the other properties
						continue;	//skip processing of this resource
					}
				}
				else	//if we don't recognize the type of WebDAV property value
				{
					throw new AssertionError("Unrecognized WebDAV property value type: "+propertyValue);
				}
				try
				{
					resource.addProperty(URI.create(propertyNamespace), webdavRDFPropertyMatcher.group(1), rdfPropertyValue);	//add the RDF property to the resource, using the same namespace URI as WebDAV but using the RDF version of the local name
				}
				catch(final IllegalArgumentException illegalArgumentException)	//if we can't turn the WebDAV namespace into an RDF namespace, we must have been wrong about the property being an RDF property to begin with (this is very, very unlikely)
				{
					Debug.warn(illegalArgumentException);						
					continue;	//skip processing of this resource
				}					
			}
			else if(WEBDAV_NAMESPACE.equals(propertyNamespace))	//if this WebDAV property is not an RDF property, see if this property is in the WebDAV namespace
			{
				if(DISPLAY_NAME_PROPERTY_NAME.equals(propertyLocalName))	//D:displayname
				{
					final String displayName=propertyValue.getText().trim();	//get the display name TODO just trim control characters (e.g. CR, LF), as we want users to be able to add whitespace
					setLabel(resource, displayName);	//set the label as the display name of the WebDAV resource			
				}				
				else if(CREATION_DATE_PROPERTY_NAME.equals(propertyLocalName))	//D:creationdate
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
				else if(RESOURCE_TYPE_PROPERTY_NAME.equals(propertyLocalName))	//D:resourcetype
				{
					if(propertyValue instanceof WebDAVDocumentFragmentPropertyValue)	//if the WebDAV property represents a document fragment
					{
						final NodeList valueNodes=((WebDAVDocumentFragmentPropertyValue)propertyValue).getDocumentFragment().getChildNodes();	//get the children of the document fragment
						if(valueNodes.getLength()==1 && COLLECTION_TYPE.equals(createQualifiedName(valueNodes.item(0)).getReferenceURI()))	//if there is one child with a reference URI of D:collection
						{
							isCollection=true;	//show that this is a collection
							addType(resource, FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME);	//add the file:folder type to indicate that this resource is a folder
						}
					}
				}
				else if(GET_CONTENT_LANGUAGE_PROPERTY_NAME.equals(propertyLocalName))	//D:getcontentlanguage
				{
					final Locale contentLanguage=createLocale(propertyValue.getText().trim());	//get the content language string and create a locale from it
					setLanguage(resource, contentLanguage);	//set the dc:languate as the content length of the WebDAV resource			
				}				
				else if(GET_CONTENT_LENGTH_PROPERTY_NAME.equals(propertyLocalName))	//D:getcontentlength
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
				else if(GET_CONTENT_TYPE_PROPERTY_NAME.equals(propertyLocalName))	//D:getcontenttype
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
				else if(GET_LAST_MODIFIED_PROPERTY_NAME.equals(propertyLocalName))	//D:getlastmodified
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
		}

		
			//TODO fix filename encoding/decoding---there's no way to know what operating system the server is using
		
			//TODO encode in UTF-8
		
/*TODO del
		if(isCollection)	//if this is a collection TODO fix label better; deal with WebDAV display name
		{
			final String label=FileUtilities.decodeFilename(filename);	//unescape any reserved characters in the filename
			addLabel(resource, label); //add the filename as a label
		}
		else	//if this file is not a directory
		{
				//unescape any reserved characters in the filename and remove the extension
			final String label=FileUtilities.removeExtension(FileUtilities.decodeFilename(filename));
			addLabel(resource, label); //add the unescaped filename without an extension as a label
		}
*/
Debug.trace("just read resource from WebDAV:", RDFUtilities.toString(resource));	//TODO del
		return resource;	//return the resource that respresents the file
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
		@exception IOException if an I/O error occurs.
		*/
	  protected void afterClose() throws IOException
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

package com.globalmentor.marmot.repository.webdav;

import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.net.URIUtilities.*;
import static com.garretwilson.net.http.webdav.WebDAVConstants.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;
import static com.garretwilson.rdf.xpackage.MIMEOntologyUtilities.*;
import static java.util.Collections.*;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;

import com.garretwilson.io.FileUtilities;
import com.garretwilson.net.http.HTTPClient;
import com.garretwilson.net.http.webdav.Depth;
import com.garretwilson.net.http.webdav.WebDAVResource;
import com.garretwilson.rdf.RDF;
import com.garretwilson.rdf.RDFResource;
import com.garretwilson.rdf.RDFUtilities;
import com.garretwilson.rdf.rdfs.RDFSUtilities;
import com.garretwilson.rdf.xpackage.FileOntologyConstants;
import com.garretwilson.text.xml.QualifiedName;
import com.garretwilson.util.Debug;
import com.garretwilson.util.NameValuePair;
import com.globalmentor.marmot.repository.AbstractRepository;

/**Repository accessed via WebDAV.
<p>This burrow recognizes the XPackage resource type <code>file:folder</code>
	and creates a directory for each such resource. Directories will be created
	transparently for other resources with other types and media types as needed
	to store child resources.</p>
@author Garret Wilson
*/
public class WebDAVRepository extends AbstractRepository
{

	/**The URI represting the XPackage file:folder type.*/	//TODO check; use static imports 
	protected final static URI FILE_FOLDER_TYPE_URI=RDFUtilities.createReferenceURI(FileOntologyConstants.FILE_ONTOLOGY_NAMESPACE_URI, FileOntologyConstants.FOLDER_TYPE_NAME);	//TODO promote to parent file-based class		

	/**The extension used for directories to hold resource children.*/
	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/**The HTTP client used to create a connection to this resource.*/
	private final HTTPClient httpClient;

		/**@return The HTTP client used to create a connection to this resource.*/
		protected HTTPClient getHTTPClient() {return httpClient;}

	/**URI contructor using the default HTTP client.
	@param baseURI The WebDAV URI to be used as the base of all resources.
	*/
	public WebDAVRepository(final URI baseURI)
	{
		this(baseURI, null);	//construct a WebDAV repository with no label
	}
	
	/**URI and label contructor using the default HTTP client.
	@param baseURI The WebDAV URI to be used as the base of all resources.
	@param label The label for the burrow, or <code>null</code> if no label should be provided.
	*/
	public WebDAVRepository(final URI baseURI, final String label)
	{
		this(baseURI, HTTPClient.getInstance(), label);	//use the default HTTP client
	}

	/**URI, HTTP client, and label contructor.
	@param baseURI The WebDAV URI to be used as the base of all resources.
	@param httpClient The HTTP client used to create a connection to this resource.	
	@param label The label for the burrow, or <code>null</code> if no label should be provided.
	*/
	public WebDAVRepository(final URI baseURI, final HTTPClient httpClient, final String label)
	{
		super(baseURI, label);	//construct the parent class with the base URI and the label
		this.httpClient=httpClient;	//save the HTTP client
	}
	
	/**Gets an input stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@exception IOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getInputStream(final URI resourceURI) throws IOException
	{
		final WebDAVResource webdavResource=new WebDAVResource(resourceURI, getHTTPClient());	//create a WebDAV resource
		return webdavResource.getInputStream();	//return an input stream to the resource
	}
	
	/**Gets an output stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IOException if there is an error accessing the resource.
	*/
	public OutputStream getOutputStream(final URI resourceURI) throws IOException
	{
		final WebDAVResource webdavResource=new WebDAVResource(resourceURI, getHTTPClient());	//create a WebDAV resource
		return webdavResource.getOutputStream();	//return an output stream to the resource
	}
	
	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@exception IOException if there is an error accessing the repository.
	*/
	public RDFResource getResourceDescription(final URI resourceURI) throws IOException
	{
		if(getReferenceURI().equals(resourceURI))	//if this is the URI of the burrow
		{
			return this;	//return the burrow itself
		}
		else	//if this is some other URI
		{
			final RDF rdf=new RDF();	//G***use a common RDF data model
			final WebDAVResource webdavResource=new WebDAVResource(resourceURI, getHTTPClient());	//create a WebDAV resource
			final List<NameValuePair<QualifiedName, ?>> propertyList=webdavResource.propFind();	//get the properties of this resource
			return createResource(rdf, resourceURI, propertyList);	//create a resource from this URI and property list
		}
	}

	/**Determines if the resource at the given URI exists.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean exists(final URI resourceURI) throws IOException
	{
		final WebDAVResource webdavResource=new WebDAVResource(resourceURI, getHTTPClient());	//create a WebDAV resource
		return webdavResource.exists();	//see if the WebDAV resource exists		
	}

	/**Determines if the resource at a given URI is a collection.
	This is a convenience method to quickly determine if a resource exists at the given URI
	and retrieving that resource would result in a resource of type <code>file:Folder</code>.
	@param resourceURI The URI of the requested resource.
	@return <code>true</code> if the resource is a collection, else <code>false</code>.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean isCollection(final URI resourceURI) throws IOException
  {
		return false;	//TODO fix
  }

	/**Determines whether the resource represented by the given URI has children.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(final URI resourceURI) throws IOException
	{
		final WebDAVResource webdavResource=new WebDAVResource(resourceURI, getHTTPClient());	//create a WebDAV resource
		return webdavResource.propFind().size()>0;	//TODO fix to use cached values
	}

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resources descriptions directly under the given resource.
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws IOException
	{
//TODO del Debug.traceStack("!!!!!!!!getting child resource descriptions");
		if(depth!=0)	//a depth of zero means don't get child resources
		{
			final WebDAVResource webdavResource=new WebDAVResource(resourceURI, getHTTPClient());	//create a WebDAV resource
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
			final List<NameValuePair<URI, List<NameValuePair<QualifiedName, ?>>>> propertyLists;
			propertyLists=webdavResource.propFind(webdavDepth);	//get the properties of the resources
			final List<RDFResource> childResourceList=new ArrayList<RDFResource>(propertyLists.size());	//create a list of child resources no larger than the number of WebDAV resource property lists
			for(final NameValuePair<URI, List<NameValuePair<QualifiedName, ?>>> propertyList:propertyLists)	//look at each property list
			{
				final URI childResourceURI=propertyList.getName();
				if(!resourceURI.equals(childResourceURI))	//if this property list is *not* for this resource
				{
					childResourceList.add(createResource(rdf, childResourceURI, propertyList.getValue()));	//create a resource from this URI and property lists
				}
			}
//TODO do the special Marmot thing about checking for special Marmot directories
			
//TODO fix				Collections.sort(resourceList);	//sort the resource by URI
			return childResourceList;	//return the list of resources we constructed
		}
		else	//if a depth of zero was requested
		{
			return emptyList();	//return an empty list
		}
	}

	

	
	
	/*Creates a resource to represent this list of properties.
	@param rdf The RDF data model to use when creating this resource.
	@param referenceURI The reference URI of the property to create.
	@param propertyList The list of property qualified names paired with WebDAV property values.
	@return A resource representing the given WebDAV property list.
	*/
	protected RDFResource createResource(final RDF rdf, final URI resourceURI, List<NameValuePair<QualifiedName, ?>> propertyList)	//G***maybe rename to getResource() for consistency	
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
		for(final NameValuePair<QualifiedName, ?> webdavProperty:propertyList)	//look at each WebDAV property
		{
			final QualifiedName propertyName=webdavProperty.getName();	//get the property name
			final String propertyNamespace=propertyName.getNamespaceURI();	//get the string version of the property namespace
			final String propertyLocalName=propertyName.getLocalName();	//get the local name of the property
			final String propertyStringValue=asInstance(webdavProperty.getValue(), String.class);	//get the value of the property as a string, if it is a string
			final NameValuePair<QualifiedName, String> propertyComplexValue=(NameValuePair<QualifiedName, String>)asInstance(webdavProperty.getValue(), NameValuePair.class);	//get the value of the property as a string, if it is a string
			if(WEBDAV_NAMESPACE.equals(propertyNamespace))	//if this property is in the WebDAV namespace
			{
				//TODO fix displayname
				//TODO fix creationdate
				if(RESOURCE_TYPE_PROPERTY_NAME.equals(propertyLocalName) && propertyComplexValue!=null)	//D:resourcetype
				{
					if(COLLECTION_TYPE.equals(propertyComplexValue.getName()))	//if this is a collection
					{
						isCollection=true;	//show that this is a collection
						RDFUtilities.addType(resource, FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME);	//add the file:folder type to indicate that this resource is a folder
					}
				}
				//TODO fix contentlength
				if(GET_CONTENT_TYPE_PROPERTY_NAME.equals(propertyLocalName) && propertyStringValue!=null)	//D:getcontenttype
				{
					try
					{
						final ContentType contentType=new ContentType(propertyStringValue);	//create a content type object TODO check for errors
						if(contentType!=null)	//if we know the content type
						{
							setContentType(resource, contentType);	//set the content type property
						}
					}
					catch(final ParseException parseException)	//if the content type is not a correct MIME type
					{
						Debug.warn(parseException);	//TODO improve error handling
					}
				}
				//TODO fix getlastmodified				
			}
		}
		if(isCollection)	//if this is a collection TODO fix label better; deal with WebDAV display name
		{
			final String label=FileUtilities.decodeFilename(filename);	//unescape any reserved characters in the filename
			RDFSUtilities.addLabel(resource, label); //add the filename as a label
		}
		else	//if this file is not a directory
		{
				//unescape any reserved characters in the filename and remove the extension
			final String label=FileUtilities.removeExtension(FileUtilities.decodeFilename(filename));
			RDFSUtilities.addLabel(resource, label); //add the unescaped filename without an extension as a label
		}
		return resource;	//return the resource that respresents the file
	}

}

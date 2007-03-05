package com.globalmentor.marmot.repository.webdav;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import static java.util.Arrays.*;
import java.util.Date;

import static java.util.Collections.*;
import java.util.List;

import javax.mail.internet.*;

import org.w3c.dom.*;

import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.net.URIUtilities.*;
import static com.garretwilson.net.http.webdav.WebDAVConstants.*;
import static com.garretwilson.net.http.webdav.WebDAVUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;
import static com.garretwilson.rdf.xpackage.FileOntologyUtilities.*;
import static com.garretwilson.rdf.xpackage.MIMEOntologyUtilities.*;

import com.garretwilson.io.FileUtilities;
import com.garretwilson.io.OutputStreamDecorator;
import com.garretwilson.net.http.*;
import com.garretwilson.net.http.webdav.*;
import com.garretwilson.rdf.RDF;
import com.garretwilson.rdf.RDFResource;
import static com.garretwilson.rdf.RDFUtilities.*;
import static com.garretwilson.rdf.rdfs.RDFSUtilities.*;
import static com.garretwilson.text.xml.XMLUtilities.*;

import com.garretwilson.text.W3CDateFormat;
import com.garretwilson.text.xml.QualifiedName;
import com.garretwilson.util.Debug;
import com.garretwilson.util.NameValuePair;
import com.globalmentor.marmot.repository.AbstractRepository;

/**Repository accessed via WebDAV.
<p>This repository recognizes the XPackage resource type <code>file:folder</code>
	and creates a directory for each such resource. Directories will be created
	transparently for other resources with other types and media types as needed
	to store child resources.</p>
@author Garret Wilson
*/
public class WebDAVRepository extends AbstractRepository	//TODO fix to recognize and allow public/private URIs
{

	/**The URI represting the XPackage file:folder type.*/	//TODO check; use static imports 
//TODO move if needed	protected final static URI FILE_FOLDER_TYPE_URI=RDFUtilities.createReferenceURI(FileOntologyConstants.FILE_ONTOLOGY_NAMESPACE_URI, FileOntologyConstants.FOLDER_TYPE_NAME);	//TODO promote to parent file-based class		

	/**The extension used for directories to hold resource children.*/
//TODO move if needed	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/**The HTTP client used to create a connection to this resource.*/
	private final HTTPClient httpClient;

		/**@return The HTTP client used to create a connection to this resource.*/
		protected HTTPClient getHTTPClient() {return httpClient;}

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
	@exception IOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getResourceInputStream(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		return webdavResource.getInputStream();	//return an input stream to the resource
	}
	
	/**Gets an output stream to the contents of the resource specified by the given URI.
	An error is generated if the resource does not exist.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource TODO cache these resources, maybe
		if(!webdavResource.exists())	//if the resource doesn't already exist
		{
			throw new HTTPNotFoundException("Cannot open output stream to non-existent resource "+resourceURI);
		}
		return webdavResource.getOutputStream();	//return an output stream to the resource
	}
	
	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public RDFResource getResourceDescription(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		if(getReferenceURI().equals(resourceURI))	//if this is the URI of the repository
		{
			return this;	//return the repository itself
		}
		else	//if this is some other URI
		{
//TODO del Debug.traceStack("!!!!!!!!getting resource description for resource", resourceURI);
			final RDF rdf=new RDF();	//G***use a common RDF data model
			final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
			final List<WebDAVProperty> propertyList=webdavResource.propFind();	//get the properties of this resource
			return createResourceDescription(rdf, resourceURI, propertyList);	//create a resource from this URI and property list
		}
	}

	/**Determines if the resource at the given URI exists.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean resourceExists(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		return webdavResource.exists();	//see if the WebDAV resource exists		
	}

	/**Determines if the resource at a given URI is a collection.
	This is a convenience method to quickly determine if a resource exists at the given URI
	and retrieving that resource would result in a resource of type <code>file:Folder</code>.
	@param resourceURI The URI of the requested resource.
	@return <code>true</code> if the resource is a collection, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean isCollection(final URI resourceURI) throws IOException
  {
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		return false;	//TODO fix
  }

	/**Determines whether the resource represented by the given URI has children.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final URI privateResourceURI=getPrivateURI(resourceURI);	//get the URI of the resource in the private namespace
		final WebDAVResource webdavResource=new WebDAVResource(privateResourceURI, getHTTPClient());	//create a WebDAV resource
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

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resources descriptions directly under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws IOException
	{
//	TODO del Debug.traceStack("!!!!!!!!getting child resource descriptions for resource", resourceURI);
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		if(depth!=0)	//a depth of zero means don't get child resources
		{
			final URI privateResourceURI=getPrivateURI(resourceURI);	//get the URI of the resource in the private namespace
			final WebDAVResource webdavResource=new WebDAVResource(privateResourceURI, getHTTPClient());	//create a WebDAV resource
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
	@exception IOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI, final RDFResource resourceDescription) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		final OutputStream outputStream=webdavResource.getOutputStream();	//get an output stream to the WebDAV resource
		return new DescriptionWriterOutputStreamDecorator(outputStream, resourceURI, resourceDescription, webdavResource);	//wrap the output stream in a decorator that will update the WebDAV properties after the contents are stored		
	}

	/**Creates a new resource with the given description and contents.
	If a resource already exists at the given URI it will be replaced.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	@exception IOException if the resource could not be created.
	*/
	public RDFResource createResource(final URI resourceURI, final RDFResource resourceDescription, final byte[] resourceContents) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		webdavResource.put(resourceContents);	//create a WebDAV resource with the guven contents
		return getResourceDescription(resourceURI);	//return a description of the new resource
	}

	/**Creates a collection in the repository.
	@param collectionURI The URI of the collection to be created.
	@return A description of the collection that was created.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error creating the collection.
	*/
	public RDFResource createCollection(final URI collectionURI) throws IOException
	{
		checkResourceURI(collectionURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(collectionURI), getHTTPClient());	//create a WebDAV resource
		webdavResource.mkCol();	//create the collection
		return getResourceDescription(collectionURI);	//return a description of the new collection
	}

	/**Sets the properties of a resource based upon the given description.
	This version delegates to {@link #setResourceProperties(URI, RDFResource, WebDAVResource)}.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException Thrown if the resource properties could not be updated.
	*/
	public RDFResource setResourceProperties(final URI resourceURI, final RDFResource resourceDescription) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		return setResourceProperties(resourceURI, resourceDescription, webdavResource);	//set the properties using the WebDAV resource object
	}

	/**Sets the properties of a resource based upon the given description.
	This implementation only supports the {@value FileOntologyConstants#MODIFIED_TIME_PROPERTY_URI} property, ignoring all other properties.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@param webdavResource The WebDAV resource for setting the resource WebDAV properties
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
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
	@exception IOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		webdavResource.copy(getPrivateURI(destinationURI));	//copy the resource with an infinite depth, overwriting the destination resource if one exists
	}

	/**Deletes a resource.
	@param resourceURI The reference URI of the resource to delete.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if the resource could not be deleted.
	*/
	public void deleteResource(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		webdavResource.delete();	//delete the resource		
	}

	/**Moves a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final WebDAVResource webdavResource=new WebDAVResource(getPrivateURI(resourceURI), getHTTPClient());	//create a WebDAV resource
		webdavResource.move(getPrivateURI(destinationURI));	//move the resource with an infinite depth, overwriting the destination resource if one exists
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
//	TODO del Debug.trace("ready to create resource description for resource", resource, "with property count", propertyList.size());
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
		for(final WebDAVProperty webdavProperty:propertyList)	//look at each WebDAV property
		{
			final QualifiedName propertyName=webdavProperty.getName();	//get the property name
			final String propertyNamespace=propertyName.getNamespaceURI();	//get the string version of the property namespace
			final String propertyLocalName=propertyName.getLocalName();	//get the local name of the property
			final WebDAVPropertyValue propertyValue=webdavProperty.getValue();	//get the value of the property
//TODO del when works			final NodeList valueNodes=documentFragmentValue.getChildNodes();	//get the children of the document fragment
/*TODO del when works
			final String propertyStringValue=asInstance(webdavProperty.getValue(), String.class);	//get the value of the property as a string, if it is a string
			final NameValuePair<QualifiedName, String> propertyComplexValue=(NameValuePair<QualifiedName, String>)asInstance(webdavProperty.getValue(), NameValuePair.class);	//get the value of the property as a string, if it is a string
*/
			if(WEBDAV_NAMESPACE.equals(propertyNamespace))	//if this property is in the WebDAV namespace
			{
				//TODO fix displayname
				//TODO fix creationdate
				if(RESOURCE_TYPE_PROPERTY_NAME.equals(propertyLocalName))	//D:resourcetype
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
				//TODO fix contentlength
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
				else if(GET_CONTENT_TYPE_PROPERTY_NAME.equals(propertyLocalName))	//D:getlastmodified
				{
					final HTTPDateFormat httpFormat=new HTTPDateFormat(HTTPDateFormat.Style.RFC1123);	//create an HTTP date formatter; WebDAV prefers the RFC 1123 style, as does HTTP
					try
					{
						final Date lastModifiedDate=httpFormat.parse(propertyValue.getText().trim());	//parse the date
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
		return resource;	//return the resource that respresents the file
	}

	/**Creates an output stream that updates the properties of a WebDAV resource after its contents are stored.
	@author Garret Wilson
	*/
	protected class DescriptionWriterOutputStreamDecorator extends OutputStreamDecorator<OutputStream>
	{
		/**The reference URI to use to identify the resource.*/
		private final URI resourceURI;

			/**@protected The reference URI to use to identify the resource.*/
			public URI getResourceURI() {return resourceURI;}
	
		/**The WebDAV resource for updating the WebDAV properties.*/
		private final WebDAVResource webdavResource;

			/**@return The WebDAV resource for updating the WebDAV properties.*/
			protected WebDAVResource getWebDAVResource() {return webdavResource;}

		/**The description of the resource to store as WebDAV properties.*/
		private final RDFResource resourceDescription;

			/**@return The description of the resource to store as WebDAV properties.*/
			protected RDFResource getResourceDescription() {return resourceDescription;}

		/**Decorates the given output stream.
		@param outputStream The output stream to decorate
		@param resourceURI The reference URI to use to identify the resource.
		@param resourceDescription The description of the resource to store as WebDAV properties.
		@param webdavResource The WebDAV resource for updating the WebDAV properties.
		@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
		*/
		public DescriptionWriterOutputStreamDecorator(final OutputStream outputStream, final URI resourceURI, final RDFResource resourceDescription, final WebDAVResource webdavResource)
		{
			super(outputStream);	//construct the parent class
			this.resourceURI=checkInstance(resourceURI, "Resource URI cannot be null.");
			this.resourceDescription=checkInstance(resourceDescription, "Resource description cannot be null.");
			this.webdavResource=checkInstance(webdavResource, "WebDAV resource cannot be null.");
		}
	
	  /**Called after the stream is successfully closed.
		This version updates the WebDAV properties to reflect the given resource description.
		@exception IOException if an I/O error occurs.
		*/
	  protected void afterClose() throws IOException
	  {
			setResourceProperties(getResourceURI(), getResourceDescription(), getWebDAVResource());	//set the properties using the WebDAV resource object
	  }

	}

}

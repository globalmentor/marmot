package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.mail.internet.ContentType;

import static com.garretwilson.io.FileUtilities.*;
import static com.garretwilson.io.OutputStreamUtilities.*;
import static com.garretwilson.lang.ObjectUtilities.*;
import com.garretwilson.net.*;
import static com.garretwilson.net.URIUtilities.*;
import com.garretwilson.urf.*;

import com.globalmentor.marmot.security.MarmotSecurity;
import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**Abstract implementation of a repository class with typical features.
<p>Resource access methods should call {@link #checkResourceURI(URI)} as a security check to ensure the given URI is within the repository.</p>
@author Garret Wilson
*/
public abstract class AbstractRepository extends DefaultURFResource implements Repository
{

	/**The resource factory for resources in the Marmot security namespace.*/
	protected final static URFResourceFactory MARMOT_SECURITY_RESOURCE_FACTORY=new JavaURFResourceFactory(MarmotSecurity.class.getPackage());

	/**The registered event listeners.*/
//TODO bring back when needed	protected final EventListenerManager eventListenerManager=new EventListenerManager();

	/**Sets the URI.
	If there currently is no private repository URI, it will be updated to match the given public repository URI.
	@param uri The new URI, or <code>null</code> if there is no URI.
	*/
	protected void setURI(final URI uri)
	{
			//TODO check for the URI being set to null
		super.setURI(uri);	//set the URI normally
		if(getPrivateRepositoryURI()==null)	//if there is no private repository URI
		{
			setPrivateRepositoryURI(uri);	//update the private repository URI to match
		}
	}

	/**Whether the repository has been opened for access.*/
	private boolean open=false;

	/**The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository.*/
	private URI privateRepositoryURI=null;

		/**@return The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository.*/
		public URI getPrivateRepositoryURI() {return privateRepositoryURI;}

		/**Sets the base URI of the private URI namespace being managed.
		@param privateRepositoryURI The base URI of the private URI namespace being managed.
		@exception NullPointerException if the given URI is <code>null</code>.
		*/
		public void setPrivateRepositoryURI(final URI privateRepositoryURI)
		{
			this.privateRepositoryURI=checkInstance(privateRepositoryURI, "Private repository URI must not be null.").normalize();
		}

		/**@return The base URI of the public URI namespace being managed; equivalent to {@link #getURI()}.*/
		public URI getPublicRepositoryURI() {return getURI();}

		/**Sets the base URI of the public URI namespace being managed, reference URI of the repository.
		If there currently is no private repository URI, it will be updated to match the given public repository URI.
		@param publicRepositoryURI The base URI of the public URI namespace being managed.
		@exception NullPointerException if the given URI is <code>null</code>.
		*/
		public void setPublicRepositoryURI(final URI publicRepositoryURI)
		{
			setURI(checkInstance(publicRepositoryURI, "Public repository URI must not be null.").normalize());
		}

		/**Translates a public URI in the repository to the equivalent private URI in the private URI namespace.
		@param publicURI The URI in the public URI namesapce.
		@return A URI equivalent to the public URI in the private URI namespace.
		*/
		protected URI getPrivateURI(final URI publicURI)
		{
			return changeBase(publicURI, getURI(), getPrivateRepositoryURI());	//change the base of the URI from the public URI namespace to the private URI namespace
		}

		/**Translates a private URI to the equivalent public URI in the public repository URI namespace.
		@param privateURI The URI in the private URI namesapce.
		@return A URI equivalent to the private URI in the public repository URI namespace.
		*/
		protected URI getPublicURI(final URI privateURI)
		{
			return changeBase(privateURI, getPrivateRepositoryURI(), getURI());	//change the base of the URI from the private URI namespace to the public URI namespace
		}

	/**Whether the repository should automatically be opened when needed.*/
	private boolean autoOpen=true;

		/**@return Whether the repository should automatically be opened when needed.*/
		public boolean isAutoOpen() {return autoOpen;}

		/**Sets whether the repository should automatically be opened when needed.
		@param autoOpen Whether the repository should automatically be opened when needed.
		*/
		public void setAutoOpen(final boolean autoOpen) {this.autoOpen=autoOpen;}

	/**Checks to make sure the resource designated by the given resource URI is within this repository.
	This version makes sure the given URI is a child of the resource reference URI.
	@param resourceURI The URI of the resource to check.
	@return The normalized form of the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	*/
	protected URI checkResourceURI(URI resourceURI)
	{
		resourceURI=checkInstance(resourceURI, "Resource URI cannot be null.").normalize();	//normalize the URI
		if(!isChild(getURI(), resourceURI))	//if the given resource URI does not designate a resource within this repository's URI namespace (this will normalize the URI, but as we need to return a normalized form it's better to normalize first so that actual normalization changes won't have to be done twice)
		{
			throw new IllegalArgumentException(resourceURI+" does not designate a resource within the repository "+getURI());
		}
		return resourceURI;	//return the normalized form of the resource URI
	}

	/**Checks to make sure that the repository is open.
	If the auto-open facility is turned on, the repository will be automatically opened if needed.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error opening the repository.
	@see #isAutoOpen()
	*/
	protected void checkOpen() throws ResourceIOException
	{
		if(!isOpen())	//if the repository is not open
		{
			if(isAutoOpen())	//if we should open the repository automatically
			{
				open();	//open the repository
			}
			else	//if we shouldn't open the repository automatically
			{
				throw new IllegalStateException("Repository is not open.");
			}
		}
	}

	/**The map of content types mapped to lowercase URI name extensions.*/
	private final Map<String, ContentType> extensionContentTypeMap=new HashMap<String, ContentType>(FILE_EXTENSION_CONTENT_TYPE_MAP);

	/**Associates the given content type with the given extension, without regard to case.
	@param extension The URI name extension with which the content type should be associated, or <code>null</code> if the content type should be associated with resources that have no extension.
	@param contentType The content type to associate with the given extension.
	@return The content type previously registered with the given extension, or <code>null</code> if no content type was previously registered.
	@exception NullPointerException if the given content type is <code>null</code>.
	*/
	public ContentType registerExtensionContentType(final String extension, final ContentType contentType)
	{
		return extensionContentTypeMap.put(extension!=null ? extension.toLowerCase() : null, checkInstance(contentType, "Content type cannot be null."));
	}

	/**Returns the content type assciated with the given extension, without regard to case.
	@param extension The URI name extension with which the content type is associated, or <code>null</code> if the content type is associated with resources that have no extension.
	@return The content type associated with the given extension, or <code>null</code> if there is no content type associated with the given extension.
	*/
	public ContentType getExtensionContentType(final String extension)
	{
		return extensionContentTypeMap.get(extension!=null ? extension.toLowerCase() : null);	//return the content type, if any, associated with the given extension
	}

	/**Default constructor with no settings.
	Settings must be configured before repository is opened.
	*/
	public AbstractRepository()
	{
	}

	/**URI contructor with no separate private URI namespace.
	@param repositoryURI The URI identifying the location of this repository.
	@exception NullPointerException if the given respository URI is <code>null</code>.
	*/
	public AbstractRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI contructor.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@exception NullPointerException if one of the given respository URIs is <code>null</code>.
	*/
	public AbstractRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		super(checkInstance(publicRepositoryURI, "Public repository URI cannot be null.").normalize());	//construct the parent class with the public reference URI
		this.privateRepositoryURI=checkInstance(privateRepositoryURI, "Private repository URI cannot be null.").normalize();
	}

	/**Creates a default empty URF data model.
	The correct resource factories will be installed to create appropriate classes in the Marmot namespace.
	@return A new default URF data model.
	*/
	protected URF createURF()
	{
		final URF urf=new URF();	//create a new URF data model
		urf.registerResourceFactory(MARMOT_SECURITY_NAMESPACE_URI, MARMOT_SECURITY_RESOURCE_FACTORY);	//register the Marmot resource factory with the data model
		return urf;	//return the new data model
	}

	/**@return Whether the repository has been opened for access.*/
	public boolean isOpen() {return open;}

	/**Opens the repository for access.
	If the repository is already open, no action occurs.
	At a minimum the respository must have a public and a private URI specified, even though these may both be the same URI.
	@exception IllegalStateException if the settings of this repository are inadequate to open the repository.
	@exception ResourceIOException if there is an error opening the repository.
	*/
	public void open() throws ResourceIOException
	{
		if(!isOpen())	//if the repository isn't yet open TODO synchronize
		{
			if(getPrivateRepositoryURI()==null)	//if the private repository URI is not set
			{
				throw new IllegalStateException("Cannot open repository without private repository URI specified.");
			}
			if(getPublicRepositoryURI()==null)	//if the public repository URI is not set
			{
				throw new IllegalStateException("Cannot open repository without public repository URI specified.");
			}
			open=true;	//show that the repository is now open
		}
	}

	/**Closes the repository.
	If the repository is already closed, no action occurs.
	@exeption ResourceIOException if there is an error closing the repository.
	*/
	public void close() throws ResourceIOException
	{
		if(isOpen())	//if the repository is open TODO synchronize
		{
			open=false;	//show that the repository is now closed
		}
	}

	/**Creates a new resource with a default description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
	If a resource with no contents is desired, {@link #createResource(URI, byte[])} with zero bytes is better suited for this task.
	This implementation delegates to {@link #createResource(URI, URFResource)} with a default description.
	@param resourceURI The reference URI to use to identify the resource.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI) throws ResourceIOException
	{
		return createResource(resourceURI, new DefaultURFResource());	//create the resource with a default description
	}

	/**Creates a new resource with a default description and contents.
	If a resource already exists at the given URI it will be replaced.
	This implementation delegates to {@link #createResource(URI, URFResource, byte[])} with a default description.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final URI resourceURI, final byte[] resourceContents) throws ResourceIOException
	{
		return createResource(resourceURI, new DefaultURFResource(), resourceContents);	//create the resource with a default description
	}

	/**Retrieves immediate child resources of the resource at the given URI.
	This implementation retrieves a single-level list of descriptions by calling {@link #getChildResourceDescriptions(URI, int)}.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@return A list of sub-resource descriptions directly under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(final URI resourceURI) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		return getChildResourceDescriptions(resourceURI, 1);	//get a list of child resource descriptions without going deeper than one level
	}

	/**Determines the URI of the collection resource of the given URI; either the given resource URI if the resource represents a collection, or the parent resource if not.
	If the given resource URI is a collection URI this method returns the given resource URI.
	If the given resource URI is not a collection URI, this implementation returns the equivalent of resolving the path {@value URIConstants#CURRENT_LEVEL_PATH_SEGMENT} to the URI.
	@param resourceURI The URI of the resource for which the collection resource URI should be returned.
	@return The URI of the indicated resource's deepest collection resource, or <code>null</code> if the given URI designates a non-collection resource with no collection parent.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URI getCollectionURI(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		checkOpen();	//make sure the repository is open
		return isCollectionURI(resourceURI) ? resourceURI : getCurrentLevel(resourceURI);	//if URI is a collection URI, return the URI; otherwise, get the current level
	}

	/**Determines the URI of the parent resource of the given URI.
	If the given resource URI is a collection URI this implementation returns the equivalent of resolving the path {@value URIConstants#PARENT_LEVEL_PATH_SEGMENT} to the URI.
	if the given resource URI is not a collection URI, this implementation returns the equivalent of resolving the path {@value URIConstants#CURRENT_LEVEL_PATH_SEGMENT} to the URI.
	If the given resource represents this repository, this implementation returns <code>null</code>.
	@param resourceURI The URI of the resource for which the parent resource URI should be returned.
	@return The URI of the indicated resource's parent resource, or <code>null</code> if the given URI designates a resource with no parent.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URI getParentResourceURI(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		checkOpen();	//make sure the repository is open
		if(resourceURI.equals(getPublicRepositoryURI()))	//if the resource is the repository URI
		{
			return null;	//the repository level has no parent
		}
		return isCollectionURI(resourceURI) ? getParentLevel(resourceURI) : getCurrentLevel(resourceURI);	//if resource is a collection URI, get the parent level; otherwise, get the current level
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
		throw new UnsupportedOperationException("Repository cannot yet delete its resources' properties");
	}

	/**Creates an infinitely deep copy of a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link Repository#copyResource(URI, URI, boolean)}.
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException
	{
		copyResource(resourceURI, destinationURI, true);	//copy the resource, overwriting any resource at the destination
	}

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link #copyResource(URI, Repository, URI, boolean)}.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException
	{
		copyResource(resourceURI, destinationRepository, destinationURI, true);	//copy the resource, overwriting any resource at the destination
	}

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository, overwriting any resource at the destionation only if requested.
	This version delegates to {@link Repository#copyResource(URI, URI, boolean)} if the given repository is this repository.
	Otherwise, this version performs a default copy operation.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		if(destinationRepository==this)	//if the resource is being copied to this repository
		{
			copyResource(resourceURI, destinationURI, overwrite);	//delegate to the internal copy method
		}
		else	//if the resource is being copied to another repository
		{
			try
			{
	//TODO del Debug.trace("ready to create resource", destinationURI, "in destination repository", destinationRepository.getReferenceURI());
					//TODO check for non-existent source resource
				final InputStream inputStream=getResourceInputStream(resourceURI);	//get an input stream to the source resource
				try
				{
						//TODO create an overwrite-aware createResource() method
					final OutputStream outputStream=destinationRepository.createResource(destinationURI, getResourceDescription(resourceURI));	//create the destination resource with the same description as the source resource, getting an output stream for storing the contents
					try
					{
						copy(inputStream, outputStream);	//copy the resource
					}
					finally
					{
						outputStream.close();	//always close the output stream
					}
				}
				finally
				{
					inputStream.close();	//always close the input stream
				}
				//TODO copy the properties
			}
			catch(final IOException ioException)	//if an I/O exception occurs
			{
				throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
			}
		}
	}

	/**Moves a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link Repository#moveResource(URI, URI, boolean)}.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException
	{
		moveResource(resourceURI, destinationURI, true);	//move the resource, overwriting any resource at the destination
	}

	/**Moves a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link #moveResource(URI, Repository, URI, boolean)}.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException
	{
		moveResource(resourceURI, destinationRepository, destinationURI, true);	//move the resource, overwriting any resource at the destination
	}

	/**Moves a resource to the specified URI in the specified repository, overwriting any resource at the destionation only if requested.
	This version delegates to {@link Repository#moveResource(URI, URI, boolean)} if the given repository is this repository.
	Otherwise, this version delegates to {@link Repository#copyResource(URI, Repository, URI, boolean)} and then delegates to {@link Repository#deleteRepository(URI)}.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		if(destinationRepository==this)	//if the resource is being moved to this repository
		{
			moveResource(resourceURI, destinationURI, overwrite);	//delegate to the internal move method
		}
		else	//if the resource is being moved to another repository
		{
			if(resourceURI.normalize().equals(getPublicRepositoryURI()))	//if they try to move the root URI
			{
				throw new IllegalArgumentException("Cannot move repository base URI "+resourceURI);
			}
			copyResource(resourceURI, destinationRepository, destinationURI, overwrite);	//copy the resource to the other repository
			deleteResource(resourceURI);	//delete the moved resource
		}
	}

	/**Translates the given error specific to the this repository type into a resource I/O exception.
	This version returns the given throwable if it is already a {@link ResourceIOException};
	otherwise, it simply wraps the given throwable in a {@link ResourceIOException}.
	@param resourceURI The URI of the resource to which the exception is related.
	@param throwable The error which should be translated to a resource I/O exception.
	@return A resource I/O exception based upon the given throwable.
	*/
	protected ResourceIOException createResourceIOException(final URI resourceURI, final Throwable throwable)
	{
		return throwable instanceof ResourceIOException ? (ResourceIOException)throwable : new ResourceIOException(resourceURI, throwable);	//default to simple exception chaining with a new resource I/O exception, if the throwable isn't already a resourc I/O exception
	}

	/**Cleans up the object for garbage collection.
	This version closes the repository.
	*/
	protected void finalize() throws Throwable
	{
		try
		{
			close();	//close the repository if it isn't already
		}
		finally
		{
			super.finalize();	//always call the parent version
		}
	}
}

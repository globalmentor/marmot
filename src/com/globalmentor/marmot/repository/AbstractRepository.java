package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import com.garretwilson.event.*;
import static com.garretwilson.io.OutputStreamUtilities.*;
import static com.garretwilson.lang.ByteConstants.*;
import com.garretwilson.lang.ClassUtilities;
import static com.garretwilson.lang.ObjectUtilities.*;
import static com.garretwilson.net.URIUtilities.*;
import com.garretwilson.rdf.*;
import com.garretwilson.rdf.rdfs.RDFSUtilities;
import com.garretwilson.util.Debug;

import static com.globalmentor.marmot.MarmotConstants.*;

/**Abstract repository class the implements common features of a burrow.
<p>Resource access methods should call {@link #checkResourceURI(URI)} as a security check to ensure the given URI is within the repository.
<p>This implementation automatically adds an <code>rdf:type</code> property of <code>marmot:XXXRepository</code>, where "XXXRepository" is the local name of the repository class.</p>
@author Garret Wilson
*/
public abstract class AbstractRepository extends TypedRDFResource implements Repository
{

	/**The registered event listeners.*/
	protected final EventListenerManager eventListenerManager=new EventListenerManager();

	/**Sets the reference URI of the resource.
	If there currently is no private repository URI, it will be updated to match the given public repository URI.
	@param uri The new reference URI, or <code>null</code> if the identifier is not known.
	*/
	public void setReferenceURI(final URI uri)
	{
			//TODO check for the reference URI being set to null
		super.setReferenceURI(uri);	//set the refeference URI normally
		if(getPrivateRepositoryURI()==null)	//if there is no private repository URI
		{
			setPrivateRepositoryURI(uri);	//update the private repository URI to match
		}
	}

	/**@return The namespace URI of the ontology defining the default type of this resource.*/
	public URI getDefaultTypeNamespaceURI() {return MARMOT_NAMESPACE_URI;}

	/**@return The local name of the default type of this resource.*/
	public String getDefaultTypeName() {return ClassUtilities.getLocalName(getClass());}

	/**Whether the repository has been opened for access.*/
	private boolean open=false;

	/**The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository.*/
	private URI privateRepositoryURI=null;

		/**@return The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository.*/
		public URI getPrivateRepositoryURI() {return privateRepositoryURI;}

		/**Sets the base URI of the private URI namespace being managed.
		@param privateRepositoryURI The base URI of the private URI namespace being managed.
		@exception NullPointerException if the given URI is null.
		*/
		public void setPrivateRepositoryURI(final URI privateRepositoryURI)
		{
			this.privateRepositoryURI=checkInstance(privateRepositoryURI, "Private repository URI must not be null.");
		}
		
		/**@return The base URI of the public URI namespace being managed; equivalent to {@link #getReferenceURI()}.*/
		public URI getPublicRepositoryURI() {return getReferenceURI();}
	
		/**Sets the base URI of the public URI namespace being managed, reference URI of the repository.
		If there currently is no private repository URI, it will be updated to match the given public repository URI.
		@param publicRepositoryURI The base URI of the public URI namespace being managed.
		@exception NullPointerException if the given URI is null.
		*/
		public void setPublicRepositoryURI(final URI publicRepositoryURI)
		{
			setReferenceURI(checkInstance(publicRepositoryURI, "Public repository URI must not be null."));
		}
		
		/**Translates a public URI in the repository to the equivalent private URI in the private URI namespace.
		@param publicURI The URI in the public URI namesapce.
		@return A URI equivalent to the public URI in the private URI namespace.
		*/
		protected URI getPrivateURI(final URI publicURI)
		{
			return changeBase(publicURI, getReferenceURI(), getPrivateRepositoryURI());	//change the base of the URI from the public URI namespace to the private URI namespace
		}

		/**Translates a private URI to the equivalent public URI in the public repository URI namespace.
		@param privateURI The URI in the private URI namesapce.
		@return A URI equivalent to the private URI in the public repository URI namespace.
		*/
		protected URI getPublicURI(final URI privateURI)
		{
			return changeBase(privateURI, getPrivateRepositoryURI(), getReferenceURI());	//change the base of the URI from the private URI namespace to the public URI namespace
		}

	/**Checks to make sure the resource designated by the given resource URI is within this repository.
	This version makes sure the given URI is a child of the resource reference URI.
	@param resourceURI The URI of the resource to check.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	*/
	protected void checkResourceURI(final URI resourceURI)
	{
		if(!isChild(getReferenceURI(), resourceURI))	//if the given resource URI does not designate a resource within this repository's URI namespace
		{
			throw new IllegalArgumentException(resourceURI+" does not designate a resource within the repository "+getReferenceURI());
		}		
	}

	/**Checks to make sure that the repository is open.
	@exception IllegalStateException if the repository is not open for access.
	*/
	protected void checkOpen()
	{
		if(!isOpen())	//if the repository is not open
		{
			throw new IllegalArgumentException("Repository is not open.");
		}
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
		super(checkInstance(publicRepositoryURI, "Public repository URI cannot be null."));	//construct the parent class with the public reference URI
		this.privateRepositoryURI=checkInstance(privateRepositoryURI, "Private repository URI cannot be null.");
	}

	/**@return Whether the repository has been opened for access.*/
	public boolean isOpen() {return open;}

	/**Opens the repository for access.
	If the repository is already open, no action occurs.
	At a minimum the respository must have a public and a private URI specified, even though these may both be the same URI. 
	@exception IllegalStateException if the settings of this repository are inadequate to open the repository.
	@exception IOException if there is an error opening the repository.
	*/
	public void open() throws IOException
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
	@exeption IOException if there is an error closing the repository.
	*/
	public void close() throws IOException
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
	This implementation delegates to {@link #createResource(URI, RDFResource)} with a default description.
	@param resourceURI The reference URI to use to identify the resource.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception IOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI) throws IOException
	{
		return createResource(resourceURI, new DefaultRDFResource());	//create the resource with a default description
	}

	/**Creates a new resource with a default description and contents.
	If a resource already exists at the given URI it will be replaced.
	This implementation delegates to {@link #createResource(URI, RDFResource, byte[])} with a default description.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception IOException if the resource could not be created.
	*/
	public RDFResource createResource(final URI resourceURI, final byte[] resourceContents) throws IOException
	{
		return createResource(resourceURI, new DefaultRDFResource(), resourceContents);	//create the resource with a default description
	}

	/**Retrieves immediate child resources of the resource at the given URI.
	This implementation retrieves a single-level list of descriptions by calling {@link #getChildResourceDescriptions(URI, int)}.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@return A list of sub-resource descriptions directly under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access.
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		checkOpen();	//make sure the repository is open
		return getChildResourceDescriptions(resourceURI, 1);	//get a list of child resource descriptions without going deeper than one level
	}

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link #copyResource(URI, URI)} if the given repository is this repository.
	Otherwise, this version performs a default copy operation.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@exception IOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws IOException
	{
		if(destinationRepository==this)	//if the resource is being copied to this repository
		{
			copyResource(resourceURI, destinationURI);	//delegate to the internal copy method
		}
		else	//if the resource is being copied to another repository
		{
//TODO del Debug.trace("ready to create resource", destinationURI, "in destination repository", destinationRepository.getReferenceURI());
Debug.trace("ready to create resource", destinationURI, "in destination repository", destinationRepository.getReferenceURI());
				//TODO check for non-existent source resource
			final InputStream inputStream=getResourceInputStream(resourceURI);	//get an input stream to the source resource
			try
			{
Debug.trace("ready to create resource in destination");
				final OutputStream outputStream=destinationRepository.createResource(destinationURI, getResourceDescription(resourceURI));	//create the destination resource with the same description as the source resource, getting an output stream for storing the contents
				try
				{
Debug.trace("ready to copy");
					copy(inputStream, outputStream);	//copy the resource
				}
				finally
				{
Debug.trace("closing copy output stream");
					outputStream.close();	//always close the output stream
				}
			}
			finally
			{
				inputStream.close();	//always close the input stream
			}
			//TODO copy the properties
		}
	}

	/**Moves a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link #moveResource(URI, URI)} if the given repository is this repository.
	Otherwise, this version delegates to {@link #copyResource(URI, Repository, URI)} and then delegates to {@link #deleteRepository(URI)}.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@exception IOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws IOException
	{
		if(destinationRepository==this)	//if the resource is being moved to this repository
		{
			moveResource(resourceURI, destinationURI);	//delegate to the internal move method
		}
		else	//if the resource is being moved to another repository
		{
			copyResource(resourceURI, destinationRepository, destinationURI);	//copy the resource to the other repository
			deleteResource(resourceURI);	//delete the moved resource
		}
	}

}

package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import com.garretwilson.event.*;
import static com.garretwilson.io.OutputStreamUtilities.*;
import com.garretwilson.lang.ClassUtilities;
import com.garretwilson.rdf.*;
import com.garretwilson.rdf.rdfs.RDFSUtilities;
import static com.globalmentor.marmot.MarmotConstants.*;

/**Abstract repository class the implements common features of a burrow.
<p>The burrow automatically adds an <code>rdf:type</code> property of
	<code>marmot:XXXBurrow</code>, where "XXXBurrow" is the local name of the
	class.</p>
@author Garret Wilson
*/
public abstract class AbstractRepository extends TypedRDFResource implements Repository
{

	/**The registered event listeners.*/
	protected final EventListenerManager eventListenerManager=new EventListenerManager();

	/**@return The namespace URI of the ontology defining the default type of this resource.*/
	public URI getDefaultTypeNamespaceURI() {return MARMOT_NAMESPACE_URI;}

	/**@return The local name of the default type of this resource.*/
	public String getDefaultTypeName() {return ClassUtilities.getLocalName(getClass());}

	/**URI contructor.
	@param repositoryURI The URI identifying the location of this repository.
	*/
	public AbstractRepository(final URI repositoryURI)
	{
		this(repositoryURI, null);	//construct the repository with no label
	}
	
	/**URI and label contructor.
	@param repositoryURI The URI identifying the location of this repository.
	@param label The label for the repository, or <code>null</code> if no label should be provided.
	*/
	public AbstractRepository(final URI repositoryURI, final String label)
	{
		super(repositoryURI);	//construct the parent class with the reference URI
		final RDF rdf=new RDF();	//G***use a common RDF data model
		if(label!=null)	//if a label was provided
		{
			RDFSUtilities.setLabel(this, label);	//replace all labels with the one provided TODO use static import
		}
	}

	/**Retrieves immediate child resources of the resource at the given URI.
	This implementation retrieves a single-level list of descriptions by calling {@link #getChildResourceDescriptions(URI, int)}.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@return A list of sub-resource descriptions directly under the given resource.
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI) throws IOException
	{
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
			final InputStream inputStream=getResourceInputStream(resourceURI);	//get an input stream to the source resource
			try
			{
				final OutputStream outputStream=destinationRepository.getResourceOutputStream(destinationURI);	//ask the destination repository for an output stream to the destination URI
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

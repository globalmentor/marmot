package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.*;
import java.util.*;

import com.garretwilson.event.*;
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
	
}

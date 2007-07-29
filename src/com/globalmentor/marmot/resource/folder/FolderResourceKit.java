package com.globalmentor.marmot.resource.folder;

import static com.garretwilson.rdf.RDFUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;

import java.net.URI;

import com.garretwilson.net.ResourceIOException;
import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.AbstractResourceKit;

/**Resource kit for handling folder resources that have no content but can contain children.
<p>Supported RDF resource types:</p>
<ul>
<li><code>file:Folder</code></li>
</ul>
@author Garret Wilson
*/
public class FolderResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public FolderResourceKit()
	{
		super(createReferenceURI(FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME), Capability.CREATE);
	}

	/**Creates a new resource with the appropriate default contents for this resource type.
	If a resource already exists at the given URI it will be replaced.
	This implementation creates a new collection.
	@param resourceURI The reference URI to use to identify the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the resource could not be created.
	*/
	public RDFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		return repository.createCollection(resourceURI);	//create a new collection
	}
}

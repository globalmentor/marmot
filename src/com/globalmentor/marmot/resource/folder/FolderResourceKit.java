package com.globalmentor.marmot.resource.folder;

import java.net.URI;

import static com.garretwilson.rdf.RDFUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;

import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.AbstractResourceKit;

import static com.guiseframework.theme.Theme.*;

/**Resource kit for handling folder resources that have no content but can contain children.
<p>Supported RDF resource types:</p>
<ul>
<li><code>file:Folder</code></li>
</ul>
@author Garret Wilson
*/
public class FolderResourceKit extends AbstractResourceKit
{

	/**Returns the URI of an open icon representing the given resource.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for an open tree node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getOpenTreeNodeIconURI(final Repository repository, final RDFResource resource)
	{
		return ICON_FOLDER_OPEN;
	}

	/**Default constructor.*/
	public FolderResourceKit()
	{
		super(ICON_FOLDER, createReferenceURI(FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME));
	}
}

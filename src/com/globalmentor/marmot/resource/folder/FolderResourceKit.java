package com.globalmentor.marmot.resource.folder;

import java.net.URI;

import static com.garretwilson.rdf.RDFUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;

import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.AbstractResourceKit;
import com.globalmentor.marmot.resource.Presentation;

import static com.guiseframework.theme.Theme.*;

/**Resource kit for handling folder resources that have no content but can contain children.
<p>Supported RDF resource types:</p>
<ul>
<li><code>file:Folder</code></li>
</ul>
@author Garret Wilson
@param <P> The type of presentation supported by this resource kit.
*/
public class FolderResourceKit<P extends Presentation> extends AbstractResourceKit<P>
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

	/**Presentation constructor.
	@param presentation The presentation implementation for supported resources.
	@exception NullPointerException if the given presentation is <code>null</code>.
	*/
	public FolderResourceKit(final P presentation)
	{
		super(presentation, ICON_FOLDER, createReferenceURI(FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME));
	}
}

package com.globalmentor.marmot.resource.folder;

import static com.garretwilson.rdf.RDFUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;

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
		super(createReferenceURI(FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME));
	}
}

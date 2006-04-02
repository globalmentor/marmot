package com.globalmentor.marmot.resource;

import java.io.IOException;

import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;

/**Abstract implementation of a resource kit.
@author Garret Wilson
*/
public abstract class AbstractResourceKit implements ResourceKit
{

	/**Initializes a resource description, creating whatever properties are appropriate.
	This version does nothing.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception IOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final RDFResource resource) throws IOException
	{
	}

}

package com.globalmentor.marmot;

import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.ResourceKit;

/**Marmot session information.
@author Garret Wilson
*/
public interface MarmotSession
{

	/**Registers a resource kit with the session. If the resource kit is already registered, no action occurs.
	@param resourceKit The resource kit to register.
	*/
	public void registerResourceKit(final ResourceKit resourceKit);

	/**Unregisters a resource kit with the session. If the resource kit is not registered, no action is taken.
	@param resourceKit The resource kit to unregister.
	*/
	public void unregisterResourceKIt(final ResourceKit resourceKit);

	/**Retrieves a resource kit appropriate for the given resource.
	This method locates a resource kit in the following priority:
	<ol>
		<li>The first resource kit supporting the resource content type.</li>
		<li>The first resource kit supporting one of the resource types.</li>
		<li>The default resource kit.</li>
	</ol>
	@param repository The repository in which the resource resides.
	@param resource The resource for which a resource kit should be returned.
	@return A resource kit to handle the given resource.
	*/
	public ResourceKit getResourceKit(final Repository repository, final RDFResource resource);
	
}
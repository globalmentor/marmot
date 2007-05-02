package com.globalmentor.marmot;

import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.Presentation;
import com.globalmentor.marmot.resource.ResourceKit;
import com.globalmentor.marmot.security.MarmotSecurityManager;

/**Marmot session information.
@param <P> The type of presentation supported by this session.
@param <RK> The type of resource kits supported by this session.
@author Garret Wilson
*/
public interface MarmotSession<P extends Presentation, RK extends ResourceKit<P>>
{

	/**@return The installed Marmot security manager.*/
	public MarmotSecurityManager getSecurityManager();

	/**Registers a resource kit with the session.
	@param resourceKit The resource kit to register.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installResourceKit(final RK resourceKit);

	/**Unregisters a resource kit with the session.
	@param resourceKit The resource kit to unregister.
	@exception IllegalStateException if the resource kit is not installed in this session.
	*/
	public void uninstallResourceKit(final RK resourceKit);

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
	public RK getResourceKit(final Repository repository, final RDFResource resource);
	
}
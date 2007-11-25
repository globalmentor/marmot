package com.globalmentor.marmot;

import com.garretwilson.urf.URFResource;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.ResourceKit;
import com.globalmentor.marmot.resource.ResourceKit.Capability;
import com.globalmentor.marmot.security.MarmotSecurityManager;

/**Marmot session information.
@param <RK> The type of resource kits supported by this session.
@author Garret Wilson
*/
public interface MarmotSession<RK extends ResourceKit>
{

	/**@return The installed Marmot security manager.*/
	public MarmotSecurityManager getSecurityManager();

	/**Registers a resource kit with the session.
	@param resourceKit The resource kit to register.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installResourceKit(final RK resourceKit);

	/**Registers a resource kit with the session, specifying if the resource kit should be considered the default resource kit.
	If this resource kit is specified as the default, it will replace any resource kit previously specified as the default.
	@param resourceKit The resource kit to register.
	@param isDefault Whether the resource kit should be the default.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installResourceKit(final RK resourceKit, final boolean isDefault);

	/**Unregisters a resource kit with the session.
	If this resource kit was previously set as the default, there will no longer be a default resource kit.
	@param resourceKit The resource kit to unregister.
	@exception IllegalStateException if the resource kit is not installed in this session.
	*/
	public void uninstallResourceKit(final RK resourceKit);

	/**@return The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit.*/
	public RK getDefaultResourceKit();

	/**@return The available resource kits.*/
	public Iterable<RK> getResourceKits();

	/**Determines if there exists  resource kit appropriate for the given resource supporting the given capabilities.
	@param repository The repository in which the resource resides.
	@param resource The resource for which a resource kit should be returned.
	@param capabilities The capabilities required for the resource kit.
	@return <code>true</code> if there exists a resource kit to handle the given resource with the given capabilities, if any, in relation to the resource.
	@see #getResourceKit(Repository, URFResource, Capability...)
	*/
	public boolean hasResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities);

	/**Retrieves a resource kit appropriate for the given resource.
	This method locates a resource kit in the following priority:
	<ol>
		<li>The first resource kit supporting the resource content type.</li>
		<li>The first resource kit supporting one of the resource types.</li>
		<li>The default resource kit.</li>
	</ol>
	@param repository The repository in which the resource resides.
	@param resource The resource for which a resource kit should be returned.
	@param capabilities The capabilities required for the resource kit.
	@return A resource kit to handle the given resource with the given capabilities, if any, in relation to the resource;
		or <code>null</code> if there is no registered resource kit with the given capabilities in relation to the resource.
	*/
	public RK getResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities);

}
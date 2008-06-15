/*
 * Copyright © 1996-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.marmot;

import java.util.Set;

import javax.mail.internet.ContentType;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.ResourceKit;
import com.globalmentor.marmot.resource.ResourceKit.Capability;
import com.globalmentor.marmot.security.MarmotSecurityManager;
import com.globalmentor.urf.URFResource;

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

	/**Registers a resource kit with the session, specifying it as the default resource kit.
	The resource kit will replaced any other resource kit designated as default.
	@param resourceKit The resource kit to register.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installDefaultResourceKit(final RK resourceKit);

	/**Registers a resource kit with the session, specifying it as the default resource kit for collections.
	The resource kit will replaced any other resource kit designated as default for collections.
	@param resourceKit The resource kit to register.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installDefaultCollectionResourceKit(final RK resourceKit);

	/**Unregisters a resource kit with the session.
	If this resource kit was previously set as the default, there will no longer be a default resource kit.
	@param resourceKit The resource kit to unregister.
	@exception IllegalStateException if the resource kit is not installed in this session.
	*/
	public void uninstallResourceKit(final RK resourceKit);

	/**@return The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit.*/
	public RK getDefaultResourceKit();

	/**@return The default resource kit to use if a specific resource kit cannot be found for a collection, or <code>null</code> if there is no default collection resource kit.*/
	public RK getDefaultCollectionResourceKit();

	/**@return The available resource kits.*/
	public Iterable<RK> getResourceKits();

	/**Returns the content types supported by all the available resource kits.
	@return An immutable set of content types supported by all available resource kits.
	@see ResourceKit#getSupportedContentTypes()
	*/
	public Set<ContentType> getSupportedContentTypes();

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
		<li>If the resource has a collection URI, the default collection resource kit.</li>
		<li>The default resource kit.</li>
	</ol>
	@param repository The repository in which the resource resides.
	@param resource The resource for which a resource kit should be returned.
	@param capabilities The capabilities required for the resource kit.
	@return A resource kit to handle the given resource with the given capabilities, if any, in relation to the resource;
		or <code>null</code> if there is no registered resource kit with the given capabilities in relation to the resource.
	*/
	public RK getResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities);

	/**Retrieves a resource kit appropriate for a MIME content type.
	This method should only be used for special-purpose functionality;
	when accessing resources {@link #getResourceKit(Repository, URFResource, Capability...)} should normally be used instead.
	@param contentType The type of content the resource contains.
	@param capabilities The capabilities required for the resource kit.
	@return A resource kit with the requested capabilities to handle the given content type, or <code>null</code> if no appropriate resource kit is registered.
	*/
	public RK getResourceKit(final ContentType contentType, final Capability... capabilities);

}
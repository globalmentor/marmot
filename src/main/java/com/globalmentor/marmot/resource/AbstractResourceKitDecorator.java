/*
 * Copyright © 1996-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.resource;

import java.io.OutputStream;
import java.net.URI;
import java.util.Set;

import static java.util.Objects.*;

import org.urframework.URFResource;

import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;
import com.globalmentor.net.ContentType;
import com.globalmentor.net.ResourceIOException;

/**
 * A resource kit that decorates another resource kit.
 * @author Garret Wilson
 */
public abstract class AbstractResourceKitDecorator implements ResourceKit {

	/** The decorated resource kit. */
	private final ResourceKit resourceKit;

	/** @return The decorated resource kit. */
	protected ResourceKit getResourceKit() {
		return resourceKit;
	}

	/**
	 * Decorated resource kit constructor.
	 * @param resourceKit The resource kit to decorate.
	 * @throws NullPointerException if the given resource kit is <code>null</code>.
	 */
	public AbstractResourceKitDecorator(final ResourceKit resourceKit) {
		this.resourceKit = requireNonNull(resourceKit, "Resource kit cannot be null.");
	}

	@Override
	public MarmotSession<?> getMarmotSession() {
		return getResourceKit().getMarmotSession();
	}

	@Override
	public void setMarmotSession(final MarmotSession<?> marmot) {
		getResourceKit().setMarmotSession(marmot);
	}

	@Override
	public ContentType getDefaultContentType() {
		return getResourceKit().getDefaultContentType();
	}

	@Override
	public Set<ContentType> getSupportedContentTypes() {
		return getResourceKit().getSupportedContentTypes();
	}

	@Override
	public Set<URI> getSupportedResourceTypes() {
		return getResourceKit().getSupportedResourceTypes();
	}

	@Override
	public String getDefaultNameExtension() {
		return getResourceKit().getDefaultNameExtension();
	}

	@Override
	public Set<Capability> getCapabilities() {
		return getResourceKit().getCapabilities();
	}

	@Override
	public boolean hasCapabilities(final Capability... capabilities) {
		return getResourceKit().hasCapabilities(capabilities);
	}

	@Override
	public URFResource getDefaultResourceDescription(final Repository repository, final URI resourceURI) throws ResourceIOException {
		return getResourceKit().getDefaultResourceDescription(repository, resourceURI);
	}

	@Override
	public void initializeResourceDescription(final Repository repository, final URFResource resource) throws ResourceIOException {
		getResourceKit().initializeResourceDescription(repository, resource);
	}

	@Override
	public URI getChildResourceCollectionURI(final Repository repository, final URI parentResourceURI) throws ResourceIOException {
		return getResourceKit().getChildResourceCollectionURI(repository, parentResourceURI);
	}

	@Override
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName) throws ResourceIOException {
		return getResourceKit().getChildResourceURI(repository, parentResourceURI, resourceName);
	}

	@Override
	public URFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException {
		return getResourceKit().createResource(repository, resourceURI);
	}

	@Override
	public URFResource createResource(final Repository repository, final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException {
		return getResourceKit().createResource(repository, resourceURI, resourceDescription);
	}

	@Override
	public boolean hasDefaultResourceContent(final Repository repository, final URI resourceURI) throws ResourceIOException {
		return getResourceKit().hasDefaultResourceContent(repository, resourceURI);
	}

	@Override
	public URFResource writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription)
			throws ResourceIOException {
		return getResourceKit().writeDefaultResourceContent(repository, resourceURI, resourceDescription);
	}

	@Override
	public void writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription,
			final OutputStream outputStream) throws ResourceIOException {
		getResourceKit().writeDefaultResourceContent(repository, resourceURI, resourceDescription, outputStream);
	}

	/*
	 * Determines whether a given user has permission to access a particular aspect of a resource.
	 * @param owner The principal that owns the repository.
	 * @param repository The repository that contains the resource.
	 * @param user The user attempting to access the resource, which may be <code>null</code> if the user is anonymous.
	 * @param aspectID The ID of the aspect requested.
	 * @return <code>true</code> if access to the given aspect is allowed for the user in relation to the indicated resource, else <code>false</code>.
	 * @throws NullPointerException if the given owner, repository, resource URI, and/or permission type is <code>null</code>.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	//TODO fix	public boolean isAllowed(final Principal owner, final Repository repository, final URI resourceURI, final Principal user, final PermissionType permissionType) throws ResourceIOException;

	@Override
	public Class<? extends ResourceAspect> getAspectType() {
		return getResourceKit().getAspectType();
	}

	@Override
	public boolean isAspectAllowed(final ResourceAspect aspect, final PermissionType permissionType) {
		return getResourceKit().isAspectAllowed(aspect, permissionType);
	}

	//	/**
	//	 * Returns the permissions that This prevents aspects from being accessed at lower permissions. For example, a rogue user may attempt to access a
	//	 * preview-permission aspect such as a high-resolution image using a permission such as
	//	 */
	/*TODO fix
		public boolean getAspectPermissions(final String aspectID, final PermissionType permissionType)
		{
			
		}
	*/

	@Override
	public ResourceContentFilter[] getAspectFilters(final ResourceAspect aspect) {
		return getResourceKit().getAspectFilters(aspect);
	}

}

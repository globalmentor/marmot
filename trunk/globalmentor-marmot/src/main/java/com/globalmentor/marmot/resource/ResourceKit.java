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

import org.urframework.URFResource;

import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;
import com.globalmentor.net.ContentType;
import com.globalmentor.net.ResourceIOException;

/**
 * Support for working with a resource in a repository.
 * @author Garret Wilson
 */
public interface ResourceKit
{

	/**
	 * Capabilities a resource kit has in relation to resources.
	 * @author Garret Wilson
	 */
	public enum Capability
	{
		/** The ability to create a resource. */
		CREATE,

		/** The ability to edit a resource. */
		EDIT;
	}

	/** An empty array of extensions. */
	//TODO del if not needed	public final static String[] NO_EXTENSIONS=new String[] {};

	/** An empty array of content types. */
	public final static ContentType[] NO_CONTENT_TYPES = new ContentType[] {};

	/** An empty array of resource type URIs. */
	public final static URI[] NO_RESOURCE_TYPES = new URI[] {};

	/**
	 * Returns the default name extension used by the resource kit.
	 * @return The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	 */
	public String getDefaultNameExtension();

	/**
	 * Returns the name extensions supported for the resource URI.
	 * @return A non-<code>null</code> array of the name extensions this resource kit supports.
	 */
	//TODO del if not needed	public String[] getSupportedExtensions();

	/** @return The Marmot session with which this resource kit is associated, or <code>null</code> if this resource kit has not yet been installed. */
	public MarmotSession<?> getMarmotSession();

	/**
	 * Sets the Marmot session with which this resource kit is associated.
	 * @param marmotSession The Marmot session with which the resource kit should be associated, or <code>null</code> if the resource kit is not installed.
	 * @throws IllegalStateException if this resource kit has already been installed in another Marmot session.
	 */
	public void setMarmotSession(final MarmotSession<?> marmotSession);

	/**
	 * Returns the default content type used for the resource kit.
	 * @return The default content type name this resource kit uses, or <code>null</code> if there is no default content type.
	 */
	public ContentType getDefaultContentType();

	/**
	 * Returns the content types supported. This is the primary method of determining which resource kit to use for a given resource.
	 * @return A read-only set of the content types this resource kit supports.
	 */
	public Set<ContentType> getSupportedContentTypes();

	/**
	 * Returns the resource types supported. This is the secondary method of determining which resource kit to use for a given resource.
	 * @return A read-only set of the URIs for the resource types this resource kit supports.
	 */
	public Set<URI> getSupportedResourceTypes();

	/**
	 * Returns the capabilities of this resource kit.
	 * @return The capabilities provided by this resource kit.
	 */
	public Set<Capability> getCapabilities();

	/**
	 * Determines if the resource kit has all the given capabilities.
	 * @param capabilities The capabilities to check.
	 * @return <code>true</code> if the resource kit has all the given capabilities, if any.
	 */
	public boolean hasCapabilities(final Capability... capabilities);

	/**
	 * Retrieves a default resource description for a given resource, without regard to whether it exists.
	 * @param repository The repository within which the resource would reside.
	 * @param resourceURI The URI of the resource for which a default resource description should be retrieved.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public URFResource getDefaultResourceDescription(final Repository repository, final URI resourceURI) throws ResourceIOException;

	/**
	 * Initializes a resource description, creating whatever properties are appropriate.
	 * @param repository The repository to use to access the resource content, if needed.
	 * @param resource The resource description to initialize.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public void initializeResourceDescription(final Repository repository, final URFResource resource) throws ResourceIOException;

	/**
	 * Returns this resource kit's installed filter based upon its ID.
	 * @param filterID The ID of the filter to return.
	 * @return The resource filter identified by the given ID.
	 * @throws IllegalArgumentException if there is no installed resource filter identified by the given ID.
	 */
	//TODO del	public ResourceFilter getFilter(final String filterID) throws IllegalArgumentException;

	/**
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

	/**
	 * Returns the URI of the collection of a child resource. This is normally the collection URI of the parent resource URI. Some implementations may prefer for
	 * child resources by default to be placed within sub-collections.
	 * @param repository The repository that contains the resource.
	 * @param parentResourceURI The URI to of the parent resource.
	 * @return The URI of the child resource collection
	 * @throws NullPointerException if the given repository and/or parent resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 * @see Repository#getCollectionURI(URI)
	 */
	public URI getChildResourceCollectionURI(final Repository repository, final URI parentResourceURI) throws ResourceIOException;

	/**
	 * Returns the URI of a child resource with the given simple name within a parent resource. This is normally the simple name resolved against the child
	 * resource collection URI, although a resource kit for collections may append an ending path separator. The simple name will be encoded before being used to
	 * construct the URI.
	 * @param repository The repository that contains the resource.
	 * @param parentResourceURI The URI to of the parent resource.
	 * @param resourceName The unencoded simple name of the child resource.
	 * @return The URI of the child resource.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 * @see #getDefaultNameExtension()
	 */
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName) throws ResourceIOException;

	/**
	 * Creates a new resource with the appropriate default contents for this resource type. If a resource already exists at the given URI it will be replaced. If
	 * the resource URI is a collection URI, a collection resource will be created.
	 * @param repository The repository that will contain the resource.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @return A description of the resource that was created.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	public URFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException;

	/**
	 * Creates a new resource with the given description and the appropriate default contents for this resource type. If a resource already exists at the given
	 * URI it will be replaced. If the resource URI is a collection URI, a collection resource will be created.
	 * @param repository The repository that will contain the resource.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @return A description of the resource that was created.
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	public URFResource createResource(final Repository repository, final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException;

	/**
	 * Indicates whether this resource has default resource content.
	 * @param repository The repository that contains the resource.
	 * @param resourceURI The reference URI to use to identify the resource, which may not exist.
	 * @throws NullPointerException if the given repository, resource URI, resource description, and/or output stream is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if the default resource content could not be written.
	 * @see #writeDefaultResourceContent(Repository, URI, URFResource)
	 * @see #writeDefaultResourceContent(Repository, URI, URFResource, OutputStream)
	 */
	public boolean hasDefaultResourceContent(final Repository repository, final URI resourceURI) throws ResourceIOException;

	/**
	 * Writes default resource content for the given resource. If content already exists for the given resource it will be replaced.
	 * @param repository The repository that contains the resource.
	 * @param resourceURI The reference URI to use to identify the resource, which may not exist.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @return A description of the resource the content of which was written.
	 * @throws NullPointerException if the given repository, resource URI, and/or resource description is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if the default resource content could not be written.
	 * @see #writeDefaultResourceContent(Repository, URI, URFResource, OutputStream)
	 */
	public URFResource writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription)
			throws ResourceIOException;

	/**
	 * Writes default resource content to the given output stream. If content already exists for the given resource it will be replaced. This version writes no
	 * content.
	 * @param repository The repository that contains the resource.
	 * @param resourceURI The reference URI to use to identify the resource, which may not exist.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @param outputStream The output stream to which to write the default content.
	 * @throws NullPointerException if the given repository, resource URI, resource description, and/or output stream is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if the default resource content could not be written.
	 */
	public void writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription,
			final OutputStream outputStream) throws ResourceIOException;

	/** @return The type of aspect supported by this resource kit, or <code>null</code> if this resource kit does not support aspects. */
	public Class<? extends ResourceAspect> getAspectType();

	/**
	 * Determines whether the given permission is appropriate for accessing the identified aspect. This prevents aspects from being accessed at lower permissions.
	 * For example, a rogue user may attempt to retrieve a preview-permission aspect such as a high-resolution image using a permission such as
	 * {@link PermissionType#UTILIZE} when a permission appropriate to the aspect, {@link PermissionType#PREVIEW}, is not allowed to the user. The question may be
	 * put, "If we only know that the user has the given permission, is it appropriate to allow the user access to the indicated aspect?"
	 * @param aspect The aspect to be accessed.
	 * @param permissionType The type of permission requested.
	 * @return <code>true</code> if access to the given aspect is allowed using the given permission, else <code>false</code>.
	 * @throws NullPointerException if the given aspect and/or permission type is <code>null</code>.
	 * @throws ClassCastException if the given aspect is not an instance of the type returned by {@link #getAspectType()}.
	 */
	public boolean isAspectAllowed(final ResourceAspect aspect, final PermissionType permissionType);

	/**
	 * Returns the permissions that This prevents aspects from being accessed at lower permissions. For example, a rogue user may attempt to access a
	 * preview-permission aspect such as a high-resolution image using a permission such as
	 */
	/*TODO fix
		public boolean getAspectPermissions(final String aspectID, final PermissionType permissionType)
		{
			
		}
	*/

	/**
	 * Returns the appropriate filters for accessing an identified aspect of the resource.
	 * @param aspect The aspect to be accessed.
	 * @throws NullPointerException if the given aspect is <code>null</code>.
	 * @throws IllegalArgumentException if the given aspect ID does not represent a valid aspect.
	 * @throws ClassCastException if the given aspect is not an instance of the type returned by {@link #getAspectType()}.
	 */
	public ResourceContentFilter[] getAspectFilters(final ResourceAspect aspect);

}

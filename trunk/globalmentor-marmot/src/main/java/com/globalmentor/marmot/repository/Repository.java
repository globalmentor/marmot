/*
 * Copyright © 1996-2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

import org.urframework.*;
import org.urframework.content.Content;

import com.globalmentor.event.ProgressListener;
import com.globalmentor.iso.datetime.ISODateTime;
import com.globalmentor.java.Disposable;
import com.globalmentor.net.*;

/**
 * A Marmot information store.
 * <p>
 * Objects stored in a repository are <dfn>resources</dfn>. Certain resources called <dfn>collections</dfn> have a parent/child relationship with other
 * resources; otherwise they are treated like other resources. There are no special methods for working with collection resources; rather, collections are
 * identified by an ending {@link URIs#PATH_SEPARATOR} character.
 * </p>
 * <p>
 * A repository has the concept of a <dfn>live property</dfn> which is dynamically set based upon resource state, such as content size and last modified
 * date/time. Live properties are definitionally inherent to the makeup of the resource. Live properties cannot be set using normal property manipulation
 * methods and are ignored if requested to be modified. See {@link #getLivePropertyURIs()}.
 * </p>
 * <p>
 * By design a repository has no direct method for explicitly and exclusively setting all properties of a resource. Instead, resources can have individual
 * properties added, removed, and set. This allows a loosely-coupled model that can be implemented by a variety of storage mechanisms and prevents inadvertent
 * removal of unmentioned properties. If it is required to exclusively specify all properties of a resource, the properties of a resource may be retrieved and
 * the unwanted properties explicitly removed.
 * </p>
 * <p>
 * The {@value Content#MODIFIED_PROPERTY_URI} property is <em>not</em> considered a live property; it is completely managed by Marmot. This is because the last
 * modification of the content, represented by this property, is distinct from the last modification of the underlying file store. However, this property does
 * have some peculiarities:
 * <ul>
 * <li>When reading a resource <em>description</em>, if no {@value Content#MODIFIED_PROPERTY_URI} property exists the repository implementation may use the
 * modified value of the underlying file store if present.</li>
 * <li>When writing resource <em>content</em>, if no {@value Content#MODIFIED_PROPERTY_URI} is given in the provided description, a new property will be
 * automatically added with the current date and time for the API methods that do not have a description parameter.</li>
 * </ul>
 * <p>
 * Collections should indicated a {@value Content#LENGTH_PROPERTY_URI} of zero unless the collection has content. A content length should never be missing.
 * </p>
 * <p>
 * Repositories that are read-only should throw {@link ResourceForbiddenException}s for accesses that attempt to modify resources.
 * </p>
 * @author Garret Wilson
 */
public interface Repository extends Disposable
{

	/** The value indicating an infinite depth when retrieving child resource descriptions. */
	public static int INFINITE_DEPTH = -1;

	/** @return The base URI of the public URI namespace being managed. */
	public URI getRootURI();

	/**
	 * Sets the base URI of the public URI namespace being managed, reference URI of the repository. If there currently is no private repository URI, it will be
	 * updated to match the given public repository URI.
	 * @param publicRepositoryURI The base URI of the public URI namespace being managed.
	 * @throws NullPointerException if the given URI is <code>null</code>.
	 */
	public void setRootURI(final URI publicRepositoryURI);

	/** @return The parent repository, or <code>null</code> if this repository has not been registered as a subrepository of another repository. */
	public Repository getParentRepository();

	/**
	 * Sets the parent of this repository. This method is used internally when a subrepository is set, and is not intended to be called by normal code.
	 * @param newParent The new parent of the repository, or <code>null</code> if the repository is being unregistered.
	 * @throws IllegalStateException if the new parent is non-<code>null</code> and the repository already has a parent.
	 * @see #registerPathRepository(URIPath, Repository)
	 */
	public void setParentRepository(final Repository newParent);

	/**
	 * Determines the root of a hierarchy of subrepositories. If this repository has no parent, this method will return this repository.
	 * @return The root parent of all the repositories.
	 */
	public Repository getRootRepository();

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace. This method resolves the private repository path to the current public repository URI.
	 * @param subrepositoryPath The private path relative to the private URI of this repository.
	 * @throws NullPointerException if the given private repository path is <code>null</code>.
	 * @throws IllegalArgumentException if the given subrepository path is absolute and/or is not a collection.
	 */
	public Repository createSubrepository(final URIPath subrepositoryPath);

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace.
	 * @param publicRepositoryURI The public URI identifying the location of the new repository.
	 * @param privateSubrepositoryPath The private path relative to the private URI of this repository.
	 * @throws NullPointerException if the given public repository URI and/or private repository path is <code>null</code>.
	 * @throws IllegalArgumentException if the given private repository path is absolute and/or is not a collection.
	 */
	public Repository createSubrepository(final URI publicRepositoryURI, final URIPath privateSubrepositoryPath);

	/**
	 * Registers a resource factory to be used to create resources with a type from the specified namespace. If a resource factory is already registered for this
	 * namespace, it will be replaced.
	 * @param typeNamespaceURI The namespace of the resource type for which this factory should be used to create objects.
	 * @param factory The resource factory that will be used to create resources of types from this namespace.
	 */
	public void registerResourceFactory(final URI typeNamespaceURI, final URFResourceFactory factory);

	/**
	 * Removes the resource factory being used to create resources with a type from the specified namespace. If there is no resource factory registered for this
	 * namespace, no action will be taken.
	 * @param typeNamespaceURI The namespace of the resource type for which this factory should be used to create objects.
	 */
	public void unregisterResourceFactory(final URI typeNamespaceURI);

	/** @return Whether the repository has been opened for access. */
	public boolean isOpen();

	/**
	 * Opens the repository for access. If the repository is already open, no action occurs. At a minimum the repository must have a root URI specified, and
	 * specific repository types may impose more constraints.
	 * @return <code>false</code> if the repository was already open, else <code>true</code>.
	 * @throws IllegalStateException if the settings of this repository are inadequate to open the repository.
	 * @throws ResourceIOException if there is an error opening the repository.
	 * @see #getRootURI()
	 */
	public void open() throws ResourceIOException;

	/**
	 * Closes the repository. If the repository is already closed, no action occurs.
	 * @throws ResourceIOException if there is an error closing the repository.
	 */
	public void close() throws ResourceIOException;

	/**
	 * Retrieves the live properties, which dynamically determined attributes of the resource such as content size.
	 * @return The thread-safe set of URIs of live properties.
	 */
	public Set<URI> getLivePropertyURIs();

	/**
	 * Associates the given content type with the given extension, without regard to case.
	 * @param extension The URI name extension with which the content type should be associated, or <code>null</code> if the content type should be associated
	 *          with resources that have no extension.
	 * @param contentType The content type to associate with the given extension.
	 * @return The content type previously registered with the given extension, or <code>null</code> if no content type was previously registered.
	 * @throws NullPointerException if the given content type is <code>null</code>.
	 */
	public ContentType registerExtensionContentType(final String extension, final ContentType contentType);

	/**
	 * Returns the content type associated with the given extension, without regard to case.
	 * @param extension The URI name extension with which the content type is associated, or <code>null</code> if the content type is associated with resources
	 *          that have no extension.
	 * @return The content type associated with the given extension, or <code>null</code> if there is no content type associated with the given extension.
	 */
	public ContentType getExtensionContentType(final String extension);

	/**
	 * Associates the given charset with the base media type of the given content type. Any association will only override resources that do not explicitly have a
	 * charset specified. Any parameters of the given content type will be ignored.
	 * @param contentType The content type with which the charset should be associated.
	 * @param charset The charset to associate with the given content type.
	 * @return The charset previously registered with the given content type, or <code>null</code> if no charset was previously registered.
	 * @throws NullPointerException if the given content type and/or charset is <code>null</code>.
	 */
	public Charset registerContentTypeCharset(final ContentType contentType, final Charset charset);

	/**
	 * Returns the charset associated with the given content type. Any parameters of the given content type will be ignored.
	 * @param contentType The content type with which the charset is associated.
	 * @return The charset associated with the given content type, or <code>null</code> if there is no charset associated with the given content type.
	 * @throws NullPointerException if the given content type is <code>null</code>.
	 */
	public Charset getContentTypeCharset(final ContentType contentType);

	/** @return the mapping of charsets associated with base content types. */
	public Map<ContentType, Charset> getContentTypeCharsets();

	/**
	 * Sets the content type charset associations to those specified in the given map. Any association will only override resources that do not explicitly have a
	 * charset specified. The current associations will be lost. Any parameters of the given content types will be ignored.
	 * @param contentTypeCharsets The associations of charsets to base content types.
	 * @throws NullPointerException if a given content type and/or charset is <code>null</code>.
	 */
	public void setContentTypeCharsets(final Map<ContentType, Charset> contentTypeCharsets);

	/**
	 * Associates the given repository with a repository path. Access to any resource with a URI beginning with the given path will delegate to the indicated
	 * repository. The public URI of the given repository will be updated to correspond to its location within this repository.
	 * @param path The relative collection path with which the repository should be associated.
	 * @param repository The repository to handle access to all resources beginning with the given path.
	 * @return The repository previously registered with the given path, or <code>null</code> if no repository was previously registered.
	 * @throws NullPointerException if the given path and/or repository is <code>null</code>.
	 * @throws IllegalArgumentException if the given path is not relative.
	 * @throws IllegalArgumentException if the given path does not represent a collection (i.e. it does not end with a path separator).
	 */
	public Repository registerPathRepository(final URIPath path, final Repository repository);

	/**
	 * Returns the repository associated with the given path.
	 * @param path The relative collection path with which a repository may be associated.
	 * @return The repository associated with the given path, or <code>null</code> if there is no repository associated with the given path.
	 * @throws NullPointerException if the given content type is <code>null</code>.
	 * @throws NullPointerException if the given path is <code>null</code>.
	 * @throws IllegalArgumentException if the given path is not relative.
	 * @throws IllegalArgumentException if the given path does not represent a collection (i.e. it does not end with a path separator).
	 */
	public Repository getPathRepository(final URIPath path);

	/** @return The read-only mapping of relative paths associated with repositories. */
	public Map<URIPath, Repository> getPathRepositories();

	/**
	 * Sets the path repository associations to those specified in the given map. Any association will only override resources that do not explicitly have a
	 * charset specified. The current associations will be lost.
	 * @param pathRepositories The associations of paths to repositories.
	 * @throws NullPointerException if a given path and/or repository is <code>null</code>.
	 * @throws IllegalArgumentException if a given path is not relative.
	 * @throws IllegalArgumentException if a given path does not represent a collection (i.e. it does not end with a path separator).
	 */
	public void setPathRepositories(final Map<URIPath, Repository> pathRepositories);

	/**
	 * Determines whether the indicated property is is a live, dynamically determined property.
	 * @param propertyURI The URI identifying the property.
	 * @return <code>true</code> if the property is a live property.
	 * @throws NullPointerException if the given property URI is <code>null</code>.
	 */
	public boolean isLivePropertyURI(final URI propertyURI);

	/**
	 * Checks to make sure the resource designated by the given resource URI is within this repository. The URI is normalized and canonicalized to ensure that:
	 * <ul>
	 * <li>Its path segments are normalized.</li>
	 * <li>It only contains valid URI characters.</li>
	 * <li>Encoded characters are in canonical case.</li>
	 * </ul>
	 * @param resourceURI The URI of the resource to check.
	 * @return The normalized form of the given resource URI.
	 * @throws NullPointerException if the given resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 */
	public URI checkResourceURI(URI resourceURI) throws IllegalArgumentException;

	/**
	 * Determines the URI of the collection resource of the given URI; either the given resource URI if the resource represents a collection, or the parent
	 * resource if not.
	 * @param resourceURI The URI of the resource for which the collection resource URI should be returned.
	 * @return The URI of the indicated resource's deepest collection resource, or <code>null</code> if the given URI designates a non-collection resource with no
	 *         collection parent.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public URI getCollectionURI(URI resourceURI) throws ResourceIOException;

	/**
	 * Determines the URI of the parent resource of the given URI.
	 * @param resourceURI The URI of the resource for which the parent resource URI should be returned.
	 * @return The URI of the indicated resource's parent resource, or <code>null</code> if the given URI designates a resource with no parent.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public URI getParentResourceURI(final URI resourceURI) throws ResourceIOException;

	/**
	 * Determines if the resource at the given URI exists.
	 * @param resourceURI The URI of the resource to check.
	 * @return <code>true</code> if the resource exists, else <code>false</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public boolean resourceExists(final URI resourceURI) throws ResourceIOException;

	/**
	 * Retrieves a description of the resource with the given URI.
	 * @param resourceURI The URI of the resource the description of which should be retrieved.
	 * @return A description of the resource with the given URI.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public URFResource getResourceDescription(final URI resourceURI) throws ResourceIOException;

	/**
	 * Retrieves the contents of the resource specified by the given URI.
	 * <p>
	 * It is usually more memory-efficient to get an input stream to the resource using {@link #getResourceInputStream(URI)}.
	 * </p>
	 * @param resourceURI The URI of the resource to access.
	 * @return The bytes representing the contents of the resource represented by the given URI.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given resource is too large to be placed in a byte array.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the resource.
	 */
	public byte[] getResourceContents(final URI resourceURI) throws ResourceIOException;

	/**
	 * Gets an input stream to the contents of the resource specified by the given URI.
	 * @param resourceURI The URI of the resource to access.
	 * @return An input stream to the resource represented by the given URI.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the resource.
	 */
	public InputStream getResourceInputStream(final URI resourceURI) throws ResourceIOException;

	/**
	 * Gets an output stream to the contents of the resource specified by the given URI. The content modified datetime is set to the current date and time. A
	 * {@link ResourceNotFoundException} should be generated if the resource does not exist, even for read-only repositories. If the repository is read-only, this
	 * method generates a {@link ResourceForbiddenException}.
	 * @param resourceURI The URI of the resource to access.
	 * @return An output stream to the resource represented by the given URI.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the resource.
	 * @see Content#MODIFIED_PROPERTY_URI
	 */
	public OutputStream getResourceOutputStream(final URI resourceURI) throws ResourceIOException;

	/**
	 * Gets an output stream to the contents of the resource specified by the given URI. The resource description will be updated with the specified content
	 * modified datetime if given. A {@link ResourceNotFoundException} should be generated if the resource does not exist, even for read-only repositories. If the
	 * repository is read-only, this method generates a {@link ResourceForbiddenException}.
	 * @param resourceURI The URI of the resource to access.
	 * @param newContentModified The new content modified datetime for the resource, or <code>null</code> if the content modified datetime should not be updated.
	 * @return An output stream to the resource represented by the given URI.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the resource.
	 * @see Content#MODIFIED_PROPERTY_URI
	 */
	public OutputStream getResourceOutputStream(final URI resourceURI, final ISODateTime newContentModified) throws ResourceIOException;

	/**
	 * Determines whether the resource represented by the given URI has children.
	 * @param resourceURI The URI of the resource.
	 * @return <code>true</code> if the specified resource has child resources.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public boolean hasChildren(final URI resourceURI) throws ResourceIOException;

	/**
	 * Retrieves child resources of the resource at the given URI.
	 * <p>
	 * If any repositories are mapped as children of the identified resource, they will be returned as well, with their descriptions retrieved from the respective
	 * subrepository.
	 * </p>
	 * @param resourceURI The URI of the resource for which sub-resources should be returned.
	 * @return A list of sub-resource descriptions under the given resource.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 * @see #getPathRepositories()
	 */
	public List<URFResource> getChildResourceDescriptions(final URI resourceURI) throws ResourceIOException;

	/**
	 * Retrieves child resources of the resource at the given URI.
	 * <p>
	 * If any repositories are mapped as children of the identified resource, they will be returned as well, with their descriptions retrieved from the respective
	 * subrepository.
	 * </p>
	 * @param resourceURI The URI of the resource for which sub-resources should be returned.
	 * @param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be
	 *          filtered.
	 * @return A list of sub-resource descriptions under the given resource.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 * @see #getPathRepositories()
	 */
	public List<URFResource> getChildResourceDescriptions(final URI resourceURI, final ResourceFilter resourceFilter) throws ResourceIOException;

	/**
	 * Retrieves child resources of the resource at the given URI.
	 * <p>
	 * If any repositories are mapped as children of the identified resource, they will be returned as well, with their descriptions retrieved from the respective
	 * subrepository.
	 * </p>
	 * @param resourceURI The URI of the resource for which sub-resources should be returned.
	 * @param depth The zero-based depth of child resources which should recursively be retrieved, or {@link #INFINITE_DEPTH} for an infinite depth.
	 * @return A list of sub-resource descriptions under the given resource.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given depth is negative.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 * @see #getPathRepositories()
	 */
	public List<URFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws ResourceIOException;

	/**
	 * Retrieves child resources of the resource at the given URI.
	 * <p>
	 * If any repositories are mapped as children of the identified resource, they will be returned as well, with their descriptions retrieved from the respective
	 * subrepository.
	 * </p>
	 * @param resourceURI The URI of the resource for which sub-resources should be returned.
	 * @param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be
	 *          filtered.
	 * @param depth The zero-based depth of child resources which should recursively be retrieved, or {@link #INFINITE_DEPTH} for an infinite depth.
	 * @return A list of sub-resource descriptions under the given resource.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given depth is negative.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 * @see #getPathRepositories()
	 */
	public List<URFResource> getChildResourceDescriptions(final URI resourceURI, final ResourceFilter resourceFilter, final int depth) throws ResourceIOException;

	/**
	 * Creates all the parent resources necessary for a resource to exist at the given URI. If any parent resources already exist, they will not be replaced.
	 * @param resourceURI The reference URI of a resource which may not exist.
	 * @return A description of the most immediate parent resource created, or <code>null</code> if no parent resources were required to be created.
	 * @throws NullPointerException if the given resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if a parent resource could not be created.
	 */
	public URFResource createParentResources(final URI resourceURI) throws ResourceIOException;

	/**
	 * Creates a new resource with a default description and returns an output stream for writing the contents of the resource. If a resource already exists at
	 * the given URI it will be replaced. The returned output stream should always be closed. If possible, the {@link Content#CREATED_PROPERTY_URI} property will
	 * be added with the current date and time. The {@link Content#MODIFIED_PROPERTY_URI} property will be added with the current date and time; this property is
	 * optional for collections. If a resource with no contents is desired, {@link #createResource(URI, byte[])} with zero bytes is better suited for this task.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @return An output stream for storing the contents of the resource.
	 * @throws NullPointerException if the given resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	public OutputStream createResource(final URI resourceURI) throws ResourceIOException;

	/**
	 * Creates a new resource with the given description and returns an output stream for writing the contents of the resource. If a resource already exists at
	 * the given URI it will be replaced. The returned output stream should always be closed. It is unspecified whether the resource description will be updated
	 * before or after the resource contents are stored. If a resource with no contents is desired, {@link #createResource(URI, URFResource, byte[])} with zero
	 * bytes is better suited for this task.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @return An output stream for storing the contents of the resource.
	 * @throws NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	public OutputStream createResource(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException;

	/**
	 * Creates a new resource with a default description and contents. If a resource already exists at the given URI it will be replaced. If possible, the
	 * {@link Content#CREATED_PROPERTY_URI} property will be added with the current date and time. The {@link Content#MODIFIED_PROPERTY_URI} property will be
	 * added with the current date and time; this property is optional for collections.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceContents The contents to store in the resource.
	 * @return A description of the resource that was created.
	 * @throws NullPointerException if the given resource URI and/or resource contents is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	public URFResource createResource(final URI resourceURI, final byte[] resourceContents) throws ResourceIOException;

	/**
	 * Creates a new resource with the given description and contents. If a resource already exists at the given URI it will be replaced.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @param resourceContents The contents to store in the resource.
	 * @return A description of the resource that was created.
	 * @throws NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	public URFResource createResource(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException;

	/**
	 * Creates a new collection resource with a default description and contents. If a resource already exists at the given URI it will be replaced. If possible,
	 * the {@link Content#CREATED_PROPERTY_URI} property will be added with the current date and time. The {@link Content#MODIFIED_PROPERTY_URI} property will be
	 * added with the current date and time; this property is optional for collections.
	 * <p>
	 * This is a convenience method that is equivalent to calling {@link #createResource(URI, byte[])} with a zero-length byte array.
	 * </p>
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @return A description of the resource that was created.
	 * @throws NullPointerException if the given resource URI and/or resource contents is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given resource URI is not a collection URI.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	public URFResource createCollectionResource(final URI resourceURI) throws ResourceIOException;

	/**
	 * Deletes a resource.
	 * <p>
	 * If no resource exists at the given URI, no action occurs and no error is generated. The resource at the root of the repository, represented by
	 * {@link #getRootURI()}, cannot be deleted and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The reference URI of the resource to delete.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource could not be deleted.
	 */
	public void deleteResource(final URI resourceURI) throws ResourceIOException;

	/**
	 * Adds properties to a given resource. All existing properties will be left unmodified.
	 * @param resourceURI The reference URI of the resource.
	 * @param properties The properties to set.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or properties is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource addResourceProperties(URI resourceURI, final URFProperty... properties) throws ResourceIOException;

	/**
	 * Adds properties to a given resource. All existing properties will be left unmodified.
	 * @param resourceURI The reference URI of the resource.
	 * @param properties The properties to set.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or properties is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource addResourceProperties(URI resourceURI, final Iterable<URFProperty> properties) throws ResourceIOException;

	/**
	 * Sets the properties of a given resource. Any existing properties with the same URIs as the given given property/value pairs will be removed. All other
	 * existing properties will be left unmodified.
	 * @param resourceURI The reference URI of the resource.
	 * @param properties The properties to set.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or properties is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource setResourceProperties(final URI resourceURI, final URFProperty... properties) throws ResourceIOException;

	/**
	 * Sets the properties of a given resource. Any existing properties with the same URIs as the given given property/value pairs will be removed. All other
	 * existing properties will be left unmodified.
	 * @param resourceURI The reference URI of the resource.
	 * @param properties The properties to set.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or properties is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource setResourceProperties(final URI resourceURI, final Iterable<URFProperty> properties) throws ResourceIOException;

	/**
	 * Removes properties from a given resource. Any existing properties with the same URIs as the given given property/value pairs will be removed. All other
	 * existing properties will be left unmodified.
	 * @param resourceURI The reference URI of the resource.
	 * @param propertyURIs The properties to remove.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or property URIs is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource removeResourceProperties(final URI resourceURI, final URI... propertyURIs) throws ResourceIOException;

	/**
	 * Removes properties from a given resource. Any existing properties with the same URIs as the given given property/value pairs will be removed. All other
	 * existing properties will be left unmodified.
	 * @param resourceURI The reference URI of the resource.
	 * @param propertyURIs The properties to remove.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or property URIs is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource removeResourceProperties(final URI resourceURI, final Iterable<URI> propertyURIs) throws ResourceIOException;

	/**
	 * Alters properties of a given resource.
	 * @param resourceURI The reference URI of the resource.
	 * @param resourceAlteration The specification of the alterations to be performed on the resource.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or resource alteration is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be altered.
	 */
	public URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException;

	//intra-repository copy

	/**
	 * Creates an infinitely deep copy of a resource to another URI in this repository. Any resource at the destination URI will be replaced.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 */
	public void copyResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException;

	/**
	 * Creates an infinitely deep copy of a resource to another URI in this repository. Any resource at the destination URI will be replaced.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 */
	public void copyResource(final URI resourceURI, final URI destinationURI, final ProgressListener progressListener) throws ResourceIOException;

	/**
	 * Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destination only if requested.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 * @throws ResourceIOException if there is an error copying the resource.
	 */
	public void copyResource(final URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException;

	/**
	 * Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destination only if requested.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 * @throws ResourceIOException if there is an error copying the resource.
	 */
	public void copyResource(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException;

	//inter-repository copy

	/**
	 * Creates an infinitely deep copy of a resource to the specified URI in the specified repository. Any resource at the destination URI will be replaced.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 */
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException;

	/**
	 * Creates an infinitely deep copy of a resource to the specified URI in the specified repository. Any resource at the destination URI will be replaced.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 */
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final ProgressListener progressListener)
			throws ResourceIOException;

	/**
	 * Creates an infinitely deep copy of a resource to the specified URI in the specified repository, overwriting any resource at the destination only if
	 * requested.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite)
			throws ResourceIOException;

	/**
	 * Creates an infinitely deep copy of a resource to the specified URI in the specified repository, overwriting any resource at the destination only if
	 * requested.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular copy.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	public void copyResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite,
			final ProgressListener progressListener) throws ResourceIOException;

	//intra-repository move

	/**
	 * Moves a resource to another URI in this repository. Any resource at the destination URI will be replaced.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 */
	public void moveResource(final URI resourceURI, final URI destinationURI) throws ResourceIOException;

	/**
	 * Moves a resource to another URI in this repository. Any resource at the destination URI will be replaced.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 */
	public void moveResource(final URI resourceURI, final URI destinationURI, final ProgressListener progressListener) throws ResourceIOException;

	/**
	 * Moves a resource to another URI in this repository, overwriting any resource at the destination only if requested.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 * @throws ResourceIOException if there is an error moving the resource.
	 */
	public void moveResource(final URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException;

	/**
	 * Moves a resource to another URI in this repository, overwriting any resource at the destination only if requested.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 * @throws ResourceIOException if there is an error moving the resource.
	 */
	public void moveResource(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException;

	//inter-repository move

	/**
	 * Moves a resource to the specified URI in the specified repository. Any resource at the destination URI will be replaced.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 */
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException;

	/**
	 * Moves a resource to the specified URI in the specified repository. Any resource at the destination URI will be replaced.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 */
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final ProgressListener progressListener)
			throws ResourceIOException;

	/**
	 * Moves a resource to the specified URI in the specified repository, overwriting any resource at the destination only if requested.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite)
			throws ResourceIOException;

	/**
	 * Moves a resource to the specified URI in the specified repository, overwriting any resource at the destination only if requested.
	 * <p>
	 * The resource at the root of the repository, represented by {@link #getRootURI()}, cannot be moved and will result in an {@link IllegalArgumentException}.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws IllegalArgumentException if the given source or destination URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given URI represents the root of the repository.
	 * @throws IllegalArgumentException if the given destination resource is a child of the given source resource, representing a circular move.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	public void moveResource(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite,
			final ProgressListener progressListener) throws ResourceIOException;

}

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

import java.net.*;

import org.urframework.*;

import com.globalmentor.net.ResourceIOException;
import com.globalmentor.net.URIPath;

import static com.globalmentor.java.Conditions.*;
import static com.globalmentor.java.Objects.*;
import static com.globalmentor.net.URIs.*;

/**
 * Abstract implementation of a repository that is backed by a source that users a hierarchy of URIs paralleling the URIs of the resources in the repository.
 * @author Garret Wilson
 */
public abstract class AbstractHierarchicalSourceRepository extends AbstractRepository
{

	/** The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository. */
	private URI sourceURI = null;

	/** @return The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository. */
	public URI getSourceURI()
	{
		return sourceURI;
	}

	/**
	 * Sets the base URI of the private URI namespace being managed. If no root URI is specified, the root URI is updated to match the source URI.
	 * @param sourceURI The base URI of the private URI namespace being managed.
	 * @throws NullPointerException if the given URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given source resource URI is not absolute or is not a collection URI.
	 * @see #setRootURI(URI)
	 */
	public void setSourceURI(final URI sourceURI)
	{
		this.sourceURI = checkCollectionURI(checkAbsolute(normalize(checkInstance(sourceURI, "Source URI must not be null."))));
		if(getRootURI() == null) //if no root URI has been set
		{
			setRootURI(this.sourceURI); //update the root URI to match the source URI
		}
	}

	/**
	 * URI constructor with no separate private URI namespace.
	 * @param rootURI The URI identifying the location of this repository.
	 */
	public AbstractHierarchicalSourceRepository(final URI rootURI)
	{
		this(rootURI, rootURI); //use the same repository URI as the public and private namespaces
	}

	/**
	 * Public repository URI and private repository URI constructor. A {@link URFResourceTURFIO} description I/O is created and initialized.
	 * @param rootURI The URI identifying the location of this repository.
	 * @param sourceURI The URI identifying the private namespace managed by this repository.
	 * @throws IllegalArgumentException if the given source URI is not absolute or is not a collection URI.
	 */
	public AbstractHierarchicalSourceRepository(final URI rootURI, final URI sourceURI)
	{
		this(rootURI, sourceURI, createDefaultURFResourceDescriptionIO()); //create a default resource description I/O using TURF
	}

	/**
	 * Public repository URI, private repository URI, and description I/O constructor.
	 * @param rootURI The URI identifying the location of this repository.
	 * @param sourceURI The URI identifying the private namespace managed by this repository.
	 * @param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	 * @throws NullPointerException the given description I/O is <code>null</code>.
	 * @throws IllegalArgumentException if the given source URI is not absolute or is not a collection URI.
	 */
	public AbstractHierarchicalSourceRepository(final URI rootURI, final URI sourceURI, final URFIO<URFResource> descriptionIO)
	{
		super(rootURI, descriptionIO);
		this.sourceURI = sourceURI != null ? checkCollectionURI(checkAbsolute(normalize(sourceURI))) : null;
	}

	/**
	 * Translates a public URI in the repository to the equivalent private URI in the private URI namespace.
	 * @param publicURI The URI in the public URI namespace.
	 * @return A URI equivalent to the public URI in the private URI namespace.
	 */
	protected URI getSourceResourceURI(final URI publicURI)
	{
		return changeBase(publicURI, getRootURI(), getSourceURI()); //change the base of the URI from the public URI namespace to the private URI namespace
	}

	/**
	 * Translates a private URI to the equivalent public URI in the public repository URI namespace.
	 * @param sourceURI The URI in the private URI namespace.
	 * @return A URI equivalent to the private URI in the public repository URI namespace.
	 */
	protected URI getRepositoryResourceURI(final URI sourceURI)
	{
		return changeBase(sourceURI, getSourceURI(), getRootURI()); //change the base of the URI from the private URI namespace to the public URI namespace
	}

	/**
	 * Translates a public URI in the repository to a URI path relative to the public URI. This is a convenience method that relativizes the URI of the resource
	 * to the root URI of the repository.
	 * @param resourceURI The URI in the public URI namespace.
	 * @return The URI path of the resource relative to the public URI.
	 * @throws NullPointerException if the given resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given resource URI is not in the public resource namespace.
	 * @see #getRootURI()
	 */
	protected URIPath getResourceURIPath(final URI resourceURI)
	{
		return URIPath.relativize(getRootURI(), resourceURI);
	}

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace. This method resolves the private repository path to the current public repository URI.
	 * @param subrepositoryPath The private path relative to the private URI of this repository.
	 * @throws NullPointerException if the given private repository path is <code>null</code>.
	 * @throws IllegalArgumentException if the given subrepository path is absolute and/or is not a collection.
	 */
	public final Repository createSubrepository(final URIPath subrepositoryPath)
	{
		return createSubrepository(resolve(getRootURI(), subrepositoryPath.checkRelative().checkCollection().toURI()), subrepositoryPath); //resolve the subrepository path to the public repository URI		
	}

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace.
	 * @param publicRepositoryURI The public URI identifying the location of the new repository.
	 * @param privateSubrepositoryPath The private path relative to the private URI of this repository.
	 * @throws NullPointerException if the given public repository URI and/or private repository path is <code>null</code>.
	 * @throws IllegalArgumentException if the given private repository path is absolute and/or is not a collection.
	 */
	public final Repository createSubrepository(final URI publicRepositoryURI, final URIPath privateSubrepositoryPath)
	{
		return createSubrepository(publicRepositoryURI, resolve(getSourceURI(), privateSubrepositoryPath.checkRelative().checkCollection().toURI())); //resolve the subrepository path to the private repository URI		
	}

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace.
	 * @param publicRepositoryURI The public URI identifying the location of the new repository.
	 * @param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	 * @throws NullPointerException if the given public repository URI and/or private repository URI is <code>null</code>.
	 */
	protected abstract Repository createSubrepository(final URI publicRepositoryURI, final URI privateRepositoryURI);

	/**
	 * {@inheritDoc} This version ensures that there is a source URI specified.
	 * @throws IllegalStateException if there is no source URI specified.
	 * @see #getSourceURI()
	 */
	@Override
	protected void openImpl() throws ResourceIOException
	{
		checkState(getSourceURI() != null, "Cannot open repository without source URI specified.");
		super.openImpl();
	}

	/**
	 * Determines whether a resource, identified by its private URI, should be made available in the public space. If this method returns <code>false</code>, the
	 * identified resource will essentially become invisible past the {@link Repository} interface. Such resources are normally used internally with special
	 * semantics to the repository implementation. This version returns <code>true</code> for all resources except:
	 * <ul>
	 * <li>The special collection contents resource named {@value #COLLECTION_CONTENT_NAME}.</li>
	 * </ul>
	 * @param privateResourceURI The private URI of a resource.
	 * @return <code>true</code> if the resource should be visible as normal, or <code>false</code> if the resource should not be made available to the public
	 *         space.
	 * @throws NullPointerException if the given URI is <code>null</code>.
	 */
	protected boolean isSourceResourceVisible(final URI privateResourceURI)
	{
		final String rawName = getRawName(privateResourceURI); //get the raw name of the resource
		if(COLLECTION_CONTENT_NAME.equals(rawName)) //if this is the collection contents
		{
			return false; //don't publish the collection contents file
		}
		return true; //publish all other resources
	}

	/**
	 * Implementation to determine if the resource at the given URI exists.
	 * <p>
	 * This version returns <code>false</code> for all resources for which {@link #isSourceResourcePublic(URI)} returns <code>false</code>.
	 * @param resourceURI The URI of the resource to check.
	 * @return <code>true</code> if the resource exists, else <code>false</code>.
	 * @exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @exception ResourceIOException if there is an error accessing the repository.
	 */
	//TODo del	protected boolean resourceExistsImpl(URI resourceURI) throws ResourceIOException

}

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

package com.globalmentor.marmot.repository;

import java.net.*;

import com.globalmentor.urf.*;
import static com.globalmentor.net.URIs.*;

/**Abstract implementation of a repository that is backed by a source that users a hierarchy of URIs paralleling the URIs of the resources in the repository.
@author Garret Wilson
*/
public abstract class AbstractHierarchicalSourceRepository extends AbstractRepository
{

	/**Translates a public URI in the repository to the equivalent private URI in the private URI namespace.
	@param publicURI The URI in the public URI namespace.
	@return A URI equivalent to the public URI in the private URI namespace.
	*/
	protected URI getSourceResourceURI(final URI publicURI)
	{
		return changeBase(publicURI, getRootURI(), getSourceURI());	//change the base of the URI from the public URI namespace to the private URI namespace
	}

	/**Translates a private URI to the equivalent public URI in the public repository URI namespace.
	@param sourceURI The URI in the private URI namespace.
	@return A URI equivalent to the private URI in the public repository URI namespace.
	*/
	protected URI getRepositoryResourceURI(final URI sourceURI)
	{
		return changeBase(sourceURI, getSourceURI(), getRootURI());	//change the base of the URI from the private URI namespace to the public URI namespace
	}

	/**URI constructor with no separate private URI namespace.
	@param repositoryURI The URI identifying the location of this repository.
	@exception NullPointerException if the given respository URI is <code>null</code>.
	*/
	public AbstractHierarchicalSourceRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI constructor.
	A {@link URFResourceTURFIO} description I/O is created and initialized.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@exception NullPointerException if one of the given respository URIs is <code>null</code>.
	*/
	public AbstractHierarchicalSourceRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		this(publicRepositoryURI, privateRepositoryURI, createDefaultURFResourceDescriptionIO());	//create a default resource description I/O using TURF
	}

	/**Public repository URI and private repository URI constructor.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	@exception NullPointerException if one of the given respository URIs and/or the description I/O is <code>null</code>.
	*/
	public AbstractHierarchicalSourceRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final URFIO<URFResource> descriptionIO)
	{
		super(publicRepositoryURI, privateRepositoryURI, descriptionIO);
	}

}

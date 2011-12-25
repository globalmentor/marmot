/*
 * Copyright © 2009-2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import static java.util.Collections.*;

import java.io.*;
import java.net.URI;
import java.util.*;

import com.globalmentor.net.*;
import com.globalmentor.urf.*;

/**Abstract implementation of a repository that does not allow resources to be modified.
<p>This repository allows resource descriptions to be manually assigned. These resource descriptions
are merged with whatever default resource descriptions are provided by the repository, if any.</p>
@author Garret Wilson
*/
public abstract class AbstractReadOnlyRepository extends AbstractRepository
{
	
	/**The map of manually specified resource descriptions keyed to resource URIs.*/
	private final Map<URI, URFResource> resourceMap=new HashMap<URI, URFResource>();

		/**Adds a resource description to supplement the default resource descriptions.
		@param resource The resource description to add.
		@return The resource description previously configured with the given URI, or <code>null</code> if no resource was previously configured.
		@exception NullPointerException if the given resource is <code>null</code>.
		@exception IllegalArgumentException if the given resource does not reside inside this repository.
		*/
		public URFResource storeResource(final URFResource resource)
		{
			final URI resourceURI=checkResourceURI(resource.getURI());	//makes sure the resource URI is valid and normalize the URI
			return resourceMap.put(resourceURI, resource);	//store the resource description, mapped to the resource URI
		}

		/**Returns the resource description configured for the given URI.
		@param resourceURI The URI of the resource the description of which to return.
		@return The resource with a description configured for the given URI, or <code>null</code> if there is no resource configured for the given URI.
		@exception NullPointerException if the given URI is <code>null</code>.
		@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
		*/
		protected URFResource retrieveResource(URI resourceURI)
		{
			resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
			return resourceMap.get(resourceURI);
		}

		/**Removes the resource description configured for the given URI.
		@param resourceURI The URI of the resource the description of which to remove.
		@return The removed resource with a description configured for the given URI, or <code>null</code> if there is no resource configured for the given URI.
		@exception NullPointerException if the given URI is <code>null</code>.
		@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
		*/
		protected URFResource removeResource(URI resourceURI)
		{
			resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
			return resourceMap.remove(resourceURI);
		}

		/**@return The read-only configured resource descriptions.*/
		protected Iterable<URFResource> getResources()
		{
			return unmodifiableCollection(resourceMap.values());	//return an unmodifiable version of the resources
		}

		/**Stores multiple resources.
		@param resources The resources to store.
		@exception NullPointerException if a given resources is <code>null</code>.
		@exception IllegalArgumentException if a given resource does not reside inside this repository.
		@see #storeResource(URFResource)
		*/
		public void setResources(final Set<URFResource> resources)
		{
			resourceMap.clear();	//clear the current resources
			for(final URFResource resource:resources)	//look at each resource
			{
				storeResource(resource);	//add this resource
			}
		}

	/**Default constructor with no root URI defined.
	The root URI must be defined before the repository is opened.
	*/
	public AbstractReadOnlyRepository()
	{
		this(null);
	}

	/**URI constructor with no separate private URI namespace.
	A {@link URFResourceTURFIO} description I/O is created and initialized.
	@param rootURI The URI identifying the location of this repository.
	*/
	public AbstractReadOnlyRepository(final URI rootURI)
	{
		this(rootURI, createDefaultURFResourceDescriptionIO());	//create a default resource description I/O using TURF
	}

	/**Root URI description I/O constructor.
	@param rootURI The URI identifying the location of this repository.
	@param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	@exception NullPointerException if the description I/O is <code>null</code>.
	*/
	public AbstractReadOnlyRepository(final URI rootURI, final URFIO<URFResource> descriptionIO)
	{
		super(rootURI, descriptionIO);
	}

	/**{@inheritDoc} This implementation throws a {@link ResourceForbiddenException} if the resource URI exists.*/
	@Override
	protected OutputStream getResourceOutputStreamImpl(final URI resourceURI, final URFDateTime newContentModified) throws ResourceIOException
	{
		if(!resourceExists(resourceURI))	//if the resource doesn't exist
		{
			throw new ResourceNotFoundException(resourceURI, "Cannot open output stream to non-existent resource "+resourceURI+" in repository.");
		}
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

	/**{@inheritDoc} This implementation throws a {@link ResourceForbiddenException}.*/
	@Override
	protected OutputStream createResourceImpl(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

	/**Alters properties of a given resource.
	<p>This implementation throws a {@link ResourceForbiddenException} if the resource URI is within this repository.</p>
	@param resourceURI The reference URI of the resource.
	@param resourceAlteration The specification of the alterations to be performed on the resource.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource alteration is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be altered.
	*/
	public URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.alterResourceProperties(resourceURI, resourceAlteration);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

	/**Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destionation only if requested. 
	<p>This implementation throws a {@link ResourceForbiddenException} if the resource URI is within this repository.</p>
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void copyResource(URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.copyResource(resourceURI, destinationURI, overwrite);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final Repository destinationRepository=getSubrepository(destinationURI);	//see if the destination URI lies within a subrepository
		if(destinationRepository!=this)	//if the destination URI lies within a subrepository
		{
			copyResource(resourceURI, destinationRepository, destinationURI, overwrite);	//copy between repositories
		}
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

	/**Deletes a resource.
	<p>This implementation throws a {@link ResourceForbiddenException} if the resource URI is within this repository.</p>
	@param resourceURI The reference URI of the resource to delete.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if the resource could not be deleted.
	*/
	public void deleteResource(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.deleteResource(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

	/**Moves a resource to another URI in this repository, overwriting any resource at the destination only if requested.
	<p>This implementation throws a {@link ResourceForbiddenException} if the resource URI is within this repository.</p>
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void moveResource(URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.moveResource(resourceURI, destinationURI, overwrite);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

}

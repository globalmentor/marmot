/*
 * Copyright © 2009 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import com.globalmentor.net.*;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.Content;

/**Abstract implementation of a repository that does not allow resources to be modified.
@author Garret Wilson
*/
public abstract class AbstractReadOnlyRepository extends AbstractRepository
{

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

	/**Gets an output stream to the contents of the resource specified by the given URI.
	The resource description will be updated with the specified content modified datetime if given.
	An error is generated if the resource does not exist.
	<p>This implementation throws a {@link ResourceForbiddenException} if the resource URI exists and is within this repository.</p>
	@param resourceURI The URI of the resource to access.
	@param newContentModified The new content modified datetime for the resource, or <code>null</code> if the content modified datetime should not be updated.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource.
	@see Content#MODIFIED_PROPERTY_URI
	*/
	public OutputStream getResourceOutputStream(URI resourceURI, URFDateTime newContentModified) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getResourceOutputStream(resourceURI, newContentModified);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		if(!resourceExists(resourceURI))	//if the resource doesn't exist
		{
			throw new ResourceNotFoundException(resourceURI, "Cannot open output stream to non-existent resource "+resourceURI+" in repository.");
		}
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

	/**Creates a new resource with the given description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
	If a resource with no contents is desired, {@link #createResource(URI, URFResource, byte[])} with zero bytes is better suited for this task.
	<p>This implementation throws a {@link ResourceForbiddenException} if the resource URI is within this repository.</p>
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public OutputStream createResource(URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createResource(resourceURI, resourceDescription);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only.");
	}

	/**Creates a new resource with the given description and contents.
	If a resource already exists at the given URI it will be replaced.
	<p>This implementation throws a {@link ResourceForbiddenException} if the resource URI is within this repository.</p>
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createResource(resourceURI, resourceDescription, resourceContents);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
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

	/**Moves a resource to another URI in this repository, overwriting any resource at the destionation only if requested.
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

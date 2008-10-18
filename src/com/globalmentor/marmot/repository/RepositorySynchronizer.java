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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

import static com.globalmentor.urf.content.Content.*;

import com.globalmentor.io.InputStreams;
import com.globalmentor.marmot.repository.Repository;
import static com.globalmentor.net.URIs.*;

import com.globalmentor.urf.URFDateTime;
import com.globalmentor.urf.URFResource;
import com.globalmentor.util.Debug;

/**Marmot synchronization support.
@author Garret Wilson
*/
public class RepositorySynchronizer
{

	/**The resolution to perform on an orphan source or destination repository that doesn't exist in the other repository.*/
	public enum OrphanResolution
	{
		/**Copy the orphan resource.*/
		COPY,
		/**Move the orphan resource.*/
		MOVE,
		/**Delete the orphan resource.*/
		DELETE,
		/**Do nothing.*/
		IGNORE;
	}

	/**How to resolve a descrepancy between a source and a destination resource content.*/
	public enum ContentResolution
	{
		/**The source will overwrite the destination; the destination is intended to be a mirror of the source.*/
		BACKUP,
		/**The destination will overwrite the source; the source is intended to be a mirror of the destination.*/
		RESTORE,
		/**Newer resources overwrite older resources; The source and destination are intended to be updated with the latest changes from each.*/
		SYNCHRONIZE,
		/**Nothing will happen.*/
		IGNORE;
	}

	/**The resolution if there is a source resource and no corresponding destination resource.*/
	private OrphanResolution orphanSourceResolution;

		/**@return The resolution if there is a source resource and no corresponding destination resource.*/
		public OrphanResolution getOrphanSourceResolution() {return orphanSourceResolution;}

	/**The resolution if there is a destination resource and no corresponding source resource.*/
	private OrphanResolution orphanDestinationResolution;

		/**@return The resolution if there is a destination resource and no corresponding source resource.*/
		public OrphanResolution getOrphanDestinationResolution() {return orphanDestinationResolution;}

	/**How a content incompatibility between resources will be resolved.*/
	private ContentResolution contentResolution;

		/**@return How a content incompatibility between resources will be resolved.*/
		public ContentResolution getContentResolution() {return contentResolution;}

	/**Default constructor with backup settings.*/
	public RepositorySynchronizer()
	{
		orphanSourceResolution=OrphanResolution.COPY;
		orphanDestinationResolution=OrphanResolution.DELETE;
		contentResolution=ContentResolution.BACKUP;
	}

	/**Synchronizes two resources in two separate repositories.
	If the resources are collections, the child resources will also be synchronized.
	@param sourceRepository The repository in which the source resource lies.
	@param sourceResourceURI The URI of the source resource.
	@param destinationRepository The repository in which the destination resource lies.
	@param destinationResourceURI The URI of the destination resource.
	@throws IOException if there is an I/O error whle synchronizing the resources.
	*/
	public void synchronize(final Repository sourceRepository, final URI sourceResourceURI, final Repository destinationRepository, final URI destinationResourceURI) throws IOException
	{
		final URFResource sourceResourceDescription=sourceRepository.resourceExists(sourceResourceURI) ? sourceRepository.getResourceDescription(sourceResourceURI) : null;	//get the description of the source resource if it exists
		final URFResource destinationResourceDescription=destinationRepository.resourceExists(destinationResourceURI) ? destinationRepository.getResourceDescription(destinationResourceURI) : null;	//get the description of the destination resource if it exists
		synchronize(sourceRepository, sourceResourceURI, sourceResourceURI, sourceResourceDescription, destinationRepository, destinationResourceURI, destinationResourceURI, destinationResourceDescription);	//synchronize using the descriptions and the initial URIs as the base URIs
	}

	/**Synchronizes two resources in two separate repositories.
	If the resources are collections, the child resources will also be synchronized.
	@param sourceRepository The repository in which the source resource lies.
	@param sourceBaseURI The base URI in the source repository; the root of the source tree being synchronized.
	@param sourceResourceURI The URI of the source resource.
	@param sourceResourceDescription The description of the source resource, or <code>null</code> if the source resource does not exist.
	@param destinationRepository The repository in which the destination resource lies.
	@param destinationBaseURI The base URI in the destination repository; the root of the destination tree being synchronized.
	@param destinationResourceURI The URI of the destination resource.
	@param destinationResourceDescription The description of the destination resource, or <code>null</code> if the destination resource does not exist.
	@throws IOException if there is an I/O error whle synchronizing the resources.
	@throws IllegalArgumentException if one of the resources is a collection and the other is not.
	*/
	protected void synchronize(final Repository sourceRepository, final URI sourceBaseURI, final URI sourceResourceURI, final URFResource sourceResourceDescription, final Repository destinationRepository, final URI destinationBaseURI, final URI destinationResourceURI, final URFResource destinationResourceDescription) throws IOException
	{
		final boolean isSourceCollection=isCollectionURI(sourceResourceDescription.getURI());	//see if the source is a collection
		final boolean isDestinationCollection=isCollectionURI(destinationResourceDescription.getURI());	//see if the destination is a collection
			//resource/collection
		if(isSourceCollection!=isDestinationCollection)	//if we have a resource/collection discrepancy
		{
			throw new IllegalArgumentException("The resources are of different types: "+sourceResourceDescription.getURI()+", "+destinationResourceDescription.getURI());	//one resource is a collection; the other is a normal resource
		}
//Debug.trace("Synchronizing from", sourceResourceURI, "to", destinationResourceURI);
		final boolean sourceExists=sourceResourceDescription!=null;	//see if the source exists
//Debug.trace("source exists", sourceExists);
		final boolean destinationExists=destinationResourceDescription!=null;	//see if the destination exists
//Debug.trace("destination exists", destinationExists);
		if(sourceExists!=destinationExists)	//if one resource exists and the other doesn't
		{
			final OrphanResolution resolution;
			if(sourceExists)	//if the source resource exists but not the destination
			{
				resolution=getOrphanSourceResolution();	//determine the resolution
				Debug.info("Orphan source resource", sourceResourceURI, "performing action", resolution, "for destination resource", destinationResourceURI);
				resolveOrphan(resolution, sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//perform the correct action when the source exists and the destination does not
/*TODO del
				switch(resolution)	//update the descriptions based upon the resolution
				{
					case MOVE:	//both move and copy result in a new output; move also removes the input
						sourceResourceDescription=null;
					case COPY:
						destinationResourceDescription=destinationRepository.getResourceDescription(destinationResourceURI); 
						break;
					case DELETE:
						sourceResourceDescription=null;
						break;
					case IGNORE:
						break;
					default:
						throw new AssertionError("Unrecognized resolution "+resolution);
				}
*/
			}
			else	//if the source resource does not exist but the destination does
			{
				resolution=getOrphanDestinationResolution();	//determine the resolution
				Debug.info("Performing action", getOrphanDestinationResolution(), "for source resource", sourceResourceURI, "on orphan destination resource", destinationResourceURI);
				resolveOrphan(resolution, destinationRepository, destinationResourceURI, sourceRepository, sourceResourceURI);	//perform the correct action when the destination exists and the source does not
/*TODO del
				switch(resolution)	//update the descriptions based upon the resolution
				{
					case MOVE:	//both move and copy result in a new output; move also removes the input
						destinationResourceDescription=null;
					case COPY:
						sourceResourceDescription=sourceRepository.getResourceDescription(sourceResourceURI); 
						break;
					case DELETE:
						destinationResourceDescription=null;
						break;
					case IGNORE:
						break;
					default:
						throw new AssertionError("Unrecognized resolution "+resolution);
				}
*/
			}
		}
		else if(sourceExists)	//if both resources exist (we know at this point that either both exist or both don't exist)
		{
			final boolean isContentSynchronized=isContentSynchronized(sourceRepository, sourceResourceDescription, destinationRepository, destinationResourceDescription);	//see if the content of the two resources are synchronized
			if(!isContentSynchronized)//if the source and destination are not synchronized
			{
				resolveContent(getContentResolution(), sourceRepository, sourceResourceDescription, destinationRepository, destinationResourceDescription);	//resolve the descrepancy between source and destination
//Debug.trace("done resolving");
			}
		}
		//TODO synchronize metadata
//Debug.trace("both exist; we did what we needed to do; nothing more should happen (other than checking collection).");
		if(isSourceCollection && sourceRepository.resourceExists(sourceResourceURI) && destinationRepository.resourceExists(destinationResourceURI))	//if now have two collections that both exist
		{
			final Map<URI, URFResource> destinationChildResourceDescriptions=new LinkedHashMap<URI, URFResource>();	//create a map for the destination resources, preserving their iteration order only as a courtesy
			for(final URFResource destinationChildResourceDescription:destinationRepository.getChildResourceDescriptions(destinationResourceURI))	//prepopulate the destination child resource map to allow quick lookup when we iterate the source child resources
			{
				destinationChildResourceDescriptions.put(destinationChildResourceDescription.getURI(), destinationChildResourceDescription);
			}
			final Map<URI, URFResource> sourceChildResourceDescriptions=new LinkedHashMap<URI, URFResource>();	//create a map for the source resources, preserving their iteration order only as a courtesy
			for(final URFResource sourceChildResourceDescription:sourceRepository.getChildResourceDescriptions(sourceResourceURI))	//iterate the source child resources
			{
				sourceChildResourceDescriptions.put(sourceChildResourceDescription.getURI(), sourceChildResourceDescription);	//store this source child resource in the map
				final URI destinationChildResourceURI=destinationBaseURI.resolve(sourceBaseURI.relativize(sourceChildResourceDescription.getURI()));	//resolve the relative child URI against the base destination URI to determine what the destnation child resource URI should be
				final URFResource destinationChildResourceDescription=destinationChildResourceDescriptions.get(destinationChildResourceURI);	//get the description of the destination child resource (although there may not be one)
				synchronize(sourceRepository, sourceBaseURI, sourceChildResourceDescription.getURI(), sourceChildResourceDescription, destinationRepository, destinationBaseURI, destinationChildResourceURI, destinationChildResourceDescription);	//synchronize this source child and the corresponding destination child, the latter of which may not exist
			}
			for(final URFResource destinationChildResourceDescription:destinationChildResourceDescriptions.values())	//iterate the destination child resources to synchronize any destination resources that may not be in the source
			{
				final URI sourceChildResourceURI=sourceBaseURI.resolve(destinationBaseURI.relativize(destinationChildResourceDescription.getURI()));	//resolve the relative child URI against the base source URI to determine what the source child resource URI should be
				final URFResource sourceChildResourceDescription=sourceChildResourceDescriptions.get(sourceChildResourceURI);	//get the description of the source child resource (although there may not be one)
				if(sourceChildResourceDescription==null)	//only synchronize destination resources for which there is no corresponding source child resource, because we already synchronized all the corresponding ones
				{
					synchronize(sourceRepository, sourceBaseURI, sourceChildResourceURI, sourceChildResourceDescription, destinationRepository, destinationBaseURI, destinationChildResourceDescription.getURI(), destinationChildResourceDescription);	//synchronize this source child and the destination child, the former of which does not exist
				}
			}
		}
	}

	/**Checks to see if the content of two existing resources are mirrors of one another.
	This method attempts to guess whether content has changed by examining various properties.
	Two collections with the same relevant individual properties are considered mirrors; child resources are not examined.
	@param sourceRepository The repository in which the source resource lies.
	@param sourceResourceDescription The source resource
	@param destinationRepository The repository in which the destination resource lies.
	@param destinationResourceDescription The destination resource.
	@return <code>true</code> If both resources are of the same type and have the same relevant properties, such as size and content.
	@throws NullPointerException if one of the repositories and/or resources is <code>null</code>.
	@throws IOException if there is a problem accessing one of the resources.
	@throws IllegalArgumentException if one of the resources is a collection and the other is not.
	*/
	public boolean isContentSynchronized(final Repository sourceRepository, final URFResource sourceResourceDescription, final Repository destinationRepository, final URFResource destinationResourceDescription) throws IOException
	{
		final boolean isSourceCollection=isCollectionURI(sourceResourceDescription.getURI());	//see if the source is a collection
		final boolean isDestinationCollection=isCollectionURI(destinationResourceDescription.getURI());	//see if the destination is a collection
			//resource/collection
		if(isSourceCollection!=isDestinationCollection)	//if we have a resource/collection discrepancy
		{
			throw new IllegalArgumentException("The resources are of different types: "+sourceResourceDescription.getURI()+", "+destinationResourceDescription.getURI());	//one resource is a collection; the other is a normal resource
		}
		final long sourceContentLength=getContentLength(sourceResourceDescription);	//get the size of the source
		final long destinationContentLength=getContentLength(destinationResourceDescription);	//get the size of the destination
			//size
		if(!isSourceCollection && (sourceContentLength<0 || destinationContentLength<0))	//if we don't know one of the sizes of non-collections
		{
			return false;	//there is a size discrepancy
		}
		if(sourceContentLength!=destinationContentLength)	//if the sizes don't match
		{
			return false;	//there is a size discrepancy
		}
			//date
		if(!isSourceCollection || sourceContentLength>0)	//ignore date descrepancies of collections with no content
		{
			final Date sourceDate=getModified(sourceResourceDescription);	//get the date of the source
			final Date destinationDate=getModified(destinationResourceDescription);	//get the date of the destination
			if(sourceDate==null || !sourceDate.equals(destinationDate))	//if the dates don't match or if there is no date
			{
				return false;	//there is a date discrepancy
			}
		}
		return true;	//the resources matched all our tests
	}

	/**Resolves an orphan situation.
	@param resolution The action to perform.
	@param orphanRepository The source repository in this context.
	@param orphanResourceURI The source resource in this context.
	@param otherRepository The destination repository in this context.
	@param otherResourceURI The destination resource in this context.
	@throws IOException if there is an I/O error while performing the action.
	*/
	protected void resolveOrphan(final OrphanResolution resolution, final Repository orphanRepository, final URI orphanResourceURI, final Repository otherRepository, final URI otherResourceURI) throws IOException
	{
Debug.info("Resolve orphan:", resolution, orphanRepository, orphanResourceURI, otherRepository, otherResourceURI);
		switch(resolution)	//see what action to take
		{
			case COPY:
				orphanRepository.copyResource(orphanResourceURI, otherRepository, otherResourceURI);	//copy the source to the destination
				break;
			case MOVE:
				orphanRepository.moveResource(orphanResourceURI, otherRepository, otherResourceURI);	//move the source to the destination
				break;
			case DELETE:
				orphanRepository.deleteResource(orphanResourceURI);	//delete the source
				break;
			case IGNORE:
				break;
			default:
				throw new AssertionError("Unrecognized resolution "+resolution);
		}		
	}

	/**Resolves a descrepancy between a source and a destination resource.
	If the resource dates are not available or are the same, the {@link ContentResolution#SYNCHRONIZE} resolution will have no effect.
	Both resources must exist.
	@param resolution How the descrepancy should be resolved
	@param sourceRepository The source repository.
	@param sourceResourceDescription The description of the source resource.
	@param destinationRepository The destination repository.
	@param destinationResourceDescription The description of the destination resource.
	@throws NullPointerException if any of the given arguments are <code>null</code>.
	@throws IOException if there is an I/O error while performing the action.
	*/
	protected void resolveContent(ContentResolution resolution, final Repository sourceRepository, final URFResource sourceResourceDescription, final Repository destinationRepository, final URFResource destinationResourceDescription) throws IOException
	{
Debug.info("Resolve content:", resolution, sourceRepository, sourceResourceDescription.getURI(), destinationRepository, destinationResourceDescription.getURI());
		if(resolution==ContentResolution.IGNORE)	//if this situation should be ignored
		{
			return;	//don't do anything
		}
		if(resolution==ContentResolution.SYNCHRONIZE) {	//if we should synchronize the resource content
			final Date sourceDate=getModified(sourceResourceDescription);	//get the date of the source
			final Date destinationDate=getModified(sourceResourceDescription);	//get the date of the destination
			if(sourceDate!=null && destinationDate!=null)	//if we have dates for the resources
			{
				final int dateComparison=sourceDate.compareTo(destinationDate);	//compare the dates
				if(dateComparison>0)	//if the destination is older than the source
				{
					resolution=ContentResolution.BACKUP;	//copy the source to the destination
				}
				else if(dateComparison<0)	//if the source is older than the destination
				{
					resolution=ContentResolution.RESTORE;	//copy the destination to the source
				}
				else	//if the two resources have the same date
				{
					return;	//don't do anything
				}
			}
		}
		final Repository inputRepository, outputRepository;
		final URFResource inputResourceDescription, outputResourceDescription;
		switch(resolution)	//see how to resolve the descrepancy; at the point the only options we haven't covered are backup and restore
		{
			case BACKUP:
				inputRepository=sourceRepository;
				inputResourceDescription=sourceResourceDescription;
				outputRepository=destinationRepository;
				outputResourceDescription=destinationResourceDescription;
				break;
			case RESTORE:
				inputRepository=destinationRepository;
				inputResourceDescription=destinationResourceDescription;
				outputRepository=sourceRepository;
				outputResourceDescription=sourceResourceDescription;
				break;
			default:
				throw new AssertionError("Unrecognized resolution "+resolution);
		}
		final URI inputResourceURI=inputResourceDescription.getURI();
		final URI outputResourceURI=outputResourceDescription.getURI();
		final URFDateTime inputContentModified=getModified(inputResourceDescription);	//get the date of the input resource, if any
		final InputStream inputStream=inputRepository.getResourceInputStream(inputResourceURI);	//get an input stream to the input resource
		try
		{
			final OutputStream outputStream=outputRepository.getResourceOutputStream(outputResourceURI, inputContentModified);	//get an output stream to the output resource, keeping the content modified datetime as the input resource, if any
			try
			{
				InputStreams.copy(inputStream, outputStream);	//copy the resource
			}
			finally
			{
				outputStream.close();	//always close the output stream
			}
		}
		finally
		{
			inputStream.close();	//always close the input stream
		}
	}

}

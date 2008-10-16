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
import java.net.URI;
import java.util.*;

import static com.globalmentor.urf.content.Content.*;

import com.globalmentor.marmot.repository.Repository;
import static com.globalmentor.net.URIs.*;
import com.globalmentor.urf.URFResource;
import com.globalmentor.util.Debug;

/**Marmot synchronization client.
@author Garret Wilson
*/
public class RepositorySynchronizer
{

	/**An action to perform for a single resource between source and destination repositories.*/
	public enum Action
	{
		/**Copy the source to the destination within the context.*/
		COPY,
		/**Move the source to the destination within the context.*/
		MOVE,
		/**Delete the source within the context.*/
		DELETE,
		/**Do nothing.*/
		IGNORE;
	}

	/**How to resolve a descrepancy between a source and a destination resource.*/
	public enum Resolution
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

	/**The action to perform if there is a source resource and no corresponding destination resource.*/
	private Action orphanSourceAction;

		/**@return The action to perform if there is a source resource and no corresponding destination resource.*/
		public Action getOrphanSourceAction() {return orphanSourceAction;}

	/**The action to perform if there is a destination resource and no corresponding source resource.*/
	private Action orphanDestinationAction;

		/**@return The action to perform if there is a destination resource and no corresponding source resource.*/
		public Action getOrphanDestinationAction() {return orphanDestinationAction;}

	/**How a content incompatibility between resources will be resolved.*/
	private Resolution contentUnsynchronizedResolution;

		/**@return How a content incompatibility between resources will be resolved.*/
		public Resolution getContentUnsynchronizedResolution() {return contentUnsynchronizedResolution;}

	/**Default constructor with backup settings.*/
	public RepositorySynchronizer()
	{
		orphanSourceAction=Action.COPY;
		orphanDestinationAction=Action.DELETE;
		contentUnsynchronizedResolution=Resolution.BACKUP;
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
Debug.trace("Synchronizing from", sourceResourceURI, "to", destinationResourceURI);
		final boolean sourceExists=sourceResourceDescription!=null;	//see if the source exists
Debug.trace("source exists", sourceExists);
		final boolean destinationExists=destinationResourceDescription!=null;	//see if the destination exists
Debug.trace("destination exists", destinationExists);
		if(sourceExists!=destinationExists)	//if one resource exists and the other doesn't
		{
			if(sourceExists)	//if the source resource exists but not the destination
			{
				Debug.info("Orphan source resource", sourceResourceURI, "performing action", getOrphanSourceAction(), "for destination resource", destinationResourceURI);
				performAction(getOrphanSourceAction(), sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//perform the correct action when the source exists and the destination does not
			}
			else	//if the source resource does not exist but the destination does
			{
				Debug.info("Performing action", getOrphanDestinationAction(), "for source resource", sourceResourceURI, "on orphan destination resource", destinationResourceURI);
				performAction(getOrphanDestinationAction(), destinationRepository, destinationResourceURI, sourceRepository, sourceResourceURI);	//perform the correct action when the destination exists and the source does not
			}			
		}
		else if(sourceExists)	//if both resources exist (we know at this point that either both exist or both don't exist)
		{
			final boolean isSourceCollection=isCollectionURI(sourceResourceDescription.getURI());	//see if the source is a collection
			final boolean isDestinationCollection=isCollectionURI(destinationResourceDescription.getURI());	//see if the destination is a collection
				//resource/collection
			if(isSourceCollection!=isDestinationCollection)	//if we have a resource/collection discrepancy
			{
				throw new IllegalArgumentException("The resources are of different types: "+sourceResourceDescription.getURI()+", "+destinationResourceDescription.getURI());	//one resource is a collection; the other is a normal resource
			}
			final boolean isSynchronized=isContentSynchronized(sourceRepository, sourceResourceDescription, destinationRepository, destinationResourceDescription);	//see if the two resources are synchronized
			if(!isSynchronized)//if the source and destination are not synchronized
			{
				Debug.info("Resolving discrepancy between source resource", sourceResourceURI, "and destination resource", destinationResourceURI, "by", getContentUnsynchronizedResolution());
				resolve(getContentUnsynchronizedResolution(), sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//resolve the descrepancy between source and destination
Debug.trace("done resolving");
			}
		}
		//TODO synchronize collection content
		//TODO synchronize metadata
Debug.trace("both exist; we did what we needed to do; nothing more should happen (other than checking collection).");
		if(sourceRepository.isCollection(sourceResourceURI) && destinationRepository.isCollection(destinationResourceURI))	//if after all the resource-level synchronization we now have two collections
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
			if(sourceDate==null || sourceDate!=destinationDate)	//if the dates don't match or if there is no date
			{
				return false;	//there is a date discrepancy
			}
		}
		return true;	//the resources matched all our tests
	}

	/**Performs an action on a source and destination resource pair.
	@param action The action to perform.
	@param sourceRepository The source repository in this context.
	@param sourceResourceURI The source resource in this context.
	@param destinationRepository The destination repository in this context.
	@param destinationResourceURI The destination resource in this context.
	@throws IOException if there is an I/O error while performing the action.
	*/
	protected void performAction(final Action action, final Repository sourceRepository, final URI sourceResourceURI, final Repository destinationRepository, final URI destinationResourceURI) throws IOException
	{
		switch(action)	//see what action to take
		{
			case COPY:
				sourceRepository.copyResource(sourceResourceURI, destinationRepository, destinationResourceURI);	//copy the source to the destination
				break;
			case MOVE:
				sourceRepository.moveResource(sourceResourceURI, destinationRepository, destinationResourceURI);	//move the source to the destination
				break;
			case DELETE:
				sourceRepository.deleteResource(sourceResourceURI);	//delete the source
				break;
			case IGNORE:
				break;
			default:
				throw new AssertionError("Unrecognized action "+action);
		}		
	}

	/**Resolves a descrepancy between a source and a destination resource.
	If the resource dates are not available, the {@link Resolution#SYNCHRONIZE} resolution will have no effect.
	@param resolution How the descrepancy should be resolved
	@param sourceRepository The source repository in this context.
	@param sourceResourceURI The source resource in this context.
	@param destinationRepository The destination repository in this context.
	@param destinationResourceURI The destination resource in this context.
	@throws IOException if there is an I/O error while performing the action.
	*/
	protected void resolve(final Resolution resolution, final Repository sourceRepository, final URI sourceResourceURI, final Repository destinationRepository, final URI destinationResourceURI) throws IOException
	{
Debug.trace("resolving descrepancy");
		switch(resolution)	//see how to resolve the descrepancy
		{
			case BACKUP:
Debug.trace("starting to backup through repository");
				sourceRepository.copyResource(sourceResourceURI, destinationRepository, destinationResourceURI);	//copy the source to the destination
Debug.trace("finished backing up through repository");
				break;
			case RESTORE:
				destinationRepository.copyResource(destinationResourceURI, sourceRepository, sourceResourceURI);	//copy the destination to the source
				break;
			case SYNCHRONIZE:	//synchronization will do nothing if a date is not availale for one of the resources or if the dates are the same
				{
					final URFResource sourceResource=sourceRepository.getResourceDescription(sourceResourceURI);	//get a description of the source resource
					final URFResource destinationResource=destinationRepository.getResourceDescription(destinationResourceURI);	//get a description of the destination resource
					final Date sourceDate=getModified(sourceResource);	//get the date of the source
					final Date destinationDate=getModified(destinationResource);	//get the date of the destination
					if(sourceDate!=null && destinationDate!=null)	//if we have dates for the resources
					{
						final int dateComparison=sourceDate.compareTo(destinationDate);	//compare the dates
						if(dateComparison<0)	//if the source is older than the destination
						{
							destinationRepository.copyResource(destinationResourceURI, sourceRepository, sourceResourceURI);	//copy the destination to the source						
						}
						else if(dateComparison>0)	//if the destination is older than the source
						{
							sourceRepository.copyResource(sourceResourceURI, destinationRepository, destinationResourceURI);	//copy the source to the destination						
						}
					}
				}
				break;
			case IGNORE:
				break;
			default:
				throw new AssertionError("Unrecognized resolution "+resolution);
		}		
	}

}

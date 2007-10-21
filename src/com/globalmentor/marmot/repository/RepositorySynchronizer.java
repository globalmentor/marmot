package com.globalmentor.marmot.repository;

import java.io.IOException;
import java.net.URI;
import java.util.Date;

import com.garretwilson.urf.URFResource;
import static com.garretwilson.urf.content.Content.*;

import com.garretwilson.util.Debug;
import com.globalmentor.marmot.repository.Repository;

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

	/**How to resolve a descrepancy between a source and a destination resource..*/
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

	/**How an incompatibility between resources will be resolved.*/
	private Resolution unsynchronizedResolution;

		/**@return How an incompatibility between resources will be resolved.*/
		public Resolution getUnsynchronizedResolution() {return unsynchronizedResolution;}

	/**Default constructor with backup settings.*/
	public RepositorySynchronizer()
	{
		orphanSourceAction=Action.COPY;
		orphanDestinationAction=Action.DELETE;
		unsynchronizedResolution=Resolution.BACKUP;
	}
	
	/**Synchronizes two resources in two separate repositories.
	If the resources are collections, the child resources will also be synchronized.
	@param sourceRepository The repository in which the source resource lies.
	@param sourceResourceURI The URI of the source resource
	@param destinationRepository The repository in which the destination resource lies.
	@param destinationResourceURI The URI of the destination resource.
	@throws IOException if there is an I/O error whle synchronizing the resources.
	*/
	public void synchronize(final Repository sourceRepository, final URI sourceResourceURI, final Repository destinationRepository, final URI destinationResourceURI) throws IOException
	{
Debug.trace("ready to synchronize source", sourceResourceURI, "and destination", destinationResourceURI);
		final boolean sourceExists=sourceRepository.resourceExists(sourceResourceURI);	//see if the source exists
Debug.trace("source exists", sourceExists);
		final boolean destinationExists=destinationRepository.resourceExists(destinationResourceURI);	//see if the destination exists
Debug.trace("destination exists", destinationExists);
		if(sourceExists!=destinationExists)	//if one resource exists and the other doesn't
		{
			if(sourceExists)	//if the source resource exists but not the destination
			{
				final boolean isSourceCollection=sourceRepository.isCollection(sourceResourceURI);	//see if the source is a collection
				if(isSourceCollection)	//if the source is a collection
				{
					Debug.info("Orphan source collection", sourceResourceURI, "creating destination collection", destinationResourceURI, "to match.");
					destinationRepository.createCollection(destinationResourceURI);	//crate a destination collection TODO allow configuration				
				}
				else	//if the source is not a collection
				{
					Debug.info("Orphan source resource", sourceResourceURI, "performing action", getOrphanSourceAction(), "for destination resource", destinationResourceURI);
					performAction(getOrphanSourceAction(), sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//perform the correct action when the source exists and the destination does not
				}
			}
			else	//if the source resource does not exist but the destination does
			{
				final boolean isDestinationCollection=destinationRepository.isCollection(destinationResourceURI);	//see if the destination is a collection
				if(isDestinationCollection)	//if the destination is a collection
				{
					Debug.info("Creating source collection", sourceResourceURI, "to match orphaned destination collection", destinationResourceURI);
					destinationRepository.deleteResource(destinationResourceURI);	//delete the destination collection  TODO allow configuration		
				}
				else	//if the destination is not a collection
				{
					Debug.info("Performing action", getOrphanDestinationAction(), "for source resource", sourceResourceURI, "on orphan destination resource", destinationResourceURI);
					performAction(getOrphanDestinationAction(), destinationRepository, destinationResourceURI, sourceRepository, sourceResourceURI);	//perform the correct action when the destination exists and the source does not
				}
			}			
		}
		else if(sourceExists)	//if both resources exist (we know at this point that either both exist or both don't exist)
		{
			final boolean isSourceCollection=sourceRepository.isCollection(sourceResourceURI);	//see if the source is a collection
			final boolean isDestinationCollection=destinationRepository.isCollection(destinationResourceURI);	//see if the destination is a collection
			if(isSourceCollection==isDestinationCollection)	//if the source and destination are compatible
			{
				final boolean isSynchronized=isSynchronized(sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//see if the two resources are synchronized
				if(!isSynchronized)//if the source and destination are not synchronized
				{
					Debug.info("Resolving discrepancy between source resource", sourceResourceURI, "and destination resource", destinationResourceURI, "by", getUnsynchronizedResolution());
					resolve(getUnsynchronizedResolution(), sourceRepository, sourceResourceURI, destinationRepository, destinationResourceURI);	//resolve the descrepancy between source and destination
Debug.trace("done resolving");
				}
			}
			else	//if the source and destination are not the same type of resource
			{
				Debug.info("Resolving discrepancy between source", sourceResourceURI, "and destination", destinationResourceURI, "resource types to match the source");
				destinationRepository.deleteResource(destinationResourceURI);	//delete the destination, whatever it is
				if(isSourceCollection)	//if the source is a collection
				{
					destinationRepository.createCollection(destinationResourceURI);	//create a destination collection TODO allow configuration									
				}
				else	//if the source is a normal resource
				{
					sourceRepository.copyResource(sourceResourceURI, destinationRepository, destinationResourceURI);	//copy the source to the destination
				}
			}
		}
Debug.trace("both existed; we did what we needed to do; nothing more should happen (other than checking collection");
		if(sourceRepository.isCollection(sourceResourceURI) && destinationRepository.isCollection(destinationResourceURI))	//if after all the resource-level synchronization we now have two collections
		{
			//TODO synchronize children
		}
	}

	/**Checks to see if two resources are mirrors of one another.
	This method considers two non-existant resources to be mirrors.
	Two collections with the same relevant individual properties are considered mirrors; child resources are not examined.
	@param sourceRepository The repository in which the source resource lies.
	@param sourceResourceURI The URI of the source resource
	@param destinationRepository The repository in which the destination resource lies.
	@param destinationResourceURI The URI of the destination resource.
	@return <code>true</code> If both resources are of the same type and have the same relevant properties, such as size and content.
	@throws IOException if there is a problem accessing one of the resources.
	*/
	public boolean isSynchronized(final Repository sourceRepository, final URI sourceResourceURI, final Repository destinationRepository, final URI destinationResourceURI) throws IOException
	{
		final boolean sourceExists=sourceRepository.resourceExists(sourceResourceURI);	//see if the source exists
		final boolean isSourceCollection=sourceExists && sourceRepository.isCollection(sourceResourceURI);	//see if the source is a collection
		final boolean destinationExists=destinationRepository.resourceExists(destinationResourceURI);	//see if the destination exists
		final boolean isDestinationCollection=destinationExists && destinationRepository.isCollection(destinationResourceURI);	//see if the destination is a collection
		if(sourceExists)	//if we have a source resource
		{
				//exists
			if(!destinationExists)	//if the destination doesn't exist, it can't be a mirror
			{
				return false;	//only the source exists, not the destination
			}
				//resource/collection
			if(isSourceCollection!=isDestinationCollection)	//if we have a resource/collection discrepancy
			{
				return false;	//one resource is a collection; the other is a normal resource
			}
			final URFResource sourceResource=sourceRepository.getResourceDescription(sourceResourceURI);	//get a description of the source resource
			final URFResource destinationResource=destinationRepository.getResourceDescription(destinationResourceURI);	//get a description of the destination resource
			final long sourceSize=getContentLength(sourceResource);	//get the size of the source
			final long destinationSize=getContentLength(destinationResource);	//get the size of the destination
				//size
			if(sourceSize<0 || sourceSize!=destinationSize)	//if the sizes don't match, or we don't know one of the sizes
			{
				return false;	//there is a size discrepancy
			}
				//date
			final Date sourceDate=getModified(sourceResource);	//get the date of the source
			final Date destinationDate=getModified(destinationResource);	//get the date of the destination
			if(sourceDate==null || sourceDate!=destinationDate)	//if the dates don't match or if there is no date
			{
				return false;	//there is a date discrepancy
			}
			return true;	//the resources matched all our tests
		}
		else	//if no source exists
		{
			return !destinationExists;	//this is only a mirror situation if the destination also doesn't exist
		}	
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

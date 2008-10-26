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

import com.globalmentor.io.InputStreams;
import static com.globalmentor.java.Objects.*;
import com.globalmentor.marmot.repository.Repository;
import static com.globalmentor.net.URIs.*;

import com.globalmentor.urf.*;
import static com.globalmentor.urf.content.Content.*;
import com.globalmentor.urf.content.Content;
import com.globalmentor.util.Debug;

/**Marmot synchronization support.
Synchronization occurs on three levels: individual resources (i.e. orphans), metadata, and content, each of which can have a different resolution specified.
@author Garret Wilson
*/
public class RepositorySynchronizer
{

	/**How to resolve a descrepancy between a source and a destination resource content or metadata.
	For orphan resources, {@link Resolution#SYNCHRONIZE} is treated the same as {@link Resolution#BACKUP}.
	*/
	public enum Resolution
	{
		/**The source will overwrite the destination; the destination is intended to be a mirror of the source.*/
		BACKUP,
		/**The destination will overwrite the source; the source is intended to be a mirror of the destination.*/
		RESTORE,
		/**The source will overwrite the destination, but missing source information will not cause destination information to be removed.*/
		PRODUCE,
		/**The destination will overwrite the source; but missing destination information will not cause source information to be removed.*/
		CONSUME,
		/**Newer information will overwrite older information; the source and destination are intended to be updated with the latest changes from each,
		although for orphan resources this will be consdered the same as {@link #BACKUP}.
		*/
		SYNCHRONIZE,
		/**Nothing will happen.*/
		IGNORE;
	}

	/**Whether this is a test run; if <code>true</code>, no modifications will be made.*/
	private boolean test=false;

		/**@return Whether this is a test run; if <code>true</code>, no modifications will be made.*/
		public boolean isTest() {return test;}

		/**Sets whether this is a test run.
		@param test Whether this is a test run; if <code>true</code>, no modifications will be made.
		*/
		public void setTest(final boolean test) {this.test=test;}

	/**How an orphan resource will be resolved.*/
	private Resolution resourceResolution;

		/**@return How an orphan resource will be resolved.*/
		public Resolution getResourceResolution() {return resourceResolution;}

		/**Sets the resolution for orphan resources.
		@param resourceResolution How an orphan resource will be resolved.
		@throws NullPointerException if the given resolution is <code>null</code>.
		*/
		public void setResourceResolution(final Resolution resourceResolution) {this.resourceResolution=checkInstance(resourceResolution, "Resource resolution cannot be null.");}

	/**How a content incompatibility between resources will be resolved.*/
	private Resolution contentResolution;

		/**@return How a content incompatibility between resources will be resolved.*/
		public Resolution getContentResolution() {return contentResolution;}

		/**Sets the resolution for content.
		@param contentResolution How a content incompatibility between resources will be resolved.
		@throws NullPointerException if the given resolution is <code>null</code>.
		*/
		public void setContentResolution(final Resolution contentResolution){this.contentResolution=checkInstance(contentResolution, "Content resolution cannot be null.");}

	/**How a metadata incompatibility between resources will be resolved.*/
	private Resolution metadataResolution;

		/**@return How a metadata incompatibility between resources will be resolved.*/
		public Resolution getMetadataResolution() {return metadataResolution;}

		/**Sets the resolution for metadata.
		@param metadataResolution How a metadata incompatibility between resources will be resolved.
		@throws NullPointerException if the given resolution is <code>null</code>.
		*/
		public void setMetadataResolution(final Resolution metadataResolution) {this.metadataResolution=checkInstance(metadataResolution, "Metadata resolution cannot be null.");}

	/**The set of source resource URIs to ignore when resolving descrepancies.*/
	private final Set<URI> ignoreSourceResourceURIs=new HashSet<URI>();

		/**Adds a source resource to be ignored when resolving descrepancies.
		@param resourceURI The URI of the source resource to ignore.
		*/
		public void addIgnoreSourceResourceURI(final URI resourceURI)
		{
			ignoreSourceResourceURIs.add(resourceURI);
		}

	/**The set of destination resource URIs to ignore when resolving descrepancies.*/
	private final Set<URI> ignoreDestinationResourceURIs=new HashSet<URI>();

		/**Adds a destination resource to be ignored when resolving descrepancies.
		@param resourceURI The URI of the destination resource to ignore.
		*/
		public void addIgnoreDestinationResourceURI(final URI resourceURI)
		{
			ignoreDestinationResourceURIs.add(resourceURI);
		}

	/**The set of metadata property URIs to ignore when resolving descrepancies.*/
	private final Set<URI> ignorePropertyURIs=new HashSet<URI>();

		/**Adds a metadata property to be ignored when resolving descrepancies.
		@param propertyURI The URI of the metadata property to ignore.
		*/
		public void addIgnorePropertyURI(final URI propertyURI)
		{
			ignorePropertyURIs.add(propertyURI);
		}

	/**Sets the resolution at the resource, content, and metadata level.
	@param resolution How incompatibilities between resources will be resolved.
	@throws NullPointerException if the given resolution is <code>null</code>.
	*/
	public void setResolution(final Resolution resolution)
	{
		setResolution(resolution);
		setContentResolution(resolution);
		setMetadataResolution(resolution);
	}

	/**Default constructor with backup settings.*/
	public RepositorySynchronizer()
	{
		resourceResolution=Resolution.BACKUP;
		contentResolution=Resolution.BACKUP;
		metadataResolution=Resolution.BACKUP;
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
	If either resource is one to be ignored, no action is taken.
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
	protected void synchronize(final Repository sourceRepository, final URI sourceBaseURI, final URI sourceResourceURI, URFResource sourceResourceDescription, final Repository destinationRepository, final URI destinationBaseURI, final URI destinationResourceURI, URFResource destinationResourceDescription) throws IOException
	{
		if(ignoreSourceResourceURIs.contains(sourceResourceURI) || ignoreDestinationResourceURIs.contains(destinationResourceURI))	//if this is a resource to ignore
		{
			return;	//don't do anything further
		}
		final boolean isSourceCollection=isCollectionURI(sourceResourceURI);	//see if the source is a collection
		final boolean isDestinationCollection=isCollectionURI(destinationResourceURI);	//see if the destination is a collection
			//resource/collection
		if(isSourceCollection!=isDestinationCollection)	//if we have a resource/collection discrepancy
		{
			throw new IllegalArgumentException("The resources are of different types: "+sourceResourceURI+", "+destinationResourceURI);	//one resource is a collection; the other is a normal resource
		}
//Debug.trace("Synchronizing from", sourceResourceURI, "to", destinationResourceURI);
		boolean sourceExists=sourceResourceDescription!=null;	//see if the source exists
//Debug.trace("source exists", sourceExists);
		boolean destinationExists=destinationResourceDescription!=null;	//see if the destination exists
//Debug.trace("destination exists", destinationExists);
		if(sourceExists!=destinationExists)	//if one resource exists and the other doesn't
		{
			final Resolution orphanResolution=getResourceResolution();
			if(sourceExists)	//if the source resource exists but not the destination
			{
				if(orphanResolution!=Resolution.IGNORE)
				{
					Debug.info(getTestStatus(), "Resolve source orphan:", orphanResolution, sourceResourceURI, destinationResourceURI);
				}
				if(!isTest())	//if this is not just a test
				{
					switch(orphanResolution)	//update the descriptions based upon the resolution
					{
						case BACKUP:
						case PRODUCE:
						case SYNCHRONIZE:
							sourceRepository.copyResource(sourceResourceURI, destinationRepository, destinationResourceURI);	//copy the source to the destination
							destinationExists=true;
							destinationResourceDescription=destinationRepository.getResourceDescription(destinationResourceURI);
							break;
						case RESTORE:
							sourceRepository.deleteResource(sourceResourceURI);	//delete the source
							sourceExists=false;
							sourceResourceDescription=null;
							break;
						case CONSUME:
						case IGNORE:
							break;
						default:
							throw new AssertionError("Unrecognized resolution "+orphanResolution);
					}
				}
			}
			else	//if the source resource does not exist but the destination does
			{
				if(orphanResolution!=Resolution.IGNORE)
				{
					Debug.info(getTestStatus(), "Resolve destination orphan:", orphanResolution, sourceResourceURI, destinationResourceURI);
				}
				if(!isTest())	//if this is not just a test
				{
					switch(orphanResolution)	//update the descriptions based upon the resolution
					{
						case BACKUP:
						case CONSUME:
						case SYNCHRONIZE:
							destinationRepository.deleteResource(destinationResourceURI);	//delete the destination
							destinationExists=false;
							destinationResourceDescription=null;
							break;
						case RESTORE:
							destinationRepository.copyResource(destinationResourceURI, sourceRepository, sourceResourceURI);	//copy the destination to the source
							sourceExists=true;
							sourceResourceDescription=sourceRepository.getResourceDescription(sourceResourceURI);
							break;
						case PRODUCE:
						case IGNORE:
							break;
						default:
							throw new AssertionError("Unrecognized resolution "+orphanResolution);
					}
				}
			}
		}
		else if(sourceExists)	//if both resources exist (we know at this point that either both exist or both don't exist)
		{
			final Date sourceContentModified=getModified(sourceResourceDescription);	//get the date of the source
			final Date destinationContentModified=getModified(destinationResourceDescription);	//get the date of the destination
			final boolean isContentSynchronized=isContentSynchronized(sourceRepository, sourceResourceDescription, sourceContentModified, destinationRepository, destinationResourceDescription, destinationContentModified);	//see if the content of the two resources are synchronized
			if(!isContentSynchronized)//if the source and destination are not synchronized
			{
				final Resolution resolution=getContentResolution();
				if(resolution!=Resolution.IGNORE)
				{
					Debug.info(getTestStatus(), "Resolve content:", resolution, sourceResourceDescription.getURI(), destinationResourceDescription.getURI());
				}
				resolveContent(resolution, sourceRepository, sourceResourceDescription, sourceContentModified, destinationRepository, destinationResourceDescription, destinationContentModified);	//resolve the descrepancy between source and destination
			}
			final Resolution metadataResolution=getMetadataResolution();
			resolveMetadata(metadataResolution, sourceRepository, sourceResourceDescription, sourceContentModified, destinationRepository, destinationResourceDescription, destinationContentModified);
		}
		if(isSourceCollection && sourceExists && destinationExists)	//if now have two collections that both exist, synchronize the children
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
	@param sourceContentModified The date and time at which the content of the source was modified, or <code>null</code> if not known.
	@param destinationRepository The repository in which the destination resource lies.
	@param destinationResourceDescription The destination resource.
	@param destinationContentModified The date and time at which the content of the destination was modified, or <code>null</code> if not known.
	@return <code>true</code> If both resources are of the same type and have the same relevant properties, such as size and content.
	@throws NullPointerException if one of the repositories and/or resources is <code>null</code>.
	@throws IOException if there is a problem accessing one of the resources.
	@throws IllegalArgumentException if one of the resources is a collection and the other is not.
	*/
	public boolean isContentSynchronized(final Repository sourceRepository, final URFResource sourceResourceDescription, final Date sourceContentModified, final Repository destinationRepository, final URFResource destinationResourceDescription, final Date destinationContentModified) throws IOException
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

	/**Resolves a content descrepancy between a source and a destination resource.
	If the resource dates are the same, the {@link Resolution#SYNCHRONIZE} resolution will have no effect.
	If only one of the resource dates is available, it is considered newer for the purpose of the {@link Resolution#SYNCHRONIZE} resolution.
	If neither resource date is available, the {@link Resolution#SYNCHRONIZE} resolution will be considered the same as the {@link Resolution#BACKUP} resolution.
	Both resources must exist.
	@param resolution How the descrepancy should be resolved
	@param sourceRepository The source repository.
	@param sourceResourceDescription The description of the source resource.
	@param sourceContentModified The date and time at which the content of the source was modified, or <code>null</code> if not known.
	@param destinationRepository The destination repository.
	@param destinationResourceDescription The description of the destination resource.
	@param destinationContentModified The date and time at which the content of the destination was modified, or <code>null</code> if not known.
	@throws NullPointerException if any of the given arguments are <code>null</code>.
	@throws IOException if there is an I/O error while performing the action.
	*/
	protected void resolveContent(Resolution resolution, final Repository sourceRepository, final URFResource sourceResourceDescription, final Date sourceContentModified, final Repository destinationRepository, final URFResource destinationResourceDescription, final Date destinationContentModified) throws IOException
	{
		if(resolution==Resolution.IGNORE)	//if this situation should be ignored
		{
			return;	//don't do anything
		}
		if(resolution==Resolution.SYNCHRONIZE)	//if we should synchronize the resource content
		{
			if(sourceContentModified!=null)	//if there is a source date
			{
				if(destinationContentModified!=null)	//if we have dates for both resources
				{
					final int dateComparison=sourceContentModified.compareTo(destinationContentModified);	//compare the dates
					if(dateComparison>0)	//if the destination is older than the source
					{
						resolution=Resolution.BACKUP;	//copy the source to the destination
					}
					else if(dateComparison<0)	//if the source is older than the destination
					{
						resolution=Resolution.RESTORE;	//copy the destination to the source
					}
					else	//if the two resources have the same date
					{
						return;	//don't do anything
					}
				}
				else	//if we only have a source date
				{
					resolution=Resolution.BACKUP;	//assume the source is newer; back it up
				}
			}
			else	//if there is no source date
			{
				if(destinationContentModified!=null)	//if there is only a destination date
				{
					resolution=Resolution.RESTORE;	//assume the destination is newer; restore it
				}
				else	//if neither resource has a date
				{
					resolution=Resolution.BACKUP;	//bias toward backup
				}
			}
		}
		final Repository inputRepository, outputRepository;
		final URFResource inputResourceDescription, outputResourceDescription;
		switch(resolution)	//see how to resolve the descrepancy; at the point the only options we haven't covered are backup and restore
		{
			case BACKUP:
			case PRODUCE:
				inputRepository=sourceRepository;
				inputResourceDescription=sourceResourceDescription;
				outputRepository=destinationRepository;
				outputResourceDescription=destinationResourceDescription;
				break;
			case RESTORE:
			case CONSUME:
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
		if(!isTest())	//if this is not just a test
		{
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

	/**Resolves metadata descrepancies between a source and a destination resource.
	If the resource dates are the same, the {@link Resolution#SYNCHRONIZE} resolution will be considered the same as the {@link Resolution#BACKUP} resolution.
	If only one of the resource dates is available, it is considered newer for the purpose of the {@link Resolution#SYNCHRONIZE} resolution.
	If neither resource date is available, the {@link Resolution#SYNCHRONIZE} resolution will be considered the same as the {@link Resolution#BACKUP} resolution.
	The content modified date is ignored for zero-length collections for determining the correct {@link Resolution#SYNCHRONIZE} resolution.
	If the resource dates are not available or are the same, the {@link Resolution#SYNCHRONIZE} resolution will have no effect.
	This method ignores the {@link Content#MODIFIED_PROPERTY_URI} property.
	Both resources must exist.
	@param resolution How the descrepancy should be resolved
	@param sourceRepository The source repository.
	@param sourceResourceDescription The description of the source resource.
	@param sourceContentModified The date and time at which the content of the source was modified, or <code>null</code> if not known.
	@param destinationRepository The destination repository.
	@param destinationResourceDescription The description of the destination resource.
	@param destinationContentModified The date and time at which the content of the destination was modified, or <code>null</code> if not known.
	@throws NullPointerException if any of the given arguments are <code>null</code>.
	@throws IOException if there is an I/O error while performing the action.
	*/
	protected void resolveMetadata(Resolution resolution, final Repository sourceRepository, final URFResource sourceResourceDescription, Date sourceContentModified, final Repository destinationRepository, final URFResource destinationResourceDescription, Date destinationContentModified) throws IOException
	{
		if(resolution==Resolution.IGNORE)	//if this situation should be ignored
		{
			return;	//don't do anything
		}
		final boolean isCollection=isCollectionURI(sourceResourceDescription.getURI());
		final long sourceContentLength=getContentLength(sourceResourceDescription);
		final long destinationContentLength=getContentLength(destinationResourceDescription);
		if(isCollection)	//ignore content created and content modified for zero-length or unknown-length collections
		{
			if(sourceContentLength<=0)	
			{
				sourceContentModified=null;
			}
			if(destinationContentLength<=0)	
			{
				destinationContentModified=null;
			}
		}
		if(resolution==Resolution.SYNCHRONIZE)	//if we should synchronize the metadata
		{
			if(sourceContentModified!=null)	//if there is a source date
			{
				if(destinationContentModified!=null)	//if we have dates for both resources
				{
					final int dateComparison=sourceContentModified.compareTo(destinationContentModified);	//compare the dates
					if(dateComparison>0)	//if the destination is older than the source
					{
						resolution=Resolution.BACKUP;	//copy the source to the destination
					}
					else if(dateComparison<0)	//if the source is older than the destination
					{
						resolution=Resolution.RESTORE;	//copy the destination to the source
					}
					else	//if the two resources have the same date
					{
						resolution=Resolution.BACKUP;	//copy the source to the destination
					}
				}
				else	//if we only have a source date
				{
					resolution=Resolution.BACKUP;	//assume the source is newer; back it up
				}
			}
			else	//if there is no source date
			{
				if(destinationContentModified!=null)	//if there is only a destination date
				{
					resolution=Resolution.RESTORE;	//assume the destination is newer; restore it
				}
				else	//if neither resource has a date
				{
					resolution=Resolution.BACKUP;	//bias toward backup
				}
			}
		}
		final Repository inputRepository, outputRepository;
		final URFResource inputResourceDescription, outputResourceDescription;
		switch(resolution)	//see how to resolve the descrepancy; at the point the only options we haven't covered are backup and restore
		{
			case BACKUP:
			case PRODUCE:
				inputRepository=sourceRepository;
				inputResourceDescription=sourceResourceDescription;
				outputRepository=destinationRepository;
				outputResourceDescription=destinationResourceDescription;
				break;
			case RESTORE:
			case CONSUME:
				inputRepository=destinationRepository;
				inputResourceDescription=destinationResourceDescription;
				outputRepository=sourceRepository;
				outputResourceDescription=sourceResourceDescription;
				break;
			default:
				throw new AssertionError("Unrecognized resolution "+resolution);
		}
		final Set<URI> outputPropertyURIRemovals=new HashSet<URI>();
		final Set<URFProperty> outputPropertyAdditions=new HashSet<URFProperty>();
		for(final URFProperty inputProperty:inputResourceDescription.getProperties())	//copy input properties not present in the output
		{
			if(!outputResourceDescription.hasProperty(inputProperty))	//if this input property is not in the output
			{
				final URI inputPropertyURI=inputProperty.getPropertyURI();
				if(!ignorePropertyURIs.contains(inputPropertyURI) && !inputRepository.isLivePropertyURI(inputPropertyURI))	//ignore specified properties and live properties
				{
					if(Content.MODIFIED_PROPERTY_URI.equals(inputPropertyURI))	//content modified only gets updated when we update content
					{
						continue;
					}
					if(isCollection && (sourceContentLength<=0 || destinationContentLength<=0))	//ignore content created for zero-length or unknown-length collections
					{
						if(Content.CREATED_PROPERTY_URI.equals(inputPropertyURI))
						{
							continue;
						}
					}
					if(resolution!=Resolution.IGNORE)
					{
						Debug.info(getTestStatus(), "Resolve metadata:", resolution, "set property", outputResourceDescription.getURI(), inputProperty);
						outputPropertyURIRemovals.add(inputPropertyURI);	//we'll replace all of these properties in the output
						outputPropertyAdditions.add(inputProperty);	//we'll add this new property and value to the output
					}
				}
			}
		}
		if(resolution!=Resolution.PRODUCE && resolution!=Resolution.CONSUME)	//produce and consume do not result in removal of information
		{
			for(final URFProperty outputProperty:outputResourceDescription.getProperties())	//remove output properties not present in the input
			{
				final URI outputPropertyURI=outputProperty.getPropertyURI();
				if(!inputResourceDescription.hasProperty(outputPropertyURI))	//if this output property URI is not in the input with any value (if it has some value at all, we've already made to make them the same)
				{
					if(!ignorePropertyURIs.contains(outputPropertyURI) && !outputRepository.isLivePropertyURI(outputPropertyURI))	//ignore specified properties and live properties
					{
						if(Content.MODIFIED_PROPERTY_URI.equals(outputPropertyURI))	//content modified only gets updated when we update content
						{
							continue;
						}
						if(isCollection && (sourceContentLength<=0 || destinationContentLength<=0))	//ignore content created for zero-length or unknown-length collections
						{
							if(Content.CREATED_PROPERTY_URI.equals(outputPropertyURI))
							{
								continue;
							}
						}
						if(resolution!=Resolution.IGNORE)
						{
							Debug.info(getTestStatus(), "Resolve metadata:", resolution, "remove property", outputResourceDescription.getURI(), outputProperty.getPropertyURI());
							outputPropertyURIRemovals.add(outputPropertyURI);	//we'll remove all of these properties in the output; if there were any replacements they will have already been added 
						}
					}
				}
			}
		}
		if(!outputPropertyURIRemovals.isEmpty() || !outputPropertyAdditions.isEmpty())	//if we have something to change
		{
			final URFResourceAlteration outputResourceAlteration=new DefaultURFResourceAlteration(outputPropertyURIRemovals, outputPropertyAdditions);
			if(!isTest())	//if this is not just a test
			{
				outputRepository.alterResourceProperties(outputResourceDescription.getURI(), outputResourceAlteration);	//alter the output resource properties
			}
		}
	}

	/**@return A status string indicating whether synchronization is in test mode.*/
	protected String getTestStatus()
	{
		return isTest() ? "(test)" : "*";
	}

}

package net.marmox;

import java.io.*;
import java.net.URI;
import java.util.Date;

import static com.garretwilson.io.Files.*;
import com.garretwilson.net.ResourceNotFoundException;

import static com.garretwilson.lang.ObjectUtilities.checkInstance;
import static com.garretwilson.net.URIs.*;
import com.garretwilson.urf.*;
import static com.garretwilson.urf.content.Content.*;
import com.garretwilson.util.AbstractCache;
import com.garretwilson.util.AbstractHashObject;

import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.ResourceFilter;

import com.guiseframework.Guise;

import net.marmox.resource.MarmoxResourceKit;

/**The Marmox cache of resources.
@author Garret Wilson
*/
public class MarmoxResourceCache extends AbstractCache<MarmoxResourceCache.ResourceCacheKey, File, MarmoxResourceCache.CachedResourceInfo>
{
	
	/**Default constructor.*/
	public MarmoxResourceCache()
	{
		super(true, 90L*24L*60L*60L*1000L);	//keep cached info for 90 days
	}

	/**Determines if a given cached value is stale.
	This version checks to see if the last modified time of the resource has changed.
	@param key The key for the cached information.
	@param cachedInfo The information that is cached.
	@return <code>true</code> if the cached information has become stale.
	@exception IOException if there was an error checking the cached information for staleness.
	*/
	public boolean isStale(final ResourceCacheKey key, final CachedResourceInfo cachedInfo) throws IOException
	{
		if(super.isStale(key, cachedInfo))	//if the default stale checks think the information is stale
		{
			return true;	//the information is stale
		}
		final MarmoxSession marmoxSession=(MarmoxSession)Guise.getInstance().getGuiseSession();	//get the current session
		final MarmoxApplication marmoxApplication=marmoxSession.getApplication();	//get the application
		final URI userURI=key.getUserURI();	//get the URI of the user
		final User user=marmoxApplication.getUser(userURI);	//get the user
		if(user==null)	//if there is no such user
		{
			throw new ResourceNotFoundException(userURI);
		}
		final Repository repository=user.getRepository();	//get the repository
		if(repository.getPublicRepositoryURI()!=key.getRepositoryURI())	//if this is not the appropriate repository
		{
			throw new IOException("Repository URI "+repository.getPublicRepositoryURI()+" did not match expected URI "+key.getRepositoryURI());
		}
		final URI resourceURI=key.getResourceURI();	//get the resource URI				
		final URFResource resource=repository.getResourceDescription(resourceURI);	//get a description of the resource
		final Date cachedModifiedTime=cachedInfo.getModifiedTime();	//get the cached modified time of the resource
		if(cachedModifiedTime!=null)	//if we know the modified time of the cached resource
		{
			final URFDateTime modifiedDateTime=getModified(resource);	//get the current modified date time of the resource
			return !cachedModifiedTime.equals(modifiedDateTime);	//if the modified time doesn't match our record, the cache is stale
		}
		return false;	//we couldn't find a reason that the cached information is stale 
	}

	/**Fetches data from the backing store.
	@param key The key describing the value to fetch.
	@return New information to cache.
	@exception IOException if there was an error fetching the value from the backing store.
	*/
	public CachedResourceInfo fetch(final ResourceCacheKey key) throws IOException
	{
		final MarmoxSession marmoxSession=(MarmoxSession)Guise.getInstance().getGuiseSession();	//get the current session
		final MarmoxApplication marmoxApplication=marmoxSession.getApplication();	//get the application
		final URI userURI=key.getUserURI();	//get the URI of the user
		final User user=marmoxApplication.getUser(userURI);	//get the user
		if(user==null)	//if there is no such user
		{
			throw new ResourceNotFoundException(userURI);
		}
		final Repository repository=user.getRepository();	//get the repository
		if(repository.getPublicRepositoryURI()!=key.getRepositoryURI())	//if this is not the appropriate repository
		{
			throw new IOException("Repository URI "+repository.getPublicRepositoryURI()+" did not match excected URI "+key.getRepositoryURI());
		}
		final URI resourceURI=key.getResourceURI();	//get the resource URI				
		URFResource resource=repository.getResourceDescription(resourceURI);	//get a description of the resource
		final URFDateTime modifiedDateTime=getModified(resource);	//get the last modified time of the resource before it is filtered
		final MarmotSession<MarmoxResourceKit> marmotSession=marmoxSession.getMarmotSession();	//get the Marmot session
		final MarmoxResourceKit resourceKit=marmotSession.getResourceKit(repository, resource);	//get the resource kit for this resource
		final String filename=getRawName(resourceURI);	//get the filename of the resource
		final String baseName=removeNameExtension(filename);	//get the base name to use	TODO important encode the name so that it can work on the file system
		final String extension=getNameExtension(filename);	//get the extension to use
		final File tempDirectory=marmoxApplication.getTempDirectory();	//get the temporary directory
		File file=createTempFile(baseName, extension, tempDirectory, true);	//create a temporary file that will be deleted on exit
		final InputStream inputStream=new BufferedInputStream(repository.getResourceInputStream(resourceURI));	//get a stream to the resource
		try
		{
			copy(inputStream, file);	//copy the resource to the file
		}
		finally
		{
			inputStream.close();	//always close the stream to the resource
		}
			//TODO cache the original file as well
		final String aspectID=key.getAspectID();	//get the aspect ID
		if(aspectID!=null)	//if there is an aspect
		{
			final ResourceFilter[] filters=resourceKit.getAspectFilters(aspectID);	//get the array of filters for this aspect
			for(final ResourceFilter filter:filters)	//for each filter
			{
				final File filterFile=createTempFile(baseName, extension, tempDirectory, true);	//create a new temporary file that will be deleted on exit
				final InputStream filterInputStream=new BufferedInputStream(new FileInputStream(file));	//create an input stream to the file
				try
				{
					final OutputStream filterOutputStream=new BufferedOutputStream(new FileOutputStream(filterFile));	//create an output stream to the new file
					try
					{
						resource=filter.filter(resource, filterInputStream, filterOutputStream);	//apply this filter
					}
					finally
					{
						filterOutputStream.close();	//always close the filter output stream
					}
				}
				finally
				{
					filterInputStream.close();	//always close the filter input stream
				}
				file.delete();	//delete the old file TODO don't delete if we cached it
				file=filterFile;	//switch to the new filtered file
			}
		}
		return new CachedResourceInfo(file, modifiedDateTime);	//return the file, which may have been filtered
	}

	/**Performs any operations that need to be done when cached information is discarded (for example, if the cached information is stale).
	This version deletes the file used for caching.
	@param key The key for the cached information.
	@param cachedInfo The information that is cached.
	@exception IOException if there was an error discarding the cached information.
	*/
/*TODO fix, maybe; but how will we know whether the file can be deleted? maybe it's still being used 
	public void discard(final ResourceCacheKey key, final CachedResourceInfo cachedInfo) throws IOException
	{
		super.discard(key, cachedInfo);	//do the default discarding
	}
*/

	/**A key for cached resources.
	@author Garret Wilson
	*/
	public static class ResourceCacheKey extends AbstractHashObject
	{

		/**The URI of the user who owns the resource.*/
		private final URI userURI;

			/**@return The URI of the user who owns the resource.*/
			public URI getUserURI() {return userURI;}

		/**The URI of the repository in which the resource is located.*/
		private final URI repositoryURI;

			/**@return The URI of the repository in which the resource is located.*/
			public URI getRepositoryURI() {return repositoryURI;}

		/**The URI of the resource.*/
		private final URI resourceURI;

			/**@return The URI of the resource.*/
			public URI getResourceURI() {return resourceURI;}

		/**The aspect of the resource, or <code>null</code> if the original resource is being cached.*/
		private final String aspectID;

			/**@return The aspect of the resource, or <code>null</code> if the original resource is being cached.*/
			public String getAspectID() {return aspectID;}

		/**User, repository, resource URI, and aspect ID constructor.
		@param user The user who owns the resource.
		@param repository The repository in which the resource is stored.
		@param resourceURI The URI of the resource.
		@param aspectID The aspect of the resource, or <code>null</code> if the original resource is being cached.
		@exception NullPointerException if the given user, repository, and/or resource URI is <code>null</code>.
		@exception IllegalArgumentException if the given user has no username and/or URI.
		*/
		public ResourceCacheKey(final User user, final Repository repository, final URI resourceURI, final String aspectID)
		{
			super(user.getURI(), repository.getURI(), checkInstance(resourceURI, "Resource URI cannot be null."), aspectID);
			user.checkID();	//make sure the given user has a reference URI
			this.userURI=user.getURI();	//get the reference URI of the user
			this.repositoryURI=repository.getURI();	//TODO ensure not null
			this.resourceURI=resourceURI;	//save the resource URI
			this.aspectID=aspectID;
		}		
	}

	/**Class for storing cached resource file information.
	If no modified time is known, this will not influence the staleness determination of cached information.
	@author Garret Wilson
	*/
	public static class CachedResourceInfo extends AbstractCache.CachedInfo<File>
	{

		/**The last known modified time of the resource represented, or <code>null</code> if the last modified time is not known.*/
		private final Date modifiedTime;

			/**@return The last known modified time of the resource represented, or <code>null</code> if the last modified time is not known.*/
			public Date getModifiedTime() {return modifiedTime;}

		/**File constructor.
		@param file The file to store.
		@param modifiedTime The last known modified time of the resource represented, or <code>null</code> if the last modified time is not known.
		*/
		public CachedResourceInfo(final File value, final Date modifiedTime)
		{
			super(value);	//construct the parent class
			this.modifiedTime=modifiedTime;
		}
	}

}

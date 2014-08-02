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

package com.globalmentor.marmot.repository.archive;

import static com.globalmentor.java.Objects.checkInstance;

import java.io.*;
import java.net.URI;

import org.urframework.*;
import org.urframework.io.URFIO;
import org.urframework.io.URFResourceTURFIO;

import com.globalmentor.cache.Cache;
import com.globalmentor.config.ConfigurationException;
import com.globalmentor.marmot.Marmot;
import com.globalmentor.marmot.MarmotResourceCache;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.net.URIPath;

import static com.globalmentor.net.URIs.*;


/**Abstract implementation of a repository backed by an archive resource.
@param <A> The type of object representing a source archive.
@author Garret Wilson
*/
public abstract class AbstractArchiveRepository<A> extends AbstractReadOnlyRepository
{

	/**The repository containing the resource that is the source of this repository's information.*/
	private Repository sourceRepository=null;

		/**Returns the repository containing the resource that is the source of this repository's information.
		If there is no source repository, the root repository will be used.
		@return The repository containing the resource that is the source of this repository's information,
		or <code>null</code> if the root repository should be used.
		*/
		public Repository getSourceRepository() {return sourceRepository;}

		/**Sets the repository containing the resource that is the source of this repository's information.
		@param sourceRepository The repository containing the source resource, or <code>null</code> if the
		root repository should be used.
		@throws IllegalArgumentException if the given repository is this repository.
		*/
		public void setSourceRepository(final Repository sourceRepository)
		{
			if(sourceRepository==this)	//we can't be our own source
			{
				throw new IllegalArgumentException("Repository cannot be its own source.");
			}
			this.sourceRepository=checkInstance(sourceRepository, "Source repository cannot be null.");
		}

		/**Determines the source repository for accessing the source archive.
		If there is no source repository specified, the root repository is returned.
		@return The source repository for accessing the source archive.
		@throws ConfigurationException if no explicit repository is set and this repository is the root repository.
		@see #getSourceRepository()
		@see #getRootRepository()
		*/
		protected Repository determineSourceRepository() throws ConfigurationException
		{
			Repository sourceRepository=getSourceRepository();	//get the specified source repository
			if(sourceRepository==null)	//if no source repository is specified
			{
				sourceRepository=getRootRepository();	//use the root repository
				if(sourceRepository==this)	//if this repository is the root repository
				{
					throw new ConfigurationException("No source repository specified, and repository has no parent repository to serve as its source.");
				}
			}
			return sourceRepository;
		}

	/**The URI of the resource in another repository indicating the source of this repository's information.*/
	private URI sourceResourceURI=null;

		/**@return The URI of the resource in another repository indicating the source of this repository's information.*/
		public URI getSourceResourceURI() {return sourceResourceURI;}

		/**Sets the source resource of this repository's information in another repository.
		@param sourceResourceURI The source resource for this repository.
		@throws NullPointerException if the given resource URI is <code>null</code>.
		@throws IllegalArgumentException if the given source resource URI is not absolute or is a collection URI.
		*/
		public void setSourceResourceURI(final URI sourceResourceURI)
		{
			this.sourceResourceURI=checkNotCollectionURI(checkAbsolute(normalize(checkInstance(sourceResourceURI, "Source resource URI must not be null."))));
		}

	/**Default constructor with no root URI defined.
	The root URI must be defined before the repository is opened.
	*/
	public AbstractArchiveRepository()
	{
		this(null);
	}

	/**URI constructor with no separate private URI namespace.
	@param rootURI The URI identifying the location of this repository.
	*/
	public AbstractArchiveRepository(final URI rootURI)
	{
		this(rootURI, rootURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI constructor.
	A {@link URFResourceTURFIO} description I/O is created and initialized.
	@param rootURI The URI identifying the location of this repository.
	@param sourceResourceURI The URI identifying the resource from which this resource gets its information.
	@throws IllegalArgumentException if the given source resource URI is not absolute or is a collection URI.
	*/
	public AbstractArchiveRepository(final URI rootURI, final URI sourceResourceURI)
	{
		this(rootURI, sourceResourceURI, createDefaultURFResourceDescriptionIO());	//create a default resource description I/O using TURF
	}

	/**Root URI description I/O constructor.
	@param rootURI The URI identifying the location of this repository.
	@param sourceResourceURI The URI identifying the private namespace managed by this repository.
	@param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	@throws NullPointerException the given description I/O is <code>null</code>.
	@throws IllegalArgumentException if the given source resource URI is not absolute or is a collection URI.
	*/
	public AbstractArchiveRepository(final URI rootURI, final URI sourceResourceURI, final URFIO<URFResource> descriptionIO)
	{
		super(rootURI, descriptionIO);
		this.sourceResourceURI=sourceResourceURI!=null ? checkNotCollectionURI(checkAbsolute(normalize(sourceResourceURI))) : null;
	}

	/**Creates a repository of the same type as this repository with the same access privileges as this one.
	This factory method is commonly used to use a parent repository as a factory for other repositories in its namespace.
	This method resolves the private repository path to the current public repository URI.
	@param subrepositoryPath The private path relative to the private URI of this repository.
	@throws NullPointerException if the given private repository path is <code>null</code>.
	@throws IllegalArgumentException if the given subrepository path is absolute and/or is not a collection.
	*/
	public final Repository createSubrepository(final URIPath subrepositoryPath)
	{
		throw new UnsupportedOperationException("Archive repositories don't allow automatic creation of subrepositories.");		
	}
	
	/**Creates a repository of the same type as this repository with the same access privileges as this one.
	This factory method is commonly used to use a parent repository as a factory for other repositories in its namespace.
	@param publicRepositoryURI The public URI identifying the location of the new repository.
	@param privateSubrepositoryPath The private path relative to the private URI of this repository.
	@throws NullPointerException if the given public repository URI and/or private repository path is <code>null</code>.
	@throws IllegalArgumentException if the given private repository path is absolute and/or is not a collection.
	*/
	public final Repository createSubrepository(final URI publicRepositoryURI, final URIPath privateSubrepositoryPath)
	{
		throw new UnsupportedOperationException("Archive repositories don't allow automatic creation of subrepositories.");		
	}

	/**The last source archive file retrieved from the cache, or <code>null</code> if the source archive file has not been retrieved from the cache.*/
	private Cache.Data<File> sourceArchiveFileData=null;

	/**The current source archive, or <code>null</code> if the source archive has not yet been retrieved.*/
	private A sourceArchive=null;
	
	/**Returns the object for accessing the source archive information.
	@return The source archive.
	@throws IOException if there is an error retrieving the source archive.
	@see #determineSourceRepository()
	*/
	protected A getSourceArchive() throws IOException
	{
		final MarmotResourceCache<?> marmotCache=Marmot.getResourceCache();
		Cache.Data<File> sourceArchiveFileData=marmotCache.getData(determineSourceRepository(), getSourceResourceURI());	//retrieve the archive file data, using a cached version if possible
		if(sourceArchiveFileData!=this.sourceArchiveFileData || sourceArchive==null)	//if we have new file data from the cache (or we've never created a source archive), we need to update the actual archive
		{
			sourceArchive=createSourceArchive(sourceArchiveFileData.getValue());	//create a new source archive from the file
			this.sourceArchiveFileData=sourceArchiveFileData;	//update the data about the latest cache information 
		}
		return sourceArchive;	//return the latest source archive; a benign race condition here could actually give us an even more recent source archive, but as we never delete the source archive, never null
	}

	/**Creates an object to represent the source archive from the given source archive file.
	The returned archive will be opened and ready for use.
	@param sourceArchiveFile The cached file of the source archive.
	@throws IOException if there is an error creating the source archive.
	@return A new source archive for the file.
	*/
	protected abstract A createSourceArchive(final File sourceArchiveFile) throws IOException;

}

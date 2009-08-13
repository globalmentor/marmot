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

import java.io.*;
import java.net.URI;

import com.globalmentor.cache.Cache;
import com.globalmentor.marmot.Marmot;
import com.globalmentor.marmot.MarmotResourceCache;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.marmot.security.MarmotSecurity;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.Content;

/**Abstract implementation of a repository backed by an archive resource.
@param <A> The type objct representing a source archive.
@author Garret Wilson
*/
public abstract class AbstractArchiveRepository<A> extends AbstractReadOnlyRepository
{
	
	/**URI constructor with no separate private URI namespace.
	@param repositoryURI The URI identifying the location of this repository.
	@exception NullPointerException if the given respository URI is <code>null</code>.
	*/
	public AbstractArchiveRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI constructor.
	A {@link URFResourceTURFIO} description I/O is created and initialized.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@exception NullPointerException if one of the given respository URIs is <code>null</code>.
	*/
	public AbstractArchiveRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		this(publicRepositoryURI, privateRepositoryURI, new URFResourceTURFIO<URFResource>(URFResource.class, URI.create("")));	//create a default resource description I/O using TURF
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO=(URFResourceTURFIO<URFResource>)getDescriptionIO();	//get the description I/O we created
		urfResourceDescriptionIO.addNamespaceURI(MarmotSecurity.MARMOT_SECURITY_NAMESPACE_URI);	//tell the I/O about the security namespace
	}

	/**Public repository URI and private repository URI constructor.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	@exception NullPointerException if one of the given respository URIs and/or the description I/O is <code>null</code>.
	*/
	public AbstractArchiveRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final URFIO<URFResource> descriptionIO)
	{
		super(publicRepositoryURI, privateRepositoryURI, descriptionIO);	//construct the parent class
	}

	/**Determines the source repository for accessing the source archive.
	This implementation returns the root repository.
	@return The source repository for accessing the source archive.
	*/
	protected Repository getSourceRepository()	//TODO allow customization
	{
		return getRootRepository();
	}

	/**The last source archive file retrieved from the cache, or <code>null</code> if the source archive file has not been retrieved from the cache.*/
	private Cache.Data<File> sourceArchiveFileData=null;

	/**The current source archive, or <code>null</code> if the source archive has not yet been retrieved.*/
	private A sourceArchive=null;
	
	/**Returns the object for accessing the source archive information.
	@return The source archive.
	@throws IOException if there is an error retrieving the source archive.
	@see #getSourceRepository()
	*/
	protected A getSourceArchive() throws IOException
	{
		final MarmotResourceCache<?> marmotCache=Marmot.getConfiguration().getResourceCache();
		//TODO change to using a sourceArchiveURI instead of a privateRepositoryURI; create a subclass of AbstractRepository for repositories that have parallel private URI hierarchies
		Cache.Data<File> sourceArchiveFileData=marmotCache.getData(getSourceRepository(), getPrivateRepositoryURI());	//retrieve the archive file data, using a cached version if possible TODO rename getPrivateRepositoryURI() to getSourceURI()
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

/*
 * Copyright © 2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.svn.svnkit;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.*;
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.tmatesoft.svn.core.SVNProperty.*;

import static com.globalmentor.io.Files.*;
import static com.globalmentor.io.InputStreams.*;
import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;

import com.globalmentor.collections.CollectionMap;
import com.globalmentor.collections.HashSetHashMap;
import com.globalmentor.event.ProgressListener;
import com.globalmentor.io.*;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.model.NameValuePair;
import com.globalmentor.net.*;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.Content;

/**
 * Subversion repository implemented by SVNKit.
 * 
 * <p>
 * This implementation supports FSFS, Subversion, WebDAV based repositories.
 * </p>
 * 
 * <p>
 * This implementation stores explicit {@link Content#CREATED_PROPERTY_URI} and {@link Content#MODIFIED_PROPERTY_URI} properties for all resources, but the
 * resource live last modified timestamp will override the given content modified date when retrieving properties. For collections, the
 * {@link Content#MODIFIED_PROPERTY_URI} property is stored on the actual collection resource, but the retrieved live content modified date is retrieved from
 * the content resource, if any.
 * </p>
 * @author Garret Wilson
 */
public class SVNKitSubversionRepository extends AbstractHierarchicalSourceRepository
{

	static
	//set up the types of Subversion repositories we support
	{
		DAVRepositoryFactory.setup();
		FSRepositoryFactory.setup();
		SVNRepositoryFactoryImpl.setup();
	}

	/**
	 * Default constructor with no root URI defined. The root URI must be defined before the repository is opened.
	 */
	public SVNKitSubversionRepository()
	{
		this((URI)null);
	}

	/**
	 * File constructor with no separate private URI namespace.
	 * @param repositoryDirectory The file identifying the directory of this repository.
	 * @throws NullPointerException if the given repository directory is <code>null</code>.
	 */
	public SVNKitSubversionRepository(final File repositoryDirectory)
	{
		this(toURI(repositoryDirectory, true)); //get a directory URI from the repository directory and use it as the base repository URI
	}

	/**
	 * URI constructor with no separate private URI namespace. The given repository URI should end in a slash.
	 * @param repositoryURI The URI identifying the location of this repository.
	 * @throws NullPointerException if the given repository URI is <code>null</code>.
	 * @throws IllegalArgumentException if the repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	 */
	public SVNKitSubversionRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI); //use the same repository URI as the public and private namespaces
	}

	/**
	 * Public repository URI and private repository directory constructor.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryDirectory The file identifying the private directory of the repository.
	 * @throws NullPointerException if the given repository URI and/or the given directory is <code>null</code>.
	 */
	public SVNKitSubversionRepository(final URI publicRepositoryURI, final File privateRepositoryDirectory)
	{
		this(publicRepositoryURI, privateRepositoryDirectory != null ? toURI(privateRepositoryDirectory, true) : null); //get a directory URI from the private repository directory and use it as the base repository URI
	}

	/**
	 * Public repository URI and private repository URI constructor. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	 * @throws NullPointerException if one of the given repository URIs is <code>null</code>.
	 */
	public SVNKitSubversionRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		super(publicRepositoryURI, privateRepositoryURI); //construct the parent class
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO = (URFResourceTURFIO<URFResource>)getDescriptionIO(); //get the description I/O
		urfResourceDescriptionIO.setBOMWritten(false); //turn off BOM generation
		urfResourceDescriptionIO.setFormatted(false); //turn off formatting
	}

	/**
	 * Creates a repository of the same type as this repository with the same access privileges as this one. This factory method is commonly used to use a parent
	 * repository as a factory for other repositories in its namespace.
	 * @param publicRepositoryURI The public URI identifying the location of the new repository.
	 * @param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	 * @throws NullPointerException if the given public repository URI and/or private repository URI is <code>null</code>.
	 */
	protected Repository createSubrepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		return new SVNKitSubversionRepository(publicRepositoryURI, privateRepositoryURI); //create and return a new file repository
	}

	/** The username to use in accessing the repository, or <code>null</code> if no username is specified. */
	private String username = null;

	/** @return The username to use in accessing the repository, or <code>null</code> if no username is specified. */
	public String getUsername()
	{
		return username;
	}

	/**
	 * Sets the username to use in accessing the repository.
	 * @param username The username to use in accessing the repository, or <code>null</code> if no username is specified.
	 */
	public void setUsername(final String username)
	{
		this.username = username;
	}

	/** The password to use in accessing the repository, or <code>null</code> if no password is specified. */
	private char[] password = null;

	/** @return The username to use in accessing the repository, or <code>null</code> if no password is specified. */
	public char[] getPassword()
	{
		return password;
	}

	/**
	 * Sets the password to use in accessing the repository.
	 * @param password The password to use in accessing the repository, or <code>null</code> if no password is specified.
	 */
	public void setPassword(final char[] password)
	{
		this.password = password;
	}

	/** The SVNKit Subversion repository, or <code>null</code> if the repository is closed. */
	private SVNRepository svnRepository = null;

	/** @return The SVNKit Subversion repository, or <code>null</code> if the repository is closed. */
	protected SVNRepository getSVNRepository()
	{
		return svnRepository;
	}

	/** {@inheritDoc} This version connects to the SVNKit repository. */
	@Override
	public void openImpl() throws ResourceIOException
	{
		super.openImpl();
		try
		{
			final URI sourceURI = getSourceURI();
			final SVNURL svnURL;
			if(FILE_SCHEME.equals(sourceURI.getScheme())) //if this is a file URI, create the form that SVNKit likes, which is file:///C:/etc (Java gives file:/C:/etc
			{
				svnURL = SVNURL.fromFile(new File(sourceURI)); //convert the URI back to a file and create the SVNURL the way SVNKit likes it
			}
			else
			//all other URIs should be in the correct format already
			{
				svnURL = SVNURL.parseURIEncoded(sourceURI.toASCIIString());
			}

			svnRepository = SVNRepositoryFactory.create(svnURL); //create a new SVNKit repository
			synchronized(svnRepository)
			{
				final ISVNAuthenticationManager authenticationManager;
				final String username = getUsername();
				if(username != null) //if a username is given
				{
					final char[] password = getPassword();
					authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(username, password != null ? new String(password) : ""); //create a default username/password authentication manager
				}
				else
				//if no username is given
				{
					authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(); //create a default authentication manager with the default authentication configured on the system for Subversion
				}
				svnRepository.setAuthenticationManager(authenticationManager); //set the repository's authentication manager
			}
		}
		catch(final SVNException svnException)
		{
			throw toResourceIOException(getSourceURI(), svnException);
		}
	}

	/** {@inheritDoc} This implementation returns <code>false</code> for all resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected boolean resourceExistsImpl(URI resourceURI) throws ResourceIOException
	{
		final URI privateResourceURI = getSourceResourceURI(resourceURI); //get the resource URI in the private space
		if(!isSourceResourceVisible(privateResourceURI)) //if this resource should not be public
		{
			return false; //ignore this resource
		}
		final boolean isCollectionURI = isCollectionURI(resourceURI); //see if the URI specifies a collection
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		try
		{
			final SVNNodeKind svnNodeKind;
			final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
			synchronized(svnRepository)
			{
				svnNodeKind = svnRepository.checkPath(resourceURIPath.toString(), -1); //see what kind of resource this is
			}
			final boolean isDirectory = svnNodeKind == SVNNodeKind.DIR; //see if this is a Subversion directory
			return svnNodeKind != SVNNodeKind.NONE && isCollectionURI == isDirectory; //see if Subversion resource exists; make sure its type corresponds with what we expect according to the URI (i.e. collection or not)
		}
		catch(final SVNException svnException)
		{
			throw toResourceIOException(resourceURI, svnException);
		}
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource getResourceDescriptionImpl(final URI resourceURI) throws ResourceIOException
	{
		final URF urf = createURF(); //create a new URF data model
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		try
		{
			final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
			synchronized(svnRepository)
			{
				final SVNDirEntry dirEntry = svnRepository.info(resourceURIPath.toString(), -1); //get the directory entry for this resource
				if(dirEntry.getKind() == SVNNodeKind.NONE) //make sure we have a resource at this URI
				{
					throw new ResourceNotFoundException(resourceURI);
				}
				return createResourceDescription(urf, resourceURI, dirEntry); //create and return a description of the resource
			}
		}
		catch(final SVNException svnException)
		{
			throw toResourceIOException(resourceURI, svnException);
		}
	}

	/** {@inheritDoc} For collections, this implementation retrieves the content of the {@value #COLLECTION_CONTENT_NAME} file, if any. */
	@Override
	protected InputStream getResourceInputStreamImpl(final URI resourceURI) throws ResourceIOException
	{
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			try
			{
				final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
				final URIPath contentURIPath; //we'll determine the URI path to use for content
				final boolean isCollection = isCollectionURI(resourceURI);
				if(isCollection) //if the resource is a collection
				{
					contentURIPath = resourceURIPath.resolve(COLLECTION_CONTENT_NAME); //the URI path to use for content uses the special collection content resource
				}
				else
				//if the resource is not a collection
				{
					contentURIPath = resourceURIPath; //we'll get the content from the file itself
				}
				final SVNNodeKind contentNodeKind = svnRepository.checkPath(contentURIPath.toString(), -1); //see what kind of resource this is
				if(contentNodeKind == SVNNodeKind.NONE) //if there is no such file TODO check to see if this is a directory, and throw the appropriate error
				{
					if(isCollection) //if we're looking for collection content, this is not a problem---the collection simply has no content
					{
						return new ByteArrayInputStream(NO_BYTES); //return an input stream to an empty byte array TODO create an EmptyInputStream class
					}
					else
					//for non-collections
					{
						throw new ResourceNotFoundException(resourceURI);
					}
				}
				final TempOutputStream tempOutputStream = new TempOutputStream(false); //create a temporary output stream that won't automatically delete its contents when closed
				svnRepository.getFile(contentURIPath.toString(), -1, null, tempOutputStream); //retrieve the contents of the content file; if SVNKit closes the output stream, it won't matter, because we turned off auto-dispose
				try
				{
					return tempOutputStream.getInputStream(); //return an input stream to the data; the returned input stream will delete the temporary file, if any
				}
				catch(final IOException ioException)
				{
					tempOutputStream.dispose(); //dispose of our output stream
					throw toResourceIOException(resourceURI, ioException);
				}
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
	}

	/** {@inheritDoc} For collections, this implementation stores the content in the {@value #COLLECTION_CONTENT_NAME} file. */
	@Override
	protected OutputStream getResourceOutputStreamImpl(final URI resourceURI, final URFDateTime newContentModified) throws ResourceIOException
	{
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			try
			{
				final SVNDirEntry dirEntry = svnRepository.info(resourceURIPath.toString(), -1); //get the directory entry for this resource
				if(dirEntry.getKind() == SVNNodeKind.NONE) //make sure we have a resource at this URI
				{
					throw new ResourceNotFoundException(resourceURI);
				}
				final TempOutputStream tempOutputStream = new TempOutputStream() //create a new temporary output stream that, before it is closed, will save the collected bytes to the existing resource
				{
					@Override
					protected void beforeClose() throws IOException //when the output stream is ready to be closed
					{
						super.beforeClose();
						final InputStream inputStream = toMarkSupportedInputStream(getInputStream()); //get an input stream to the data, making sure it supports mark/reset
						try
						{
							setResourceContents(resourceURI, null, dirEntry, inputStream); //set resource contents from the resource input stream
						}
						finally
						//always make sure the input stream is closed; this is especially important if we were using a temporary file
						{
							inputStream.close();
						}
					}
				};
				return tempOutputStream; //return the temporary output stream we created
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
	}

	/** {@inheritDoc} This implementation ignores child resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected boolean hasChildrenImpl(final URI resourceURI) throws ResourceIOException
	{
		if(!isCollectionURI(resourceURI)) //only collection can have children 
		{
			return false;
		}
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			try
			{
				if(svnRepository.checkPath(resourceURIPath.toString(), -1) != SVNNodeKind.DIR) //only Subversion directories can have children
				{
					return false;
				}
				return !svnRepository.getDir(resourceURIPath.toString(), -1, null, (Collection<?>)null).isEmpty(); //see if there is at least one child
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
	}

	/** {@inheritDoc} This implementation does not include child resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	public List<URFResource> getChildResourceDescriptionsImpl(final URI resourceURI, final ResourceFilter resourceFilter, final int depth)
			throws ResourceIOException
	{
		if(depth == 0 || !isCollectionURI(resourceURI)) //a depth of zero means don't get child resources; likewise, non-collections can't have children
		{
			return emptyList(); //return an empty list
		}
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		try
		{
			final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
			final Collection<SVNDirEntry> dirEntries;
			synchronized(svnRepository) //we aren't locking the actual repository, so we might as well synchronize our access to it at a local level rather than holding it across the children iteration
			{
				if(svnRepository.checkPath(resourceURIPath.toString(), -1) != SVNNodeKind.DIR) //only Subversion directories can have children
				{
					return emptyList();
				}
				@SuppressWarnings("unchecked")
				final Collection<SVNDirEntry> childDirEntries = svnRepository.getDir(resourceURIPath.toString(), -1, null, (Collection<?>)null); //get a collection of child directory entries
				dirEntries = childDirEntries; //save the directory entries; distinct variables are used solely to suppress the unchecked cast warning at a smaller granularity
			}
			final URF urf = createURF(); //create a new URF data model
			final List<URFResource> childResources = new ArrayList<URFResource>();
			for(final SVNDirEntry dirEntry : dirEntries) //for each of the child resource directory entries
			{
				final URI childResourceURI = resourceURI.resolve(dirEntry.getRelativePath()); //determine the full URI
				if(childResourceURI.equals(resourceURI)) //ignore the resource itself
				{
					continue;
				}
				if(getSubrepository(childResourceURI) == this) //if this child wouldn't be located in a subrepository (i.e. ignore resources obscured by subrepositories)
				{
					if(resourceFilter == null || resourceFilter.isPass(childResourceURI)) //if we should include this resource based upon its URI
					{
						final URFResource childResourceDescription = createResourceDescription(urf, childResourceURI, dirEntry); //create a resource from this URI and directory entry
						if(resourceFilter == null || resourceFilter.isPass(childResourceDescription)) //if we should include this resource based upon its description
						{
							childResources.add(childResourceDescription); //add this child resource description to our list
							if(depth != 0 && dirEntry.getKind() == SVNNodeKind.DIR) //if this child is a directory and we haven't reached the bottom
							{
								final int newDepth = depth != INFINITE_DEPTH ? depth - 1 : depth; //reduce the depth by one, unless we're using the unlimited depth value
								childResources.addAll(getChildResourceDescriptions(childResourceURI, resourceFilter, newDepth)); //get a list of child descriptions for the resource we just created and add them to the list
							}
						}
					}
				}
			}
			//aggregate any mapped subrepositories
			for(final Repository childSubrepository : getChildSubrepositories(resourceURI)) //see if any subrepositories are mapped as children of this repository
			{
				final URI childSubrepositoryURI = childSubrepository.getRootURI(); //get the URI of the subrepository
				childResources.add(childSubrepository.getResourceDescription(childSubrepositoryURI)); //get a description of the subrepository root resource
				if(depth == INFINITE_DEPTH || depth > 0) //if we should get child resources lower in the hierarchy
				{
					childResources.addAll(childSubrepository.getChildResourceDescriptions(childSubrepositoryURI, resourceFilter, depth == INFINITE_DEPTH ? depth
							: depth - 1)); //get descriptions of subrepository children
				}
			}
			return childResources; //return the list of resources we constructed
		}
		catch(final SVNException svnException)
		{
			throw toResourceIOException(resourceURI, svnException);
		}
	}

	/** {@inheritDoc} This implementation updates resource properties before storing the contents of the resource. */
	@Override
	protected OutputStream createResourceImpl(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		final TempOutputStream tempOutputStream = new TempOutputStream() //create a new temporary output stream that, before it is closed, will save the collected bytes to a new resource
		{
			@Override
			protected void beforeClose() throws IOException //when the output stream is ready to be closed
			{
				super.beforeClose();
				final InputStream inputStream = toMarkSupportedInputStream(getInputStream()); //get an input stream to the data, making sure it supports mark/reset
				try
				{
					setResourceContents(resourceURI, resourceDescription, null, inputStream); //set resource contents from the resource input stream
				}
				finally
				//always make sure the input stream is closed; this is especially important if we were using a temporary file
				{
					inputStream.close();
				}
			}
		};
		return tempOutputStream; //return the temporary output stream we created 
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource createResourceImpl(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents)
			throws ResourceIOException
	{
		synchronized(svnRepository)
		{
			try
			{
				setResourceContents(resourceURI, resourceDescription, null, new ByteArrayInputStream(resourceContents)); //create the resource, providing the resource contents in an input stream
				final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
				final SVNDirEntry dirEntry = svnRepository.info(resourceURIPath.toString(), -1); //get the updated directory entry for this resource
				return createResourceDescription(createURF(), resourceURI, dirEntry); //create and return the latest description of the resource
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
	}

	/**
	 * Stores the contents of a new or existing resource with the given optional description and contents from an input stream. The resource URI is guaranteed to
	 * be normalized and valid for the repository and the repository is guaranteed to be open.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource, or <code>null</code> if the resource properties should not be altered; the resource URI is
	 *          ignored.
	 * @param dirEntry The SVNKit directory entry of the existing file or directory, or <code>null</code> if the resource does not exist and is being created.
	 * @param inputStream The input stream containing the contents to store in the resource.
	 * @throws NullPointerException if the given resource URI and/or input stream is <code>null</code>.
	 * @throws IllegalArgumentException if the given input stream does not support mark/reset.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	protected void setResourceContents(final URI resourceURI, final URFResource resourceDescription, final SVNDirEntry dirEntry, final InputStream inputStream)
			throws ResourceIOException
	{
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		final URIPath contentURIPath; //we'll determine the URI path to use for content
		final boolean isCollection = isCollectionURI(resourceURI);
		if(isCollection) //if the resource is a collection
		{
			contentURIPath = resourceURIPath.resolve(COLLECTION_CONTENT_NAME); //the URI path to use for content uses the special collection content resource
		}
		else
		//if the resource is not a collection
		{
			contentURIPath = resourceURIPath; //we'll get the content from the file itself
		}
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			try
			{
				//see if we have a content resource; if the collection doesn't exist yet, then of course the content file doesn't yet exist
				//this check must be done outside of an edit or we will get a SVNKit reentrant error
				final boolean contentFileExists = dirEntry != null ? svnRepository.checkPath(contentURIPath.toString(), -1) != SVNNodeKind.NONE : false;
				//TODO probably transfer the check for a non-collection content file existing here as well, so this variable will be put to use for both kinds of resources
				final ISVNEditor editor = svnRepository.getCommitEditor("Marmot resource creation.", null, true, null); //get a commit editor to the repository
				try
				{
					editor.openRoot(-1); //open the root to start making changes
					if(isCollectionURI(resourceURI)) //if we're creating a collection
					{
						if(dirEntry != null) //if the directory supposedly exists
						{
							editor.openDir(resourceURIPath.toString(), -1); //open the directory for modification
						}
						else
						{
							editor.addDir(resourceURIPath.toString(), null, -1); //show that we are adding a directory to the repository
						}
						final boolean hasContent = !isEmpty(inputStream); //see if content is given
						//if the directory already exists, we need to always make sure the content file, if any, is up-to-date;
						//otherwise, for a new collection, we only care if we have something to write
						if(dirEntry != null || hasContent)
						{
							if(contentFileExists || hasContent) //if the file doesn't exist and there's nothing to write, there's nothing to do
							{
								//we'll write content even if we have no content---if there once was a content file,
								//we'll keep it---even a zero-byte file---in order to maintain modified dates and such:
								//if(hasContent) //if we have content to write
								if(contentFileExists) //if the content file exists
								{
									editor.openFile(contentURIPath.toString(), -1); //open the content file for modification
								}
								else
								{
									editor.addFile(contentURIPath.toString(), null, -1); //add the content file
								}
								editor.applyTextDelta(contentURIPath.toString(), null); //start with a blank checksum; we'll not compare the file to any existing file, even when updating files
								final SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
								final String checksum = deltaGenerator.sendDelta(contentURIPath.toString(), inputStream, editor, true); //create a new delta for the contents
								editor.closeFile(contentURIPath.toString(), checksum); //finish the content file addition
							}
						}
						if(resourceDescription != null) //if we have a description of the resource, set its properties
						{
							alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), editor, dirEntry,
									SVNNodeKind.DIR); //alter the properties to be exactly those specified by the given resource description
						}
						editor.closeDir(); //close the directory we added
					}
					else
					//if we're creating a non-collection resource
					{
						if(dirEntry != null) //if the file supposedly exists
						{
							editor.openFile(resourceURIPath.toString(), -1); //open the file for modification
						}
						else
						{
							editor.addFile(resourceURIPath.toString(), null, -1); //show that we are adding a file to the repository
						}
						editor.applyTextDelta(resourceURIPath.toString(), null); //start with a blank checksum; we'll not compare the file to any existing file, even when updating files
						final SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
						final String checksum = deltaGenerator.sendDelta(resourceURIPath.toString(), inputStream, editor, true); //create a new delta for the contents
						if(resourceDescription != null) //if we have a description of the resource, set its properties
						{
							alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), editor, dirEntry,
									SVNNodeKind.FILE); //alter the properties to be exactly those specified by the given resource description
						}
						editor.closeFile(resourceURIPath.toString(), checksum); //finish the file addition
					}
					editor.closeDir(); //close the root
					editor.closeEdit(); //try to finalize the edit
				}
				catch(final SVNException svnException)
				{
					editor.abortEdit(); //abort the edit we had scheduled
					throw svnException; //rethrow the exception
				}
			}
			catch(final IOException ioException)
			{
				throw toResourceIOException(resourceURI, ioException);
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
	}

	/** {@inheritDoc} This implementation ignores requests to delete all resource for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected void deleteResourceImpl(final URI resourceURI) throws ResourceIOException
	{
		final URI sourceResourceURI = getSourceResourceURI(resourceURI);
		if(isSourceResourceVisible(sourceResourceURI)) //if this is a visible resource
		{
			final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
			final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
			synchronized(svnRepository)
			{
				try
				{
					final ISVNEditor editor = svnRepository.getCommitEditor("Marmot resource deletion.", null, true, null); //get a commit editor to the repository
					try
					{
						editor.openRoot(-1); //open the root to start making changes
						editor.deleteEntry(resourceURIPath.toString(), -1); //delete the directory
						editor.closeDir(); //close the root
						editor.closeEdit(); //try to finalize the edit
					}
					catch(final SVNException svnException)
					{
						editor.abortEdit(); //abort the edit we had scheduled
						throw svnException; //rethrow the exception
					}
				}
				catch(final SVNException svnException)
				{
					throw toResourceIOException(resourceURI, svnException);
				}
			}
		}
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource alterResourcePropertiesImpl(final URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException
	{
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			try
			{
				final SVNDirEntry dirEntry = svnRepository.info(resourceURIPath.toString(), -1); //get the directory entry for this resource
				final SVNNodeKind nodeKind = dirEntry.getKind();
				if(nodeKind == SVNNodeKind.NONE) //make sure we have a resource at this URI
				{
					throw new ResourceNotFoundException(resourceURI);
				}
				final ISVNEditor editor = svnRepository.getCommitEditor("Marmot resource property modification.", null, true, null); //get a commit editor to the repository
				try
				{
					editor.openRoot(-1); //open the root to start making changes
					if(nodeKind == SVNNodeKind.FILE) //open the file or directory for editing
					{
						editor.openFile(resourceURIPath.toString(), -1);
					}
					else if(nodeKind == SVNNodeKind.DIR)
					{
						editor.openDir(resourceURIPath.toString(), -1);
					}
					else
					{
						throw new ResourceIOException(resourceURI, "Unrecognized directory entry node kind: " + nodeKind);
					}
					alterResourceProperties(resourceURI, resourceAlteration, editor, dirEntry, nodeKind); //create alter the properties of this resource
					if(nodeKind == SVNNodeKind.FILE) //if this was a file, close its edits
					{
						editor.closeFile(resourceURIPath.toString(), null);
					}
					editor.closeEdit(); //try to finalize the edit
				}
				catch(final SVNException svnException)
				{
					editor.abortEdit(); //abort the edit we had scheduled
					throw svnException; //rethrow the exception
				}
				return createResourceDescription(createURF(), resourceURI, dirEntry); //get the latest description of the resource and return it
			}
			catch(final IOException ioException)
			{
				throw toResourceIOException(resourceURI, ioException);
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
	}

	/**
	 * Alters properties of a given resource. This implementation does not support removing specific properties by value.
	 * <p>
	 * This implementation has a race condition for adding new property values for properties that already exist in that simultaneous additions could clobber all
	 * the additions but the last one.
	 * </p>
	 * @param resourceURI The reference URI of the resource.
	 * @param resourceAlteration The specification of the alterations to be performed on the resource.
	 * @param editor The editor indicating the in-progress commit edits.
	 * @param dirEntry The SVNKit directory entry of the file or directory, or <code>null</code> if no directory entry is available (e.g. for a file not yet
	 *          created).
	 * @param nodeKind The SVNKit kind of node the properties of which are being altered.
	 * @throws NullPointerException if the given resource URI, resource alteration, editor, and/or node kind is <code>null</code>.
	 * @throws NullPointerException if the given directory entry is <code>null</code> and a property addition (as opposed to a property setting, that is, a
	 *           property addition without a corresponding property URI removal) is requested.
	 * @throws IOException if the resource properties could not be altered.
	 * @throws UnsupportedOperationException if a property is requested to be removed by value.
	 */
	protected void alterResourceProperties(final URI resourceURI, final URFResourceAlteration resourceAlteration, final ISVNEditor editor,
			final SVNDirEntry dirEntry, final SVNNodeKind nodeKind) throws SVNException, IOException
	{
		final boolean isDir = nodeKind == SVNNodeKind.DIR; //see if this is a directory or a file being modified
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		if(!resourceAlteration.getPropertyRemovals().isEmpty()) //if there are properties to be removed by value
		{
			throw new UnsupportedOperationException("This implementation does not support removing properties by value.");
		}
		final Set<URI> livePropertyURIs = getLivePropertyURIs(); //get the set of live properties
		final Set<URI> propertyURIRemovals = new HashSet<URI>(resourceAlteration.getPropertyURIRemovals()); //get the property URI removals, which we'll optimize based upon the property settings
		URFResource resourceDescription = null; //we'll only retrieve the existing resource description if needed
		//determine the URF properties that should be added for each URF property
		final CollectionMap<URI, URFProperty, Set<URFProperty>> urfPropertyURIPropertyAdditions = new HashSetHashMap<URI, URFProperty>(); //create a map of sets of properties to add, keyed to their property URIs, so that we can find multiple property values for a single property if present
		for(final URFProperty propertyAddition : resourceAlteration.getPropertyAdditions()) //look at all the property additions
		{
			final URI propertyURI = propertyAddition.getPropertyURI(); //get the URI of the URF property
			if(!livePropertyURIs.contains(propertyURI)) //if this is not a live property
			{
				if(!resourceAlteration.getPropertyURIRemovals().contains(propertyURI)) //if a property addition was requested instead of a property setting (i.e. without first removing all the URI properties), we'll need to first gather the existing properties
				{
					if(resourceDescription == null) //if we don't yet have a description for the resource
					{
						resourceDescription = createResourceDescription(createURF(), resourceURI, dirEntry); //get a description of the resource
					}
					for(final URFProperty existingProperty : resourceDescription.getProperties(propertyURI)) //gather the existing properties; we'll have to combine them all into one property value
					{
						urfPropertyURIPropertyAdditions.addItem(propertyURI, existingProperty); //indicate that this is another URF property to add for this property URI
					}
				}
				urfPropertyURIPropertyAdditions.addItem(propertyURI, propertyAddition); //indicate that this is another URF property to add/set for this property URI
				propertyURIRemovals.remove(propertyURI); //indicate that we don't have to remove this property, because it will be removed by setting it
			}
		}
		//at this point we have only properties to set and properties to remove
		//convert the URF property set and removals to SVNKit property sets
		final Map<String, SVNPropertyValue> setSVNKitProperties = new HashMap<String, SVNPropertyValue>(urfPropertyURIPropertyAdditions.size()
				+ propertyURIRemovals.size()); //keep track of which SVNKit properties to set based upon the URF properties to add
		for(final Map.Entry<URI, Set<URFProperty>> urfPropertyURIPropertyAdditionEntries : urfPropertyURIPropertyAdditions.entrySet()) //look at all the properties to add
		{
			final Set<URFProperty> urfPropertyAdditions = urfPropertyURIPropertyAdditionEntries.getValue(); //get the URF properties to add
			final NameValuePair<URI, String> propertyTextValue = encodePropertiesTextValue(resourceURI, urfPropertyAdditions); //encode the properties into a single value
			setSVNKitProperties.put(encodePropertyURILocalName(propertyTextValue.getName()), SVNPropertyValue.create(propertyTextValue.getValue())); //store this value for later setting
		}
		//determine the properties to remove
		for(final URI propertyURIRemoval : propertyURIRemovals) //look at all the property removals left after removing that which are irrelevant
		{
			if(!livePropertyURIs.contains(propertyURIRemoval)) //if this is not a live property
			{
				setSVNKitProperties.put(encodePropertyURILocalName(propertyURIRemoval), null); //in SVNKit a null value indicates that the property should be removed
			}
		}
		//actually set/remove the properties
		for(final Map.Entry<String, SVNPropertyValue> setProperty : setSVNKitProperties.entrySet())
		{
			final String propertyName = setProperty.getKey();
			final SVNPropertyValue propertyValue = setProperty.getValue();
			if(isDir)
			{
				editor.changeDirProperty(propertyName, propertyValue);
			}
			else
			{
				editor.changeFileProperty(resourceURIPath.toString(), propertyName, propertyValue);
			}
		}
	}

	/**
	 * {@inheritDoc} This implementation throws a {@link ResourceNotFoundException} for all resource for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>false</code>.
	 */
	@Override
	protected void copyResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException
	{
		transferResource(resourceURI, destinationURI, overwrite, false, progressListener); //copy the resource
	}

	/**
	 * {@inheritDoc} This implementation throws a {@link ResourceNotFoundException} for all resource for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>false</code>.
	 */
	@Override
	protected void moveResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException
	{
		transferResource(resourceURI, destinationURI, overwrite, true, progressListener); //copy the resource and then delete the source resource
	}

	/**
	 * Copies or moves a resource to another URI in this repository, overwriting any resource at the destination only if requested. The resource URI is guaranteed
	 * to be normalized and valid for the repository (not the root), and the repository is guaranteed to be open. The destination resource URI is guaranteed not
	 * to be a child of the source resource URI.
	 * <p>
	 * This implementation throws a {@link ResourceNotFoundException} for all resource for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param move <code>true</code> if the source resource should be removed after the copy.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	protected void transferResource(final URI resourceURI, final URI destinationURI, final boolean overwrite, final boolean move,
			final ProgressListener progressListener) throws ResourceIOException
	{
		if(!isSourceResourceVisible(getSourceResourceURI(resourceURI))) //if this is not a visible resource
		{
			throw new ResourceNotFoundException(resourceURI);
		}
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		final URIPath destinationURIPath = getResourceURIPath(destinationURI); //get the path to the destination resource
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			try
			{
				final SVNDirEntry dirEntry = svnRepository.info(resourceURIPath.toString(), -1); //get the directory entry for this resource
				final SVNNodeKind nodeKind = dirEntry.getKind();
				if(nodeKind == SVNNodeKind.NONE) //make sure we have a resource at this URI
				{
					throw new ResourceNotFoundException(resourceURI);
				}
				final SVNNodeKind destinationNodeKind = svnRepository.checkPath(destinationURIPath.toString(), -1);
				if(destinationNodeKind != SVNNodeKind.NONE && !overwrite) //if the destination resource already exists but we shouldn't overwrite
				{
					throw new ResourceStateException(destinationURI, "Destination resource already exists.");
				}
				//TODO add a checkNodeKind(resourceURI) here and all over the place to ensure that we don't have a file for a collection and vice/versa
				final ISVNEditor editor = svnRepository.getCommitEditor("Marmot resource copy.", null, true, null); //get a commit editor to the repository
				try
				{
					editor.openRoot(-1); //open the root to start making changes
					if(destinationNodeKind != SVNNodeKind.NONE) //if the destination resource already exists
					{
						editor.deleteEntry(destinationURIPath.toString(), -1); //delete the destination resource
					}
					if(isCollectionURI(resourceURI)) //if we're copying a collection
					{
						editor.addDir(destinationURIPath.toString(), resourceURIPath.toString(), dirEntry.getRevision()); //copy the existing directory at its latest revision
						editor.closeDir(); //close the copied directory
					}
					else
					{
						editor.addFile(destinationURIPath.toString(), resourceURIPath.toString(), dirEntry.getRevision()); //copy the existing file at its latest revision
					}
					if(move) //if this is a move
					{
						editor.deleteEntry(resourceURIPath.toString(), -1); //delete the source resource
					}
					editor.closeDir(); //close the root
					editor.closeEdit(); //try to finalize the edit
				}
				catch(final SVNException svnException)
				{
					editor.abortEdit(); //abort the edit we had scheduled
					throw svnException; //rethrow the exception
				}
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
	}

	/**
	 * Creates a resource description to represent a single SVNKit node if its properties are not yet known. The resource is assumed to exist. This method is
	 * thread-safe.
	 * @param urf The URF data model to use when creating this resource.
	 * @param resourceURI The URI of the resource being described.
	 * @param dirEntry The directory entry for the Subversion node.
	 * @return A resource description of the given SVNKit node.
	 * @throws NullPointerException if the given data model, resource URI, and/or directory entry is <code>null</code>.
	 * @throws SVNException if there is an error creating the resource description.
	 * @throws IllegalArgumentException if a non-collection URI is given to access a directory.
	 */
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, final SVNDirEntry dirEntry) throws SVNException, ResourceIOException //TODO recheck all these exception types
	{
		return createResourceDescription(urf, resourceURI, dirEntry, null);
	}

	/**
	 * Determines whether the given property is in a reserved namespace. Reserved namespaces include:
	 * <ul>
	 * <li>{@link SVNProperty#SVN_PREFIX}.</li>
	 * <li>{@link SVNProperty#SVNKIT_PREFIX}.</li>
	 * </ul>
	 * @param propertyName The name of the property to check.
	 * @return <code>true</code> if the property is in one of the known reserved namespaces.
	 * @throws NullPointerException if the given property name is <code>null</code>.
	 */
	protected boolean isReservedNamespaceProperty(final String propertyName)
	{
		return propertyName.startsWith(SVN_PREFIX) || propertyName.startsWith(SVNKIT_PREFIX);
	}

	/**
	 * Creates a resource description to represent a single SVNKit node. The resource is assumed to exist. This method is thread-safe.
	 * @param urf The URF data model to use when creating this resource.
	 * @param resourceURI The URI of the resource being described.
	 * @param dirEntry The directory entry for the Subversion node.
	 * @param properties The properties that are known, or <code>null</code> if properties have not yet been retrieved for the resource.
	 * @return A resource description of the given SVNKit node.
	 * @throws NullPointerException if the given data model, resource URI, and/or directory entry is <code>null</code>.
	 * @throws SVNException if there is an error creating the resource description.
	 * @throws IllegalArgumentException if a non-collection URI is given to access a directory.
	 */
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, final SVNDirEntry dirEntry, SVNProperties properties)
			throws SVNException, ResourceIOException
	{
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		final URFResource resource = urf.createResource(resourceURI); //create a default resource description
		SVNNodeKind nodeKind = dirEntry.getKind(); //find out what kind of node this is
		//		final String filename = resourceFile.getName(); //get the name of the file
		final long contentLength; //we'll update the content length if we can
		URFDateTime contentModified = null; //we'll get the content modified from the file or, for a directory, from its content file, if any---but not from a directory itself
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			if(nodeKind == SVNNodeKind.DIR) //if this is a directory
			{
				if(!isCollectionURI(resourceURI)) //if a non-collection URI was used for the directory
				{
					throw new IllegalArgumentException("Non-collection URI " + resourceURI + " used for directory " + resourceURI);
				}
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				final URIPath contentURIPath = getResourceURIPath(contentURI);
				final SVNDirEntry contentDirEntry = svnRepository.info(contentURIPath.toString(), -1); //get the directory entry for the special collection content resource
				if(contentDirEntry != null) //if there is a special collection content file
				{
					contentLength = contentDirEntry.getSize(); //use the size of the special collection content resource
					contentModified = new URFDateTime(contentDirEntry.getDate()); //set the modified timestamp as the last modified date of the content file 
				}
				else
				//if there is no special collection content file
				{
					contentLength = 0; //the collection has no content
					contentModified = null; //don't return a last modified date 

				}
			}
			else
			//if this file is not a directory
			{
				contentLength = dirEntry.getSize(); //use the size of the file
				contentModified = new URFDateTime(dirEntry.getDate()); //set the modified timestamp as the last modified date of the resource file			
			}
			if(dirEntry.hasProperties()) //if there are additional properties
			{
				if(properties == null) //if no properties were given
				{
					properties = new SVNProperties(); //load the properties from the repository
					if(nodeKind == SVNNodeKind.FILE) //open the file or directory for editing
					{
						svnRepository.getFile(resourceURIPath.toString(), -1, properties, null);
					}
					else if(nodeKind == SVNNodeKind.DIR)
					{
						svnRepository.getDir(resourceURIPath.toString(), -1, properties, (Collection<?>)null);
					}
					else
					{
						throw new ResourceIOException(resourceURI, "Unrecognized directory entry node kind: " + nodeKind);
					}
				}
				@SuppressWarnings("unchecked")
				final Map<String, SVNPropertyValue> propertyValues = (Map<String, SVNPropertyValue>)properties.asMap(); //get a map of the Subversion properties
				for(final Map.Entry<String, SVNPropertyValue> propertyValueEntry : propertyValues.entrySet()) //look at the Subversion properties
				{
					final String propertyName = propertyValueEntry.getKey();
					final SVNPropertyValue propertyValue = propertyValueEntry.getValue();
					if(!isReservedNamespaceProperty(propertyName) && propertyValue.isString()) //if this is a non-reserved Subversion property with a string value
					{
						try
						{
							final URI propertyURI = decodePropertyURILocalName(propertyName); //the URF property URI may be encoded as the local name of the Subversion custom property
							decodePropertiesTextValue(resource, propertyURI, propertyValueEntry.getValue().getString()); //decode the text value into the resource
						}
						catch(final IllegalArgumentException illegalArgumentException) //if the Subversion custom property local name wasn't an encoded URI, ignore the error and skip this property
						{
						}
					}
				}
			}
			//live properties
			setContentLength(resource, contentLength); //indicate the length of the content
			//in SVNKit for the time being the live last-modified date overrides everything
			if(contentModified != null) //if we have a content modified time
			{
				setModified(resource, contentModified); //set the modified timestamp as the last modified date
			}
			//TODO fix synchronize-last-modified-time business; see WebDavRepository
		}
		return resource; //return the resource that represents the file
	}

	/** {@inheritDoc} This version calls clears and releases the password, if any. */
	@Override
	public synchronized void dispose()
	{
		try
		{
			super.dispose();
		}
		finally
		{
			if(password != null) //if we have a password
			{
				fill(password, (char)0); //erase the password from memory as a security measure
				password = null; //release the password
			}
		}
	}
}

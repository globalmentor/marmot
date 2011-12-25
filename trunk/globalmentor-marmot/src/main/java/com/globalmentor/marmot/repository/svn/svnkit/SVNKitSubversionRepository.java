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

import static java.util.Collections.*;

import static com.globalmentor.io.Files.*;
import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.Objects.*;
import static com.globalmentor.marmot.repository.Repositories.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;

import com.globalmentor.io.*;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.net.*;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.Content;

import static com.globalmentor.urf.TURF.*;

/**
 * Subversion repository implemented by SVNKit.
 * 
 * <p>
 * This implementation supports FSFS, Subversion, WebDAV based repositories.
 * </p>
 * 
 * <p>
 * This implementation uses the file last modified timestamp to store the {@value Content#MODIFIED_PROPERTY_URI} property. The content modified property is not
 * saved for collections with no content.
 * </p>
 * //TODO
 * <p>
 * This implementation ignores hidden files when considering child resources.
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
		this(getDirectoryURI(repositoryDirectory)); //get a directory URI from the repository directory and use it as the base repository URI
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
		this(publicRepositoryURI, privateRepositoryDirectory != null ? getDirectoryURI(privateRepositoryDirectory) : null); //get a directory URI from the private repository directory and use it as the base repository URI
	}

	/**
	 * Public repository URI and private repository URI constructor. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	 * @throws NullPointerException if one of the given repository URIs is <code>null</code>. //TODO relax; improve @throws IllegalArgumentException if the
	 *           private repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	 */
	public SVNKitSubversionRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		super(publicRepositoryURI, privateRepositoryURI); //construct the parent class
		/*TODO decide if how we want initialization to occur, especially using PLOOP
				if(!FILE_SCHEME.equals(privateRepositoryURI.getScheme()))	//if the private repository URI scheme is not the file scheme
				{
					throw new IllegalArgumentException(privateRepositoryURI+" does not use the "+FILE_SCHEME+" URI scheme.");
				}
		*/
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
				final SVNDirEntry dirEntry = svnRepository.info(resourceURIPath.toString(), -1); //get the directory entry for this repository
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
				if(isCollectionURI(resourceURI)) //if the resource is a collection
				{
					/*TODO fix for collection URIs
					final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
					final File contentFile = new File(getSourceResourceURI(contentURI)); //create a file object from the private URI of the special collection content resource
					if(contentFile.exists()) //if there is a special collection content resource
					{
						return new FileInputStream(contentFile); //return an input stream to the file
					}
					else
					*/
					//if there is no collection content resource
					{
						return new ByteArrayInputStream(NO_BYTES); //return an input stream to an empty byte array
					}
				}
				else
				//if the resource is not a collection
				{
					final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
					final SVNNodeKind svnNodeKind = svnRepository.checkPath(resourceURIPath.toString(), -1); //see what kind of resource this is
					if(svnNodeKind == SVNNodeKind.NONE) //make sure we have a resource at this URI
					{
						throw new ResourceNotFoundException(resourceURI);
					}
					final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); //create a byte array output stream to hold the file TODO improve to create temporary files for large resources and return an input stream to them
					svnRepository.getFile(resourceURIPath.toString(), -1, null, byteArrayOutputStream); //retrieve the contents of the file
					return new ByteArrayInputStream(byteArrayOutputStream.toByteArray()); //get the bytes from the output stream and return them as an input stream
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
		throw new ResourceForbiddenException(resourceURI, "This repository is read-only."); //TODO fix
	}

	/**
	 * Gets an output stream to the contents of the given resource file. This version returns a direct output stream to the file.
	 * @param resourceURI The URI of the resource to access.
	 * @param resourceFile The file representing the resource.
	 * @return An output stream to the resource represented by given resource file.
	 * @throws IOException if there is an error accessing the resource.
	 */
	protected OutputStream getResourceOutputStream(final URI resourceURI, final File resourceFile) throws IOException
	{
		return new FileOutputStream(resourceFile); //return an output stream to the file
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
		throw new UnsupportedOperationException();
		/*TODO fix
				try
				{
					final File resourceFile = new File(getSourceResourceURI(resourceURI)); //create a file object for the resource
					if(resourceFile.exists()) //if the resource file already exists (either as a file or a directory)
					{
						delete(resourceFile, true); //delete the file/directory and all its children, if any
					}
					final File contentFile; //determine the file to use for storing content
					if(isCollectionURI(resourceURI)) //if the resource is a collection
					{
						final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
						contentFile = new File(getSourceResourceURI(contentURI)); //create a file object from the private URI of the special collection content resource
						mkdir(resourceFile); //create the directory
					}
					else
					//if the resource is not a collection
					{
						contentFile = resourceFile; //use the normal resource file
						//TODO should we see if a directory exists?
						createNewFile(resourceFile); //create a new file
					}
					return new DescriptionWriterOutputStreamDecorator(new FileOutputStream(contentFile, true), resourceDescription, resourceFile); //wrap the output stream to the content file in a decorator that will write the properties after the contents are stored
				}
				catch(final IOException ioException) //if an I/O exception occurs
				{
					throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
				}
		*/
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource createResourceImpl(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents)
			throws ResourceIOException
	{
		final URIPath resourceURIPath = getResourceURIPath(resourceURI); //get the path to the resource
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			try
			{
				//TODO del if not needed				final long latestRevision=svnRepository.getLatestRevision();	//get the latest repository revision
				ISVNEditor commitEditor = svnRepository.getCommitEditor("Marmot modification", null, true, null); //get a commit editor to the repository
				commitEditor.openRoot(-1); //open the root to start making changing

				//				final URIPath contentURIPath;	//determine the path to use for storing content

				if(isCollectionURI(resourceURI)) //if we're creating a collection
				{
					//TODO decide what to do if collection exists
					commitEditor.addDir(resourceURIPath.toString(), null, -1); //show that we are adding a directory to the repository
					if(resourceContents.length > 0) //if we have contents for the directory
					{
						final URIPath contentURIPath = resourceURIPath.resolve(COLLECTION_CONTENT_NAME); //determine the URI path to use for content, using the special collection content resource
						commitEditor.applyTextDelta(contentURIPath.toString(), null); //this is a new file; start with a blank checksum TODO fix for existing files
						final SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
						final String checksum = deltaGenerator.sendDelta(contentURIPath.toString(), new ByteArrayInputStream(resourceContents), commitEditor, true); //create a new delta for the contents
						//TODO update last-modified properties
						commitEditor.closeFile(resourceURIPath.toString(), checksum); //finish the file addition
					}
					//TODO update properties
					commitEditor.closeDir(); //close the directory we added
				}
				else
				//if we're creating a non-collection resource
				{
					commitEditor.addFile(resourceURIPath.toString(), null, -1); //show that we are adding a file to the repository
					commitEditor.applyTextDelta(resourceURIPath.toString(), null); //this is a new file; start with a blank checksum TODO fix for existing files
					final SVNDeltaGenerator deltaGenerator = new SVNDeltaGenerator();
					final String checksum = deltaGenerator.sendDelta(resourceURIPath.toString(), new ByteArrayInputStream(resourceContents), commitEditor, true); //create a new delta for the contents
					//TODO update properties
					commitEditor.closeFile(resourceURIPath.toString(), checksum); //finish the file addition
				}
				commitEditor.closeDir(); //close the root
				try
				{
					commitEditor.closeEdit(); //try to finalize the edit
				}
				catch(final SVNException svnException)
				{
					commitEditor.abortEdit(); //abort the edit we had scheduled
					throw svnException; //rethrow the exception
				}
			}
			catch(final SVNException svnException)
			{
				throw toResourceIOException(resourceURI, svnException);
			}
		}
		return getResourceDescription(resourceURI); //get the updated resource description
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
					ISVNEditor commitEditor = svnRepository.getCommitEditor("Marmot modification", null, true, null); //get a commit editor to the repository
					commitEditor.openRoot(-1); //open the root to start making changing
					commitEditor.deleteEntry(resourceURIPath.toString(), -1); //delete the directory
					commitEditor.closeDir(); //close the directory we deleted
					try
					{
						commitEditor.closeEdit(); //try to finalize the edit
					}
					catch(final SVNException svnException)
					{
						commitEditor.abortEdit(); //abort the edit we had scheduled
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

	/**
	 * Alters properties of a given resource.
	 * @param resourceURI The reference URI of the resource.
	 * @param resourceAlteration The specification of the alterations to be performed on the resource.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or resource alteration is <code>null</code>.
	 * @throws ResourceIOException if the resource properties could not be altered.
	 */
	public URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException
	{
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) //if the resource URI lies within a subrepository
		{
			return subrepository.alterResourceProperties(resourceURI, resourceAlteration); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return alterResourceProperties(resourceURI, resourceAlteration, new File(getSourceResourceURI(resourceURI))); //create a file object and alter the properties for the file
	}

	/**
	 * Alters properties of a given resource. Live properties are ignored.
	 * @param resourceURI The reference URI of the resource.
	 * @param resourceAlteration The specification of the alterations to be performed on the resource.
	 * @param resourceFile The file to use in updating the resource properties.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI, resource alteration, and/or resource file is <code>null</code>.
	 * @throws ResourceIOException if the resource properties could not be altered.
	 */
	protected URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration, final File resourceFile)
			throws ResourceIOException
	{
		throw new UnsupportedOperationException();
		/*TODO fix
				final URF urf = createURF(); //create a new URF data model
				try
				{
					final URFResource resourceDescription = createResourceDescription(urf, resourceURI, resourceFile); //get a description from a file created from the URI from the private namespace
					resourceDescription.alter(resourceAlteration); //alter the resource according to the specification
					saveResourceDescription(resourceDescription, resourceFile); //save the altered resource description
					return createResourceDescription(urf, resourceURI, resourceFile); //get a new description from the file, because the live properties might have changed
				}
				catch(final IOException ioException) //if an I/O exception occurs
				{
					throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
				}
		*/
	}

	/**
	 * Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destination only if requested.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error copying the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	public void copyResource(URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) //if the resource URI lies within a subrepository
		{
			subrepository.copyResource(resourceURI, destinationURI, overwrite); //delegate to the subrepository
		}
		final Repository destinationRepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationRepository != this) //if the destination URI lies within a subrepository
		{
			copyResource(resourceURI, destinationRepository, destinationURI, overwrite); //copy between repositories
		}
		checkOpen(); //make sure the repository is open
		throw new UnsupportedOperationException(); //TODO implement
	}

	/**
	 * Moves a resource to another URI in this repository, overwriting any resource at the destionation only if requested.
	 * @param resourceURI The URI of the resource to be moved.
	 * @param destinationURI The URI to which the resource should be moved.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws IllegalArgumentException if the given resource URI is the base URI of the repository.
	 * @throws ResourceIOException if there is an error moving the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	public void moveResource(URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) //if the resource URI lies within a subrepository
		{
			subrepository.moveResource(resourceURI, destinationURI, overwrite); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		if(normalize(resourceURI).equals(getRootURI())) //if they try to move the root URI
		{
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		throw new UnsupportedOperationException(); //TODO implement

		//TODO move resource description if needed
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
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, final SVNDirEntry dirEntry) throws SVNException
	{
		return createResourceDescription(urf, resourceURI, dirEntry, null);
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
			throws SVNException
	{
		final URFResource resource = urf.createResource(resourceURI); //create a default resource description
		SVNNodeKind svnNodeKind = dirEntry.getKind(); //find out what kind of node this is
		//		final String filename = resourceFile.getName(); //get the name of the file
		long contentLength = 0; //we'll update the content length if we can
		URFDateTime contentModified = null; //we'll get the content modified from the file or, for a directory, from its content file, if any---but not from a directory itself
		final SVNRepository svnRepository = getSVNRepository(); //get the SVNKit repository and prevent other threads for accessing it simultaneously
		synchronized(svnRepository)
		{
			if(svnNodeKind == SVNNodeKind.DIR) //if this is a directory
			{
				if(!isCollectionURI(resourceURI)) //if a non-collection URI was used for the directory
				{
					throw new IllegalArgumentException("Non-collection URI " + resourceURI + " used for directory " + resourceURI);
				}
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				//				if(svnRepository.checkPath(getResourceURIPath(contentURI).toString(), -1)==SVNNodeKind.FILE) //if there is a special collection content file
				//				{
				final SVNDirEntry contentDirEntry = svnRepository.info(getResourceURIPath(contentURI).toString(), -1); //get the directory entry for the special collection content resource
				if(contentDirEntry != null) //if there is a special collection content resource
				{
					contentLength = contentDirEntry.getSize(); //use the size of the special collection content resource
					contentModified = new URFDateTime(contentDirEntry.getDate()); //set the modified timestamp as the last modified date of the content file			
				}
				//					properties=contentDirEntry.hasProperties() ? svnRepository.in
			}
			else
			//if this file is not a directory
			{
				contentLength = dirEntry.getSize(); //use the size of the file
				contentModified = new URFDateTime(dirEntry.getDate()); //set the modified timestamp as the last modified date of the resource file			
			}
			setContentLength(resource, contentLength); //indicate the length of the content
			if(contentModified != null) //if we have a content modified time
			{
				setModified(resource, contentModified); //set the modified timestamp as the last modified date
			}
			//TODO verify where content type comes from
			if(dirEntry.hasProperties()) //if there are additional properties
			{
				if(properties == null) //if no properties were given
				{
					properties = new SVNProperties(); //load the properties from the repository
					svnRepository.getFile(getResourceURIPath(resourceURI).toString(), -1, properties, null);
				}
				@SuppressWarnings("unchecked")
				final Map<String, SVNPropertyValue> propertyValues = (Map<String, SVNPropertyValue>)properties.asMap(); //get a map of the Subversion properties
				for(final Map.Entry<String, SVNPropertyValue> propertyValueEntry : propertyValues.entrySet()) //look at the Subversion properties
				{
					final String propertyName = propertyValueEntry.getKey();
					final SVNPropertyValue propertyValue = propertyValueEntry.getValue();
					if(SVNProperty.isRegularProperty(propertyName) && propertyValue.isString()) //if this is a regular Subversion property with a string value
					{
						final String propertyLocalName = SVNProperty.shortPropertyName(propertyName); //get the local part of the property name
						try
						{
							final URI propertyURI = decodePropertyURILocalName(propertyLocalName); //the URF property URI may be encoded as the local name of the Subversion custom property
							decodePropertiesTextValue(urf, resource, propertyURI, propertyValueEntry.getValue().getString(), getDescriptionIO()); //decode the text value into the property
						}
						catch(final IllegalArgumentException illegalArgumentException) //if the Subversion custom property local name wasn't an encoded URI, ignore the error and skip this property
						{
						}
					}
				}
			}
			//TODO fix synchronize-last-modified-time business; see WebDavRepository
		}
		return resource; //return the resource that represents the file
	}

	/**
	 * Saves a resource description for a single file. Live properties are ignored. If the {@value Content#MODIFIED_PROPERTY_URI} property is present, it is not
	 * saved and the file modified time is updated to match that value. If the {@value Content#CREATED_PROPERTY_URI} property is present and it is identical to
	 * the {@value Content#MODIFIED_PROPERTY_URI} property, it is not saved. If the {@value Content#CREATED_PROPERTY_URI} property is present and the resource is
	 * a collection with no content, it is not saved. If the resource description file does not exist and there are no properties to save, no resource description
	 * file is created.
	 * @param resourceDescription The resource description to save; the resource URI is ignored.
	 * @param resourceFile The file of a resource.
	 * @throws IOException if there is an error save the resource description.
	 * @see #getResourceDescriptionFile(File)
	 */
	/*TODO fix
		protected void saveResourceDescription(URFResource resourceDescription, final File resourceFile) throws IOException
		{
			final URI resourceURI = getRepositoryResourceURI(toURI(resourceFile)); //get a public URI to represent the file resource
			resourceDescription = new DefaultURFResource(resourceDescription, resourceURI); //create a temporary resource so that we can remove the live properties and to make sure we use the correct URI
			for(final URI livePropertyURI : getLivePropertyURIs()) //look at all live properties
			{
				resourceDescription.removePropertyValues(livePropertyURI); //remove all values for this live property
			}
			final File contentFile; //determine the file to use for storing content
			final boolean isCollection = isCollectionURI(resourceURI); //see if this is a collection
			if(isCollection) //if the resource is a collection
			{
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				final File tempContentFile = new File(getSourceResourceURI(contentURI)); //create a file object from the private URI of the special collection content resource
				contentFile = tempContentFile.exists() ? tempContentFile : resourceFile; //if the content file doesn't exist, we can't update its modified time
			}
			else
			//if the resource is not a collection
			{
				contentFile = resourceFile; //use the normal resource file
			}
			final URFDateTime modified = getModified(resourceDescription); //see if the description indicates the last modified time
			if(modified != null) //if the last modified time was indicated
			{
				resourceDescription.removePropertyValues(Content.MODIFIED_PROPERTY_URI); //remove all last-modified values from the description we'll actually save
			}
			final URFDateTime created = getCreated(resourceDescription); //see if the description indicates the created time
			if(created != null) //if the created time is present
			{
				if((modified != null && created.getTime() == modified.getTime()) //if the created time is the same as the modified time TODO decide how useful these are
						|| (isCollection && contentFile == resourceFile)) //or if this is a collection with no content
				{
					resourceDescription.removePropertyValues(Content.CREATED_PROPERTY_URI); //remove all created timestamp values from the description to save, as Java can't distinguish between content created and modified and they'll both be initialized from the same value, anyway, when reading
				}
			}
			final File resourceDescriptionFile = getResourceDescriptionFile(resourceFile); //get the file for storing the description
			if(resourceDescription.hasProperties() || resourceDescriptionFile.exists()) //if there are any properties to set (otherwise, don't create an empty properties file) or the description file already exists
			{
				try
				{
					Files.write(resourceDescriptionFile, resourceURI, resourceDescription, getDescriptionIO()); //write the description, using the resource URI as the base URI
				}
				catch(final IOException ioException) //if an error occurs
				{
					throw new IOException("Error writing resource description to " + resourceDescriptionFile, ioException);
				}
			}
			if(modified != null) //if a modification timestamp was indicated
			{
				if(!isCollection || contentFile != resourceFile) //don't update the content modified for collections with no content
				{
					if(!contentFile.setLastModified(modified.getTime())) //update the content file's record of the last modified time
					{
						throw new IOException("Error updating content modified time of " + resourceFile);
					}
				}
			}
		}
	*/

	//TODO fix DescriptionWriterOutputStreamDecorator

	//TODO add finalize() to clear password from memory 
}

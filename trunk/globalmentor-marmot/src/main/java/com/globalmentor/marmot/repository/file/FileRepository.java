/*
 * Copyright © 1996-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.file;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.urframework.*;
import org.urframework.content.Content;
import org.urframework.io.URFFiles;
import org.urframework.io.URFResourceTURFIO;

import static java.util.Collections.*;
import static org.urframework.TURF.*;
import static org.urframework.content.Content.*;

import static com.globalmentor.io.Files.*;
import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.Objects.*;
import static com.globalmentor.net.URIs.*;

import com.globalmentor.event.ProgressListener;
import com.globalmentor.io.*;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.net.*;

/**
 * Repository stored in a file system.
 * <p>
 * This implementation considers {@link Content#CREATED_PROPERTY_URI} to be a live property, although it is never retrieved because Java 6 cannot access the
 * file creation time.
 * </p>
 * <p>
 * This implementation uses the file last modified timestamp to store the {@value Content#MODIFIED_PROPERTY_URI} property. The content modified property is not
 * saved for collections with no content. The creation timestamp {@value Content#CREATED_PROPERTY_URI} is not stored in order to prevent needless creation of
 * description files.
 * </p>
 * <p>
 * This implementation ignores hidden files when considering child resources.
 * </p>
 * @author Garret Wilson
 */
public class FileRepository extends AbstractHierarchicalSourceRepository
{

	//TODO see http://lists.apple.com/archives/java-dev/2006/Aug/msg00325.html ; fix non-ASCII characters getting in filename URI

	//TODO encode special characters, especially the colon on NTFS

	/** The URI representing the XPackage file:folder type. */
	//TODO check; use static imports 
	//TODO move if needed	protected final static URI FILE_FOLDER_TYPE_URI=RDFUtilities.createReferenceURI(FileOntologyConstants.FILE_ONTOLOGY_NAMESPACE_URI, FileOntologyConstants.FOLDER_TYPE_NAME);	//TODO promote to parent file-based class		

	/** The extension used for directories to hold resource children. */
	//TODO move if needed	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/** The name component of the Marmot description of a file resource. */
	public final static String MARMOT_DESCRIPTION_NAME = "marmot-description";

	/**
	 * The file filter for listing files in a directory. The file filter returns those resources for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>true</code>.
	 */
	private final FileFilter fileFilter = new FileFilter()
	{
		/**
		 * Tests whether or not the specified abstract pathname is one of the file types we recognize. This implementation ignores hidden files. This implementation
		 * delegates to {@link FileRepository#isSourceResourceVisible(URI)}.
		 * @param file The abstract pathname to be tested.
		 * @return <code>true</code> if and only if the indicated file should be included.
		 * @throws NullPointerException if the given file is <code>null</code>.
		 * @see File#isHidden()
		 * @see FileRepository#isSourceResourceVisible(URI)
		 */
		public boolean accept(final File file)
		{
			if(file.isHidden()) //ignore hidden files
			{
				return false;
			}
			return isSourceResourceVisible(toURI(file)); //see if the private URI represented by this file should be accepted 
		}
	};

	/**
	 * Returns the file filter for listing files in a directory. The file filter returns those resources for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>true</code>.
	 * @return The file filter for listing files in a directory.
	 */
	protected FileFilter getFileFilter()
	{
		return fileFilter;
	}

	/**
	 * Default constructor with no root URI defined. The root URI must be defined before the repository is opened.
	 */
	public FileRepository()
	{
		this((URI)null);
	}

	/**
	 * File constructor with no separate private URI namespace.
	 * @param repositoryDirectory The file identifying the directory of this repository.
	 * @throws NullPointerException if the given repository directory is <code>null</code>.
	 */
	public FileRepository(final File repositoryDirectory)
	{
		this(toURI(repositoryDirectory, true)); //get a directory URI from the repository directory and use it as the base repository URI
	}

	/**
	 * URI constructor with no separate private URI namespace. The given repository URI should end in a slash.
	 * @param repositoryURI The URI identifying the location of this repository.
	 * @throws NullPointerException if the given repository URI is <code>null</code>.
	 * @throws IllegalArgumentException if the repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	 */
	public FileRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI); //use the same repository URI as the public and private namespaces
	}

	/**
	 * Public repository URI and private repository directory constructor.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryDirectory The file identifying the private directory of the repository.
	 * @throws NullPointerException if the given repository URI and/or the given directory is <code>null</code>.
	 */
	public FileRepository(final URI publicRepositoryURI, final File privateRepositoryDirectory)
	{
		this(publicRepositoryURI, privateRepositoryDirectory != null ? toURI(privateRepositoryDirectory, true) : null); //get a directory URI from the private repository directory and use it as the base repository URI
	}

	/**
	 * Public repository URI and private repository URI constructor. The given private repository URI should end in a slash.
	 * @param publicRepositoryURI The URI identifying the location of this repository.
	 * @param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	 * @throws NullPointerException if one of the given repository URIs is <code>null</code>. //TODO relax; improve @throws IllegalArgumentException if the
	 *           private repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	 */
	public FileRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		super(publicRepositoryURI, privateRepositoryURI); //construct the parent class
		addLivePropertyURI(Content.CREATED_PROPERTY_URI); //this repository considers content created a live property
		/*TODO decide if how we want initialization to occur, especially using PLOOP
				if(!FILE_SCHEME.equals(privateRepositoryURI.getScheme()))	//if the private repository URI scheme is not the file scheme
				{
					throw new IllegalArgumentException(privateRepositoryURI+" does not use the "+FILE_SCHEME+" URI scheme.");
				}
		*/
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO = (URFResourceTURFIO<URFResource>)getDescriptionIO(); //get the description I/O
		urfResourceDescriptionIO.setFormatted(true); //turn on formatting
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
		return new FileRepository(publicRepositoryURI, privateRepositoryURI); //create and return a new file repository
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
		final File file = new File(privateResourceURI); //get the file object for this resource
		if(isCollectionURI) //if this should be a collection
		{
			return file.isDirectory(); //see if a directory exists
		}
		else
		{
			return file.exists() && !file.isDirectory(); //see if a non-directory file exists; don't allow non-collection URI find a directory (file systems usually don't allow both a file and a directory of the same name, so they allow the ending-slash form to be optional)
		}
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource getResourceDescriptionImpl(final URI resourceURI) throws ResourceIOException
	{
		final URF urf = createURF(); //create a new URF data model
		try
		{
			return createResourceDescription(urf, resourceURI, new File(getSourceResourceURI(resourceURI))); //create and return a description from a file created from the URI from the private namespace
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/** {@inheritDoc} For collections, this implementation retrieves the content of the {@value #COLLECTION_CONTENT_NAME} file, if any. */
	@Override
	protected InputStream getResourceInputStreamImpl(final URI resourceURI) throws ResourceIOException
	{
		try
		{
			final File resourceFile = new File(getSourceResourceURI(resourceURI)); //create a file object from the private URI
			if(isCollectionURI(resourceURI)) //if the resource is a collection
			{
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				final File contentFile = new File(getSourceResourceURI(contentURI)); //create a file object from the private URI of the special collection content resource
				if(contentFile.exists()) //if there is a special collection content resource
				{
					return new FileInputStream(contentFile); //return an input stream to the file
				}
				else
				//if there is no collection content resource
				{
					checkFileExists(resourceFile); //make sure the real problem isn't that the resource file itself doesn't exist
					return new ByteArrayInputStream(NO_BYTES); //return an input stream to an empty byte array
				}
			}
			else
			//if the resource is not a collection
			{
				return new FileInputStream(resourceFile); //return an input stream to the file; this will check for the file's existence
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/** {@inheritDoc} For collections, this implementation stores the content in the {@value #COLLECTION_CONTENT_NAME} file. */
	@Override
	protected OutputStream getResourceOutputStreamImpl(final URI resourceURI, URFDateTime newContentModified) throws ResourceIOException
	{
		try
		{
			final File resourceFile = new File(getSourceResourceURI(resourceURI)); //get a file for the resource
			checkFileExists(resourceFile);
			final File contentFile; //determine the file to use for storing content
			final boolean isCollection = isCollectionURI(resourceURI);
			if(isCollection) //if the resource is a collection
			{
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				contentFile = new File(getSourceResourceURI(contentURI)); //create a file object from the private URI of the special collection content resource
			}
			else
			//if the resource is not a collection
			{
				contentFile = resourceFile; //use the normal resource file
			}
			if(newContentModified == null) //because we use the file modified value to keep track of the content modified property, we must *always* update the content modified, if only to keep the modified datetime we already have, because the file system will update this value automatically without our asking
			{
				newContentModified = new URFDateTime(contentFile.lastModified()); //get the current last modified date of the file; after the file is written, we'll update it with what it was before
			}
			final URF urf = createURF(); //create a new URF data model
			final URFResource resourceDescription = createResourceDescription(urf, resourceURI, resourceFile); //get a description from a file created from the URI from the private namespace
			setModified(resourceDescription, newContentModified); //update the content modified of the description
			final OutputStream outputStream = new FileOutputStream(contentFile); //create an output stream to the content file
			return new DescriptionWriterOutputStreamDecorator(outputStream, resourceDescription, resourceFile); //wrap the output stream to the content file in a decorator that will write the properties after the contents are stored
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
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
		final File resourceFile = new File(getSourceResourceURI(resourceURI)); //create a file object for the resource
		try
		{
			if(isCollectionURI(resourceURI))
			{
				checkDirectoryExists(resourceFile);
				return resourceFile.listFiles(getFileFilter()).length > 0; //see if there is more than one file in this directory meeting our criteria
			}
			else
			{
				checkFileExists(resourceFile);
				return false;
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/** {@inheritDoc} This implementation does not include child resources for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	public List<URFResource> getChildResourceDescriptionsImpl(final URI resourceURI, final ResourceFilter resourceFilter, final int depth)
			throws ResourceIOException
	{
		if(depth != 0) //a depth of zero means don't get child resources
		{
			final File resourceDirectory = new File(getSourceResourceURI(resourceURI)); //create a file object for the resource
			final List<URFResource> childResourceList = new ArrayList<URFResource>(); //create a list to hold the files that are not directories	
			if(isCollectionURI(resourceURI) && resourceDirectory.isDirectory()) //if there is a directory for this resource
			{
				final URF urf = createURF(); //create a new URF data model
				final File[] files = resourceDirectory.listFiles(getFileFilter()); //get a list of all files in the directory
				for(final File file : files) //for each file in the directory
				{
					final URI childResourcePrivateURI = toURI(file);
					if(isSourceResourceVisible(childResourcePrivateURI)) //if the associated child resource is visible
					{
						final URI childResourcePublicURI = getRepositoryResourceURI(childResourcePrivateURI); //get a public URI to represent the file resource
						if(getSubrepository(childResourcePublicURI) == this) //if this child wouldn't be located in a subrepository (i.e. ignore resources obscured by subrepositories)
						{
							if(resourceFilter == null || resourceFilter.isPass(childResourcePublicURI)) //if we should include this resource based upon its URI
							{
								final URFResource childResourceDescription;
								try
								{
									childResourceDescription = createResourceDescription(urf, childResourcePublicURI, file); //create a resource description for this file
								}
								catch(final IOException ioException) //if an I/O exception occurs
								{
									throw toResourceIOException(getRepositoryResourceURI(toURI(file)), ioException); //translate the exception to a resource I/O exception and throw that, using a public URI to represent the file resource
								}
								if(resourceFilter == null || resourceFilter.isPass(childResourceDescription)) //if we should include this resource based upon its description
								{
									childResourceList.add(childResourceDescription); //add the resource to our list
									if(depth != 0 && file.isDirectory()) //if this file is a directory and we haven't reached the bottom
									{
										final int newDepth = depth != INFINITE_DEPTH ? depth - 1 : depth; //reduce the depth by one, unless we're using the unlimited depth value
										childResourceList.addAll(getChildResourceDescriptions(childResourcePublicURI, resourceFilter, newDepth)); //get a list of child descriptions for the resource we just created and add them to the list
									}
								}
							}
						}
					}
				}
				//aggregate any mapped subrepositories
				for(final Repository childSubrepository : getChildSubrepositories(resourceURI)) //see if any subrepositories are mapped as children of this repository
				{
					final URI childSubrepositoryURI = childSubrepository.getRootURI(); //get the URI of the subrepository
					childResourceList.add(childSubrepository.getResourceDescription(childSubrepositoryURI)); //get a description of the subrepository root resource
					if(depth == INFINITE_DEPTH || depth > 0) //if we should get child resources lower in the hierarchy
					{
						childResourceList.addAll(childSubrepository.getChildResourceDescriptions(childSubrepositoryURI, resourceFilter, depth == INFINITE_DEPTH ? depth
								: depth - 1)); //get descriptions of subrepository children
					}
				}
			}
			return childResourceList; //return the list of resources we constructed
		}
		else
		//if a depth of zero was requested
		{
			return emptyList(); //return an empty list
		}
	}

	/** {@inheritDoc} This implementation updates resource properties before storing the contents of the resource. */
	@Override
	protected OutputStream createResourceImpl(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
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
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource createResourceImpl(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents)
			throws ResourceIOException
	{
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
			if(resourceContents.length > 0 || !isCollectionURI(resourceURI)) //don't write empty content for a new collection
			{
				final OutputStream outputStream = new FileOutputStream(contentFile); //get an output stream to the file of the private URI
				try
				{
					outputStream.write(resourceContents); //write the resource contents to the file
				}
				finally
				{
					outputStream.close(); //always close the output stream
				}
			}
			return alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), resourceFile); //set the properties using the file
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/** {@inheritDoc} This implementation ignores requests to delete all resource for which {@link #isSourceResourceVisible(URI)} returns <code>false</code>. */
	@Override
	protected void deleteResourceImpl(final URI resourceURI) throws ResourceIOException
	{
		try
		{
			final URI sourceResourceURI = getSourceResourceURI(resourceURI);
			if(isSourceResourceVisible(sourceResourceURI)) //if this is a visible resource
			{
				final File resourceFile = new File(sourceResourceURI); //create a file object for the resource
				if(resourceFile.exists()) //if the file exists
				{
					delete(resourceFile, true); //recursively delete the file or directory
				}
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/** {@inheritDoc} */
	@Override
	protected URFResource alterResourcePropertiesImpl(final URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException
	{
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
	}

	/**
	 * {@inheritDoc} This implementation throws a {@link ResourceNotFoundException} for all resource for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>false</code>.
	 */
	@Override
	protected void copyResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException
	{
		if(!isSourceResourceVisible(getSourceResourceURI(resourceURI))) //if this is not a visible resource
		{
			throw new ResourceNotFoundException(resourceURI);
		}
		//perform all the copying manually, because using Files.copy() will not transfer any NTFS alternate data streams
		try
		{
			final File sourceFile = new File(getSourceResourceURI(resourceURI)); //create a file object for the source resource
			final File destinationFile = new File(getSourceResourceURI(destinationURI)); //create a file object for the destination resource
			//TODO add beginning and ending progress events, along with a system of levels
			if(sourceFile.isDirectory()) //directory
			{
				if(!overwrite)
				{
					if(destinationFile.isDirectory())
					{
						throw new ResourceStateException(destinationURI, "Destination resource already exists.");
					}
				}
				final File destinationParentFile = destinationFile.getParentFile(); //make sure the destination parent file exists
				if(destinationParentFile != null)
				{
					if(!destinationParentFile.isDirectory())
					{
						throw new ResourceStateException(destinationURI, "Parent of copy destination resource " + destinationURI + " does not exist.");
					}
				}
				copy(sourceFile, destinationFile, false, overwrite, progressListener); //copy the directory, but only make a shallow copy
				final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
				final File contentFile = new File(getSourceResourceURI(contentURI)); //create a file object from the private URI of the special collection content resource
				final URI destinationContentURI = resolve(destinationURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content at the destination
				final File destinationContentFile = new File(getSourceResourceURI(destinationContentURI)); //create a file object from the private URI of the special collection content resource at the destination
				if(contentFile.exists()) //if there is a special collection content resource
				{
					copy(contentFile, destinationFile, true, true); //copy over the collection content resource
				}
				else
				//if there is no collection content resource
				{
					if(destinationContentFile.exists()) //make sure the destination has no content file to match
					{
						delete(destinationContentFile);
					}
				}
				for(final File childSourceFile : sourceFile.listFiles(fileFilter)) //list all the files representing resources in the directory
				{
					final String filename = childSourceFile.getName();
					final File childDestinationFile = new File(destinationFile, filename);
					//copy each child
					copyResourceImpl(getRepositoryResourceURI(toURI(childSourceFile)),
							getRepositoryResourceURI(toURI(childDestinationFile, childSourceFile.isDirectory())), overwrite, progressListener); //make sure the destination URI correctly represents directories
				}
			}
			else
			//file
			{
				checkFileExists(sourceFile);
				if(!overwrite)
				{
					if(destinationFile.exists())
					{
						throw new ResourceStateException(destinationURI, "Destination resource already exists.");
					}
				}
				copy(sourceFile, destinationFile, false, overwrite, progressListener); //copy the file
			}
			final File sourceDescriptionFile = getResourceDescriptionFile(sourceFile); //get the file used for storing the description
			final File destinationDescriptionFile = getResourceDescriptionFile(destinationFile); //get the destination file used for storing the description
			if(sourceDescriptionFile.exists()) //if the source file has a description file
			{
				copy(sourceDescriptionFile, destinationDescriptionFile); //always copy over the description file---we don't want to risk that a single resource copy has an outdated description file that was already existing
			}
			else
			//if the source file has no description file
			{
				if(destinationDescriptionFile.exists()) //remove the destination description file if it exists (which might happen if we copy a resource without a description, overwriting a resource that had a description
				{
					delete(destinationDescriptionFile);
				}
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		/*this won't work on NTFS, because using Files.copy() doesn't transfer ADS
		final File sourceFile = new File(getSourceResourceURI(resourceURI)); //create a file object for the source resource
		final File destinationFile = new File(getSourceResourceURI(destinationURI)); //create a file object for the destination resource
		try
		{
			copy(sourceFile, destinationFile, overwrite, progressListener); //recursively copy the files/directories
			final File sourceDescriptionFile = getResourceDescriptionFile(sourceFile); //get the file used for storing the description
			final File destinationDescriptionFile = getResourceDescriptionFile(destinationFile); //get the destination file used for storing the description
			if(sourceDescriptionFile.exists()) //if the source file has a description file
			{
				copy(sourceDescriptionFile, destinationDescriptionFile); //always copy over the description file---we don't want to risk that a single resource copy has an outdated description file that was already existing
			}
			else
			//if the source file has no description file
			{
				if(destinationDescriptionFile.exists()) //remove the destination description file if it exists (which might happen if we copy a resource without a description, overwriting a resource that had a description
				{
					delete(destinationDescriptionFile);
				}
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
		*/
	}

	/**
	 * {@inheritDoc} This implementation throws a {@link ResourceNotFoundException} for all resource for which {@link #isSourceResourceVisible(URI)} returns
	 * <code>false</code>.
	 */
	@Override
	protected void moveResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException
	{
		if(!isSourceResourceVisible(getSourceResourceURI(resourceURI))) //if this is not a visible resource
		{
			throw new ResourceNotFoundException(resourceURI);
		}
		//TODO do something with progress listener
		final File sourceFile = new File(getSourceResourceURI(resourceURI)); //create a file object for the source resource
		final File destinationFile = new File(getSourceResourceURI(destinationURI)); //create a file object for the destination resource
		try
		{
			if(!overwrite) //if we shouldn't automatically overwrite destination resources
			{
				if(destinationFile.exists())
				{
					throw new ResourceStateException(destinationURI, "Destination resource already exists.");
				}
			}
			final File sourceDescriptionFile = getResourceDescriptionFile(sourceFile); //get the file used for storing the description
			final File destinationDescriptionFile = getResourceDescriptionFile(destinationFile); //get the destination file used for storing the description
			if(!sourceDescriptionFile.exists() && destinationDescriptionFile.exists()) //if the source file has no description file, delete any existing destination description file already existing
			{
				delete(destinationDescriptionFile); //this must be done before the move, because after the move there will be no source description file if the source description file was a stream of the source file
			}
			move(sourceFile, destinationFile, overwrite); //move the resource; this should move all contained description files and NTFS ADSs, if present
			if(sourceDescriptionFile.exists()) //if the source file still has a description file, it was a separate file
			{
				copy(sourceDescriptionFile, destinationDescriptionFile); //always copy over the description file---we don't want to risk that a single resource copy has an outdated description file that was already existing
				delete(sourceDescriptionFile); //do a separate copy/delete just in case File.renameTo() doesn't work with NTFS streams
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/**
	 * Creates a resource description to represent a single file.
	 * @param urf The URF data model to use when creating this resource.
	 * @param resourceURI The URI of the resource being described.
	 * @param resourceFile The file for which a resource should be created.
	 * @return A resource description of the given file.
	 * @throws FileNotFoundException if the given resource file does not exist.
	 * @throws IOException if there is an error creating the resource description.
	 * @throws IllegalArgumentException if a non-collection URI is given to access a directory.
	 */
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, final File resourceFile) throws IOException
	{
		checkFileExists(resourceFile);
		final URFResource resource = loadResourceDescription(urf, resourceFile); //load the resource description, if there is one
		long contentLength = 0; //we'll update the content length if we can
		URFDateTime contentModified = null; //we'll get the content modified from the file or, for a directory, from its content file, if any---but not from a directory itself
		if(resourceFile.isDirectory()) //if this is a directory
		{
			if(!isCollectionURI(resourceURI)) //if a non-collection URI was used for the directory
			{
				throw new IllegalArgumentException("Non-collection URI " + resourceURI + " used for directory " + resourceFile);
			}
			final URI contentURI = resolve(resourceURI, COLLECTION_CONTENT_NAME); //determine the URI to use for content
			final File contentFile = new File(getSourceResourceURI(contentURI)); //create a file object from the private URI of the special collection content resource
			if(contentFile.exists()) //if there is a special collection content resource
			{
				contentLength = contentFile.length(); //use the length of the special collection content resource
				contentModified = new URFDateTime(contentFile.lastModified()); //set the modified timestamp as the last modified date of the content file			
			}
		}
		else
		//if this file is not a directory
		{
			/*TODO fix
							//unescape any reserved characters in the filename and remove the extension
						final String label=FileUtilities.removeExtension(FileUtilities.decodeFilename(filename));
						addLabel(resource, label); //add the unescaped filename without an extension as a label
			*/
			contentLength = resourceFile.length(); //use the file length
			contentModified = new URFDateTime(resourceFile.lastModified()); //set the modified timestamp as the last modified date of the resource file			
			//TODO del			updateContentType(resource);	//update the content type information based upon the repository defaults
		}
		setContentLength(resource, contentLength); //indicate the length of the content
		if(contentModified != null) //if we have a content modified time
		{
			setModified(resource, contentModified); //set the modified timestamp as the last modified date
		}
		return resource; //return the resource that represents the file
	}

	/**
	 * Determines the file that holds the description of the given resource file. This version uses a separate distinct file beginning with the Unix hidden
	 * prefix, containing {@value #MARMOT_DESCRIPTION_NAME}, and ending with the extension for TURF files.
	 * @param resourceFile The file of a resource.
	 * @return A new file designating the location of the resource description.
	 */
	protected File getResourceDescriptionFile(final File resourceFile)
	{
		//TODO only use the UNIX hidden filename prefix for UNIX file systems---probably in a subclass
		if(resourceFile.isDirectory()) //if this is a directory
		{
			return new File(resourceFile, addExtension(addExtension(Files.UNIX_HIDDEN_FILENAME_PREFIX + COLLECTION_CONTENT_NAME, MARMOT_DESCRIPTION_NAME),
					NAME_EXTENSION)); //return a file in the form ".@.marmot-description.turf"
		}
		else
		//if this is not a directory
		{
			return changeName(resourceFile,
					addExtension(addExtension(Files.UNIX_HIDDEN_FILENAME_PREFIX + resourceFile.getName(), MARMOT_DESCRIPTION_NAME), NAME_EXTENSION)); //return a file in the form ".file.marmot-description.turf"
		}
	}

	/**
	 * Determines whether a resource, identified by its private URI, is a description file. This version checks for a file beginning with the Unix hidden prefix,
	 * containing {@value #MARMOT_DESCRIPTION_NAME}, and ending with the extension for TURF files.
	 * @param privateResourceURI The private URI of a resource.
	 * @return <code>true</code> if the resource is a description file for another resource.
	 * @throws NullPointerException if the given URI is <code>null</code>.
	 */
	protected boolean isSourceResourceDescription(final URI privateResourceURI)
	{
		if(!isCollectionURI(privateResourceURI)) //description files are not collections
		{
			final String name = URIs.getName(privateResourceURI);
			return name != null && name.startsWith(Files.UNIX_HIDDEN_FILENAME_PREFIX)
					&& name.endsWith(MARMOT_DESCRIPTION_NAME + NAME_EXTENSION_SEPARATOR + NAME_EXTENSION); //see if the name matches the pattern TODO use a regex pattern
		}
		return false;
	}

	/**
	 * {@inheritDoc} This version removes the following resources from the public space:
	 * <ul>
	 * <li>Any resource description file, as determined by {@link #isSourceResourceDescription(URI)}.</li>
	 * </ul>
	 * @see #isSourceResourceDescription(URI)
	 */
	@Override
	protected boolean isSourceResourceVisible(final URI privateResourceURI)
	{
		if(isSourceResourceDescription(privateResourceURI)) //if this is a URI for a resource description
		{
			return false; //the resource isn't public
		}
		return super.isSourceResourceVisible(privateResourceURI); //do the default checks
	}

	/**
	 * Loads a resource description for a single file.
	 * @param urf The URF data model to use when creating this resource.
	 * @param resourceFile The file of a resource.
	 * @return A resource description of the given file.
	 * @throws IOException if there is an error loading the resource description.
	 * @see #getResourceDescriptionFile(File)
	 */
	protected URFResource loadResourceDescription(final URF urf, final File resourceFile) throws IOException
	{
		final URI resourceURI = getRepositoryResourceURI(toURI(resourceFile)); //get a public URI to represent the file resource
		final File resourceDescriptionFile = getResourceDescriptionFile(resourceFile); //get the file for storing the description
		try
		{
			final URFResource resource;
			if(resourceDescriptionFile.exists()) //if there is a description file
			{
				resource = URFFiles.read(resourceDescriptionFile, urf, resourceURI, getDescriptionIO()); //read the description using the given URF instance, using the resource URI as the base URI
			}
			else
			//if there is no description file
			{
				resource = urf.createResource(resourceURI); //create a default resource description
			}
			return resource; //return the resource description
		}
		catch(final IOException ioException) //if an error occurs
		{
			throw new IOException("Error reading resource description from " + resourceDescriptionFile, ioException);
		}
	}

	/**
	 * Saves a resource description for a single file. Live properties, including {@value Content#CREATED_PROPERTY_URI}, are ignored. If the
	 * {@value Content#MODIFIED_PROPERTY_URI} property is present, it is not saved and the file modified time is updated to match that value. If the resource
	 * description file does not exist and there are no properties to save, no resource description file is created.
	 * <p>
	 * If the {@link Content#MODIFIED_PROPERTY_URI} property is being set/added, all previous values are ignored (i.e. the {@link Content#MODIFIED_PROPERTY_URI}
	 * is always considered to be <em>set</em>, and never <em>add</em>).
	 * </p>
	 * @param resourceDescription The resource description to save; the resource URI is ignored.
	 * @param resourceFile The file of a resource.
	 * @throws IOException if there is an error save the resource description.
	 * @see #getResourceDescriptionFile(File)
	 */
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
		final File resourceDescriptionFile = getResourceDescriptionFile(resourceFile); //get the file for storing the description
		if(resourceDescription.hasProperties() || resourceDescriptionFile.exists()) //if there are any properties to set (otherwise, don't create an empty properties file) or the description file already exists
		{
			try
			{
				URFFiles.write(resourceDescriptionFile, resourceURI, resourceDescription, getDescriptionIO()); //write the description, using the resource URI as the base URI
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

	/**
	 * {@inheritDoc} This version makes the following translations:
	 * <dl>
	 * <dt>{@link FileNotFoundException}</dt>
	 * <dd>{@link ResourceNotFoundException}</dd>
	 * </dl>
	 */
	@Override
	protected ResourceIOException toResourceIOException(final URI resourceURI, final Throwable throwable)
	{
		if(throwable instanceof FileNotFoundException)
		{
			return new ResourceNotFoundException(resourceURI, throwable);
		}
		else
		//if this is not one of our specially-handled exceptions
		{
			return super.toResourceIOException(resourceURI, throwable); //convert the exception normally
		}
	}

	/**
	 * Creates an output stream that saves the properties of a file after its contents are stored.
	 * @see FileRepository#saveResourceDescription(URFResource, File)
	 * @author Garret Wilson
	 */
	protected class DescriptionWriterOutputStreamDecorator extends OutputStreamDecorator<OutputStream>
	{
		/** The description of the resource to store. */
		private final URFResource resourceDescription;

		/** @return The description of the resource. */
		protected URFResource getResourceDescription()
		{
			return resourceDescription;
		}

		/** The file for updating the properties. */
		private final File resourceFile;

		/** @return The file for updating the properties. */
		protected File getResourceFile()
		{
			return resourceFile;
		}

		/**
		 * Decorates the given output stream.
		 * @param outputStream The output stream to decorate
		 * @param resourceDescription The description of the resource to store; the URI of the description is ignored.
		 * @param resourceFile The file for updating the properties.
		 * @throws NullPointerException if the given output stream, resourceURI, resource description, and/or resource file is <code>null</code>.
		 */
		public DescriptionWriterOutputStreamDecorator(final OutputStream outputStream, final URFResource resourceDescription, final File resourceFile)
		{
			super(outputStream); //construct the parent class
			this.resourceDescription = checkInstance(resourceDescription, "Resource description cannot be null.");
			this.resourceFile = checkInstance(resourceFile, "Resource file cannot be null.");
		}

		/**
		 * Called after the stream is successfully closed. This version updates the file properties to reflect the given resource description.
		 * @throws ResourceIOException if an I/O error occurs.
		 */
		protected void afterClose() throws ResourceIOException
		{
			try
			{
				saveResourceDescription(getResourceDescription(), getResourceFile()); //save the resource description
			}
			catch(final IOException ioException) //if an I/O exception occurs
			{
				throw toResourceIOException(getRepositoryResourceURI(toURI(getResourceFile())), ioException); //translate the exception to a resource I/O exception and throw that
			}
		}

	}
}

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

package com.globalmentor.marmot.repository.file;

import java.io.*;
import java.net.URI;
import java.util.*;

import static java.util.Arrays.fill;
import static java.util.Collections.*;

import static com.globalmentor.io.FileConstants.*;
import static com.globalmentor.io.Files.*;
import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.Objects.checkInstance;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;

import com.globalmentor.io.*;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.net.*;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.Content;

import static com.globalmentor.urf.TURF.*;

/**Repository stored in a filesystem.
<p>This repository recognizes the URF type <code>urf.List</code>
	and creates a directory for each such resource. Directories will be created
	transparently for other resources with other types and media types as needed
	to store child resources.</p>
<p>This implementation uses the file last modified timestamp to store the {@value Content#MODIFIED_PROPERTY_URI} property.</p>
@author Garret Wilson
*/
public class FileRepository extends AbstractRepository	//TODO fix content length for collections
{

		//TODO encode special characters, especially the colon on NTFS

	/**The URI represting the XPackage file:folder type.*/	//TODO check; use static imports 
//TODO move if needed	protected final static URI FILE_FOLDER_TYPE_URI=RDFUtilities.createReferenceURI(FileOntologyConstants.FILE_ONTOLOGY_NAMESPACE_URI, FileOntologyConstants.FOLDER_TYPE_NAME);	//TODO promote to parent file-based class		

	/**The extension used for directories to hold resource children.*/
//TODO move if needed	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class

	/**The name component of the Marmot description of a file resource.*/
	public final static String MARMOT_DESCRIPTION_NAME="marmot-description";

	/**The file filter for listing files in a directory.
	The file filter returns those resources for which {@link #isPrivateResourcePublic(URI)} returns <code>true</code>.
	*/
	private final FileFilter fileFilter=new FileFilter()
			{
				/**Tests whether or not the specified abstract pathname is one of the file types we recognize.
				This implementation delegates to #is
				@param pathname The abstract pathname to be tested.
				@return <code>true</code> if and only if <code>pathname</code> should be included.
				*/
				public boolean accept(final File pathname)
				{
					return isPrivateResourcePublic(pathname.toURI());	//see if the private URI represented by this file should be accepted 
				}
			};
		
	/**Returns the file filter for listing files in a directory.
	The file filter returns those resources for which {@link #isPrivateResourcePublic(URI)} returns <code>true</code>.
	@return The file filter for listing files in a directory.
	*/
	protected FileFilter getFileFilter() {return fileFilter;}

	/**Default constructor with no settings.
	Settings must be configured before repository is opened.
	*/
/*TODO del if not needed
	public FileRepository()
	{
	}
*/

	/**File contructor with no separate private URI namespace.
	@param repositoryDirectory The file identifying the directory of this repository.
	@exception NullPointerException if the given respository directory is <code>null</code>.
	*/
	public FileRepository(final File repositoryDirectory)
	{
		this(getDirectoryURI(repositoryDirectory));	//get a directory URI from the repository directory and use it as the base repository URI
	}

	/**URI contructor with no separate private URI namespace.
	The given repository URI should end in a slash.
	@param repositoryURI The URI identifying the location of this repository.
	@exception NullPointerException if the given respository URI is <code>null</code>.
	@exception IllegalArgumentException if the repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	*/
	public FileRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository directory contructor.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryDirectory The file identifying the private directory of the repository.
	@exception NullPointerException if the given respository URI and/or the given directory is <code>null</code>.
	*/
	public FileRepository(final URI publicRepositoryURI, final File privateRepositoryDirectory)
	{
		this(publicRepositoryURI, getDirectoryURI(privateRepositoryDirectory));	//get a directory URI from the private repository directory and use it as the base repository URI
	}

	/**Public repository URI and private repository URI contructor.
	The given private repository URI should end in a slash.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@exception NullPointerException if one of the given respository URIs is <code>null</code>.
//TODO relax; improve	@exception IllegalArgumentException if the private repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	*/
	public FileRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		super(publicRepositoryURI, privateRepositoryURI);	//construct the parent class
/*TODO decide if how we want initialization to occur, especially using PLOOP
		if(!FILE_SCHEME.equals(privateRepositoryURI.getScheme()))	//if the private respository URI scheme is not the file scheme
		{
			throw new IllegalArgumentException(privateRepositoryURI+" does not use the "+FILE_SCHEME+" URI scheme.");
		}
*/
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO=(URFResourceTURFIO<URFResource>)getDescriptionIO();	//get the description I/O
		urfResourceDescriptionIO.setFormatted(true);	//turn on formatting
	}

	/**Creates a repository of the same type as this repository with the same access privileges as this one.
	This factory method is commonly used to use a parent repository as a factory for other repositories in its namespace.
	@param publicRepositoryURI The public URI identifying the location of the new repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@throws NullPointerException if the given public repository URI and/or private repository URI is <code>null</code>.
	*/
	protected Repository createSubrepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		return new FileRepository(publicRepositoryURI, privateRepositoryURI);	//create and return a new file repository
	}

	/**Gets an input stream to the contents of the resource specified by the given URI.
	For collections, this implementation retrieves the content of the {@value #COLLECTION_CONTENT_NAME} file, if any.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getResourceInputStream(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getResourceInputStream(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		try
		{
			if(isCollectionURI(resourceURI) && isCollection(resourceURI))	//if the resource is a collection (make sure the resource URI is also a collection URI so that we can be sure of resolving the collection contents name; file collections should only have collection URIs anyway)
			{
				final URI contentURI=resourceURI.resolve(COLLECTION_CONTENT_NAME);	//determine the URI to use for content
				final File contentFile=new File(getPrivateURI(contentURI));	//create a file object from the private URI of the special collection content resource
				if(contentFile.exists())	//if there is a special collection content resource
				{
					return new FileInputStream(contentFile);	//return an input stream to the file
				}
				else	//if there is no collection content resource
				{
					return new ByteArrayInputStream(NO_BYTES);	//return an input stream to an empty byte array
				}
			}
			else	//if the resource is not a collection
			{
				final File file=new File(getPrivateURI(resourceURI));	//create a file object from the private URI
				return new FileInputStream(file);	//return an input stream to the file
			}
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Gets an output stream to the contents of the resource specified by the given URI.
	An error is generated if the resource does not exist.
	For collections, this implementation sets the content of the {@value #COLLECTION_CONTENT_NAME} file, if any.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(URI resourceURI) throws ResourceIOException	//TODO fix to update modified date
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getResourceOutputStream(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		try
		{
			final File resourceFile=new File(getPrivateURI(resourceURI));	//get a file for the resource
			if(!resourceFile.exists())	//if the file doesn't exist
			{
				throw new FileNotFoundException("Cannot open output stream to non-existent file "+resourceFile+" in repository.");
			}
			final File contentFile;	//determine the file to use for storing content
			if(isCollectionURI(resourceURI) && isCollection(resourceURI))	//if the resource is a collection (make sure the resource URI is also a collection URI so that we can be sure of resolving the collection content name; file collections should only have collection URIs anyway)
			{
				final URI contentURI=resourceURI.resolve(COLLECTION_CONTENT_NAME);	//determine the URI to use for content
				contentFile=new File(getPrivateURI(contentURI));	//create a file object from the private URI of the special collection content resource
			}
			else	//if the resource is not a collection
			{
				contentFile=resourceFile;	//use the normal resource file
			}
			return getResourceOutputStream(resourceURI, contentFile);	//return an output stream to the content file
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Gets an output stream to the contents of the given resource file.
	This version returns a direct output stream to the file. 
	@param resourceURI The URI of the resource to access.
	@param resourceFile The file representing the resource.
	@return An output stream to the resource represented by given resource file.
	@exception IOException if there is an error accessing the resource.
	*/
	protected OutputStream getResourceOutputStream(final URI resourceURI, final File resourceFile) throws IOException
	{
		return new FileOutputStream(resourceFile);	//return an output stream to the file
	}

	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URFResource getResourceDescription(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getResourceDescription(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final URF urf=createURF();	//create a new URF data model
		try
		{
			return createResourceDescription(urf, resourceURI, new File(getPrivateURI(resourceURI)));	//create and return a description from a file created from the URI from the private namespace
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Determines if the resource at the given URI exists.
	This implementation returns <code>false</code> for all resources for which {@link #isPrivateResourcePublic(URI)} returns <code>false</code>.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean resourceExists(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.resourceExists(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final URI privateResourceURI=getPrivateURI(resourceURI);	//get the resource URI in the private space
		if(!isPrivateResourcePublic(privateResourceURI))	//if this resource should not be public
		{
			return false;	//ignore this resource
		}
		final boolean isCollectionURI=isCollectionURI(resourceURI);	//see if the URI specifies a collection
		final File file=new File(privateResourceURI);	//get the file object for this resource
		return file.exists() && (isCollectionURI || !file.isDirectory());	//see if the file of the private URI exists; don't allow a non-collection URI to find a non-directory URI, though (file systems usually don't allow both a file and a directory of the same name, so they allow the ending-slash form to be optional)
	}

	/**Determines if the resource at a given URI is a collection.
	This implementation returns <code>false</code> for all resources for which {@link #isPrivateResourcePublic(URI)} returns <code>false</code>.
	@param resourceURI The URI of the requested resource.
	@return <code>true</code> if the resource is a collection, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean isCollection(URI resourceURI) throws ResourceIOException
  {
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.isCollection(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final URI privateResourceURI=getPrivateURI(resourceURI);	//get the resource URI in the private space
		if(!isPrivateResourcePublic(privateResourceURI))	//if this resource should not be public
		{
			return false;	//ignore this resource
		}
		return isCollectionURI(resourceURI) && new File(privateResourceURI).isDirectory();	//see if the file of the private URI is a directory; don't allow a non-collection URI to find a non-directory URI, though (file systems usually don't allow both a file and a directory of the same name, so they allow the ending-slash form to be optional)
  }

	/**Determines whether the resource represented by the given URI has children.
	This implementation ignores child resources for which {@link #isPrivateResourcePublic(URI)} returns <code>false</code>.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.hasChildren(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		return isCollectionURI(resourceURI) && resourceFile.isDirectory() && resourceFile.listFiles(getFileFilter()).length>0;	//see if this is a directory and there is more than one file in this directory
	}

	/**Retrieves child resources of the resource at the given URI.
	This implementation does not include child resources for which {@link #isPrivateResourcePublic(URI)} returns <code>false</code>.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be filtered.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(URI resourceURI, final ResourceFilter resourceFilter, final int depth) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getChildResourceDescriptions(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		if(depth!=0)	//a depth of zero means don't get child resources
		{
			final File resourceDirectory=new File(getPrivateURI(resourceURI));	//create a file object for the resource
			final List<URFResource> childResourceList=new ArrayList<URFResource>();	//create a list to hold the files that are not directories	
			if(isCollectionURI(resourceURI) && resourceDirectory.isDirectory())	//if there is a directory for this resource
			{
				final URF urf=createURF();	//create a new URF data model
				final File[] files=resourceDirectory.listFiles(getFileFilter());	//get a list of all files in the directory
				for(final File file:files)	//for each file in the directory
				{
					final URI childResourcePublicURI=getPublicURI(file.toURI());	//get a public URI to represent the file resource
					if(getSubrepository(childResourcePublicURI)==this)	//if this child wouldn't be located in a subrepository (i.e. ignore resources obscured by subrepositories)
					{
						if(resourceFilter==null || resourceFilter.isPass(childResourcePublicURI))	//if we should include this resource based upon its URI
						{
							final URFResource childResourceDescription;
							try
							{
								childResourceDescription=createResourceDescription(urf, childResourcePublicURI, file);	//create a resource description for this file
							}
							catch(final IOException ioException)	//if an I/O exception occurs
							{
								throw createResourceIOException(getPublicURI(file.toURI()), ioException);	//translate the exception to a resource I/O exception and throw that, using a public URI to represent the file resource
							}
							if(resourceFilter==null || resourceFilter.isPass(childResourceDescription))	//if we should include this resource based upon its description
							{
								childResourceList.add(childResourceDescription);	//add the resource to our list
								if(depth!=0 && file.isDirectory())	//if this file is a directory and we haven't reached the bottom
								{
									final int newDepth=depth>0 ? depth-1 : depth;	//reduce the depth by one, unless we're using the unlimited depth value
									childResourceList.addAll(getChildResourceDescriptions(childResourcePublicURI, resourceFilter, newDepth));	//get a list of child descriptions for the resource we just created and add them to the list
								}
							}
						}
					}
				}
					//aggregate any mapped subrepositories
				for(final Repository childSubrepository:getChildSubrepositories(resourceURI))	//see if any subrepositories are mapped as children of this repository
				{
					final URI childSubrepositoryURI=childSubrepository.getURI();	//get the URI of the subrepository
					childResourceList.add(childSubrepository.getResourceDescription(childSubrepositoryURI));	//get a description of the subrepository root resource
					if(depth==-1 || depth>0)	//if we should get child resources lower in the hierarchy
					{
						childResourceList.addAll(childSubrepository.getChildResourceDescriptions(childSubrepositoryURI, resourceFilter, depth==-1 ? depth : depth-1));	//get descriptions of subrepository children
					}
				}
			}
			return childResourceList;	//return the list of resources we constructed
		}
		else	//if a depth of zero was requested
		{
			return emptyList();	//return an empty list
		}
	}

	/**Creates a new resource with the given description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
	If not already present in the given description, the {@link Content#CREATED_PROPERTY_URI} property will be added with the current date and time.
	If not already present in the given description, the {@link Content#MODIFIED_PROPERTY_URI} property will be added with the current date and time.
	If a resource with no contents is desired, {@link #createResource(URI, URFResource, byte[])} with zero bytes is better suited for this task.
	This implementation updates resource properties before storing the contents of the resource.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public OutputStream createResource(URI resourceURI, URFResource resourceDescription) throws ResourceIOException	//TODO fix to prevent resources with special names
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createResource(resourceURI, resourceDescription);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		try
		{
			final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
			if(resourceFile.exists())	//if the resource file already exists (either as a file or a directory)
			{
				delete(resourceFile, true);	//delete the file/directory and all its children, if any
			}
			final File contentFile;	//determine the file to use for storing content
			if(isCollectionURI(resourceURI))	//if the resource is a collection
			{
				final URI contentURI=resourceURI.resolve(COLLECTION_CONTENT_NAME);	//determine the URI to use for content
				contentFile=new File(getPrivateURI(contentURI));	//create a file object from the private URI of the special collection content resource
				mkdir(resourceFile);	//create the directory
			}
			else	//if the resource is not a collection
			{
				contentFile=resourceFile;	//use the normal resource file
				//TODO should we see if a directory exists?
				createNewFile(resourceFile);	//create a new file
			}
			resourceDescription=ensureModifiedProperties(resourceDescription);	//add the content modified properties as needed
			return new DescriptionWriterOutputStreamDecorator(new FileOutputStream(contentFile, true), resourceURI, resourceDescription, resourceFile);	//wrap the output stream to the content file in a decorator that will update the properties after the contents are stored
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Creates a new resource with the given description and contents.
	If a resource already exists at the given URI it will be replaced.
	If not already present in the given description, the {@link Content#CREATED_PROPERTY_URI} property will be added with the current date and time.
	If not already present in the given description, the {@link Content#MODIFIED_PROPERTY_URI} property will be added with the current date and time.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(URI resourceURI, URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException	//TODO fix to prevent resources with special names
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createResource(resourceURI, resourceDescription, resourceContents);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		try
		{
			final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
			if(resourceFile.exists())	//if the resource file already exists (either as a file or a directory)
			{
				delete(resourceFile, true);	//delete the file/directory and all its children, if any
			}
			final File contentFile;	//determine the file to use for storing content
			if(isCollectionURI(resourceURI))	//if the resource is a collection
			{
				final URI contentURI=resourceURI.resolve(COLLECTION_CONTENT_NAME);	//determine the URI to use for content
				contentFile=new File(getPrivateURI(contentURI));	//create a file object from the private URI of the special collection content resource
				mkdir(resourceFile);	//create the directory
			}
			else	//if the resource is not a collection
			{
				contentFile=resourceFile;	//use the normal resource file
				//TODO should we see if a directory exists?
				createNewFile(resourceFile);	//create a new file
			}
			if(resourceContents.length>0 || !isCollectionURI(resourceURI))	//don't write empty content for a new collection
			{
				final OutputStream outputStream=new FileOutputStream(contentFile);	//get an output stream to the file of the private URI
				try
				{
					outputStream.write(resourceContents);	//write the resource contents to the file
				}
				finally
				{
					outputStream.close();	//always close the output stream
				}
			}
			resourceDescription=ensureModifiedProperties(resourceDescription);	//add the content modified properties as needed
  		return alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), resourceFile);	//set the properties using the file
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Alters properties of a given resource.
	@param resourceURI The reference URI of the resource.
	@param resourceAlteration The specification of the alterations to be performed on the resource.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource alteration is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be altered.
	*/
	public URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.alterResourceProperties(resourceURI, resourceAlteration);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		return alterResourceProperties(resourceURI, resourceAlteration, new File(getPrivateURI(resourceURI)));	//create a file object and alter the properties for the file
	}

	/**Alters properties of a given resource.
	Live properties are ignored.
	@param resourceURI The reference URI of the resource.
	@param resourceAlteration The specification of the alterations to be performed on the resource.
	@param resourceFile The file to use in updating the resource properties.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI, resource alteration, and/or resource file is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be altered.
	*/
	protected URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration, final File resourceFile) throws ResourceIOException
	{
		final URF urf=createURF();	//create a new URF data model
		try
		{
			final URFResource resourceDescription=createResourceDescription(urf, resourceURI, resourceFile);	//get a description from a file created from the URI from the private namespace
			resourceDescription.alter(resourceAlteration);	//alter the resource according to the specification
			final URFResource tempDescription=new DefaultURFResource(resourceDescription);	//create a temporary resource so that we can remove the live properties
			for(final URI livePropertyURI:getLivePropertyURIs())	//look at all live properties
			{
				tempDescription.removePropertyValues(livePropertyURI);	//remove all values for this live property
			}
			saveResourceDescription(tempDescription, resourceFile);	//save the resource description
			return resourceDescription;	//return the updated description
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}
	
	/**Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destionation only if requested. 
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void copyResource(URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.copyResource(resourceURI, destinationURI, overwrite);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		throw new UnsupportedOperationException();	//TODO implement
	}

	/**Deletes a resource.
	@param resourceURI The reference URI of the resource to delete.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if the resource could not be deleted.
	*/
	public void deleteResource(URI resourceURI) throws ResourceIOException	//TODO fix to prevent resources with special names
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.deleteResource(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		try
		{
			if(resourceURI.normalize().equals(getPublicRepositoryURI()))	//if they try to delete the root URI
			{
				throw new IllegalArgumentException("Cannot delete repository base URI "+resourceURI);
			}
			final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
			/*TODO del any associated directories
			if(resourceFile.isFile())	//if this is a file and not a directory
			{
				final File directory=getResourceDirectory(resourceURI);	//get the directory to use for the URI
				if(directory.exists())	//if a directory exists for this resource
				{
					FileUtilities.delete(directory, true);	//recursively delete the directory						
				}
			}
	*/
			delete(resourceFile, true);	//recursively delete the file or directory
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Moves a resource to another URI in this repository, overwriting any resource at the destionation only if requested.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void moveResource(URI resourceURI, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.moveResource(resourceURI, destinationURI, overwrite);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		if(resourceURI.normalize().equals(getPublicRepositoryURI()))	//if they try to move the root URI
		{
			throw new IllegalArgumentException("Cannot move repository base URI "+resourceURI);
		}
		throw new UnsupportedOperationException();	//TODO implement
		
		//TODO move resource description if needed
	}

	/**Creates a resource description to represent a single file.
	@param urf The URF data model to use when creating this resource.
	@param resourceURI The URI of the resource being described.
	@param resourceFile The file for which a resource should be created.
	@return A resource description of the given file.
	@exception IOException if there is an error creating the resource description.
	*/
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, final File resourceFile) throws IOException
	{
		final URFResource resource=loadResourceDescription(urf, resourceFile);	//load the resource description, if there is one
		final String filename=resourceFile.getName();	//get the name of the file
		long contentLength=0;	//we'll update the content length if we can
		URFDateTime contentModified=new URFDateTime(resourceFile.lastModified());	//get the last modified date of the file; we'll update this if needed
		if(resourceFile.isDirectory())	//if this is a directory
		{
			if(isCollection(resourceURI))	//if the resource is a collection (make sure the resource URI is also a collection URI so that we can be sure of resolving the collection contents name; file collections should only have collection URIs anyway)
			{
				final URI contentURI=resourceURI.resolve(COLLECTION_CONTENT_NAME);	//determine the URI to use for content
				final File contentFile=new File(getPrivateURI(contentURI));	//create a file object from the private URI of the special collection content resource
				if(contentFile.exists())	//if there is a special collection content resource
				{
					contentLength=contentFile.length();	//use the length of the special collection content resource
					contentModified=new URFDateTime(contentFile.lastModified());	//set the modified timestamp as the last modified date of the content file			
				}
			}
		}
		else	//if this file is not a directory
		{
/*TODO fix
				//unescape any reserved characters in the filename and remove the extension
			final String label=FileUtilities.removeExtension(FileUtilities.decodeFilename(filename));
			addLabel(resource, label); //add the unescaped filename without an extension as a label
*/
			contentLength=resourceFile.length();	//use the file length
//TODO del			updateContentType(resource);	//update the content type information based upon the repository defaults
		}
		setContentLength(resource, contentLength);	//indicate the length of the content
		setModified(resource, contentModified);	//set the modified timestamp as the last modified date
		final URFDateTime created=getCreated(resource);	//try to determine the creation date and time; the stored creation time will always trump everything else
		if(created==null)	//if there is no creation date
		{
			setCreated(resource, contentModified);	//set the created time as the last modified date of the file, as Java doesn't allow access to the creation time
		}
		return resource;	//return the resource that respresents the file
	}

	/**Determines the file that holds the description of the given resource file.
	This version uses a separate destinct file beginning with the Unix hidden prefix,
	containing {@value #MARMOT_DESCRIPTION_NAME},	and ending with the extension for TURF files.
	@param resourceFile The file of a resource.
	@return A new file designating the location of the resource description.
	*/
	protected File getResourceDescriptionFile(final File resourceFile)	//TODO update isPrivateResourcePublic() to hide these description files
	{
			//TODO only use the UNIX hidden filename prefix for UNIX file systems---probably in a subclass
			//TODO check to see if this is a directory and, if so, use the format "@.marmot-description.turf"
		return changeName(resourceFile, addExtension(UNIX_HIDDEN_FILENAME_PREFIX+resourceFile.getName()+EXTENSION_SEPARATOR+MARMOT_DESCRIPTION_NAME, TURF_NAME_EXTENSION));	//return a file in the form ".file.marmot-description.turf"
	}

	/**Loads a resource description for a single file.
	@param urf The URF data model to use when creating this resource.
	@param resourceFile The file of a resource.
	@return A resource description of the given file.
	@exception IOException if there is an error loading the resource description.
	@see #getResourceDescriptionFile(File)
	*/
	protected URFResource loadResourceDescription(final URF urf, final File resourceFile) throws IOException
	{
		final URI resourceURI=getPublicURI(resourceFile.toURI());	//get a public URI to represent the file resource
		final File resourceDescriptionFile=getResourceDescriptionFile(resourceFile);	//get the file for storing the description
		try
		{
			final URFResource resource;
			if(resourceDescriptionFile.exists())	//if there is a description file
			{
				resource=Files.read(resourceDescriptionFile, urf, resourceURI, getDescriptionIO());	//read the description using the given URF instance, using the resource URI as the base URI
			}
			else	//if there is no description file
			{
				resource=urf.createResource(resourceURI); //create a default resource description
			}
			return resource;	//return the resource description
		}
		catch(final IOException ioException)	//if an error occurs
		{
			throw new IOException("Error reading resource description from "+resourceDescriptionFile, ioException);
		}
	}

	/**Saves a resource description for a single file.
	Except for as noted below, the description is saved as given with no modifications.
	If the {@value Content#MODIFIED_PROPERTY_URI} property is present, it is not saved and the file modified time is updated to match that value.
	If the {@value Content#CREATED_PROPERTY_URI} property is present and it is identical to the {@value Content#MODIFIED_PROPERTY_URI} property, it is not saved.
	If the resource description file does not exist and there are no properties to save, no resource description file is created.
	@param resourceDescription The resource description to save.
	@param resourceFile The file of a resource.
	@exception IOException if there is an error save the resource description.
	@see #getResourceDescriptionFile(File)
	*/
	protected void saveResourceDescription(URFResource resourceDescription, final File resourceFile) throws IOException
	{
		final URFDateTime modified=getModified(resourceDescription);	//see if the description indicates the last modified time
		if(modified!=null)	//if the last modified time was indicated
		{
			resourceDescription=new DefaultURFResource(resourceDescription);	//create a copy of the description
			resourceDescription.removePropertyValues(Content.MODIFIED_PROPERTY_URI);	//remove all last-modified values from the desciption we'll actually save
			final URFDateTime created=getCreated(resourceDescription);	//see if the description indicates the created date
			if(created!=null && created.getTime()==modified.getTime())	//if the created time is the same as the modified time
			{
				resourceDescription.removePropertyValues(Content.CREATED_PROPERTY_URI);	//remove all created timestamp values from the desciption to save, as Java can't distinguish between content created and modified and they'll both be initialized from the same value, anyway, when reading
			}
		}
		final URI resourceURI=getPublicURI(resourceFile.toURI());	//get a public URI to represent the file resource
		final File resourceDescriptionFile=getResourceDescriptionFile(resourceFile);	//get the file for storing the description
		if(resourceDescription.hasProperties() || resourceDescriptionFile.exists())	//if there are any properties to set (otherwise, don't create an empty properties file) or the description file already exists
		{
			try
			{
				Files.write(resourceDescriptionFile, resourceURI, resourceDescription, getDescriptionIO());	//write the description, using the resource URI as the base URI
			}
			catch(final IOException ioException)	//if an error occurs
			{
				throw new IOException("Error writing resource description to "+resourceDescriptionFile, ioException);
			}
		}
		if(modified!=null)	//if a modification timestamp was indicated
		{
			final File contentFile;	//determine the file to use for storing content
			if(isCollectionURI(resourceURI))	//if the resource is a collection
			{
				final URI contentURI=resourceURI.resolve(COLLECTION_CONTENT_NAME);	//determine the URI to use for content
				final File tempContentFile=new File(getPrivateURI(contentURI));	//create a file object from the private URI of the special collection content resource
				contentFile=tempContentFile.exists() ? tempContentFile : resourceFile;	//if the content file doesn't exist, we can't update its modified time
			}
			else	//if the resource is not a collection
			{
				contentFile=resourceFile;	//use the normal resource file
			}
			if(!contentFile.setLastModified(modified.getTime()))	//update the content file's record of the last modified time
			{
				throw new IOException("Error updating content modified time of "+resourceFile);
			}
		}
	}

	/**Translates the given error specific to this repository type into a resource I/O exception.
	This version makes the following translations:
	<dl>
		<dt>{@link FileNotFoundException}</dt> <dd>{@link ResourceNotFoundException}</dd>
	</dl>
	@param resourceURI The URI of the resource to which the exception is related.
	@param throwable The error which should be translated to a resource I/O exception.
	@return A resource I/O exception based upon the given throwable.
	*/
	protected ResourceIOException createResourceIOException(final URI resourceURI, final Throwable throwable) 
	{
		if(throwable instanceof FileNotFoundException)
		{
			return new ResourceNotFoundException(resourceURI, throwable);
		}
		else	//if this is not one of our specially-handled exceptions
		{
			return super.createResourceIOException(resourceURI, throwable);	//convert the exceptoin normally
		}
	}

	/**Creates an output stream that updates the properties of a file after its contents are stored.
	@author Garret Wilson
	*/
	protected class DescriptionWriterOutputStreamDecorator extends OutputStreamDecorator<OutputStream>
	{
		/**The URI of the resource.*/
		private final URI resourceURI;

			/**@protected The URI of the resource.*/
			public URI getResourceURI() {return resourceURI;}
	
		/**The description of the resource to store.*/
		private final URFResource resourceDescription;

			/**@return The description of the resource.*/
			protected URFResource getResourceDescription() {return resourceDescription;}

		/**The file for updating the properties.*/
		private final File resourceFile;

			/**@return The file for updating the properties.*/
			protected File getResourceFile() {return resourceFile;}

		/**Decorates the given output stream.
		@param outputStream The output stream to decorate
		@param resourceURI The URI of the resource.
		@param resourceDescription The description of the resource to store as WebDAV properties.
		@param resourceFile The file for updating the properties.
		@exception NullPointerException if the given output stream, resourceURI, resource description, and/or resource file is <code>null</code>.
		*/
		public DescriptionWriterOutputStreamDecorator(final OutputStream outputStream, final URI resourceURI, final URFResource resourceDescription, final File resourceFile)
		{
			super(outputStream);	//construct the parent class
			this.resourceURI=checkInstance(resourceURI, "Resource URI cannot be null.");
			this.resourceDescription=checkInstance(resourceDescription, "Resource description cannot be null.");
			this.resourceFile=checkInstance(resourceFile, "Resource file cannot be null.");
		}
	
	  /**Called after the stream is successfully closed.
		This version updates the WebDAV properties to reflect the given resource description.
		@exception ResourceIOException if an I/O error occurs.
		*/
	  protected void afterClose() throws ResourceIOException
	  {
	  	try
	  	{
	  		alterResourceProperties(getResourceURI(), DefaultURFResourceAlteration.createResourceAlteration(getResourceDescription()), resourceFile);	//set the properties using the resource file
			}
			catch(final IOException ioException)	//if an I/O exception occurs
			{
				throw createResourceIOException(getResourceURI(), ioException);	//translate the exception to a resource I/O exception and throw that
			}
	  }

	}
}

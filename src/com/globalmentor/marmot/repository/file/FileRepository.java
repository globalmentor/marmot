package com.globalmentor.marmot.repository.file;

import java.io.*;
import java.net.URI;
import java.util.*;
import static java.util.Collections.*;

import static com.globalmentor.io.FileConstants.*;
import static com.globalmentor.io.Files.*;
import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;

import com.globalmentor.io.*;
import com.globalmentor.marmot.repository.AbstractRepository;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.*;
import com.globalmentor.urf.*;

import static com.globalmentor.urf.TURF.*;

/**Repository stored in a filesystem.
<p>This repository recognizes the URF type <code>urf.List</code>
	and creates a directory for each such resource. Directories will be created
	transparently for other resources with other types and media types as needed
	to store child resources.</p>
@author Garret Wilson
*/
public class FileRepository extends AbstractRepository
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
	
	/**Gets an input stream to the contents of the resource specified by the given URI.
	For collections, this implementation retrieves the content of the {@value #COLLECTION_CONTENTS_NAME} file, if any.
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
				final URI contentsURI=resourceURI.resolve(COLLECTION_CONTENTS_NAME);	//determine the URI to use for contents
				final File file=new File(getPrivateURI(contentsURI));	//create a file object from the private URI of the special collection contents resource
				if(file.exists())	//if there is a special collection contents resource
				{
					return new FileInputStream(file);	//return an input stream to the file
				}
				else	//if there is no collection contents resource
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
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(URI resourceURI) throws ResourceIOException
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
			final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the private URI
			if(!resourceFile.exists())	//if the file doesn't exist
			{
				throw new FileNotFoundException("Cannot open output stream to non-existent file "+resourceFile+" in repository.");
			}
			return new FileOutputStream(resourceFile);	//return an output stream to the file
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
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
			return createResourceDescription(urf, new File(getPrivateURI(resourceURI)));	//create and return a description from a file created from the URI from the private namespace
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
	@param includeCollections Whether collection resources should be included.
	@param includeNonCollections Whether non-collection resources should be included.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resources descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public List<URFResource> getChildResourceDescriptions(URI resourceURI, final boolean includeCollections, final boolean includeNonCollections, final int depth) throws ResourceIOException
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
			final List<URFResource> resourceList=new ArrayList<URFResource>();	//create a list to hold the files that are not directories	
			if(isCollectionURI(resourceURI) && resourceDirectory.isDirectory())	//if there is a directory for this resource
			{
				final URF urf=createURF();	//create a new URF data model
				final File[] files=resourceDirectory.listFiles(getFileFilter());	//get a list of all files in the directory
				for(final File file:files)	//for each file in the directory
				{
					final boolean isFileDirectory=file.isDirectory();	//see if this file is a directory
					final URFResource childResource;
					try
					{
						childResource=createResourceDescription(urf, file);	//create a resource description for this file
					}
					catch(final IOException ioException)	//if an I/O exception occurs
					{
						throw createResourceIOException(getPublicURI(file.toURI()), ioException);	//translate the exception to a resource I/O exception and throw that, using a public URI to represent the file resource
					}
					final boolean includeChildResource=isFileDirectory ? includeCollections : includeNonCollections;	//see if we should include this resource
					if(includeChildResource)	//if we should include this child resource
					{
						resourceList.add(childResource);	//add the resource to our list
					}
					final int newDepth=depth>0 ? depth-1 : depth;	//reduce the depth by one, unless we're using the unlimited depth value
					if(isFileDirectory && depth!=0)	//if this file is a directory and we haven't reached the bottom
					{
						final List<URFResource> childResourceDescriptionList=getChildResourceDescriptions(childResource.getURI(), newDepth);	//get a list of child descriptions for the resource we just created
						final URFListResource<URFResource> childrenListResource=new URFListResource<URFResource>(childResourceDescriptionList);	//create an URF list of the children
//TODO del if no longer needed						setContents(childResource, childrenListResource);	//add the children as the contents of the resource
					}
				}
			}
			return resourceList;	//return the list of resources we constructed
		}
		else	//if a depth of zero was requested
		{
			return emptyList();	//return an empty list
		}
	}

	/**Creates a new resource with the given description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
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
	public OutputStream createResource(URI resourceURI, final URFResource resourceDescription) throws ResourceIOException	//TODO fix to prevent resources with special names
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
	//TODO bring back if needed		resourceFile.createNewFile();	//create a new file as necessary
			//TODO update the description
			return new FileOutputStream(new File(getPrivateURI(resourceURI)));	//return an output stream to the file of the private URI
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Creates a new resource with the given description and contents.
	If a resource already exists at the given URI it will be replaced.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException	//TODO fix to prevent resources with special names
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
			final OutputStream outputStream=new FileOutputStream(new File(getPrivateURI(resourceURI)));	//get an output stream to the file of the private URI
			try
			{
				outputStream.write(resourceContents);	//write the resource contents to the file
			}
			finally
			{
				outputStream.close();	//always close the output stream
			}
			//TODO update the description
			return createResourceDescription(createURF(), resourceFile);	//create and return a description of the new file
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Creates a collection in the repository.
	@param collectionURI The URI of the collection to be created.
	@return A description of the collection that was created.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error creating the collection.
	*/
	public URFResource createCollection(URI collectionURI) throws ResourceIOException	//TODO fix to prevent resources with special names
	{
			//TODO do we want to check to make sure this is a collection URI?
		collectionURI=checkResourceURI(collectionURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(collectionURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createCollection(collectionURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		try
		{
			final File directoryFile=new File(getPrivateURI(collectionURI));	//create a file object for the resource
			mkdir(directoryFile);	//create the directory
			return createResourceDescription(createURF(), directoryFile);	//create and return a description of the new directory
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(collectionURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Sets the properties of a resource based upon the given description.
	This version delegates to {@link #setResourceProperties(URI, URFResource, File)}.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException Thrown if the resource properties could not be updated.
	*/
	public URFResource setResourceProperties(URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.setResourceProperties(resourceURI, resourceDescription);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		return setResourceProperties(resourceURI, resourceDescription, resourceFile);	//update the resource properties using the file object
	}

	/**Sets the properties of a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified. 
	@param resourceURI The reference URI of the resource.
	@param properties The properties to set.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or properties is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource setResourceProperties(URI resourceURI, final URFProperty... properties) throws ResourceIOException	//TODO add thread safety
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.setResourceProperties(resourceURI, properties);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final URF urf=createURF();	//create a new URF data model
		final URFResource resourceDescription;
		try
		{
			resourceDescription=createResourceDescription(urf, new File(getPrivateURI(resourceURI)));	//get a description from a file created from the URI from the private namespace
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		for(final URFProperty property:properties)	//for each new property
		{
			resourceDescription.removePropertyValues(property.getPropertyURI());	//remove all properties with the given property URI
		}
		for(final URFProperty property:properties)	//for each property given
		{
			resourceDescription.addPropertyValue(property.getPropertyURI(), property.getValue());	//add this property value to the description TODO use addProperty() method when it exists
		}
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		return setResourceProperties(resourceURI, resourceDescription, resourceFile);	//update the resource properties using the file object
	}

	/**Removes properties from a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified. 
	@param resourceURI The reference URI of the resource.
	@param propertyURIs The properties to remove.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or property URIs is <code>null</code>.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource removeResourceProperties(URI resourceURI, final URI... propertyURIs) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.removeResourceProperties(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		final URF urf=createURF();	//create a new URF data model
		final URFResource resourceDescription;
		try
		{
			resourceDescription=createResourceDescription(urf, new File(getPrivateURI(resourceURI)));	//get a description from a file created from the URI from the private namespace
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		for(final URI propertyURI:propertyURIs)	//look at each property URI to remove
		{
			resourceDescription.removePropertyValues(propertyURI);	//remove all the values for this property URI
		}
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		return setResourceProperties(resourceURI, resourceDescription, resourceFile);	//update the resource properties using the file object
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
		if(!resourceAlteration.getPropertyRemovals().isEmpty())	//if there are properties to be removed by value
		{
			throw new UnsupportedOperationException("This implementation does not support removing properties by value.");
		}
		
		final URF urf=createURF();	//create a new URF data model
		final URFResource resourceDescription;
		try
		{
			resourceDescription=createResourceDescription(urf, new File(getPrivateURI(resourceURI)));	//get a description from a file created from the URI from the private namespace
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		for(final URI removePropertyURI:resourceAlteration.getPropertyURIRemovals())	//look at each property URI to remove
		{
			resourceDescription.removePropertyValues(removePropertyURI);	//remove all the values for this property URI
		}
		for(final URFProperty removeProperty:resourceAlteration.getPropertyRemovals())	//look at each property to remove
		{
			resourceDescription.removeProperty(removeProperty);	//remove the property
		}
		for(final URFProperty addProperty:resourceAlteration.getPropertyAdditions())	//look at each property to add
		{
			resourceDescription.addProperty(addProperty);	//add the property
		}
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		return setResourceProperties(resourceURI, resourceDescription, resourceFile);	//update the resource properties using the file object
	}

	/**Sets the properties of a resource based upon the given description.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@param resourceFile The file to use in updating the resource properties.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception ResourceIOException Thrown if the resource properties could not be updated.
	*/
	protected URFResource setResourceProperties(final URI resourceURI, final URFResource resourceDescription, final File resourceFile) throws ResourceIOException
	{
		final URFDateTime modifiedTime=getModified(resourceDescription);	//get the modified time designation, if there is one
		if(modifiedTime!=null)	//if there is a modified time designated
		{
			resourceFile.setLastModified(modifiedTime.getTime());	//update the last modified time TODO does this work for directories? should we check?
		}
		final File resourceDescriptionFile=getResourceDescriptionFile(resourceFile);	//get the resource description file
		final URFResource saveResourceDescription=new DefaultURFResource(resourceDescription);	//create a separate description we'll use for saving
		
//TODO important: remove the file-related and MIME-related live properties
		try
		{
			saveResourceDescription(saveResourceDescription, resourceFile);	//save the resource description
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
		return getResourceDescription(resourceURI);	//return the new resource description
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
	@param resourceFile The file for which a resource should be created.
	@return A resource description of the given file.
	@exception IOException if there is an error creating the resource description.
	*/
	protected URFResource createResourceDescription(final URF urf, final File resourceFile) throws IOException
	{
		final URFResource resource=loadResourceDescription(urf, resourceFile);	//load the resource description, if there is one
//TODO del if not needed		final URI resourceURI=getPublicURI(resourceFile.toURI());	//get a public URI to represent the file resource
//TODO del if not needed		final RDFResource resource=rdf.createResource(resourceURI);	//create a resource to represent the file
			//TODO update logic not to override the explicit properties loaded, unless we decide to have some live properties
		final String filename=resourceFile.getName();	//get the name of the file
		if(resourceFile.isDirectory())	//if this is a directory
		{
//TODO del; changed approach			resource.addTypeURI(LIST_CLASS_URI);	//add the urf:List type to indicate that this resource is a folder
			setModified(resource, new URFDateTime(resourceFile.lastModified()));	//set the modified timestamp as the last modified date of the file			
		}
		else	//if this file is not a directory
		{
/*TODO fix
				//unescape any reserved characters in the filename and remove the extension
			final String label=FileUtilities.removeExtension(FileUtilities.decodeFilename(filename));
			addLabel(resource, label); //add the unescaped filename without an extension as a label
*/
			setContentLength(resource, resourceFile.length());	//set the file length
			setModified(resource, new URFDateTime(resourceFile.lastModified()));	//set the modified time as the last modified date of the file			
			updateContentType(resource);	//update the content type information based upon the repository defaults
		}

//TODO del Debug.trace("returning RDF:", RDFUtilities.toString(resource));
		
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
	@param resourceDescription The resource description to save.
	@param resourceFile The file of a resource.
	@exception IOException if there is an error save the resource description.
	@see #getResourceDescriptionFile(File)
	*/
	protected void saveResourceDescription(final URFResource resourceDescription, final File resourceFile) throws IOException
	{
		final URI resourceURI=getPublicURI(resourceFile.toURI());	//get a public URI to represent the file resource
		final File resourceDescriptionFile=getResourceDescriptionFile(resourceFile);	//get the file for storing the description
		try
		{
			Files.write(resourceDescriptionFile, resourceURI, resourceDescription, getDescriptionIO());	//write the description, using the resource URI as the base URI
		}
		catch(final IOException ioException)	//if an error occurs
		{
			throw new IOException("Error writing resource description to "+resourceDescriptionFile, ioException);
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

}

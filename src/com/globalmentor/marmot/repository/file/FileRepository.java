package com.globalmentor.marmot.repository.file;

import java.io.*;
import java.net.URI;
import java.util.*;
import static java.util.Collections.*;

import javax.mail.internet.ContentType;

import static com.garretwilson.io.FileConstants.*;
import com.garretwilson.io.FileUtilities;
import static com.garretwilson.io.FileUtilities.*;
import static com.garretwilson.lang.CharSequenceUtilities.*;
import static com.garretwilson.net.URIConstants.*;
import com.garretwilson.rdf.*;
import com.garretwilson.util.Debug;

import static com.garretwilson.rdf.dublincore.DCUtilities.*;
import static com.garretwilson.rdf.rdfs.RDFSUtilities.*;
import static com.garretwilson.rdf.xpackage.FileOntologyConstants.*;
import static com.garretwilson.rdf.xpackage.FileOntologyUtilities.*;
import static com.garretwilson.rdf.xpackage.MIMEOntologyUtilities.*;
import static com.garretwilson.rdf.xpackage.XPackageUtilities.*;

import com.globalmentor.marmot.repository.AbstractRepository;

/**Repository stored in a filesystem.
<p>This repository recognizes the XPackage resource type <code>file:folder</code>
	and creates a directory for each such resource. Directories will be created
	transparently for other resources with other types and media types as needed
	to store child resources.</p>
@author Garret Wilson
*/
public class FileRepository extends AbstractRepository
{

	/**The URI represting the XPackage file:folder type.*/	//TODO check; use static imports 
//TODO move if needed	protected final static URI FILE_FOLDER_TYPE_URI=RDFUtilities.createReferenceURI(FileOntologyConstants.FILE_ONTOLOGY_NAMESPACE_URI, FileOntologyConstants.FOLDER_TYPE_NAME);	//TODO promote to parent file-based class		

	/**The extension used for directories to hold resource children.*/
//TODO move if needed	protected final static String DIRECTORY_EXTENSION="@";	//TODO promote to parent file-based class


	/**The file filter for listing files in a directory.*/
	protected final static FileFilter FILE_FILTER=new FileFilter()
			{
				/**Tests whether or not the specified abstract pathname is one of the file types we recognize.
				@param pathname The abstract pathname to be tested.
				@return <code>true</code> if and only if <code>pathname</code> should be included.
				*/
				public boolean accept(final File pathname)
				{
					if(startsWith(pathname.getName(), EXTENSION_SEPARATOR))	//if this file begins with '.'
						return false;	//ignore files beginning with the extension character
					if(pathname.isDirectory())	//if this is a directory
						return true;	//always accept directories
					return true;	//TODO add user-based security
				}
			};	

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
	@exception IllegalArgumentException if the repository URI does not use the {@value URIConstants#FILE_SCHEME} scheme.
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
	@exception IllegalArgumentException if the private repository URI does not use the {@value URIConstants#FILE_SCHEME} scheme.
	*/
	public FileRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		super(publicRepositoryURI, privateRepositoryURI);	//construct the parent class
		if(!FILE_SCHEME.equals(privateRepositoryURI.getScheme()))	//if the private respository URI scheme is not the file scheme
		{
			throw new IllegalArgumentException(privateRepositoryURI+" does not use the "+FILE_SCHEME+" URI scheme.");
		}
	}
	
	/**Gets an input stream to the contents of the resource specified by the given URI.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	*/
	public InputStream getResourceInputStream(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		return new FileInputStream(new File(getPrivateURI(resourceURI)));	//return an input stream to the file of the private URI
	}
	
	/**Gets an output stream to the contents of the resource specified by the given URI.
	An error is generated if the resource does not exist.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the resource.
	*/
	public OutputStream getResourceOutputStream(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the private URI
		if(!resourceFile.exists())	//if the file doesn't exist
		{
			throw new FileNotFoundException("Cannot open output stream to non-existent file "+resourceFile+" in repository.");
		}
		return new FileOutputStream(resourceFile);	//return an output stream to the file
	}
	
	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public RDFResource getResourceDescription(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		if(getReferenceURI().equals(resourceURI))	//if this is the URI of the repository
		{
			return this;	//return the repository itself
		}
		else	//if this is some other URI
		{
			final RDF rdf=new RDF();	//TODO use a common RDF data model
			return createResourceDescription(rdf, new File(getPrivateURI(resourceURI)));	//create and return a description from a file created from the URI from the private namespace
		}
	}

	/**Determines if the resource at the given URI exists.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean resourceExists(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		return new File(getPrivateURI(resourceURI)).exists();	//see if the file of the private URI exists
	}

	/**Determines if the resource at a given URI is a collection.
	This is a convenience method to quickly determine if a resource exists at the given URI
	and retrieving that resource would result in a resource of type <code>file:Folder</code>.
	@param resourceURI The URI of the requested resource.
	@return <code>true</code> if the resource is a collection, else <code>false</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean isCollection(final URI resourceURI) throws IOException
  {
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		return new File(getPrivateURI(resourceURI)).isDirectory();	//see if the file of the private URI is a directory
  }

	/**Determines whether the resource represented by the given URI has children.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public boolean hasChildren(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		return resourceFile.isDirectory() && resourceFile.listFiles(FILE_FILTER).length>0;	//see if this is a directory and there is more than one file in this directory
	}

	/**Retrieves child resources of the resource at the given URI.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or <code>-1</code> for an infinite depth.
	@return A list of sub-resources descriptions directly under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error accessing the repository.
	*/
	public List<RDFResource> getChildResourceDescriptions(final URI resourceURI, final int depth) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		if(depth!=0)	//a depth of zero means don't get child resources
		{
			final File resourceDirectory=new File(getPrivateURI(resourceURI));	//create a file object for the resource
			final List<RDFResource> resourceList=new ArrayList<RDFResource>();	//create a list to hold the files that are not directories	
			if(resourceDirectory.isDirectory())	//if there is a directory for this resource
			{
				final RDF rdf=new RDF();	//create a new RDF data model
				final File[] files=resourceDirectory.listFiles(FILE_FILTER);	//get a list of all files in the directory
				for(final File file:files)	//for each file in the directory
				{
					final RDFResource resource=createResourceDescription(rdf, file);	//create a resource description for this file
					final int newDepth=depth>0 ? depth-1 : depth;	//reduce the depth by one, unless we're using the unlimited depth value
					final List<RDFResource> childResourceDescriptionList=getChildResourceDescriptions(resource.getReferenceURI(), newDepth);	//get a list of child descriptions for the resource we just created
					final RDFListResource childrenListResource=RDFListResource.create(rdf, childResourceDescriptionList);	//create an RDF list of the children
					setManifest(resource, childrenListResource);	//add the children as the manifest of the folder resource
					resourceList.add(resource);	//add the resource to our list
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
	If a resource with no contents is desired, {@link #createResource(URI, RDFResource, byte[])} with zero bytes is better suited for this task.
	This implementation updates resource properties before storing the contents of the resource.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IOException if the resource could not be created.
	*/
	public OutputStream createResource(final URI resourceURI, final RDFResource resourceDescription) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
//TODO bring back if needed		resourceFile.createNewFile();	//create a new file as necessary
		//TODO update the description
		return new FileOutputStream(new File(getPrivateURI(resourceURI)));	//return an output stream to the file of the private URI
	}

	/**Creates a new resource with the given description and contents.
	If a resource already exists at the given URI it will be replaced.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	@exception IOException if the resource could not be created.
	*/
	public RDFResource createResource(final URI resourceURI, final RDFResource resourceDescription, final byte[] resourceContents) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
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
		return createResourceDescription(new RDF(), resourceFile);	//create and return a description of the new file
	}

	/**Creates a collection in the repository.
	@param collectionURI The URI of the collection to be created.
	@return A description of the collection that was created.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error creating the collection.
	*/
	public RDFResource createCollection(final URI collectionURI) throws IOException
	{
		checkResourceURI(collectionURI);	//makes sure the resource URI is valid
		final File directoryFile=new File(getPrivateURI(collectionURI));	//create a file object for the resource
		mkdir(directoryFile);	//create the directory
		return createResourceDescription(new RDF(), directoryFile);	//create and return a description of the new directory
	}

	/**Deletes a resource.
	@param resourceURI The reference URI of the resource to delete.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if the resource could not be deleted.
	*/
	public void deleteResource(final URI resourceURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		if(resourceFile.isFile())	//if this is a file and not a directory
		{
/*TODO del any associated directories
			final File directory=getResourceDirectory(resourceURI);	//get the directory to use for the URI
			if(directory.exists())	//if a directory exists for this resource
			{
				FileUtilities.delete(directory, true);	//recursively delete the directory						
			}
*/
		}
		delete(resourceFile, true);	//recursively delete the file or directory	
	}

	/**Sets the properties of a resource based upon the given description.
	This version delegates to {@link #setResourceProperties(URI, RDFResource, File)}.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException Thrown if the resource properties could not be updated.
	*/
	public RDFResource setResourceProperties(final URI resourceURI, final RDFResource resourceDescription) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		final File resourceFile=new File(getPrivateURI(resourceURI));	//create a file object for the resource
		return setResourceProperties(resourceURI, resourceDescription, resourceFile);	//update the resource properties using the file object
	}

	/**Sets the properties of a resource based upon the given description.
	This implementation only supports the {@value FileOntologyConstants#MODIFIED_TIME_PROPERTY_URI} property, updating the file attribute to match, and ignores all other properties.
	@param resourceURI The reference URI of the resource.
	@param resourceDescription A description of the resource with the properties to set; the resource URI is ignored.
	@param resourceFile The file to use in updating the resource properties.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException Thrown if the resource properties could not be updated.
	*/
	protected RDFResource setResourceProperties(final URI resourceURI, final RDFResource resourceDescription, final File resourceFile) throws IOException
	{
		final Date modifiedTime=getModifiedTime(resourceDescription);	//get the modified time designation, if there is one
		if(modifiedTime!=null)	//if there is a modified time designated
		{
			resourceFile.setLastModified(modifiedTime.getTime());	//update the last modified time TODO does this work for directories? should we check?
		}
/*TODO del when works
		for(final RDFPropertyValuePair propertyValuePair:resourceDescription.getProperties())	//look at each property
		{
			if(MODIFIED_TIME_PROPERTY_URI.equals(propertyValuePair.getName()))	//if this is the 
			
		}
*/
		return getResourceDescription(resourceURI);	//return the new resource description
	}

	/**Creates an infinitely deep copy of a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error copying the resource.
	*/
	public void copyResource(final URI resourceURI, final URI destinationURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		throw new UnsupportedOperationException();	//TODO implement
	}

	/**Moves a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IOException if there is an error moving the resource.
	*/
	public void moveResource(final URI resourceURI, final URI destinationURI) throws IOException
	{
		checkResourceURI(resourceURI);	//makes sure the resource URI is valid
		throw new UnsupportedOperationException();	//TODO implement
	}

	/**Creates a resource description to represent a single file.
	@param rdf The RDF data model to use when creating this resource.
	@param file The file for which a resource should be created.
	@return A resource description of the given file.
	*/
	protected RDFResource createResourceDescription(final RDF rdf, final File file)	
	{
		final URI resourceURI=getPublicURI(file.toURI());	//get a public URI to represent the file resource
		final RDFResource resource=rdf.locateResource(resourceURI);	//create a resource to represent the file
/*TODO del
		final RK resourceKit=getResourceKitManager().getResourceKit(this, resource);	//get a resource kit for this resource
		if(resourceKit!=null)	//if we found a resource kit for this resource
		{
			resourceKit.initialize(this, rdf, resource);	//initialize the resource
		}
*/
			//create a label G***maybe only do this if the resource kit has not added a label
		final String filename=file.getName();	//get the name of the file
		if(file.isDirectory())	//if this is a directory
		{
			final String label=FileUtilities.decodeFilename(filename);	//unescape any reserved characters in the filename
			addType(resource, FILE_ONTOLOGY_NAMESPACE_URI, FOLDER_TYPE_NAME);	//add the file:folder type to indicate that this resource is a folder
			addLabel(resource, label); //add the filename as a label
			setModifiedTime(resource, new Date(file.lastModified()));	//set the modified time as the last modified date of the file			
		}
		else	//if this file is not a directory
		{
				//unescape any reserved characters in the filename and remove the extension
			final String label=FileUtilities.removeExtension(FileUtilities.decodeFilename(filename));
			addLabel(resource, label); //add the unescaped filename without an extension as a label

//TODO del Debug.trace("we're looking at file", file, "which exists", file.exists());
/*TODO del
Debug.trace("we're looking at file", file, "which exists", file.exists(), "first resting a bit");

try
{
	Thread.sleep(2000L);
} catch (InterruptedException e)
{
	// TODO Auto-generated catch block
	Debug.error(e);
}
*/
//TODO del Debug.trace("ready to add RDF file size:", file.length());
			
			setSize(resource, file.length());	//set the file length
			setModifiedTime(resource, new Date(file.lastModified()));	//set the modified time as the last modified date of the file			
			final ContentType contentType=getMediaType(filename);	//try to find the content type from the filename
			if(contentType!=null)	//if we know the content type
			{
				setContentType(resource, contentType);	//set the content type property
			}
		}

//TODO del Debug.trace("returning RDF:", RDFUtilities.toString(resource));
		
		return resource;	//return the resource that respresents the file
	}
	
}

package com.globalmentor.marmot.repository.file;

import java.io.*;
import java.net.URI;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.URIs;
import com.globalmentor.urf.DefaultURFResourceAlteration;
import com.globalmentor.urf.URFResource;

import static com.globalmentor.io.FileConstants.*;
import static com.globalmentor.io.Files.*;
import static com.globalmentor.net.URIs.isCollectionURI;

/**Repository stored in an NTFS filesystem.
This implementation stores resource descriptions in NTFS Alternate Data Streams (ADS).
@author Garret Wilson
@see <a href="http://support.microsoft.com/kb/105763">How To Use NTFS Alternate Data Streams</a>
*/
public class NTFSFileRepository extends FileRepository
{

	/**File contructor with no separate private URI namespace.
	@param repositoryDirectory The file identifying the directory of this repository.
	@exception NullPointerException if the given respository directory is <code>null</code>.
	*/
	public NTFSFileRepository(final File repositoryDirectory)
	{
		super(repositoryDirectory);	//construct the parent class
	}

	/**URI contructor with no separate private URI namespace.
	The given repository URI should end in a slash.
	@param repositoryURI The URI identifying the location of this repository.
	@exception NullPointerException if the given respository URI is <code>null</code>.
	@exception IllegalArgumentException if the repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	*/
	public NTFSFileRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository directory contructor.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryDirectory The file identifying the private directory of the repository.
	@exception NullPointerException if the given respository URI and/or the given directory is <code>null</code>.
	*/
	public NTFSFileRepository(final URI publicRepositoryURI, final File privateRepositoryDirectory)
	{
		this(publicRepositoryURI, getDirectoryURI(privateRepositoryDirectory));	//get a directory URI from the private repository directory and use it as the base repository URI
	}

	/**Public repository URI and private repository URI contructor.
	The given private repository URI should end in a slash.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@exception NullPointerException if one of the given respository URIs is <code>null</code>.
	@exception IllegalArgumentException if the private repository URI does not use the {@value URIs#FILE_SCHEME} scheme.
	*/
	public NTFSFileRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		super(publicRepositoryURI, privateRepositoryURI);	//construct the parent class
	}

	/**Creates a repository of the same type as this repository with the same access privileges as this one.
	This factory method is commonly used to use a parent repository as a factory for other repositories in its namespace.
	@param publicRepositoryURI The public URI identifying the location of the new repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@throws NullPointerException if the given public repository URI and/or private repository URI is <code>null</code>.
	*/
	protected Repository createSubrepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		return new NTFSFileRepository(publicRepositoryURI, privateRepositoryURI);	//create and return a new NTFS file repository
	}

	/**Determines the file that holds the description of the given resource file.
	This version uses an NTFS Alternate Data Stream of {@value #MARMOT_DESCRIPTION_NAME} in the resource file.
	@param resourceFile The file of a resource.
	@return A new file designating the location of the resource description.
	*/
	protected File getResourceDescriptionFile(final File resourceFile)
	{
		return changeName(resourceFile, resourceFile.getName()+NTFS_ADS_DELIMITER+MARMOT_DESCRIPTION_NAME);	//return a file in the form "file.ext:marmot-description"
	}

	/**Gets an output stream to the contents of the given resource file.
	For collections, this implementation sets the content of the {@value #COLLECTION_CONTENTS_NAME} file, if any.
	If this resource is not a collection, this version gets the resource description, deletes the file, and creates a new file with the same description because
	simply opening a new file output stream to overwrite a file will overwrite the NTFS streams.
	For collections, an output stream is retrieved normally because the special collection content file does not maintain properties. 
	@param resourceURI The URI of the resource to access.
	@param resourceFile The file representing the resource.
	@return An output stream to the resource represented by given resource file.
	@exception IOException if there is an error accessing the resource.
	*/
	protected OutputStream getResourceOutputStream(final URI resourceURI, final File resourceFile) throws IOException
	{
		if(isCollectionURI(resourceURI))	//if the resource is a collection
		{
			return super.getResourceOutputStream(resourceURI, resourceFile);	//open an output stream to the special resource file normally
		}
		else	//if the resource is not a collection, perform special processing to allow resource properties to be maintained
		{
			final URFResource resourceDescription=createResourceDescription(createURF(), resourceFile);	//get a description of the existing file
			delete(resourceFile);	//delete the file
			createNewFile(resourceFile);	//create a new file
			if(resourceDescription.hasProperties())	//if there are any properties to set (otherwise, don't create an empty properties file) TODO improve; this will always have properties; it would be best to check to see if there are any non-live properties
			{
	  		alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createResourceAlteration(resourceDescription), resourceFile);	//set the properties using the file
			}
			return new FileOutputStream(resourceFile, true);	//return an output stream to the file, appending to the new, empty file we created
		}
	}

}

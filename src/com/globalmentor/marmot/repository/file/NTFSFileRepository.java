package com.globalmentor.marmot.repository.file;

import java.io.*;
import java.net.URI;

import com.globalmentor.net.URIs;

import static com.globalmentor.io.FileConstants.*;
import static com.globalmentor.io.Files.*;

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

	/**Determines the file that holds the description of the given resource file.
	This version uses an NTFS Alternate Data Stream of {@value #MARMOT_DESCRIPTION_NAME} in the resource file.
	@param resourceFile The file of a resource.
	@return A new file designating the location of the resource description.
	*/
	protected File getResourceDescriptionFile(final File resourceFile)
	{
		return changeName(resourceFile, resourceFile.getName()+NTFS_ADS_DELIMITER+MARMOT_DESCRIPTION_NAME);	//return a file in the form "file.ext:marmot-description"
	}

}

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
import java.util.*;
import static java.util.Collections.emptyList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.CharSequences.*;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.net.*;
import static com.globalmentor.net.URIs.*;
import com.globalmentor.urf.*;
import static com.globalmentor.urf.content.Content.*;

/**A repository backed by a Zip archive resource.
@author Garret Wilson
@see <a href="http://www.pkware.com/documents/casestudies/APPNOTE.TXT">.ZIP File Format Specification</a>
*/
public class ZipArchiveRepository extends AbstractArchiveRepository<ZipFile>
{

	/**Default constructor with no root URI defined.
	The root URI must be defined before the repository is opened.
	*/
	public ZipArchiveRepository()
	{
		this(null);
	}

	/**URI constructor with no separate private URI namespace.
	@param rootURI The URI identifying the location of this repository.
	*/
	public ZipArchiveRepository(final URI rootURI)
	{
		this(rootURI, rootURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI constructor.
	A {@link URFResourceTURFIO} description I/O is created and initialized.
	@param rootURI The URI identifying the location of this repository.
	@param sourceResourceURI The URI identifying the private namespace managed by this repository.
	*/
	public ZipArchiveRepository(final URI rootURI, final URI sourceResourceURI)
	{
		this(rootURI, sourceResourceURI, createDefaultURFResourceDescriptionIO());	//create a default resource description I/O using TURF
	}

	/**Public repository URI and private repository URI constructor.
	@param rootURI The URI identifying the location of this repository.
	@param sourceResourceURI The URI identifying the private namespace managed by this repository.
	@param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	@exception NullPointerException if the given description I/O is <code>null</code>.
	*/
	public ZipArchiveRepository(final URI rootURI, final URI sourceResourceURI, final URFIO<URFResource> descriptionIO)
	{
		super(rootURI, sourceResourceURI, descriptionIO);
	}

	/**Creates an object to represent the source archive from the given source archive file.
	The returned archive will be opened and ready for use.
	@param sourceArchiveFile The cached file of the source archive.
	@throws IOException if there is an error creating the source archive.
	@return A new source archive for the file.
	*/
	protected ZipFile createSourceArchive(final File sourceArchiveFile) throws IOException
	{
		return new ZipFile(sourceArchiveFile);	//create a new zip file for reading
	}

	/**Determines the public URI to represent the given zip entry.
	@param zipEntry The zip entry for which a public resource URI should be returned.
	@return A public resource URI for the given zip entry.
	@throws NullPointerException if the given zip entry is <code>null</code>.
	*/
	protected URI getPublicURI(final ZipEntry zipEntry)
	{
		final String zipEntryName=zipEntry.getName();
		return getRootURI().resolve(URIPath.createURIPathURI(URIPath.encode(zipEntryName)));	//encode the zip entry name and resolve it to the repository URI
	}
	
	/**Retrieves zip entries to a cached source archive file representing the resource identified by the given resource UI.
	The resource URI is expected to already be normalized.
	@param zipFile The zip file from which to get the zip entry.
	@param resourceURI The public URI of the resource within the respository.
	@return A zip entry to access the contents of the given resource within the repository.
	@throws NullPointerException if the given zip file and/or resource URI is <code>null</code>.
	@throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@throws IOException if there is an error getting a zip entry to represent the given resource URI.
	@throws ResourceNotFoundException if there is no zip entry that matches the given resource URI.
	*/
	protected ZipEntry getZipEntry(final ZipFile zipFile, final URI resourceURI) throws IOException
	{
		final URIPath uriPath=relativize(getRootURI(), resourceURI);	//get the path within the zip file
		final boolean isCollection=uriPath.isCollection();
		String uriPathString=uriPath.toDecodedString();	//get the decoded string form of the path
		if(isCollection)
		{
			uriPathString=uriPathString.substring(0, uriPathString.length()-1);	//remove the last slash, as zip files don't store directory names with trailing slashes
		}
		final ZipEntry zipEntry=zipFile.getEntry(uriPath.toDecodedString());	//return a zip entry from the archive based upon the path
		if(zipEntry==null || (isCollection && !zipEntry.isDirectory()))	//if there is no such zip entry, or if a collection was requested but the returned zip entry is not a directory
		{
			throw new ResourceNotFoundException(resourceURI, "The resource "+resourceURI+" does not exist.");
		}
		return zipEntry;	//return the zip entry that we found
	}

	/**Retrieves zip entries from a cached source archive file representing the resource identified by the given resource UI.
	The zip entry for the resource itself will not be included.
	The resource URI is expected to already be normalized.
	@param zipFile The zip file from which to get the zip entry.
	@param resourceURI The public URI of a resource within the respository.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or {@link Repository#INFINITE_DEPTH} for an infinite depth.
	@return Zip entries to access the contents of the children of the given resources within the repository.
	@throws NullPointerException if the given zip file and/or resource URI is <code>null</code>.
	@throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@throws IOException if there is an error getting zip entries to represent children of given resource URI.
	@throws ResourceNotFoundException if there is no zip entry that matches the given resource URI.
	*/
	protected List<ZipEntry> getChildZipEntries(final ZipFile zipFile, final URI resourceURI, final int depth) throws IOException
	{
		final ZipEntry resourceZipEntry=getRootURI().equals(resourceURI) ? null : getZipEntry(zipFile, resourceURI);	//get the zip entry for this resource URI, to make sure it exists and to get its name; if this is the URI of the repository itself, use null to specify the root
		return getChildZipEntries(zipFile, resourceZipEntry, depth);	//get the child zip entries for this zip entry
	}
	
	/**Retrieves zip entries from a zip file that represent child zip entries of the given parent zip entry.
	Providing a parent zip entry of <code>null</code> and a depth of {@link Repository#INFINITE_DEPTH} will result in
	all zip entries being returned.
	@param zipFile The zip file from which to get the zip entry.
	@param parentZipEntry The parent entry for which child zip entries should be returned,
		or <code>null</code> if child entries of the root should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or {@link Repository#INFINITE_DEPTH} for an infinite depth.
	@return Zip entries the names of which are child paths of the given parent zip entry.
	@throws NullPointerException if the given zip file is <code>null</code>.
	@throws IOException if there is an error getting zip entries to represent children of given parent zip entry.
	*/
	public static List<ZipEntry> getChildZipEntries(final ZipFile zipFile, final ZipEntry parentZipEntry, final int depth) throws IOException
	{
		if(depth==0 || (parentZipEntry!=null && !parentZipEntry.isDirectory()))	//if the parent zip entry is not a directory, or they requested no child entries
		{
			return emptyList();	//return an empty list; non-directories have no children
		}
		final String parentZipEntryBaseName=parentZipEntry!=null ? parentZipEntry.getName() : "";	//get the name that will be the base of all child entries
		final int resourceZipEntryBaseNameLength=parentZipEntryBaseName.length();
		final List<ZipEntry> childZipEntries=new ArrayList<ZipEntry>();
		final Enumeration<? extends ZipEntry> zipEntries=zipFile.entries();	//get an enumeration to all the zip entries in the file
		while(zipEntries.hasMoreElements())	//while there are more elements
		{
			final ZipEntry zipEntry=zipEntries.nextElement();
			final String zipEntryName=zipEntry.getName();
			if(zipEntryName.startsWith(parentZipEntryBaseName) && (parentZipEntry==null || !zipEntryName.equals(parentZipEntry.getName())))	//if this entry starts with the base, it is a descendant; but ignore the zip entry itself
			{
				boolean addEntry=depth==INFINITE_DEPTH;	//if infinite depth was requested, we should always add the entries
				if(!addEntry)	//if infinite depth wasn't requested
				{
					int zipEntryDepth=count(zipEntryName, PATH_SEPARATOR, resourceZipEntryBaseNameLength);	//directories end with path separators; e.g. first/second/third/ has three path separators and has a depth of three
					if(!zipEntry.isDirectory())	//the number of path separators for files is one less than the depth; e.g. first/second/third.txt has two path separators and has a depth of three
					{
						++zipEntryDepth;	//compensate for the lack of the ending path separator
					}
					addEntry=zipEntryDepth<=depth;	//see if this path is under or equal to the requested depth
				}
				if(addEntry)	//if we should add this entry
				{
					childZipEntries.add(zipEntry);	//add this as a child entry
				}
			}
		}
		return childZipEntries;
	}
	
	/**Gets an input stream to the contents of the resource specified by the given URI.
	For collections, this implementation retrieves the content of the {@value #COLLECTION_CONTENT_NAME} file, if any.
	@param resourceURI The URI of the resource to access.
	@return An input stream to the resource represented by the given URI.
	@throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@throws ResourceIOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
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
			if(isCollectionURI(resourceURI))	//if the resource is a collection (including the root resource)
			{
				return new ByteArrayInputStream(NO_BYTES);	//return an input stream to an empty byte array
			}
			else	//if the resource is not a collection
			{
				final ZipFile zipFile=getSourceArchive();	//get the archive
				final ZipEntry zipEntry=getZipEntry(zipFile, resourceURI);	//get the entry for this resource
				return zipFile.getInputStream(zipEntry);	//return an input stream to the entry
			}
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Retrieves a description of the resource with the given URI.
	@param resourceURI The URI of the resource the description of which should be retrieved.
	@return A description of the resource with the given URI.
	@throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@throws ResourceIOException if there is an error accessing the repository.
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
			final ZipEntry resourceZipEntry=getRootURI().equals(resourceURI) ? null : getZipEntry(getSourceArchive(), resourceURI);	//get the zip entry for this resource URI, or null if this is the root resource URI
			return createResourceDescription(urf, resourceURI, resourceZipEntry);	//create and return a description from a zip entry from the archive
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Determines if the resource at the given URI exists.
	This implementation returns <code>false</code> for all resources for which {@link #isSourceResourcePublic(URI)} returns <code>false</code>.
	@param resourceURI The URI of the resource to check.
	@return <code>true</code> if the resource exists, else <code>false</code>.
	@throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@throws ResourceIOException if there is an error accessing the repository.
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
		if(getRootURI().equals(resourceURI))	//the root resource always exists
		{
			return true;
		}
		try
		{
			getZipEntry(getSourceArchive(), resourceURI);	//get a zip entry for this resource URI
			return true;	//if we succeed, the resource exists
		}
		catch(final ResourceNotFoundException resourceNotFoundException)	//if the resource was not found
		{
			return false;
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Determines whether the resource represented by the given URI has children.
	This implementation ignores child resources for which {@link #isSourceResourcePublic(URI)} returns <code>false</code>.
	@param resourceURI The URI of the resource.
	@return <code>true</code> if the specified resource has child resources.
	@throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@throws ResourceIOException if there is an error accessing the repository.
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
		try
		{
			return !getChildZipEntries(getSourceArchive(), resourceURI, 1).isEmpty();	//TODO improve to keep from going through the entire file once a directory is found
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
		}
	}

	/**Retrieves child resources of the resource at the given URI.
	This implementation does not include child resources for which {@link #isSourceResourcePublic(URI)} returns <code>false</code>.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be filtered.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or {@link Repository#INFINITE_DEPTH} for an infinite depth.
	@return A list of sub-resource descriptions under the given resource.
	@throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@throws ResourceIOException if there is an error accessing the repository.
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
			try
			{
				final ZipFile zipFile=getSourceArchive();
				final ZipEntry resourceZipEntry=getRootURI().equals(resourceURI) ? null : getZipEntry(zipFile, resourceURI);	//get the zip entry for the resource, or null if this is the root resource URI
				final List<ZipEntry> childZipEntries=getChildZipEntries(zipFile, resourceZipEntry, depth);	//get the child zip entries for the resource; the depth is taken care of so we don't have to manually recurse in this method
				assert isCollectionURI(resourceURI)==(resourceZipEntry==null || resourceZipEntry.isDirectory());	//whether the zip entry is a directory should match whether the zip entry is a directory (or the root)
				final List<URFResource> childResourceList=new ArrayList<URFResource>();	//create a list to hold the child resources	
				if(!childZipEntries.isEmpty())	//if we have child resources
				{
					assert isCollectionURI(resourceURI);	//we should only have child resources for collections
					final URF urf=createURF();	//create a new URF data model
					for(final ZipEntry childResourceZipEntry:childZipEntries)	//for each child zip entry
					{
						final URI childResourceURI=getPublicURI(childResourceZipEntry);	//get a public URI to represent the zip entry
						if(getSubrepository(childResourceURI)==this)	//if this child wouldn't be located in a subrepository (i.e. ignore resources obscured by subrepositories)
						{
							if(resourceFilter==null || resourceFilter.isPass(childResourceURI))	//if we should include this resource based upon its URI
							{
								final URFResource childResourceDescription;
								try
								{
									childResourceDescription=createResourceDescription(urf, childResourceURI, childResourceZipEntry);	//create a resource description for this child resource zip entry
								}
								catch(final IOException ioException)	//if an I/O exception occurs
								{
									throw createResourceIOException(childResourceURI, ioException);	//translate the exception to a resource I/O exception and throw that for this child resource zip entry
								}
								if(resourceFilter==null || resourceFilter.isPass(childResourceDescription))	//if we should include this resource based upon its description
								{
									childResourceList.add(childResourceDescription);	//add the resource to our list
								}
							}
						}
					}
						//aggregate any mapped subrepositories
					for(final Repository childSubrepository:getChildSubrepositories(resourceURI))	//see if any subrepositories are mapped as children of this repository
					{
						final URI childSubrepositoryURI=childSubrepository.getRootURI();	//get the URI of the subrepository
						childResourceList.add(childSubrepository.getResourceDescription(childSubrepositoryURI));	//get a description of the subrepository root resource
						if(depth==INFINITE_DEPTH || depth>0)	//if we should get child resources lower in the hierarchy
						{
							childResourceList.addAll(childSubrepository.getChildResourceDescriptions(childSubrepositoryURI, resourceFilter, depth==INFINITE_DEPTH ? depth : depth-1));	//get descriptions of subrepository children
						}
					}
				}
				return childResourceList;	//return the list of resources we constructed
			}
			catch(final IOException ioException)	//if an I/O exception occurs
			{
				throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
			}
		}
		else	//if a depth of zero was requested
		{
			return emptyList();	//return an empty list
		}
	}
	
	/**Creates a resource description to represent a zip entry.
	<p>This implementation merges the resource description returned by {@link #retrieveResource(URI)}, if any.</p>
	@param urf The URF data model to use when creating this resource.
	@param resourceURI The URI of the resource being described.
	@param resourceZipEntry The zip entry for which a resource should be created,
	or <code>null</code> if a resource description should be created for the root resource of the repository.
	@return A resource description of the given file.
	@throws IOException if there is an error creating the resource description.
	@throws IllegalArgumentException if a non-collection URI is given to access a directory.
	*/
	protected URFResource createResourceDescription(final URF urf, final URI resourceURI, final ZipEntry resourceZipEntry) throws IOException
	{
		final URFResource resource=urf.createResource(resourceURI); //create a default resource description
		final URFResource configuredResource=retrieveResource(resourceURI);	//get the configured resource, if any
		if(configuredResource!=null)	//if a resource has been configured for this URI
		{
			resource.addAllProperties(configuredResource);	//add all the configured properties
		}
		long contentLength=0;	//we'll update the content length if we can
		URFDateTime contentModified=null;	//we'll get the content modified from the file or, for a directory, from its content file, if any---but not from a directory itself
		if(resourceZipEntry!=null)	//if this is not the root resource
		{
			if(!resourceZipEntry.isDirectory())	//if this is not a directory
			{
				contentLength=resourceZipEntry.getSize();	//use the uncompressed size of the zip entry
				contentModified=new URFDateTime(resourceZipEntry.getTime());	//set the modified timestamp as the last modified date of the zip entry			
			}
		}
		setContentLength(resource, contentLength);	//indicate the length of the content
		if(contentModified!=null)	//if we have a content modified time
		{
			setModified(resource, contentModified);	//set the modified timestamp as the last modified date
		}
		return resource;	//return the resource that respresents the zip entry
	}
}

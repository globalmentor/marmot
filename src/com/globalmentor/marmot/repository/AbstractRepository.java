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

package com.globalmentor.marmot.repository;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

import static java.util.Arrays.*;
import static java.util.Collections.*;

import com.globalmentor.collections.CollectionMap;
import com.globalmentor.collections.HashSetHashMap;
import com.globalmentor.io.*;
import com.globalmentor.marmot.Marmot;
import com.globalmentor.marmot.security.MarmotSecurity;
import com.globalmentor.net.*;
import com.globalmentor.urf.*;
import com.globalmentor.urf.content.Content;

import static com.globalmentor.io.Files.*;
import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.Objects.*;
import static com.globalmentor.marmot.Marmot.*;
import static com.globalmentor.marmot.security.MarmotSecurity.*;
import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;

/**Abstract implementation of a repository with typical features.
<p>This implementation uses the special name {@value #COLLECTION_CONTENT_NAME} to represent the contents (as opposed to the contained resources) of a collection resource.</p>
<p>Resource access methods should call {@link #checkResourceURI(URI)} as a security check to ensure the given URI is within the repository.</p>
<p>This implementation considers the following properties to be live properties:</p>
<ul>
	<li>{@value Content#ACCESSED_PROPERTY_URI}</li>
	<li>{@value Content#LENGTH_PROPERTY_URI}</li>
</ul>
<p>This implementation initializes the map of extension contents to {@link Files#FILE_EXTENSION_CONTENT_TYPE_MAP}.</p>
@author Garret Wilson
*/
public abstract class AbstractRepository implements Repository
{

	/**The resource factory for resources in the Marmot namespace.*/
	protected final static URFResourceFactory MARMOT_RESOURCE_FACTORY=new JavaURFResourceFactory(Marmot.class.getPackage());
	/**The resource factory for resources in the Marmot security namespace.*/
	protected final static URFResourceFactory MARMOT_SECURITY_RESOURCE_FACTORY=new JavaURFResourceFactory(MarmotSecurity.class.getPackage());

	/**The name of a resource used to store the content of a collection.*/
	public final static String COLLECTION_CONTENT_NAME="@";	//TODO add checks to prevent this resource from being accessed directly

	/**The set of URIs that are considered live.*/
	protected final static Set<URI> LIVE_PROPERTY_URIS=unmodifiableSet(new HashSet<URI>(asList(Content.ACCESSED_PROPERTY_URI, Content.LENGTH_PROPERTY_URI)));

	/**The I/O implementation that writes and reads a resource with the same reference URI as its base URI.*/
	private final URFIO<URFResource> descriptionIO;

		/**@return The I/O implementation that writes and reads a resource with the same reference URI as its base URI.*/
		protected URFIO<URFResource> getDescriptionIO() {return descriptionIO;}

	/**The registered event listeners.*/
//TODO bring back when needed	protected final EventListenerManager eventListenerManager=new EventListenerManager();

	/**Sets the URI.
	If there currently is no private repository URI, it will be updated to match the given public repository URI.
	@param uri The new URI, or <code>null</code> if there is no URI.
	*/
/*TODO fix
	protected void setURI(final URI uri)
	{
			//TODO check for the URI being set to null
//TODO bring back		super.setURI(uri);	//set the URI normally
		if(getPrivateRepositoryURI()==null)	//if there is no private repository URI
		{
			setPrivateRepositoryURI(uri);	//update the private repository URI to match
		}
	}
*/

	/**The parent repository, or <code>null</code> if this repository has not been registered as a subrepository of another repository.*/
	private Repository parent=null;
	
		/**@return The parent repository, or <code>null</code> if this repository has not been registered as a subrepository of another repository.*/
		public Repository getParentRepository() {return parent;}

		/**Sets the parent of this repository.
		This method is used internally when a subrepository is set, and is not intended to be called by normal code. 
		@param newParent The new parent of the repository, or <code>null</code> if the repository is being unregistered.
		@throws IllegalStateException if the new parent is non-<code>null</code> and the repository already has a parent.
		@see #registerPathRepository(URIPath, Repository)
		*/
		public void setParentRepository(final Repository newParent)
		{
			if(parent!=null && newParent!=null && newParent!=parent)	//if the parent is being changed without first removing the old parent
			{
				throw new IllegalStateException("Repository parent cannot be changed without first unregistering.");
			}
			parent=newParent;
		}

		/**Determines the root of a hierararchy of subrepositories.
		If this repository has no parent, this method will return this repository.
		@return The root parent of all the repositories.
		*/
		public Repository getRootRepository()
		{
			Repository rootRepository=this;
			Repository parentRepository;
			while((parentRepository=rootRepository.getParentRepository())!=null)	//walk up the chain until we run out of parent repositories
			{
				rootRepository=parentRepository;	//move the root up a level
			}
			return rootRepository;	//return whatever root repository we determined
		}

	/**Whether the repository has been opened for access.*/
	private boolean open=false;

	/**The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository.*/
	private URI sourceURI=null;

		/**@return The base URI of the private URI namespace being managed, which may be the same as the public URI of this repository.*/
		public URI getSourceURI() {return sourceURI;}

		/**Sets the base URI of the private URI namespace being managed.
		@param sourceURI The base URI of the private URI namespace being managed.
		@exception NullPointerException if the given URI is <code>null</code>.
		*/
		public void setSourceURI(final URI sourceURI)
		{
			this.sourceURI=checkInstance(sourceURI, "Source URI must not be null.").normalize();
		}

	/**The base URI of the public URI namespace being managed.*/
	private URI rootURI=null;
		
		/**@return The base URI of the public URI namespace being managed.*/
		public URI getRootURI() {return rootURI;}

		/**Sets the base URI of the public URI namespace being managed, reference URI of the repository.
		If there currently is no private repository URI, it will be updated to match the given public repository URI.
		The public URIs of the sub-repositories will be updated accordingly.
		@param rootURI The base URI of the public URI namespace being managed.
		@exception NullPointerException if the given URI is <code>null</code>.
		@see #getPathRepositories()
		*/
		public void setRootURI(final URI rootURI)
		{
			this.rootURI=checkInstance(rootURI, "Root URI must not be null.").normalize();
			for(final Map.Entry<URIPath, Repository> pathRepositoryEntry:pathRepositoryMap.entrySet())	//look at each path to repository mapping
			{
				pathRepositoryEntry.getValue().setRootURI(resolve(getRootURI(), pathRepositoryEntry.getKey()));	//update the public URI of the repository to match its location in the repository
			}
		}

	/**Whether the repository should automatically be opened when needed.*/
	private boolean autoOpen=true;

		/**@return Whether the repository should automatically be opened when needed.*/
		public boolean isAutoOpen() {return autoOpen;}

		/**Sets whether the repository should automatically be opened when needed.
		@param autoOpen Whether the repository should automatically be opened when needed.
		*/
		public void setAutoOpen(final boolean autoOpen) {this.autoOpen=autoOpen;}

	/**Checks to make sure that the repository is open.
	If the auto-open facility is turned on, the repository will be automatically opened if needed.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error opening the repository.
	@see #isAutoOpen()
	*/
	protected void checkOpen() throws ResourceIOException
	{
		if(!isOpen())	//if the repository is not open
		{
			if(isAutoOpen())	//if we should open the repository automatically
			{
				open();	//open the repository
			}
			else	//if we shouldn't open the repository automatically
			{
				throw new IllegalStateException("Repository is not open.");
			}
		}
	}

	/**The map of content types mapped to lowercase URI name extensions.*/
	private final Map<String, ContentType> extensionContentTypeMap=new HashMap<String, ContentType>(FILE_EXTENSION_CONTENT_TYPE_MAP);

		/**Associates the given content type with the given extension, without regard to case.
		@param extension The URI name extension with which the content type should be associated, or <code>null</code> if the content type should be associated with resources that have no extension.
		@param contentType The content type to associate with the given extension.
		@return The content type previously registered with the given extension, or <code>null</code> if no content type was previously registered.
		@exception NullPointerException if the given content type is <code>null</code>.
		*/
		public ContentType registerExtensionContentType(final String extension, final ContentType contentType)
		{
			return extensionContentTypeMap.put(extension!=null ? extension.toLowerCase() : null, checkInstance(contentType, "Content type cannot be null."));
		}
	
		/**Returns the content type assciated with the given extension, without regard to case.
		@param extension The URI name extension with which the content type is associated, or <code>null</code> if the content type is associated with resources that have no extension.
		@return The content type associated with the given extension, or <code>null</code> if there is no content type associated with the given extension.
		*/
		public ContentType getExtensionContentType(final String extension)
		{
			return extensionContentTypeMap.get(extension!=null ? extension.toLowerCase() : null);	//return the content type, if any, associated with the given extension
		}

	/**The map of charsets mapped to base media types.*/
	private final Map<String, Charset> baseContentTypeCharsetMap=new HashMap<String, Charset>();

		/**Associates the given charset with the base media type of the given content type.
		Any association will only override resources that do not explicitly have a charset specified.
		Any parameters of the given content type will be ignored.
		@param contentType The content type with which the charset should be associated.
		@param charset The charset to associate with the given content type.
		@return The charset previously registered with the given content type, or <code>null</code> if no charset was previously registered.
		@exception NullPointerException if the given content type and/or charset is <code>null</code>.
		*/
		public Charset registerContentTypeCharset(final ContentType contentType, final Charset charset)
		{
			return baseContentTypeCharsetMap.put(contentType.getBaseType(), checkInstance(charset, "Charset cannot be null."));
		}

		/**Returns the charset assciated with the given content type.
		Any parameters of the given content type will be ignored.
		@param contentType The content type with which the charset is associated.
		@return The charset associated with the given content type, or <code>null</code> if there is no charset associated with the given content type.
		@exception NullPointerException if the given content type is <code>null</code>.
		*/
		public Charset getContentTypeCharset(final ContentType contentType)
		{
			return baseContentTypeCharsetMap.get(contentType.getBaseType());	//return the charset, if any, associated with the given base content type
		}

		/**@return The read-only mapping of charsets associated with base content types.*/
		public Map<ContentType, Charset> getContentTypeCharsets()
		{
			final Map<ContentType, Charset> contentTypeCharsetMap=new HashMap<ContentType, Charset>(baseContentTypeCharsetMap.size());	//create a new map to hold actual content type objects
			for(final Map.Entry<String, Charset> baseContentTypeCharsetEntry:baseContentTypeCharsetMap.entrySet())	//look at each mapping
			{
				contentTypeCharsetMap.put(ContentType.getInstance(baseContentTypeCharsetEntry.getKey()), baseContentTypeCharsetEntry.getValue());	//add this mapping to the map
			}
			return unmodifiableMap(contentTypeCharsetMap);	//return a read-only version of the map we created
		}

		/**Sets the content type charset associations to those specified in the given map.
		Any association will only override resources that do not explicitly have a charset specified.
		The current associations will be lost.
		Any parameters of the given content types will be ignored.
		@param contentTypeCharsets The associations of charsets to base content types.
		@exception NullPointerException if a given content type and/or charset is <code>null</code>.
		*/
		public void setContentTypeCharsets(final Map<ContentType, Charset> contentTypeCharsets)
		{
			baseContentTypeCharsetMap.clear();	//clear the current mappings
			for(final Map.Entry<ContentType, Charset> contentTypeCharsetEntry:contentTypeCharsets.entrySet())	//look at each mapping
			{
				registerContentTypeCharset(contentTypeCharsetEntry.getKey(), contentTypeCharsetEntry.getValue());	//register this association
			}
		}

	/**The map of repositories keyed to relative collection paths.*/
	private final Map<URIPath, Repository> pathRepositoryMap=new HashMap<URIPath, Repository>();

	/**The map of repositories pairs keyed to relative parent collection paths.*/
	private final CollectionMap<URIPath, Repository, Set<Repository>> parentPathRepositoryMap=new HashSetHashMap<URIPath, Repository>();

		/**Associates the given repository with a repository path.
		Access to any resource with a URI beginning with the given path will delegate to the indicated repository.
		The public URI of the given repository will be updated to correspond to its location within this repository.
		@param path The relative collection path with which the repository should be associated.
		@param repository The repository to handle access to all resources beginning with the given path.
		@return The repository previously registered with the given path, or <code>null</code> if no repository was previously registered.
		@exception NullPointerException if the given path and/or repository is <code>null</code>.
		@exception IllegalArgumentException if the given path is not relative.
		@exception IllegalArgumentException if the given path does not represent a collection (i.e. it does not end with a path separator).
		*/
		public Repository registerPathRepository(final URIPath path, final Repository repository)
		{
			if(getRootURI()!=null)	//if the root URI has been initialized
			{
				repository.setRootURI(resolve(getRootURI(), path));	//update the public URI of the repository to match its location in the repository
			}
			if(!URIPath.ROOT_URI_PATH.equals(path))	//if this is not the root path (it's not normal to map the root path to another repository, but check for it anyway)
			{
				final URIPath parentPath=path.getParentPath();	//get the parent path
				parentPathRepositoryMap.addItem(parentPath, repository);	//associate this repository with the parent path
			}
			final Repository oldRepository=pathRepositoryMap.put(path.checkRelative().checkCollection(), checkInstance(repository, "Repository cannot be null."));
			//TODO unregister the old repository
			repository.setParentRepository(this);	//indicate that this is now the parent of the registered subrepository
			return oldRepository;	//return the previous repository, if any, registered for the given path
		}

		/**Returns the repository associated with the given path.
		@param path The relative collection path with which a repository may be associated.
		@return The repository associated with the given path, or <code>null</code> if there is no repository associated with the given path.
		@exception NullPointerException if the given content type is <code>null</code>.
		@exception NullPointerException if the given path is <code>null</code>.
		@exception IllegalArgumentException if the given path is not relative.
		@exception IllegalArgumentException if the given path does not represent a collection (i.e. it does not end with a path separator).
		*/
		public Repository getPathRepository(final URIPath path)
		{
			return pathRepositoryMap.get(path.checkRelative().checkCollection());	//return the repository, if any, associated with the given path
		}

		/**@return The read-only mapping of relative paths associated with repositories.*/
		public Map<URIPath, Repository> getPathRepositories()
		{
			return unmodifiableMap(pathRepositoryMap);	//return an unmodifiable version of the map
		}

		/**Sets the path repository associations to those specified in the given map.
		Any association will only override resources that do not explicitly have a charset specified.
		The current associations will be lost.
		@param pathRepositories The associations of paths to repositories.
		@exception NullPointerException if a given path and/or repository is <code>null</code>.
		@exception IllegalArgumentException if a given path is not relative.
		@exception IllegalArgumentException if a given path does not represent a collection (i.e. it does not end with a path separator).
		*/
		public void setPathRepositories(final Map<URIPath, Repository> pathRepositories)
		{
			pathRepositoryMap.clear();	//clear the current mappings
			parentPathRepositoryMap.clear();
			for(final Map.Entry<URIPath, Repository> pathRepositoryEntry:pathRepositories.entrySet())	//look at each mapping
			{
				registerPathRepository(pathRepositoryEntry.getKey(), pathRepositoryEntry.getValue());	//register this association
			}
		}
			
	/**Checks to make sure the resource designated by the given resource URI is within this repository.
	This version makes sure the given URI is a child of the resource reference URI.
	@param resourceURI The URI of the resource to check.
	@return The normalized form of the given resource.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	*/
	protected URI checkResourceURI(URI resourceURI)
	{
		resourceURI=checkInstance(resourceURI, "Resource URI cannot be null.").normalize();	//normalize the URI
		if(!isChild(getRootURI(), resourceURI))	//if the given resource URI does not designate a resource within this repository's URI namespace (this will normalize the URI, but as we need to return a normalized form it's better to normalize first so that actual normalization changes won't have to be done twice)
		{
			throw new IllegalArgumentException(resourceURI+" does not designate a resource within the repository "+getRootURI());
		}
		return resourceURI;	//return the normalized form of the resource URI
	}

	/**Determines if the given resource URI is physically located within a sub-repository mapped to a path within this resource.
	@param resourceURI The URI of a resource within this repository; must already be normalized.
	@return The repository in which the resource URI is physically located; either this repository or a sub-repository.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@see #checkResourceURI(URI)
	@see #getPathRepositories()
	*/
	protected Repository getSubrepository(final URI resourceURI)
	{
		final URI repositoryURI=getRootURI();	//get the URI of the repository
		final URIPath resourcePath=new URIPath(repositoryURI.relativize(resourceURI));	//get the path of the resource relative to the resource
		URIPath levelPath=resourcePath.getCurrentLevel();	//walk up the levels, starting at the current level
		while(!levelPath.isEmpty())	//while the resource path isn't empty
		{
			final Repository repository=pathRepositoryMap.get(levelPath);	//see if there is a repository mapped to this level
			if(repository!=null)	//if we found a repository mapped to this level
			{
				return repository;	//return the sub-repository
			}
			levelPath=levelPath.getParentLevel();	//look at the next higher level
		}
		return this;	//indicate that the resource isn't in any subrepository
	}

	/**Retrieves the subrepositories, if any, mapped under a given parent path.
	For example, if subrepositories are mapped to parent/sub1 and parent/sub2, getting child repositories
	for http://example.com/parent/ will return the two mapped subrepositories.
	@param parentResourceURI The URI of a resource within this repository that may be the parent of one or more subrepositories; should be a collection URI, and must already be normalized.
	@return A set of repositories mapped to paths which are direct children of the given resource URI.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	*/
	@SuppressWarnings("unchecked")
	protected Set<Repository> getChildSubrepositories(final URI parentResourceURI)
	{
		final URI repositoryURI=getRootURI();	//get the URI of the repository
		final URIPath resourcePath=new URIPath(repositoryURI.relativize(parentResourceURI));	//get the path of the resource relative to the resource
		final Set<Repository> childSubrepositories=parentPathRepositoryMap.get(resourcePath);	//see if there are any subrepositories mapped under the given parent resource URI
		return childSubrepositories!=null ? unmodifiableSet(childSubrepositories) : (Set<Repository>)EMPTY_SET;	//return an unmodifiablel set of the subrepositories, if there are any
	}

	/**A map of resource factories, keyed to namespace URIs.*/
	private final Map<URI, URFResourceFactory> namespaceURIResourceFactoryMap=new HashMap<URI, URFResourceFactory>();

		/**Registers a resource factory to be used to create resources with a type from the specified namespace.
		If a resource factory is already registered for this namespace, it will be replaced.
		@param typeNamespaceURI The namespace of the resource type for which this factory should be used to create objects.
		@param factory The resource factory that will be used to create resources of types from this namespace.
		*/
		public void registerResourceFactory(final URI typeNamespaceURI, final URFResourceFactory factory)
		{
			namespaceURIResourceFactoryMap.put(typeNamespaceURI, factory);
		}

		/**Removes the resource factory being used to create resources with a type from the specified namespace.
		If there is no resource factory registered for this namespace, no action will be taken.
		@param typeNamespaceURI The namespace of the resource type for which this factory should be used to create objects.
		*/
		public void unregisterResourceFactory(final URI typeNamespaceURI)
		{
			namespaceURIResourceFactoryMap.remove(typeNamespaceURI);
		}

	/**Creates and initializes default I/O for URF resource descriptions.
	@return Default URF resource description I/O.
	*/
	protected static URFIO<URFResource> createDefaultURFResourceDescriptionIO()
	{
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO=new URFResourceTURFIO<URFResource>(URFResource.class, URI.create(""));	//create a default resource description I/O using TURF
		urfResourceDescriptionIO.addNamespaceURI(MarmotSecurity.MARMOT_SECURITY_NAMESPACE_URI);	//tell the I/O about the security namespace
	//TODO del		urfResourceDescriptionIO.setFormatted(false);	//turn off formatting
		return urfResourceDescriptionIO;
	}

	/**Default constructor with no root URI defined.
	The root URI must be defined before the repository is opened.
	*/
	public AbstractRepository()
	{
		this(null);
	}

	/**URI constructor with no separate private URI namespace.
	@param repositoryURI The URI identifying the location of this repository.
	@exception NullPointerException if the given respository URI is <code>null</code>.
	*/
	public AbstractRepository(final URI repositoryURI)
	{
		this(repositoryURI, repositoryURI);	//use the same repository URI as the public and private namespaces
	}

	/**Public repository URI and private repository URI constructor.
	A {@link URFResourceTURFIO} description I/O is created and initialized.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@exception NullPointerException if one of the given respository URIs is <code>null</code>.
	*/
	public AbstractRepository(final URI publicRepositoryURI, final URI privateRepositoryURI)
	{
		this(publicRepositoryURI, privateRepositoryURI, createDefaultURFResourceDescriptionIO());	//create a default resource description I/O using TURF
	}

	/**Public repository URI and private repository URI constructor.
	@param publicRepositoryURI The URI identifying the location of this repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	@exception NullPointerException if one of the given respository URIs and/or the description I/O is <code>null</code>.
	*/
	public AbstractRepository(final URI publicRepositoryURI, final URI privateRepositoryURI, final URFIO<URFResource> descriptionIO)
	{
//TODO bring back		super(checkInstance(publicRepositoryURI, "Public repository URI cannot be null.").normalize());	//construct the parent class with the public reference URI
		this.rootURI=publicRepositoryURI!=null ? publicRepositoryURI.normalize() : null;
		this.sourceURI=privateRepositoryURI!=null ? privateRepositoryURI.normalize() : null;
//TODO del; update null-related documentation		this.privateRepositoryURI=checkInstance(privateRepositoryURI, "Private repository URI cannot be null.").normalize();
		this.descriptionIO=checkInstance(descriptionIO, "Description I/O cannot be null.");	//save the description I/O
		registerResourceFactory(MARMOT_NAMESPACE_URI, MARMOT_RESOURCE_FACTORY);	//register the Marmot factory
		registerResourceFactory(MARMOT_SECURITY_NAMESPACE_URI, MARMOT_SECURITY_RESOURCE_FACTORY);	//register the Marmot resource factory
	}

	/**Creates a repository of the same type as this repository with the same access privileges as this one.
	This factory method is commonly used to use a parent repository as a factory for other repositories in its namespace.
	This method resolves the private repository path to the current public repository URI.
	@param subrepositoryPath The private path relative to the private URI of this repository.
	@throws NullPointerException if the given private repository path is <code>null</code>.
	@throws IllegalArgumentException if the given subrepository path is absolute and/or is not a collection.
	*/
	public final Repository createSubrepository(final URIPath subrepositoryPath)
	{
		return createSubrepository(getRootURI().resolve(subrepositoryPath.checkRelative().checkCollection().toURI()), subrepositoryPath);	//resolve the subrepository path to the public repository URI		
	}
	
	/**Creates a repository of the same type as this repository with the same access privileges as this one.
	This factory method is commonly used to use a parent repository as a factory for other repositories in its namespace.
	@param publicRepositoryURI The public URI identifying the location of the new repository.
	@param privateSubrepositoryPath The private path relative to the private URI of this repository.
	@throws NullPointerException if the given public repository URI and/or private repository path is <code>null</code>.
	@throws IllegalArgumentException if the given private repository path is absolute and/or is not a collection.
	*/
	public final Repository createSubrepository(final URI publicRepositoryURI, final URIPath privateSubrepositoryPath)
	{
		return createSubrepository(publicRepositoryURI, getSourceURI().resolve(privateSubrepositoryPath.checkRelative().checkCollection().toURI()));	//resolve the subrepository path to the private repository URI		
	}

	/**Creates a repository of the same type as this repository with the same access privileges as this one.
	This factory method is commonly used to use a parent repository as a factory for other repositories in its namespace.
	@param publicRepositoryURI The public URI identifying the location of the new repository.
	@param privateRepositoryURI The URI identifying the private namespace managed by this repository.
	@throws NullPointerException if the given public repository URI and/or private repository URI is <code>null</code>.
	*/
	protected abstract Repository createSubrepository(final URI publicRepositoryURI, final URI privateRepositoryURI);

	/**Creates a default empty URF data model.
	The correct resource factories will be installed to create appropriate classes in the Marmot namespace.
	@return A new default URF data model.
	*/
	protected URF createURF()
	{
		final URF urf=new URF();	//create a new URF data model
		for(final Map.Entry<URI, URFResourceFactory> namespaceURIResourceFactoryMapEntry:namespaceURIResourceFactoryMap.entrySet())	//for each resource factory and corresponding URI
		{
			urf.registerResourceFactory(namespaceURIResourceFactoryMapEntry.getKey(), namespaceURIResourceFactoryMapEntry.getValue());	//register the resource factories with the URF data model
		}
		return urf;	//return the new data model
	}

	/**@return Whether the repository has been opened for access.*/
	public boolean isOpen() {return open;}

	/**Opens the repository for access.
	If the repository is already open, no action occurs.
	At a minimum the respository must have a public and a private URI specified, even though these may both be the same URI.
	@exception IllegalStateException if the settings of this repository are inadequate to open the repository.
	@exception ResourceIOException if there is an error opening the repository.
	*/
	public void open() throws ResourceIOException
	{
		if(!isOpen())	//if the repository isn't yet open TODO synchronize
		{
			if(getSourceURI()==null)	//if the private repository URI is not set
			{
				throw new IllegalStateException("Cannot open repository without private repository URI specified.");
			}
			if(getRootURI()==null)	//if the public repository URI is not set
			{
				throw new IllegalStateException("Cannot open repository without public repository URI specified.");
			}
			open=true;	//show that the repository is now open
		}
	}

	/**Closes the repository.
	If the repository is already closed, no action occurs.
	@exeption ResourceIOException if there is an error closing the repository.
	*/
	public void close() throws ResourceIOException
	{
		if(isOpen())	//if the repository is open TODO synchronize
		{
			open=false;	//show that the repository is now closed
		}
	}

	/**Retrieves the live properties, which dynamically determined attributes of the resource such as content size. 
	@return The thread-safe set of URIs of live properties.
	*/
	public Set<URI> getLivePropertyURIs() {return LIVE_PROPERTY_URIS;}

	/**Determines whether the indicated property is is a live, dynamically determined property.
	@param propertyURI The URI identifying the property.
	@return <code>true</code> if the property is a live property.
	@throws NullPointerException if the given property URI is <code>null</code>.
	*/
	public boolean isLivePropertyURI(final URI propertyURI)
	{
		return getLivePropertyURIs().contains(checkInstance(propertyURI, "Property URI cannot be null."));
	}

	/**Gets an output stream to the contents of the resource specified by the given URI.
	The content modified datetime is set to the current date and time.
	An error is generated if the resource does not exist.
	@param resourceURI The URI of the resource to access.
	@return An output stream to the resource represented by the given URI.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the resource.
	@see Content#MODIFIED_PROPERTY_URI
	*/
	public final OutputStream getResourceOutputStream(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getResourceOutputStream(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		return getResourceOutputStream(resourceURI, new URFDateTime());	//get an output stream with a new modified datetime of now
	}
	
	/**Creates all the parent resources necessary for a resource to exist at the given URI.
	If any parent resources already exist, they will not be replaced.
	@param resourceURI The reference URI of a resource which may not exist.
	@return A description of the most immediate parent resource created, or <code>null</code> if no parent resources were required to be created.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if a parent resource could not be created.
	*/
	public URFResource createParentResources(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createParentResources(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		if(resourceURI.equals(getRootURI()))	//if this is the resource URI
		{
			return null;	//we didn't have to create anything
		}
		final URI parentResourceURI=getParentResourceURI(resourceURI);	//get the parent resource URI
		URFResource lastCreatedParentResource=createParentResources(parentResourceURI);	//create any necessary parents of the parent
		if(!resourceExists(parentResourceURI))	//if the parent does not exist
		{
			lastCreatedParentResource=createResource(parentResourceURI, NO_BYTES);	//create the parent collection
		}
		return lastCreatedParentResource;	//return the last parent created, if any
	}

	/**Creates a new resource with a default description and returns an output stream for writing the contents of the resource.
	If a resource already exists at the given URI it will be replaced.
	The returned output stream should always be closed.
	The {@link Content#CREATED_PROPERTY_URI} property will be added with the current date and time.
	The {@link Content#MODIFIED_PROPERTY_URI} property will be added with the current date and time.
	If a resource with no contents is desired, {@link #createResource(URI, byte[])} with zero bytes is better suited for this task.
	This implementation delegates to {@link #createResource(URI, URFResource)} with a default description.
	@param resourceURI The reference URI to use to identify the resource.
	@return An output stream for storing the contents of the resource.
	@exception NullPointerException if the given resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public final OutputStream createResource(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createResource(resourceURI);	//delegate to the subrepository
		}
		final URFResource resourceDescription=new DefaultURFResource(resourceURI);
		final URFDateTime dateTime=new URFDateTime();	//create a new timestamp representing this instant
		Content.setCreated(resourceDescription, dateTime);	//set the created and modified times to the current time 
		Content.setCreated(resourceDescription, dateTime);
		return createResource(resourceURI, resourceDescription);	//create the resource with the default description
	}

	/**Creates a new resource with a default description and contents.
	If a resource already exists at the given URI it will be replaced.
	The {@link Content#CREATED_PROPERTY_URI} property will be added with the current date and time.
	The {@link Content#MODIFIED_PROPERTY_URI} property will be added with the current date and time.
	This implementation delegates to {@link #createResource(URI, URFResource, byte[])} with a default description.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceContents The contents to store in the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given resource URI and/or resource contents is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource could not be created.
	*/
	public final URFResource createResource(URI resourceURI, final byte[] resourceContents) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.createResource(resourceURI, resourceContents);	//delegate to the subrepository
		}
		final URFResource resourceDescription=new DefaultURFResource(resourceURI);
		final URFDateTime dateTime=new URFDateTime();	//create a new timestamp representing this instant
		Content.setCreated(resourceDescription, dateTime);	//set the created and modified times to the current time 
		Content.setCreated(resourceDescription, dateTime);
		return createResource(resourceURI, resourceDescription, resourceContents);	//create the resource with the default description
	}

	/**Retrieves child resources of the resource at the given URI.
	This implementation retrieves a single-level list of unfiltered child resources by calling {@link #getChildResourceDescriptions(URI, int)}.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public final List<URFResource> getChildResourceDescriptions(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getChildResourceDescriptions(resourceURI);	//delegate to the subrepository
		}
		return getChildResourceDescriptions(resourceURI, 1);	//get a list of child resource descriptions without going deeper than one level
	}

	/**Retrieves child resources of the resource at the given URI.
	This implementation retrieves a single-level list of descriptions by delegating to {@link #getChildResourceDescriptions(URI, ResourceFilter, int)}.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be filtered.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public final List<URFResource> getChildResourceDescriptions(URI resourceURI, final ResourceFilter resourceFilter) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getChildResourceDescriptions(resourceURI, resourceFilter);	//delegate to the subrepository
		}
		return getChildResourceDescriptions(resourceURI, resourceFilter, 1);	//get a list of child resource descriptions without going deeper than one level
	}

	/**Retrieves child resources of the resource at the given URI.
	This implementation retrieves an unfiltered list of child resources by delegating to {@link #getChildResourceDescriptions(URI, ResourceFilter, int)}.
	@param resourceURI The URI of the resource for which sub-resources should be returned.
	@param depth The zero-based depth of child resources which should recursively be retrieved, or {@link Repository#INFINITE_DEPTH} for an infinite depth.
	@return A list of sub-resource descriptions under the given resource.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public final List<URFResource> getChildResourceDescriptions(URI resourceURI, final int depth) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getChildResourceDescriptions(resourceURI, depth);	//delegate to the subrepository
		}
		return getChildResourceDescriptions(resourceURI, null, depth);	//get a list of child resource descriptions without filtering
	}

	/**Adds properties to a given resource.
	All existing properties will be left unmodified.
	This implementation creates an {@link URFResourceAlteration} and delegates to {@link #alterResourceProperties(URI, URFResourceAlteration)}.
	@param resourceURI The reference URI of the resource.
	@param properties The properties to set.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or properties is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource addResourceProperties(URI resourceURI, final URFProperty... properties) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.addResourceProperties(resourceURI, properties);	//delegate to the subrepository
		}
		return alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createAddPropertiesAlteration(properties));	//create an alteration for adding properties and alter the resource
	}

	/**Sets the properties of a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified.
	This implementation creates an {@link URFResourceAlteration} and delegates to {@link #alterResourceProperties(URI, URFResourceAlteration)}.
	@param resourceURI The reference URI of the resource.
	@param properties The properties to set.
	@return The updated description of the resource.
	@exception NullPointerException if the given resource URI and/or properties is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if the resource properties could not be updated.
	*/
	public URFResource setResourceProperties(URI resourceURI, final URFProperty... properties) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.setResourceProperties(resourceURI, properties);	//delegate to the subrepository
		}
		return alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createSetPropertiesAlteration(properties));	//create an alteration for setting properties and alter the resource
	}

	/**Removes properties from a given resource.
	Any existing properties with the same URIs as the given given property/value pairs will be removed.
	All other existing properties will be left unmodified. 
	This implementation creates an {@link URFResourceAlteration} and delegates to {@link #alterResourceProperties(URI, URFResourceAlteration)}.
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
			return subrepository.removeResourceProperties(resourceURI, propertyURIs);	//delegate to the subrepository
		}
		return alterResourceProperties(resourceURI, DefaultURFResourceAlteration.createRemovePropertiesAlteration(propertyURIs));	//create an alteration for removing properties and alter the resource
	}

	/**Determines the URI of the collection resource of the given URI; either the given resource URI if the resource represents a collection, or the parent resource if not.
	If the given resource URI is a collection URI this method returns the given resource URI.
	If the given resource URI is not a collection URI, this implementation returns the equivalent of resolving the path {@value URIs#CURRENT_LEVEL_PATH_SEGMENT} to the URI.
	@param resourceURI The URI of the resource for which the collection resource URI should be returned.
	@return The URI of the indicated resource's deepest collection resource, or <code>null</code> if the given URI designates a non-collection resource with no collection parent.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URI getCollectionURI(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			return subrepository.getCollectionURI(resourceURI);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		return isCollectionURI(resourceURI) ? resourceURI : getCurrentLevel(resourceURI);	//if URI is a collection URI, return the URI; otherwise, get the current level
	}

	/**Determines the URI of the parent resource of the given URI.
	If the given resource URI is a collection URI this implementation returns the equivalent of resolving the path {@value URIs#PARENT_LEVEL_PATH_SEGMENT} to the URI.
	if the given resource URI is not a collection URI, this implementation returns the equivalent of resolving the path {@value URIs#CURRENT_LEVEL_PATH_SEGMENT} to the URI.
	If the given resource represents this repository, this implementation returns <code>null</code>.
	@param resourceURI The URI of the resource for which the parent resource URI should be returned.
	@return The URI of the indicated resource's parent resource, or <code>null</code> if the given URI designates a resource with no parent.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public URI getParentResourceURI(URI resourceURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			if(!subrepository.getRootURI().equals(resourceURI))	//don't ask the subrepository's root URI for a parent resource URI, as the repository has no parent URI in terms of that repository
			{
				return subrepository.getParentResourceURI(resourceURI);	//delegate to the subrepository
			}
		}
		checkOpen();	//make sure the repository is open
		if(resourceURI.equals(getRootURI()))	//if the resource is the repository URI
		{
			return null;	//the repository level has no parent
		}
		final URI parentResourceURI=isCollectionURI(resourceURI) ? getParentLevel(resourceURI) : getCurrentLevel(resourceURI);	//if resource is a collection URI, get the parent level; otherwise, get the current level
		assert !resourceURI.equals(parentResourceURI) : "Parent URI should be different from the resource URI.";	//the resource URI will never be null, but the parent resource URI may be---but they should never be the same (default URI resolution won't work for opaque URIs)
		return parentResourceURI;	//return the URI of the parent resource
	}

	/**Creates an infinitely deep copy of a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link Repository#copyResource(URI, URI, boolean)}.
	@param resourceURI The URI of the resource to be copied.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	*/
	public void copyResource(URI resourceURI, final URI destinationURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			copyResource(resourceURI, destinationURI);	//delegate to the subrepository
		}
		copyResource(resourceURI, destinationURI, true);	//copy the resource, overwriting any resource at the destination
	}

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link #copyResource(URI, Repository, URI, boolean)}.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	*/
	public void copyResource(URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.copyResource(resourceURI, destinationRepository, destinationURI);	//delegate to the subrepository
		}
		copyResource(resourceURI, destinationRepository, destinationURI, true);	//copy the resource, overwriting any resource at the destination
	}

	/**Creates an infinitely deep copy of a resource to the specified URI in the specified repository, overwriting any resource at the destionation only if requested.
	This version delegates to {@link Repository#copyResource(URI, URI, boolean)} if the given repository is this repository.
	Otherwise, this version performs a default copy operation.
	@param resourceURI The URI of the resource to be copied.
	@param destinationRepository The repository to which the resource should be copied, which may be this repository.
	@param destinationURI The URI to which the resource should be copied.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception ResourceIOException if there is an error copying the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void copyResource(URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.copyResource(resourceURI, destinationRepository, destinationURI, overwrite);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		if(destinationRepository==this)	//if the resource is being copied to this repository
		{
			copyResource(resourceURI, destinationURI, overwrite);	//delegate to the internal copy method
		}
		else	//if the resource is being copied to another repository
		{
			try
			{
	//TODO del Debug.trace("ready to create resource", destinationURI, "in destination repository", destinationRepository.getReferenceURI());
				final boolean isCollection=isCollectionURI(resourceURI);	//see if the resource is a collection
				final URFResource resourceDescription=getResourceDescription(resourceURI);	//get a description of the resource
				final long contentLength=getContentLength(resourceDescription);	//get the size of the resource content
				if(contentLength==0)	//if this is a resource with no content, don't needlessly create content (especially important for collections)
				{
					destinationRepository.createResource(destinationURI, resourceDescription, NO_BYTES);	//create a zero-byte resource with the given description
				}
				else	//if there is content
				{
						//TODO check for non-existent source resource
					final InputStream inputStream=getResourceInputStream(resourceURI);	//get an input stream to the source resource
					try
					{
							//TODO create an overwrite-aware createResource() method
						final OutputStream outputStream=destinationRepository.createResource(destinationURI, resourceDescription);	//create the destination resource with the same description as the source resource, getting an output stream for storing the contents
						try
						{
							InputStreams.copy(inputStream, outputStream);	//copy the resource
						}
						finally
						{
							outputStream.close();	//always close the output stream
						}
					}
					finally
					{
						inputStream.close();	//always close the input stream
					}
				}
				//TODO copy the child resources
			}
			catch(final IOException ioException)	//if an I/O exception occurs
			{
				throw createResourceIOException(resourceURI, ioException);	//translate the exception to a resource I/O exception and throw that
			}
		}
	}

	/**Moves a resource to another URI in this repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link Repository#moveResource(URI, URI, boolean)}.
	@param resourceURI The URI of the resource to be moved.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	*/
	public void moveResource(URI resourceURI, final URI destinationURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.moveResource(resourceURI, destinationURI);	//delegate to the subrepository
		}
		moveResource(resourceURI, destinationURI, true);	//move the resource, overwriting any resource at the destination
	}

	/**Moves a resource to the specified URI in the specified repository.
	Any resource at the destination URI will be replaced.
	This version delegates to {@link #moveResource(URI, Repository, URI, boolean)}.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	*/
	public void moveResource(URI resourceURI, final Repository destinationRepository, final URI destinationURI) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.moveResource(resourceURI, destinationRepository, destinationURI);	//delegate to the subrepository
		}
		moveResource(resourceURI, destinationRepository, destinationURI, true);	//move the resource, overwriting any resource at the destination
	}

	/**Moves a resource to the specified URI in the specified repository, overwriting any resource at the destionation only if requested.
	This version delegates to {@link Repository#moveResource(URI, URI, boolean)} if the given repository is this repository.
	Otherwise, this version delegates to {@link Repository#copyResource(URI, Repository, URI, boolean)} and then delegates to {@link Repository#deleteRepository(URI)}.
	@param resourceURI The URI of the resource to be moved.
	@param destinationRepository The repository to which the resource should be moved, which may be this repository.
	@param destinationURI The URI to which the resource should be moved.
	@param overwrite <code>true</code> if any existing resource at the destination should be overwritten,
		or <code>false</code> if an existing resource at the destination should cause an exception to be thrown.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception IllegalStateException if the repository is not open for access and auto-open is not enabled.
	@exception IllegalArgumentException if the given resource URI is the base URI of the repository.
	@exception ResourceIOException if there is an error moving the resource.
	@exception ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	*/
	public void moveResource(URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite) throws ResourceIOException
	{
		resourceURI=checkResourceURI(resourceURI);	//makes sure the resource URI is valid and normalize the URI
		final Repository subrepository=getSubrepository(resourceURI);	//see if the resource URI lies within a subrepository
		if(subrepository!=this)	//if the resource URI lies within a subrepository
		{
			subrepository.moveResource(resourceURI, destinationRepository, destinationURI, overwrite);	//delegate to the subrepository
		}
		checkOpen();	//make sure the repository is open
		if(destinationRepository==this)	//if the resource is being moved to this repository
		{
			moveResource(resourceURI, destinationURI, overwrite);	//delegate to the internal move method
		}
		else	//if the resource is being moved to another repository
		{
			if(resourceURI.normalize().equals(getRootURI()))	//if they try to move the root URI
			{
				throw new IllegalArgumentException("Cannot move repository base URI "+resourceURI);
			}
			copyResource(resourceURI, destinationRepository, destinationURI, overwrite);	//copy the resource to the other repository
			deleteResource(resourceURI);	//delete the moved resource
		}
	}

	/**Updates the {@value Content#TYPE_PROPERTY_URI} property of the given resource.
	This method should be called before each non-collection resource description is returned.
	If the resource has no {@value Content#TYPE_PROPERTY_URI} property defined, a content type will be looked up from the extension of the resource name, if any, using {@link #getExtensionContentType(Charset)}.
	No default content type is provided for a resource with a collection URI (i.e. a URI ending in {@value URIs#PATH_SEPARATOR}).
	If the resource has no {@value Content#CHARSET_PROPERTY_URI} property defined, a charset will be determined if possible using {@link #getContentTypeCharset(ContentType)}.
	@param resource The resource the content type of which should be updated.
	@see Content#TYPE_PROPERTY_URI
	@see Content#CHARSET_PROPERTY_URI
	*/
/*TODO del when works; transferred to MarmotSession
	protected void updateContentType(final URFResource resource)	//TODO consider passing a MarmotSession to all the repository methods, and asking the MarmotSession for defaults 
	{
		ContentType contentType=getContentType(resource);	//get the specified content type of the resource
		if(contentType==null)	//if no content type is specified
		{
			final URI resourceURI=resource.getURI();	//get the resource URI
			final String resourceName=resourceURI!=null && !isCollectionURI(resourceURI) ? URIs.getName(resourceURI) : null;	//get the resource name, if any
			if(resourceName!=null && !resourceName.isEmpty())	//if we have a non-empty name (only collections URIs should return empty names, so this non-empty verification is redundant)
			{
				contentType=getExtensionContentType(getNameExtension(resourceName));	//get the registered content type, if any, for the resource's extension (which may be null)
				if(contentType!=null)	//if we found a content type
				{
					setContentType(resource, contentType);	//update the content type property
				}
			}
		}
		if(contentType!=null)	//if we know a content type, update the charset, if needed
		{
			Charset charset=getCharset(resource);	//get the specified charset of the resource
			if(charset==null)	//if no charset is specified
			{
				charset=getContentTypeCharset(contentType);	//get the registered charset, if any, for the content type
				if(charset!=null)	//if we found a charset
				{
					setCharset(resource, charset);	//update the charset property
				}
			}
		}
	}
*/

	/**Determines whether a resource, identified by its private URI, should be made available in the public space.
	If this method returns <code>false</code>, the identified resource will essentially become invisible past the {@link Repository} interface.
	Such resources are normally used internally with special semantics to the repository implementation.
	This version returns <code>true</code> for all resources except:
	<ul>
		<li>The special collection contents resource named {@value #COLLECTION_CONTENT_NAME}.</li>
	</ul>
	@param privateResourceURI The private URI of a resource.
	@return <code>true</code> if the resource should be visible as normal, or <code>false</code> if the resource should not be made available to the public space.
	@exception NullPointerException if the given URI is <code>null</code>.
	*/
	protected boolean isSourceResourcePublic(final URI privateResourceURI)	//TODO maybe change to a path to allow non-hierarchical source repositories to use, or transfer to AbstractHierarchicalSourceRepository
	{
		final String rawName=getRawName(privateResourceURI);	//get the raw name of the resource
		if(COLLECTION_CONTENT_NAME.equals(rawName))	//if this is the collection contents
		{
			return false;	//don't publish the collection contents file
		}
		return true;	//publish all other resources
	}

	/**Translates the given error specific to the this repository type into a resource I/O exception.
	This version returns the given throwable if it is already a {@link ResourceIOException};
	otherwise, it simply wraps the given throwable in a {@link ResourceIOException}.
	@param resourceURI The URI of the resource to which the exception is related.
	@param throwable The error which should be translated to a resource I/O exception.
	@return A resource I/O exception based upon the given throwable.
	*/
	protected ResourceIOException createResourceIOException(final URI resourceURI, final Throwable throwable)
	{
		return throwable instanceof ResourceIOException ? (ResourceIOException)throwable : new ResourceIOException(resourceURI, throwable);	//default to simple exception chaining with a new resource I/O exception, if the throwable isn't already a resourc I/O exception
	}

	/**Determines whether the given resource has properties that are not live.
	@param resourceDescription The description of the resource.
	*/
/*TODO del
	protected boolean hasDeadProperties(final URFResource resourceDescription)
	{
		
	}
*/

	/**Cleans up the object for garbage collection.
	This version closes the repository.
	*/
	protected void finalize() throws Throwable
	{
		try
		{
			close();	//close the repository if it isn't already
		}
		finally
		{
			super.finalize();	//always call the parent version
		}
	}
}

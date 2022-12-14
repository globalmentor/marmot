/*
 * Copyright © 1996-2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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
import java.util.Collections;

import org.urframework.*;
import org.urframework.content.Content;
import org.urframework.io.URFIO;
import org.urframework.io.URFResourceTURFIO;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Arrays.*;
import static java.util.Collections.*;
import static java.util.Objects.*;
import static org.urframework.URF.*;
import static org.urframework.content.Content.*;

import com.globalmentor.collections.*;
import com.globalmentor.event.ProgressListener;
import com.globalmentor.io.*;
import com.globalmentor.iso.datetime.ISODateTime;
import com.globalmentor.java.Strings;
import com.globalmentor.log.Log;
import com.globalmentor.marmot.Marmot;
import com.globalmentor.marmot.security.MarmotSecurity;
import com.globalmentor.model.*;
import com.globalmentor.net.*;

import static com.globalmentor.collections.Sets.*;
import static com.globalmentor.io.Files.*;
import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.Characters.*;
import static com.globalmentor.java.Conditions.*;
import static com.globalmentor.marmot.security.MarmotSecurity.*;
import static com.globalmentor.net.URIs.*;

/**
 * Abstract implementation of a repository with typical features.
 * <p>
 * This implementation guarantees that opening and closing the repository, as well as ensuring the repository is open via the {@link #checkOpen()} method, is
 * thread-safe.
 * </p>
 * <p>
 * This implementation uses the special name {@value #COLLECTION_CONTENT_NAME} to represent the contents (as opposed to the contained resources) of a collection
 * resource.
 * </p>
 * <p>
 * Resource access methods should call {@link #checkResourceURI(URI)} as a security check to ensure the given URI is within the repository.
 * </p>
 * <p>
 * This implementation considers the following properties to be live properties:
 * </p>
 * <ul>
 * <li>{@link Content#ACCESSED_PROPERTY_URI}</li>
 * <li>{@link Content#LENGTH_PROPERTY_URI}</li>
 * </ul>
 * <p>
 * This implementation initializes the map of extension contents to {@link Files#FILE_EXTENSION_CONTENT_TYPE_MAP}.
 * </p>
 * <p>
 * To assist in implementing repositories that don't support custom namespaces, this class allows encoding of URF properties by using some other namespace
 * namespace with a local name encoded version of the URF property URI. The standard URI escape character, {@value URIs#ESCAPE_CHAR}, is not a valid name
 * character.
 * </p>
 * @author Garret Wilson
 */
public abstract class AbstractRepository implements Repository {

	/** The resource factory for resources in the Marmot namespace. */
	protected static final URFResourceFactory MARMOT_RESOURCE_FACTORY = new JavaURFResourceFactory(Marmot.class.getPackage());
	/** The resource factory for resources in the Marmot security namespace. */
	protected static final URFResourceFactory MARMOT_SECURITY_RESOURCE_FACTORY = new JavaURFResourceFactory(MarmotSecurity.class.getPackage());

	/** The set of URIs that are considered live by default. */
	public static final Set<URI> DEFAULT_LIVE_PROPERTY_URIS = immutableSetOf(Content.ACCESSED_PROPERTY_URI, Content.LENGTH_PROPERTY_URI);

	/**
	 * The name of a resource used to store the content of a collection. Normally the only properties stored with this special resource are the
	 * {@link Content#LENGTH_PROPERTY_URI} and {@link Content#MODIFIED_PROPERTY_URI} properties; all other properties are stored with the logical collection
	 * resource the contents of which this hidden resource contains.
	 */
	public static final String COLLECTION_CONTENT_NAME = "@"; //TODO add checks to prevent this resource from being accessed directly

	/** The set of URIs that are considered live. */
	private Set<URI> livePropertyURIs = DEFAULT_LIVE_PROPERTY_URIS;

	/**
	 * Adds a property considered live.
	 * @param livePropertyURI The additional property to consider live.
	 */
	protected void addLivePropertyURI(final URI livePropertyURI) {
		livePropertyURIs = immutableSetOf(livePropertyURIs, livePropertyURI); //add this live property to our existing live properties
	}

	@Override
	public Set<URI> getLivePropertyURIs() {
		return livePropertyURIs;
	}

	@Override
	public boolean isLivePropertyURI(final URI propertyURI) {
		return getLivePropertyURIs().contains(requireNonNull(propertyURI, "Property URI cannot be null."));
	}

	/** The I/O implementation that writes and reads a resource with the same reference URI as its base URI. */
	private final URFIO<URFResource> descriptionIO;

	/** @return The I/O implementation that writes and reads a resource with the same reference URI as its base URI. */
	protected URFIO<URFResource> getDescriptionIO() {
		return descriptionIO;
	}

	/** The registered event listeners. */
	//TODO bring back when needed	protected final EventListenerManager eventListenerManager=new EventListenerManager();

	/**
	 * Sets the URI. If there currently is no private repository URI, it will be updated to match the given public repository URI.
	 * @param uri The new URI, or <code>null</code> if there is no URI.
	 */
	/*TODO fix
		protected void setURI(final URI uri)
		{
				//TODO check for the URI being set to null
	//TODO bring back		super.setURI(uri);	//set the URI normally
			if(getPrivateRepositoryURI()==null) {	//if there is no private repository URI
				setPrivateRepositoryURI(uri);	//update the private repository URI to match
			}
		}
	*/

	/** The parent repository, or <code>null</code> if this repository has not been registered as a subrepository of another repository. */
	private Repository parent = null;

	@Override
	public Repository getParentRepository() {
		return parent;
	}

	@Override
	public void setParentRepository(final Repository newParent) {
		if(parent != null && newParent != null && newParent != parent) { //if the parent is being changed without first removing the old parent
			throw new IllegalStateException("Repository parent cannot be changed without first unregistering.");
		}
		parent = newParent;
	}

	@Override
	public Repository getRootRepository() {
		Repository rootRepository = this;
		Repository parentRepository;
		while((parentRepository = rootRepository.getParentRepository()) != null) { //walk up the chain until we run out of parent repositories
			rootRepository = parentRepository; //move the root up a level
		}
		return rootRepository; //return whatever root repository we determined
	}

	/** Whether the repository has been opened for access. */
	private ReadWriteLockObjectHolder<Boolean> open = new ReadWriteLockObjectHolder<Boolean>(Boolean.FALSE);

	/** The base URI of the public URI namespace being managed. */
	private URI rootURI = null;

	@Override
	public URI getRootURI() {
		return rootURI;
	}

	@Override
	public void setRootURI(final URI rootURI) {
		this.rootURI = normalize(requireNonNull(rootURI, "Root URI must not be null."));
		for(final Map.Entry<URIPath, Repository> pathRepositoryEntry : pathRepositoryMap.entrySet()) { //look at each path to repository mapping
			pathRepositoryEntry.getValue().setRootURI(resolve(getRootURI(), pathRepositoryEntry.getKey())); //update the public URI of the repository to match its location in the repository
		}
	}

	/** Whether the repository should automatically be opened when needed. */
	private boolean autoOpen = true;

	/** @return Whether the repository should automatically be opened when needed. */
	public boolean isAutoOpen() {
		return autoOpen;
	}

	/**
	 * Sets whether the repository should automatically be opened when needed.
	 * @param autoOpen Whether the repository should automatically be opened when needed.
	 */
	public void setAutoOpen(final boolean autoOpen) {
		this.autoOpen = autoOpen;
	}

	/**
	 * Checks to make sure that the repository is open. If the auto-open facility is turned on, the repository will be automatically opened if needed.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceIOException if there is an error opening the repository.
	 * @see #isAutoOpen()
	 */
	protected void checkOpen() throws ResourceIOException {
		if(!isOpen()) { //if the repository is not open
			if(isAutoOpen()) { //if we should open the repository automatically
				open(); //open the repository; the race condition here is benign, as the open() method will check again to make sure the repository isn't open
			} else { //if we shouldn't open the repository automatically
				throw new IllegalStateException("Repository is not open.");
			}
		}
	}

	/** The map of content types mapped to lowercase URI name extensions. */
	private final Map<String, ContentType> extensionContentTypeMap = new HashMap<String, ContentType>(FILE_EXTENSION_CONTENT_TYPE_MAP);

	@Override
	public ContentType registerExtensionContentType(final String extension, final ContentType contentType) {
		return extensionContentTypeMap.put(extension != null ? extension.toLowerCase() : null, requireNonNull(contentType, "Content type cannot be null."));
	}

	@Override
	public ContentType getExtensionContentType(final String extension) {
		return extensionContentTypeMap.get(extension != null ? extension.toLowerCase() : null); //return the content type, if any, associated with the given extension
	}

	/** The map of charsets mapped to base media types. */
	private final Map<String, Charset> baseContentTypeCharsetMap = new HashMap<String, Charset>();

	@Override
	public Charset registerContentTypeCharset(final ContentType contentType, final Charset charset) {
		return baseContentTypeCharsetMap.put(contentType.getBaseType(), requireNonNull(charset, "Charset cannot be null."));
	}

	@Override
	public Charset getContentTypeCharset(final ContentType contentType) {
		return baseContentTypeCharsetMap.get(contentType.getBaseType()); //return the charset, if any, associated with the given base content type
	}

	@Override
	public Map<ContentType, Charset> getContentTypeCharsets() {
		final Map<ContentType, Charset> contentTypeCharsetMap = new HashMap<ContentType, Charset>(baseContentTypeCharsetMap.size()); //create a new map to hold actual content type objects
		for(final Map.Entry<String, Charset> baseContentTypeCharsetEntry : baseContentTypeCharsetMap.entrySet()) { //look at each mapping
			contentTypeCharsetMap.put(ContentType.create(baseContentTypeCharsetEntry.getKey()), baseContentTypeCharsetEntry.getValue()); //add this mapping to the map
		}
		return unmodifiableMap(contentTypeCharsetMap); //return a read-only version of the map we created
	}

	@Override
	public void setContentTypeCharsets(final Map<ContentType, Charset> contentTypeCharsets) {
		baseContentTypeCharsetMap.clear(); //clear the current mappings
		for(final Map.Entry<ContentType, Charset> contentTypeCharsetEntry : contentTypeCharsets.entrySet()) { //look at each mapping
			registerContentTypeCharset(contentTypeCharsetEntry.getKey(), contentTypeCharsetEntry.getValue()); //register this association
		}
	}

	/** The map of repositories keyed to relative collection paths. */
	private final Map<URIPath, Repository> pathRepositoryMap = new HashMap<URIPath, Repository>();

	/** The map of repositories pairs keyed to relative parent collection paths. */
	private final CollectionMap<URIPath, Repository, Set<Repository>> parentPathRepositoryMap = new HashSetHashMap<URIPath, Repository>();

	@Override
	public Repository registerPathRepository(final URIPath path, final Repository repository) {
		if(getRootURI() != null) { //if the root URI has been initialized
			repository.setRootURI(resolve(getRootURI(), path)); //update the public URI of the repository to match its location in the repository
		}
		if(!URIPath.ROOT_URI_PATH.equals(path)) { //if this is not the root path (it's not normal to map the root path to another repository, but check for it anyway)
			final URIPath parentPath = path.getParentPath(); //get the parent path
			parentPathRepositoryMap.addItem(parentPath, repository); //associate this repository with the parent path
		}
		final Repository oldRepository = pathRepositoryMap.put(path.checkRelative().checkCollection(), requireNonNull(repository, "Repository cannot be null."));
		//TODO unregister the old repository
		repository.setParentRepository(this); //indicate that this is now the parent of the registered subrepository
		return oldRepository; //return the previous repository, if any, registered for the given path
	}

	@Override
	public Repository getPathRepository(final URIPath path) {
		return pathRepositoryMap.get(path.checkRelative().checkCollection()); //return the repository, if any, associated with the given path
	}

	@Override
	public Map<URIPath, Repository> getPathRepositories() {
		return unmodifiableMap(pathRepositoryMap); //return an unmodifiable version of the map
	}

	@Override
	public void setPathRepositories(final Map<URIPath, Repository> pathRepositories) {
		pathRepositoryMap.clear(); //clear the current mappings
		parentPathRepositoryMap.clear();
		for(final Map.Entry<URIPath, Repository> pathRepositoryEntry : pathRepositories.entrySet()) { //look at each mapping
			registerPathRepository(pathRepositoryEntry.getKey(), pathRepositoryEntry.getValue()); //register this association
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version makes sure the given URI is a child of the repository root URI.
	 * </p>
	 * @see #getRootURI()
	 */
	@Override
	public URI checkResourceURI(URI resourceURI) throws IllegalArgumentException {
		resourceURI = canonicalize(normalize(requireNonNull(resourceURI, "Resource URI cannot be null."))); //normalize and canonicalize the URI
		if(!isChild(getRootURI(), resourceURI)) { //if the given resource URI does not designate a resource within this repository's URI namespace (this will normalize the URI, but as we need to return a normalized form it's better to normalize first so that actual normalization changes won't have to be done twice)
			throw new IllegalArgumentException(resourceURI + " does not designate a resource within the repository " + getRootURI());
		}
		return resourceURI; //return the normalized form of the resource URI
	}

	/**
	 * Determines if the given resource URI is physically located within a sub-repository mapped to a path within this resource.
	 * @param resourceURI The URI of a resource within this repository; must already be normalized.
	 * @return The repository in which the resource URI is physically located; either this repository or a sub-repository.
	 * @throws NullPointerException if the given resource URI is <code>null</code>.
	 * @see #checkResourceURI(URI)
	 * @see #getPathRepositories()
	 */
	protected Repository getSubrepository(final URI resourceURI) {
		final URI repositoryURI = getRootURI(); //get the URI of the repository
		final URIPath resourcePath = new URIPath(repositoryURI.relativize(resourceURI)); //get the path of the resource relative to the resource
		URIPath levelPath = resourcePath.getCurrentLevel(); //walk up the levels, starting at the current level
		while(!levelPath.isEmpty()) { //while the resource path isn't empty
			final Repository repository = pathRepositoryMap.get(levelPath); //see if there is a repository mapped to this level
			if(repository != null) { //if we found a repository mapped to this level
				return repository; //return the sub-repository
			}
			levelPath = levelPath.getParentLevel(); //look at the next higher level
		}
		return this; //indicate that the resource isn't in any subrepository
	}

	/**
	 * Retrieves the subrepositories, if any, mapped under a given parent path. For example, if subrepositories are mapped to parent/sub1 and parent/sub2, getting
	 * child repositories for http://example.com/parent/ will return the two mapped subrepositories.
	 * @param parentResourceURI The URI of a resource within this repository that may be the parent of one or more subrepositories; should be a collection URI,
	 *          and must already be normalized.
	 * @return A set of repositories mapped to paths which are direct children of the given resource URI.
	 * @throws NullPointerException if the given resource URI is <code>null</code>.
	 */
	protected Set<Repository> getChildSubrepositories(final URI parentResourceURI) {
		final URI repositoryURI = getRootURI(); //get the URI of the repository
		final URIPath resourcePath = new URIPath(repositoryURI.relativize(parentResourceURI)); //get the path of the resource relative to the resource
		final Set<Repository> childSubrepositories = parentPathRepositoryMap.get(resourcePath); //see if there are any subrepositories mapped under the given parent resource URI
		return childSubrepositories != null ? unmodifiableSet(childSubrepositories) : Collections.<Repository>emptySet(); //return an unmodifiable set of the subrepositories, if there are any
	}

	/** A map of resource factories, keyed to namespace URIs. */
	private final Map<URI, URFResourceFactory> namespaceURIResourceFactoryMap = new HashMap<URI, URFResourceFactory>();

	@Override
	public void registerResourceFactory(final URI typeNamespaceURI, final URFResourceFactory factory) {
		namespaceURIResourceFactoryMap.put(typeNamespaceURI, factory);
	}

	@Override
	public void unregisterResourceFactory(final URI typeNamespaceURI) {
		namespaceURIResourceFactoryMap.remove(typeNamespaceURI);
	}

	/**
	 * Creates and initializes default I/O for URF resource descriptions.
	 * @return Default URF resource description I/O.
	 */
	protected static URFIO<URFResource> createDefaultURFResourceDescriptionIO() {
		final URFResourceTURFIO<URFResource> urfResourceDescriptionIO = new URFResourceTURFIO<URFResource>(URFResource.class, URI.create("")); //create a default resource description I/O using TURF
		urfResourceDescriptionIO.addNamespaceURI(MarmotSecurity.MARMOT_SECURITY_NAMESPACE_URI); //tell the I/O about the security namespace
		//TODO del		urfResourceDescriptionIO.setFormatted(false);	//turn off formatting
		return urfResourceDescriptionIO;
	}

	/**
	 * Default constructor with no root URI defined. The root URI must be defined before the repository is opened.
	 */
	public AbstractRepository() {
		this(null);
	}

	/**
	 * URI constructor with no separate private URI namespace. A {@link URFResourceTURFIO} description I/O is created and initialized.
	 * @param rootURI The URI identifying the location of this repository.
	 */
	public AbstractRepository(final URI rootURI) {
		this(rootURI, createDefaultURFResourceDescriptionIO()); //create a default resource description I/O using TURF
	}

	/**
	 * Public repository URI and private repository URI constructor.
	 * @param rootURI The URI identifying the location of this repository.
	 * @param descriptionIO The I/O implementation that writes and reads a resource with the same reference URI as its base URI.
	 * @throws NullPointerException if the description I/O is <code>null</code>.
	 */
	public AbstractRepository(final URI rootURI, final URFIO<URFResource> descriptionIO) {
		this.rootURI = rootURI != null ? normalize(rootURI) : null;
		this.descriptionIO = requireNonNull(descriptionIO, "Description I/O cannot be null."); //save the description I/O
		registerResourceFactory(Marmot.NAMESPACE_URI, MARMOT_RESOURCE_FACTORY); //register the Marmot factory
		registerResourceFactory(MARMOT_SECURITY_NAMESPACE_URI, MARMOT_SECURITY_RESOURCE_FACTORY); //register the Marmot resource factory
	}

	/**
	 * Creates a default empty URF data model. The correct resource factories will be installed to create appropriate classes in the Marmot namespace.
	 * @return A new default URF data model.
	 */
	protected URF createURF() {
		final URF urf = new URF(); //create a new URF data model
		for(final Map.Entry<URI, URFResourceFactory> namespaceURIResourceFactoryMapEntry : namespaceURIResourceFactoryMap.entrySet()) { //for each resource factory and corresponding URI
			urf.registerResourceFactory(namespaceURIResourceFactoryMapEntry.getKey(), namespaceURIResourceFactoryMapEntry.getValue()); //register the resource factories with the URF data model
		}
		return urf; //return the new data model
	}

	@Override
	public boolean isOpen() {
		return open.getObject().booleanValue();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Child classes should override {@link #openImpl()}.
	 * </p>
	 */
	@Override
	public final synchronized void open() throws ResourceIOException {
		if(!isOpen()) { //if the repository isn't yet open
			open.writeLock().lock(); //do actual opening under a write lock to prevent multiple attempts at opening at the same time
			try {
				if(!isOpen()) { //now that we're under a write lock, check again to see if the repository is open; if not
					openImpl(); //perform the actual opening
					open.setObject(Boolean.TRUE); //show that the repository is now open
				}
			} finally {
				open.writeLock().unlock();
			}
		}
	}

	/**
	 * Opens the repository for access.
	 * <p>
	 * This method is for subclasses to override to actually implement opening.
	 * </p>
	 * @throws IllegalStateException if the settings of this repository are inadequate to open the repository.
	 * @throws ResourceIOException if there is an error opening the repository.
	 * @see #getRootURI()
	 */
	protected void openImpl() throws ResourceIOException {
		checkState(getRootURI() != null, "Cannot open repository without root URI specified.");
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Child classes should override {@link #closeImpl()}.
	 * </p>
	 */
	@Override
	public final synchronized void close() throws ResourceIOException {
		if(!isOpen()) { //if the repository isn't yet open
			open.writeLock().lock(); //do actual opening under a write lock to prevent multiple attempts at opening at the same time
			try {
				if(!isOpen()) { //now that we're under a write lock, check again to see if the repository is open; if not
					closeImpl(); //perform the actual closing
					open.setObject(Boolean.FALSE); //show that the repository is now closed
				}
			} finally {
				open.writeLock().unlock();
			}
		}
	}

	/**
	 * Closes the repository.
	 * <p>
	 * This method should overridden by child classes to actually implement closing.
	 * </p>
	 * @throws ResourceIOException if there is an error closing the repository.
	 */
	protected void closeImpl() throws ResourceIOException {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version normalizes the URI, delegates to any subrepository if appropriate, and ensures the repository is open. Child classes should override
	 * {@link #resourceExistsImpl(URI)}.
	 * </p>
	 */
	@Override
	public final boolean resourceExists(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.resourceExists(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return resourceExistsImpl(resourceURI);
	}

	/**
	 * Implementation to determine if the resource at the given URI exists. The resource URI is guaranteed to be normalized and valid for the repository and the
	 * repository is guaranteed to be open.
	 * @param resourceURI The URI of the resource to check.
	 * @return <code>true</code> if the resource exists, else <code>false</code>.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	protected abstract boolean resourceExistsImpl(URI resourceURI) throws ResourceIOException;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version normalizes the URI, delegates to any subrepository if appropriate, and ensures the repository is open. Child classes should override
	 * {@link #getResourceDescriptionImpl(URI)}.
	 * </p>
	 */
	@Override
	public final URFResource getResourceDescription(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getResourceDescription(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getResourceDescriptionImpl(resourceURI);
	}

	/**
	 * Implementation to retrieve a description of the resource with the given URI. The resource URI is guaranteed to be normalized and valid for the repository
	 * and the repository is guaranteed to be open.
	 * @param resourceURI The URI of the resource the description of which should be retrieved.
	 * @return A description of the resource with the given URI.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	protected abstract URFResource getResourceDescriptionImpl(final URI resourceURI) throws ResourceIOException; //TODO add subclass functionality to ignore non-visible resources

	/**
	 * {@inheritDoc}
	 * <p>
	 * Child classes should override {@link #getResourceContentsImpl(URI)}.
	 * </p>
	 */
	@Override
	public final byte[] getResourceContents(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getResourceContents(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getResourceContentsImpl(resourceURI);
	}

	/**
	 * Retrieves the contents of the resource specified by the given URI. The resource URI is guaranteed to be normalized and valid for the repository and the
	 * repository is guaranteed to be open.
	 * <p>
	 * This implementation delegates to {@link #getResourceInputStream(URI)} and reads the entire stream. Many repositories will be able to provide a more
	 * efficient implementation.
	 * </p>
	 * @param resourceURI The URI of the resource to access.
	 * @return The bytes representing the contents of the resource represented by the given URI.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws IllegalArgumentException if the given resource is too large to be placed in a byte array.
	 * @throws IllegalStateException if the repository is not open for access and auto-open is not enabled.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the resource, such as a missing file or a resource that has no contents.
	 */
	protected byte[] getResourceContentsImpl(final URI resourceURI) throws ResourceIOException {
		final URFResource resourceDescription = getResourceDescription(resourceURI); //get a description of the resource
		final long contentLength = getContentLength(resourceDescription); //get the content length
		if(contentLength > Integer.MAX_VALUE) { //if the resource is too large to be placed in a byte array
			throw new IllegalArgumentException("Resource " + resourceURI + " is too large to return as a byte array: " + contentLength);
		}
		try {
			//use the content length in creating the byte array out put stream if we can; otherwise, use a default value to start with
			final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(contentLength >= 0 ? (int)contentLength : 1 << 10);
			final InputStream inputStream = getResourceInputStream(resourceURI); //get an input stream to the resource
			try {
				Streams.copy(inputStream, byteArrayOutputStream); //copy the stream to our output stream
			} finally {
				inputStream.close();
			}
			return byteArrayOutputStream.toByteArray(); //return the bytes we read
		} catch(final IOException ioException) { //if an I/O exception occurs
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version normalizes the URI, delegates to any subrepository if appropriate, and ensures the repository is open. Child classes should override
	 * {@link #getResourceInputStreamImpl(URI)}.
	 * </p>
	 */
	@Override
	public final InputStream getResourceInputStream(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getResourceInputStream(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getResourceInputStreamImpl(resourceURI);
	}

	/**
	 * Implementation to get an input stream to the contents of the resource specified by the given URI. The resource URI is guaranteed to be normalized and valid
	 * for the repository and the repository is guaranteed to be open.
	 * @param resourceURI The URI of the resource to access.
	 * @return An input stream to the resource represented by the given URI.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the resource.
	 */
	protected abstract InputStream getResourceInputStreamImpl(final URI resourceURI) throws ResourceIOException;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version normalizes the URI, delegates to any subrepository if appropriate, and ensures the repository is open. Child classes should override
	 * {@link #getResourceOutputStreamImpl(URI, ISODateTime)}.
	 * </p>
	 */
	@Override
	public final OutputStream getResourceOutputStream(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getResourceOutputStream(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getResourceOutputStreamImpl(resourceURI, new ISODateTime()); //get an output stream with a new modified datetime of now
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version normalizes the URI, delegates to any subrepository if appropriate, and ensures the repository is open. Child classes should override
	 * {@link #getResourceOutputStreamImpl(URI, ISODateTime)}.
	 * </p>
	 */
	@Override
	public OutputStream getResourceOutputStream(URI resourceURI, final ISODateTime newContentModified) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getResourceOutputStream(resourceURI, newContentModified); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getResourceOutputStreamImpl(resourceURI, newContentModified); //get an output stream with a new modified datetime of now
	}

	/**
	 * Gets an output stream to the contents of the resource specified by the given URI. The resource description will be updated with the specified content
	 * modified datetime if given. A {@link ResourceNotFoundException} should be generated if the resource does not exist, even for read-only repositories. If the
	 * repository is read-only, this method generates a {@link ResourceForbiddenException}.
	 * @param resourceURI The URI of the resource to access.
	 * @param newContentModified The new content modified datetime for the resource, or <code>null</code> if the content modified datetime should not be updated.
	 * @return An output stream to the resource represented by the given URI.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the resource.
	 * @see Content#MODIFIED_PROPERTY_URI
	 */
	protected abstract OutputStream getResourceOutputStreamImpl(final URI resourceURI, final ISODateTime newContentModified) throws ResourceIOException;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version normalizes the URI, delegates to any subrepository if appropriate, and ensures the repository is open. Child classes should override
	 * {@link #hasChildrenImpl(URI)}.
	 * </p>
	 */
	@Override
	public final boolean hasChildren(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.hasChildren(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return hasChildrenImpl(resourceURI);
	}

	/**
	 * Implementation to determine whether the resource represented by the given URI has children. The resource URI is guaranteed to be normalized and valid for
	 * the repository and the repository is guaranteed to be open.
	 * @param resourceURI The URI of the resource.
	 * @return <code>true</code> if the specified resource has child resources.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	protected abstract boolean hasChildrenImpl(final URI resourceURI) throws ResourceIOException;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation retrieves a single-level list of unfiltered child resources by calling
	 * {@link #getChildResourceDescriptionsImpl(URI, ResourceFilter, int)}.
	 * </p>
	 */
	@Override
	public final List<URFResource> getChildResourceDescriptions(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getChildResourceDescriptions(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getChildResourceDescriptionsImpl(resourceURI, null, 1); //get a list of child resource descriptions without going deeper than one level
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation retrieves a single-level list of descriptions by delegating to {@link #getChildResourceDescriptionsImpl(URI, ResourceFilter, int)}.
	 * </p>
	 */
	@Override
	public final List<URFResource> getChildResourceDescriptions(URI resourceURI, final ResourceFilter resourceFilter) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getChildResourceDescriptions(resourceURI, resourceFilter); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getChildResourceDescriptionsImpl(resourceURI, resourceFilter, 1); //get a list of child resource descriptions without going deeper than one level
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation retrieves an unfiltered list of child resources by delegating to {@link #getChildResourceDescriptionsImpl(URI, ResourceFilter, int)}.
	 * </p>
	 */
	@Override
	public final List<URFResource> getChildResourceDescriptions(URI resourceURI, final int depth) throws ResourceIOException {
		checkArgumentNotNegative(depth);
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getChildResourceDescriptions(resourceURI, depth); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getChildResourceDescriptionsImpl(resourceURI, null, depth); //get a list of child resource descriptions without filtering
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #getChildResourceDescriptionsImpl(URI, ResourceFilter, int)}.
	 * </p>
	 */
	@Override
	public final List<URFResource> getChildResourceDescriptions(URI resourceURI, final ResourceFilter resourceFilter, final int depth)
			throws ResourceIOException {
		checkArgumentNotNegative(depth);
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getChildResourceDescriptions(resourceURI, resourceFilter, depth); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return getChildResourceDescriptionsImpl(resourceURI, resourceFilter, depth);
	}

	/**
	 * Implementation to retrieve child resources of the resource at the given URI. The resource URI is guaranteed to be normalized and valid for the repository,
	 * the depth is guaranteed not to be negative, and the repository is guaranteed to be open.
	 * <p>
	 * If any repositories are mapped as children of the identified resource, they will be returned as well, with their descriptions retrieved from the respective
	 * subrepository.
	 * </p>
	 * @param resourceURI The URI of the resource for which sub-resources should be returned.
	 * @param resourceFilter The filter that determines whether child resources should be included, or <code>null</code> if the child resources should not be
	 *          filtered.
	 * @param depth The zero-based depth of child resources which should recursively be retrieved, or {@link #INFINITE_DEPTH} for an infinite depth.
	 * @return A list of sub-resource descriptions under the given resource.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 * @see #getPathRepositories()
	 */
	protected abstract List<URFResource> getChildResourceDescriptionsImpl(final URI resourceURI, final ResourceFilter resourceFilter, final int depth)
			throws ResourceIOException;

	@Override
	public URFResource createParentResources(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.createParentResources(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		if(resourceURI.equals(getRootURI())) { //if this is the resource URI
			return null; //we didn't have to create anything
		}
		final URI parentResourceURI = getParentResourceURI(resourceURI); //get the parent resource URI
		URFResource lastCreatedParentResource = createParentResources(parentResourceURI); //create any necessary parents of the parent
		if(!resourceExists(parentResourceURI)) { //if the parent does not exist
			lastCreatedParentResource = createCollectionResource(parentResourceURI); //create the parent collection
		}
		return lastCreatedParentResource; //return the last parent created, if any
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #createResourceImpl(URI, URFResource)} with a default description. Child classes should override
	 * {@link #createResourceImpl(URI, URFResource)}.
	 * </p>
	 */
	@Override
	public final OutputStream createResource(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.createResource(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		final URFResource resourceDescription = new DefaultURFResource(resourceURI);
		final ISODateTime dateTime = new ISODateTime(); //create a new timestamp representing this instant
		setCreated(resourceDescription, dateTime); //set the created and modified times to the current time 
		setModified(resourceDescription, dateTime);
		return createResourceImpl(resourceURI, resourceDescription); //create the resource with the default description
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Child classes should override {@link #createResourceImpl(URI, URFResource)}.
	 * </p>
	 */
	@Override
	public final OutputStream createResource(URI resourceURI, final URFResource resourceDescription) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.createResource(resourceURI, resourceDescription); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return createResourceImpl(resourceURI, resourceDescription);
	}

	@Override
	public URFResource createCollectionResource(final URI resourceURI) throws ResourceIOException {
		return createResource(checkCollectionURI(resourceURI), NO_BYTES);
	}

	/**
	 * Implementation to create a new resource with the given description and returns an output stream for writing the contents of the resource. If a resource
	 * already exists at the given URI it will be replaced. The returned output stream should always be closed. It is unspecified whether the resource description
	 * will be updated before or after the resource contents are stored. The resource URI is guaranteed to be normalized and valid for the repository and the
	 * repository is guaranteed to be open.
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @return An output stream for storing the contents of the resource.
	 * @throws NullPointerException if the given resource URI and/or resource description is <code>null</code>.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	protected abstract OutputStream createResourceImpl(final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #createResourceImpl(URI, URFResource, byte[])} with a default description.
	 * </p>
	 */
	@Override
	public final URFResource createResource(URI resourceURI, final byte[] resourceContents) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.createResource(resourceURI, resourceContents); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		final URFResource resourceDescription = new DefaultURFResource(resourceURI);
		final ISODateTime dateTime = new ISODateTime(); //create a new timestamp representing this instant
		setCreated(resourceDescription, dateTime); //set the created and modified times to the current time 
		setModified(resourceDescription, dateTime);
		return createResourceImpl(resourceURI, resourceDescription, resourceContents); //create the resource with the default description
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Child classes should override {@link #createResourceImpl(URI, URFResource, byte[])}.
	 * </p>
	 */
	@Override
	public final URFResource createResource(URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.createResource(resourceURI, resourceDescription, resourceContents); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return createResourceImpl(resourceURI, resourceDescription, resourceContents);
	}

	/**
	 * Implementation to create a new resource with the given description and contents. If a resource already exists at the given URI it will be replaced. The
	 * resource URI is guaranteed to be normalized and valid for the repository and the repository is guaranteed to be open.
	 * <p>
	 * This implementation delegates to {@link #createResourceImpl(URI, URFResource)} and writes the given resources, but it is expected that many repositories
	 * can provide a more efficient implementation.
	 * </p>
	 * @param resourceURI The reference URI to use to identify the resource.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @param resourceContents The contents to store in the resource.
	 * @return A description of the resource that was created.
	 * @throws NullPointerException if the given resource URI, resource description, and/or resource contents is <code>null</code>.
	 * @throws ResourceIOException if the resource could not be created.
	 */
	protected URFResource createResourceImpl(final URI resourceURI, final URFResource resourceDescription, final byte[] resourceContents)
			throws ResourceIOException {
		try {
			final OutputStream outputStream = createResourceImpl(resourceURI, resourceDescription); //open an output stream to the resource
			outputStream.write(resourceContents); //write the contents
			outputStream.close(); //close the output stream
			return getResourceDescription(resourceURI); //return an updated description of the resource
		} catch(final IOException ioException) { //if an I/O exception occurs
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Child classes should override {@link #deleteResourceImpl(URI)}.
	 * </p>
	 */
	@Override
	public final void deleteResource(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.deleteResource(resourceURI); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(resourceURI.equals(getRootURI())) { //if they try to delete the root URI
			throw new IllegalArgumentException("Cannot delete repository root URI " + resourceURI);
		}
		deleteResourceImpl(resourceURI);
	}

	/**
	 * Implementation to delete a resource. If no resource exists at the given URI, no action occurs and no error is generated. The resource URI is guaranteed to
	 * be normalized and valid for the repository (as well as not equaling the repository root) and the repository is guaranteed to be open.
	 * @param resourceURI The reference URI of the resource to delete.
	 * @throws ResourceIOException if the resource could not be deleted.
	 */
	protected abstract void deleteResourceImpl(final URI resourceURI) throws ResourceIOException;

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version delegates to {@link #addResourceProperties(URI, Iterable)}. Child classes normally should override
	 * {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * </p>
	 */
	@Override
	public final URFResource addResourceProperties(URI resourceURI, final URFProperty... properties) throws ResourceIOException {
		return addResourceProperties(resourceURI, asList(properties));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version delegates to {@link #addResourcePropertiesImpl(URI, Iterable)}. Child classes normally should override
	 * {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * </p>
	 */
	@Override
	public final URFResource addResourceProperties(URI resourceURI, final Iterable<URFProperty> properties) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.addResourceProperties(resourceURI, properties); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return addResourcePropertiesImpl(resourceURI, properties);
	}

	/**
	 * Adds properties to a given resource. All existing properties will be left unmodified. The resource URI is guaranteed to be normalized and valid for the
	 * repository and the repository is guaranteed to be open. This implementation creates an {@link URFResourceAlteration} and delegates to
	 * {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * @param resourceURI The reference URI of the resource.
	 * @param properties The properties to set.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or properties is <code>null</code>.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	protected URFResource addResourcePropertiesImpl(final URI resourceURI, final Iterable<URFProperty> properties) throws ResourceIOException {
		return alterResourcePropertiesImpl(resourceURI, DefaultURFResourceAlteration.createAddPropertiesAlteration(properties)); //create an alteration for adding properties and alter the resource
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version delegates to {@link #setResourceProperties(URI, Iterable)}. Child classes normally should override
	 * {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * </p>
	 */
	@Override
	public final URFResource setResourceProperties(URI resourceURI, final URFProperty... properties) throws ResourceIOException {
		return setResourceProperties(resourceURI, asList(properties));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version delegates to {@link #setResourcePropertiesImpl(URI, Iterable)}. Child classes normally should override
	 * {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * </p>
	 */
	@Override
	public final URFResource setResourceProperties(URI resourceURI, final Iterable<URFProperty> properties) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.setResourceProperties(resourceURI, properties); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return setResourcePropertiesImpl(resourceURI, properties);
	}

	/**
	 * Sets the properties of a given resource. Any existing properties with the same URIs as the given given property/value pairs will be removed. All other
	 * existing properties will be left unmodified. The resource URI is guaranteed to be normalized and valid for the repository and the repository is guaranteed
	 * to be open. This implementation creates an {@link URFResourceAlteration} and delegates to {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * @param resourceURI The reference URI of the resource.
	 * @param properties The properties to set.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or properties is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource setResourcePropertiesImpl(URI resourceURI, final Iterable<URFProperty> properties) throws ResourceIOException {
		return alterResourcePropertiesImpl(resourceURI, DefaultURFResourceAlteration.createSetPropertiesAlteration(properties)); //create an alteration for setting properties and alter the resource
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #removeResourceProperties(URI, Iterable)}. Child classes normally should override
	 * {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * </p>
	 */
	@Override
	public final URFResource removeResourceProperties(URI resourceURI, final URI... propertyURIs) throws ResourceIOException {
		return removeResourceProperties(resourceURI, asList(propertyURIs));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #removeResourcePropertiesImpl(URI, Iterable)}. Child classes normally should override
	 * {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * </p>
	 */
	@Override
	public final URFResource removeResourceProperties(URI resourceURI, final Iterable<URI> propertyURIs) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.removeResourceProperties(resourceURI, propertyURIs); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return removeResourcePropertiesImpl(resourceURI, propertyURIs);
	}

	/**
	 * Removes properties from a given resource. Any existing properties with the same URIs as the given given property/value pairs will be removed. All other
	 * existing properties will be left unmodified. The resource URI is guaranteed to be normalized and valid for the repository and the repository is guaranteed
	 * to be open. This implementation creates an {@link URFResourceAlteration} and delegates to {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * @param resourceURI The reference URI of the resource.
	 * @param propertyURIs The properties to remove.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or property URIs is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be updated.
	 */
	public URFResource removeResourcePropertiesImpl(URI resourceURI, final Iterable<URI> propertyURIs) throws ResourceIOException {
		return alterResourcePropertiesImpl(resourceURI, DefaultURFResourceAlteration.createRemovePropertiesAlteration(propertyURIs)); //create an alteration for removing properties and alter the resource
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Child classes should override {@link #alterResourcePropertiesImpl(URI, URFResourceAlteration)}.
	 * </p>
	 */
	@Override
	public final URFResource alterResourceProperties(URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.alterResourceProperties(resourceURI, resourceAlteration); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return alterResourcePropertiesImpl(resourceURI, resourceAlteration);
	}

	/**
	 * Implementation to alter properties of a given resource. The resource URI is guaranteed to be normalized and valid for the repository and the repository is
	 * guaranteed to be open.
	 * @param resourceURI The reference URI of the resource.
	 * @param resourceAlteration The specification of the alterations to be performed on the resource.
	 * @return The updated description of the resource.
	 * @throws NullPointerException if the given resource URI and/or resource alteration is <code>null</code>.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if the resource properties could not be altered.
	 */
	protected abstract URFResource alterResourcePropertiesImpl(final URI resourceURI, final URFResourceAlteration resourceAlteration) throws ResourceIOException;

	@Override
	public URI getCollectionURI(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			return subrepository.getCollectionURI(resourceURI); //delegate to the subrepository
		}
		checkOpen(); //make sure the repository is open
		return isCollectionURI(resourceURI) ? resourceURI : getCurrentLevel(resourceURI); //if URI is a collection URI, return the URI; otherwise, get the current level
	}

	@Override
	public URI getParentResourceURI(URI resourceURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			if(!subrepository.getRootURI().equals(resourceURI)) { //don't ask the subrepository's root URI for a parent resource URI, as the repository has no parent URI in terms of that repository
				return subrepository.getParentResourceURI(resourceURI); //delegate to the subrepository
			}
		}
		checkOpen(); //make sure the repository is open
		if(resourceURI.equals(getRootURI())) { //if the resource is the repository URI
			return null; //the repository level has no parent
		}
		final URI parentResourceURI = isCollectionURI(resourceURI) ? getParentLevel(resourceURI) : getCurrentLevel(resourceURI); //if resource is a collection URI, get the parent level; otherwise, get the current level
		assert !resourceURI.equals(parentResourceURI) : "Parent URI should be different from the resource URI."; //the resource URI will never be null, but the parent resource URI may be---but they should never be the same (default URI resolution won't work for opaque URIs)
		return parentResourceURI; //return the URI of the parent resource
	}

	//intra-repository copy

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, URI destinationURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationURI); //delegate to the subrepository
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			copyResourceImpl(resourceURI, destinationSubrepository, destinationURI, true, null); //copy to the subrepository
			return;
		}
		copyResourceImpl(resourceURI, destinationURI, true, null); //copy the resource, overwriting any resource at the destination
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, URI destinationURI, final ProgressListener progressListener) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationURI, progressListener); //delegate to the subrepository
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			copyResourceImpl(resourceURI, destinationSubrepository, destinationURI, true, progressListener); //copy to the subrepository
			return;
		}
		copyResourceImpl(resourceURI, destinationURI, true, progressListener); //copy the resource, overwriting any resource at the destination
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, URI destinationURI, final boolean overwrite) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationURI, overwrite); //delegate to the subrepository
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			copyResourceImpl(resourceURI, destinationSubrepository, destinationURI, overwrite, null); //copy to the subrepository
			return;
		}
		copyResourceImpl(resourceURI, destinationURI, overwrite, null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationURI, overwrite, progressListener); //delegate to the subrepository
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			copyResourceImpl(resourceURI, destinationSubrepository, destinationURI, overwrite, progressListener); //copy to the subrepository
			return;
		}
		copyResourceImpl(resourceURI, destinationURI, overwrite, progressListener);
	}

	/**
	 * Creates an infinitely deep copy of a resource to another URI in this repository, overwriting any resource at the destination only if requested. The
	 * resource URI is guaranteed to be normalized and valid for the repository and the repository is guaranteed to be open. The destination resource URI is
	 * guaranteed not to be a child of the source resource URI.
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	protected abstract void copyResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException; //TODO here and in all the copy methods, make sure we're not copying from collection to non-collection and vice-versa

	//inter-repository copy

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, final Repository destinationRepository, URI destinationURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationRepository, destinationURI); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			copyResourceImpl(resourceURI, destinationURI, true, null); //delegate to the internal copy method
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		copyResourceImpl(resourceURI, destinationRepository, destinationURI, true, null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, final Repository destinationRepository, URI destinationURI, final ProgressListener progressListener)
			throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationRepository, destinationURI, progressListener); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			copyResourceImpl(resourceURI, destinationURI, true, progressListener); //delegate to the internal copy method
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		copyResourceImpl(resourceURI, destinationRepository, destinationURI, true, progressListener);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, final Repository destinationRepository, URI destinationURI, final boolean overwrite)
			throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationRepository, destinationURI, overwrite); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			copyResourceImpl(resourceURI, destinationURI, overwrite, null); //delegate to the internal copy method
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		copyResourceImpl(resourceURI, destinationRepository, destinationURI, overwrite, null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #copyResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void copyResource(URI resourceURI, final Repository destinationRepository, URI destinationURI, final boolean overwrite,
			final ProgressListener progressListener) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.copyResource(resourceURI, destinationRepository, destinationURI, overwrite, progressListener); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			copyResourceImpl(resourceURI, destinationURI, overwrite, progressListener); //delegate to the internal copy method
			return;
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular copy from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		copyResourceImpl(resourceURI, destinationRepository, destinationURI, overwrite, progressListener);
	}

	/**
	 * Creates an infinitely deep copy of a resource to the specified URI in the specified repository, overwriting any resource at the destination only if
	 * requested. The resource URI is guaranteed to be normalized and valid for the repository and the repository is guaranteed to be open. The destination
	 * resource URI is guaranteed not to be a child of the source resource URI. The destination repository is guaranteed to be a different repository than this
	 * repository.
	 * <p>
	 * This version performs a default copy operation. Normally child classes do not need to override this version.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error copying the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	protected void copyResourceImpl(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite,
			final ProgressListener progressListener) throws ResourceIOException {
		try {
			//TODO del Log.trace("ready to create resource", destinationURI, "in destination repository", destinationRepository.getReferenceURI());
			final boolean isCollection = isCollectionURI(resourceURI); //see if the resource is a collection
			final URFResource resourceDescription = getResourceDescription(resourceURI); //get a description of the resource; this will throw an exception if the source resource doesn't exist
			if(!overwrite) { //if we're not allowed to overwrite files
				if(destinationRepository.resourceExists(destinationURI)) { //if the destination resource exists TODO create an overwrite-aware createResource() method to make this more efficient
					throw new ResourceStateException(destinationURI, "Destination resource already exists.");
				}
			}
			final long contentLength = getContentLength(resourceDescription); //get the size of the resource content
			if(contentLength == 0) { //if this is a resource with no content, don't needlessly create content (especially important for collections)
				destinationRepository.createResource(destinationURI, resourceDescription, NO_BYTES); //create a zero-byte resource with the given description
			} else { //if there is content
				//TODO check for non-existent source resource
				final InputStream inputStream = getResourceInputStream(resourceURI); //get an input stream to the source resource
				try {
					//TODO create an overwrite-aware createResource() method
					final OutputStream outputStream = destinationRepository.createResource(destinationURI, resourceDescription); //create the destination resource with the same description as the source resource, getting an output stream for storing the contents
					try {
						Streams.copy(inputStream, outputStream, contentLength, progressListener); //copy the resource
					} finally {
						outputStream.close(); //always close the output stream
					}
				} finally {
					inputStream.close(); //always close the input stream
				}
			}
			//TODO copy the child resources
		} catch(final IOException ioException) { //if an I/O exception occurs
			throw toResourceIOException(resourceURI, ioException); //translate the exception to a resource I/O exception and throw that
		}
	}

	//intra-repository move

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, URI destinationURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationURI); //delegate to the subrepository
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			moveResourceImpl(resourceURI, destinationSubrepository, destinationURI, true, null); //move to the subrepository
			return;
		}
		moveResourceImpl(resourceURI, destinationURI, true, null); //move the resource, overwriting any resource at the destination
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, URI destinationURI, final ProgressListener progressListener) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationURI, progressListener); //delegate to the subrepository
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			moveResourceImpl(resourceURI, destinationSubrepository, destinationURI, true, progressListener); //move to the subrepository
			return;
		}
		moveResourceImpl(resourceURI, destinationURI, true, progressListener); //move the resource, overwriting any resource at the destination
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, URI destinationURI, final boolean overwrite) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationURI, overwrite); //delegate to the subrepository
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			moveResourceImpl(resourceURI, destinationSubrepository, destinationURI, overwrite, null); //move to the subrepository
			return;
		}
		moveResourceImpl(resourceURI, destinationURI, overwrite, null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the destination URI lies in a subrepository, this version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * Otherwise, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationURI, overwrite, progressListener); //delegate to the subrepository
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI);
		}
		checkOpen(); //make sure the repository is open
		final Repository destinationSubrepository = getSubrepository(destinationURI); //see if the destination URI lies within a subrepository
		if(destinationSubrepository != this) { //if the destination URI lies within a subrepository
			moveResourceImpl(resourceURI, destinationSubrepository, destinationURI, overwrite, progressListener); //move to the subrepository
			return;
		}
		moveResourceImpl(resourceURI, destinationURI, overwrite, progressListener);
	}

	/**
	 * Moves a resource to another URI in this repository, overwriting any resource at the destination only if requested. The resource URI is guaranteed to be
	 * normalized and valid for the repository (not the root), and the repository is guaranteed to be open. The destination resource URI is guaranteed not to be a
	 * child of the source resource URI.
	 * <p>
	 * A default implementation of this method is provided that delegates to {@link #copyResourceImpl(URI, URI, boolean, ProgressListener)} followed by
	 * {@link #deleteResourceImpl(URI)}, but in most cases subclasses should override this method to provide a more efficient implementation based upon the
	 * underlying store.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	protected void moveResourceImpl(final URI resourceURI, final URI destinationURI, final boolean overwrite, final ProgressListener progressListener)
			throws ResourceIOException { //TODO here and in all the move methods, make sure we're not moving from collection to non-collection and vice-versa
		copyResourceImpl(resourceURI, destinationURI, overwrite, progressListener); //copy the resource
		deleteResourceImpl(resourceURI); //delete the resource
	}

	//inter-repository move

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, final Repository destinationRepository, URI destinationURI) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationRepository, destinationURI); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			moveResourceImpl(resourceURI, destinationURI, true, null); //delegate to the internal move method
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		moveResourceImpl(resourceURI, destinationRepository, destinationURI, true, null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, final Repository destinationRepository, URI destinationURI, final ProgressListener progressListener)
			throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationRepository, destinationURI, progressListener); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			moveResourceImpl(resourceURI, destinationURI, true, progressListener); //delegate to the internal move method
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		moveResourceImpl(resourceURI, destinationRepository, destinationURI, true, progressListener);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, final Repository destinationRepository, URI destinationURI, final boolean overwrite)
			throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationRepository, destinationURI, overwrite); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			moveResourceImpl(resourceURI, destinationURI, overwrite, null); //delegate to the internal move method
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		moveResourceImpl(resourceURI, destinationRepository, destinationURI, overwrite, null);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the given resource is in this repository, this version delegates to {@link #moveResourceImpl(URI, URI, boolean, ProgressListener)}. Otherwise, this
	 * version delegates to {@link #moveResourceImpl(URI, Repository, URI, boolean, ProgressListener)}.
	 * </p>
	 */
	@Override
	public final void moveResource(URI resourceURI, final Repository destinationRepository, URI destinationURI, final boolean overwrite,
			final ProgressListener progressListener) throws ResourceIOException {
		resourceURI = checkResourceURI(resourceURI); //makes sure the resource URI is valid and normalize the URI
		destinationURI = destinationRepository.checkResourceURI(destinationURI); //makes sure the resource URI is valid and normalize the URI
		final Repository subrepository = getSubrepository(resourceURI); //see if the resource URI lies within a subrepository
		if(subrepository != this) { //if the resource URI lies within a subrepository
			subrepository.moveResource(resourceURI, destinationRepository, destinationURI, overwrite, progressListener); //delegate to the subrepository
			return;
		}
		checkOpen(); //make sure the repository is open
		if(destinationRepository == this) { //if the resource is being copied to this repository
			moveResourceImpl(resourceURI, destinationURI, overwrite, progressListener); //delegate to the internal move method
			return;
		}
		if(resourceURI.equals(getRootURI())) { //if they try to move the root URI
			throw new IllegalArgumentException("Cannot move repository base URI " + resourceURI);
		}
		if(isChild(resourceURI, destinationURI)) {
			throw new IllegalArgumentException("Cannot perform circular move from " + resourceURI + " to " + destinationURI + " even between repositories.");
		}
		moveResourceImpl(resourceURI, destinationRepository, destinationURI, overwrite, progressListener);
	}

	/**
	 * Moves a resource to the specified URI in the specified repository, overwriting any resource at the destination only if requested. The resource URI is
	 * guaranteed to be normalized and valid for the repository (not the root), and the repository is guaranteed to be open. The destination resource URI is
	 * guaranteed not to be a child of the source resource URI. The destination repository is guaranteed to be a different repository than this repository.
	 * <p>
	 * This version performs a default move operation. Normally child classes do not need to override this version.
	 * </p>
	 * @param resourceURI The URI of the resource to be copied.
	 * @param destinationRepository The repository to which the resource should be copied, which may be this repository.
	 * @param destinationURI The URI to which the resource should be copied.
	 * @param overwrite <code>true</code> if any existing resource at the destination should be overwritten, or <code>false</code> if an existing resource at the
	 *          destination should cause an exception to be thrown.
	 * @param progressListener A listener to be notified of progress, or <code>null</code> if no progress notifications is requested.
	 * @throws ResourceNotFoundException if the identified resource does not exist.
	 * @throws ResourceIOException if there is an error moving the resource.
	 * @throws ResourceStateException if overwrite is specified not to occur and a resource exists at the given destination.
	 */
	protected void moveResourceImpl(final URI resourceURI, final Repository destinationRepository, final URI destinationURI, final boolean overwrite,
			final ProgressListener progressListener) throws ResourceIOException {
		copyResource(resourceURI, destinationRepository, destinationURI, overwrite, progressListener); //copy the resource to the other repository
		deleteResource(resourceURI); //delete the moved resource in this repository
	}

	/**
	 * Translates the given error specific to the this repository type into a resource I/O exception.
	 * <p>
	 * This version returns the given throwable if it is already a {@link ResourceIOException}. It also makes the following translations:
	 * </p>
	 * <dl>
	 * <dt>{@link IllegalStateException}</dt>
	 * <dd>{@link ResourceNotFoundException}</dd>
	 * </dl>
	 * <p>
	 * Otherwise, it simply wraps the given throwable in a {@link ResourceIOException}.
	 * </p>
	 * @param resourceURI The URI of the resource to which the exception is related.
	 * @param throwable The error which should be translated to a resource I/O exception.
	 * @return A resource I/O exception based upon the given throwable.
	 */
	protected ResourceIOException toResourceIOException(final URI resourceURI, final Throwable throwable) {
		if(throwable instanceof ResourceIOException) {
			return (ResourceIOException)throwable;
		} else if(throwable instanceof IllegalStateException) {
			return new ResourceStateException(resourceURI, throwable);
		} else {
			return new ResourceIOException(resourceURI, throwable); //default to simple exception chaining with a new resource I/O exception
		}
	}

	/**
	 * Determines whether the given resource has properties that are not live.
	 * @param resourceDescription The description of the resource.
	 */
	/*TODO del
		protected boolean hasDeadProperties(final URFResource resourceDescription)
		{
			
		}
	*/

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version calls {@link #close()}.
	 * </p>
	 */
	@Override
	public synchronized void dispose() {
		try {
			close();
		} catch(final ResourceIOException resourceIOException) {
			Log.error(resourceIOException);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version closes the repository.
	 * </p>
	 */
	@Override
	protected void finalize() throws Throwable {
		try {
			close(); //close the repository if it isn't already
		} finally {
			super.finalize(); //always call the parent version
		}
	}

	/**
	 * Updates a map of some sort of property representations by converting legacy namespaced properties to the canonical namespaced properties.
	 * <p>
	 * Each legacy namespaced property is converted to canonical form. If the canonical form already exists, the legacy namespaced property is removed. Otherwise,
	 * it is placed back in the map using the canonical property URI. Essentially, legacy namespace properties will be converted to canonical form, but later
	 * explicit canonical form properties will always override the legacy forms.
	 * </p>
	 * <p>
	 * This method is useful for preprocessing all gathered properties from some underlying store before they are actually updated in the resource description
	 * being retrieved.
	 * </p>
	 * @param <V> Some type of property representation in the map, keyed to property URIs.
	 * @param <M> The type of map holding the property representations.
	 * @param propertyURIValueMap The map of some sort of property representations, keyed to the property URI (some of which may be in legacy form).
	 * @throws NullPointerException if the given map is <code>null</code>.
	 * @see URF#convertLegacyNamespacedURI(URI)
	 */
	protected static <V, M extends Map<URI, V>> void updateLegacyNamespacedProperties(final M propertyURIValueMap) {
		//start by iterating through the keys of the map; most of the time, we probably won't have to change anything
		Iterator<URI> propertyURIIterator = propertyURIValueMap.keySet().iterator();
		Set<URI> keySet = null; //if something needs to be changed, we'll have to make a copy of the keys so we can modify the map
		while(propertyURIIterator.hasNext()) {
			final URI legacyPropertyURI = propertyURIIterator.next(); //get the next property URI---which *may* or may not be a legacy URI
			final URI canonicalPropertyURI = convertLegacyNamespacedURI(legacyPropertyURI); //convert it from a legacy form
			if(!canonicalPropertyURI.equals(legacyPropertyURI)) { //if the property URI changed (signaling that this was legacy property URI form)
				if(keySet == null) { //if we don't have a set of keys yet, we were in read-only mode; now make a copy of the keys and start over
					keySet = new HashSet<URI>(propertyURIValueMap.keySet()); //make a copy of the keys
					propertyURIIterator = keySet.iterator(); //we'll switch to a new iterator that essentially starts over iterating through the propertyURIs---but in a separate copy this time
					continue; //bail from this iteration
				}
				//now we know we're in write mode---we can change the map
				if(!propertyURIValueMap.containsKey(canonicalPropertyURI)) { //if the map doesn't already have a canonical form of this legacy URI (the canonical form always wins)
					final V value = propertyURIValueMap.get(legacyPropertyURI); //get the value for the legacy form
					propertyURIValueMap.put(canonicalPropertyURI, value); //put the value in the map, this time keyed to the canonical form
				}
				propertyURIValueMap.remove(legacyPropertyURI); //either way, we remove the legacy form---it was either updated or obsoleted
			}
		}
	}

	/**
	 * Creates a single text value to represent the given URF property and value(s). At least one property must be given. If a single text property is provided
	 * that does not begin with {@link TURF#SIGNATURE}, the value is stored unmodified. Otherwise, a TURF description is created and stored as the text value.
	 * <p>
	 * This method is part of a pair of methods to allow multiple typed values encoded in a single string for repositories that don't natively allow multiple or
	 * typed properties.
	 * </p>
	 * @param resourceURI The URI of the resource.
	 * @param properties The URF properties to represent as values encoded in a single string
	 * @return A property URI and a text value representing the given URF properties.
	 * @throws IOException if there is an I/O error while performing the action.
	 * @throws NullPointerException if the given properties and/or description I/O is <code>null</code>.
	 * @throws IllegalArgumentException if no properties are given.
	 * @throws IllegalArgumentException if all of the properties do not have the same property URI.
	 * @throws IllegalArgumentException if there is an error creating the text value.
	 * @see #decodePropertiesTextValue(URFResource, URI, String)
	 * @see #getDescriptionIO()
	 */
	protected NameValuePair<URI, String> encodePropertiesTextValue(final URI resourceURI, final Iterable<URFProperty> properties) throws IOException {
		final Iterator<URFProperty> propertyIterator = properties.iterator();
		if(!propertyIterator.hasNext()) { //if no properties are given
			throw new IllegalArgumentException("At least one URF property must be provided to create an encoded property.");
		}
		URFProperty property = propertyIterator.next(); //get the first property
		final URI propertyURI = property.getPropertyURI(); //get the URI of the URF property
		URFResource propertyValue; //keep track of the last property value
		final URFResource propertyDescription = new DefaultURFResource(resourceURI); //create a new resource description just for this property
		do { //for each URF property
			if(!propertyURI.equals(property.getPropertyURI())) { //if this URF property has a different URI
				throw new IllegalArgumentException("All URF properties expected to have property URI " + propertyURI + "; found " + property.getPropertyURI() + ".");
			}
			propertyValue = property.getValue(); //note the property value
			propertyDescription.addProperty(property); //add this property to the resource
			if(propertyIterator.hasNext()) { //get the next property
				property = propertyIterator.next();
			}
		} while(propertyIterator.hasNext());
		if(propertyDescription.getPropertyCount() == 1) { //if we only wound up with a single property
			final String textValue = asString(propertyValue); //see if we value is a simple string
			if(textValue != null && !textValue.startsWith(TURF.SIGNATURE)) { //if we have a single string variable that doesn't start with the TURF signature
				return new NameValuePair<URI, String>(propertyURI, textValue); //abandon use of the description---just return the string value itself 
			}
		}
		return new NameValuePair<URI, String>(propertyURI, Strings.write(resourceURI, propertyDescription, getDescriptionIO(), UTF_8)); //write the description to a string, using the resource URI as the base URI
	}

	/**
	 * Adds the identified property of the given resource from the given text, which may indicate a set of properties encoded in URF.
	 * <p>
	 * If the property text value begins with {@link TURF#SIGNATURE}, it is assumed to be stored in TURF; the identified property is removed and replaced with all
	 * given properties stored in the parsed TURF description. Otherwise, the text value is assumed to be just another text value and is added to the resource
	 * using the identified property.
	 * </p>
	 * <p>
	 * This method is part of a pair of methods to allow multiple typed values encoded in a single string for repositories that don't natively allow multiple or
	 * typed properties.
	 * </p>
	 * @param resource The resource the properties of which to add.
	 * @param propertyURI The URI of the property to add.
	 * @param propertyTextValue The text value to add; this value may represent several properties encoded as TURF.
	 * @throws NullPointerException if the given resource, property URI, and/or property text value is <code>null</code>.
	 * @throws IllegalArgumentException if the given property text value purports to be TURF but has serialization errors.
	 * @see #encodePropertiesTextValue(URI, Iterable)
	 * @see #createURF()
	 * @see #getDescriptionIO()
	 */
	protected void decodePropertiesTextValue(final URFResource resource, final URI propertyURI, final String propertyTextValue) {
		if(propertyTextValue.startsWith(TURF.SIGNATURE)) { //if this property value is stored in TURF
			try {
				//read a description of the resource from the property, recognizing the resource serialized with URI "" as indicating the given resource
				final URFResource propertyDescription = getDescriptionIO().read(createURF(), new ByteArrayInputStream(propertyTextValue.getBytes(UTF_8)),
						resource.getURI());
				resource.removePropertyValues(propertyURI); //if we were successful (that is, the property text value had no errors), remove any values already present for this value
				long addedPropertyCount = 0;
				for(final URFProperty property : propertyDescription.getProperties(propertyURI)) { //for each read property that we expect in the description
					++addedPropertyCount;
					resource.addProperty(property); //add this property to the given description
				}
				if(addedPropertyCount == 0) { //if no properties were added, something wasn't quite right---we should have always some properties; maybe they weren't stored with the correct URI 
					Log.warn("No properties found for URI " + propertyURI + " in description: " + propertyTextValue);
				}
			} catch(final IOException ioException) { //if we had any problem interpreting the text value as TURF
				throw new IllegalArgumentException("Invalid URF property value.", ioException);
			}
		} else { //if this is a normal string property
			resource.addPropertyValue(propertyURI, propertyTextValue); //add the string value to the resource
		}
	}
}

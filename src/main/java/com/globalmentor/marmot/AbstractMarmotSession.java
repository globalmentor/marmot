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

package com.globalmentor.marmot;

import com.globalmentor.collections.CollectionMap;
import com.globalmentor.collections.CopyOnWriteArrayListConcurrentHashMap;
import com.globalmentor.io.Files;
import java.net.*;
import java.nio.charset.Charset;
import java.util.*;

import static java.util.Collections.*;
import static java.util.Objects.*;

import java.util.concurrent.*;

import org.urframework.*;
import org.urframework.content.*;

import static com.globalmentor.java.Objects.*;
import static com.globalmentor.net.URIs.*;
import com.globalmentor.marmot.repository.*;
import com.globalmentor.marmot.resource.*;
import com.globalmentor.marmot.resource.ResourceKit.Capability;
import com.globalmentor.marmot.security.*;
import com.globalmentor.net.ContentType;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.net.URIs;

/**
 * A Marmot session with installed resource kits.
 * @param <RK> The type of resource kits supported by this session.
 * @author Garret Wilson
 */
public abstract class AbstractMarmotSession<RK extends ResourceKit> implements MarmotSession<RK> {

	/** The installed Marmot security manager. */
	private MarmotSecurityManager securityManager = new DefaultMarmotSecurityManager();

	@Override
	public MarmotSecurityManager getSecurityManager() {
		return securityManager;
	}

	/** The set of resource kits. */
	private Set<RK> resourceKits = new CopyOnWriteArraySet<RK>();

	@Override
	public Iterable<RK> getResourceKits() {
		return unmodifiableSet(resourceKits);
	}

	/** The map of content types mapped to lowercase URI name extensions. */
	private final Map<String, ContentType> extensionContentTypeMap = new HashMap<String, ContentType>(Files.FILE_EXTENSION_CONTENT_TYPE_MAP);

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

	//TODO should we use set maps instead of list maps here?

	/** The map of resource kit lists, keyed to resource kit classes. */
	private CollectionMap<Class<?>, RK, List<RK>> classResourceKitsMap = new CopyOnWriteArrayListConcurrentHashMap<Class<?>, RK>();

	/** The map of resource kit lists, keyed to supported content type base types. */
	private CollectionMap<String, RK, List<RK>> contentTypeResourceKitsMap = new CopyOnWriteArrayListConcurrentHashMap<String, RK>();

	/** The map of resource kit lists, keyed to supported resource type URIs. */
	private CollectionMap<URI, RK, List<RK>> resourceTypeResourceKitsMap = new CopyOnWriteArrayListConcurrentHashMap<URI, RK>();

	/**
	 * The thread-safe immutable set of content types supported by all the available resource kits.
	 * <em>Caution: {@link ContentType} does not correctly support {@link ContentType#equals(Object)}, which means equality in a set will be determined by identity.</em>
	 */
	private Set<ContentType> supportedContentTypes = emptySet();

	@Override
	public Set<ContentType> getSupportedContentTypes() {
		return supportedContentTypes;
	}

	/** The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit. */
	private RK defaultResourceKit = null;

	@Override
	public RK getDefaultResourceKit() {
		return defaultResourceKit;
	}

	/**
	 * Sets the default resource kit.
	 * @param defaultResourceKit The default resource kit if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit.
	 */
	protected void setDefaultResourceKit(final RK defaultResourceKit) {
		this.defaultResourceKit = defaultResourceKit;
	}

	/**
	 * The default resource kit to use if a specific resource kit cannot be found for a collection, or <code>null</code> if there is no default collection
	 * resource kit.
	 */
	private RK defaultCollectionResourceKit = null;

	@Override
	public RK getDefaultCollectionResourceKit() {
		return defaultCollectionResourceKit;
	}

	/**
	 * Sets the default collection resource kit.
	 * @param defaultCollectionResourceKit The default resource kit if a specific resource kit cannot be found for a collection, or <code>null</code> if there is
	 *          no default resource kit.
	 */
	protected void setDefaultCollectionResourceKit(final RK defaultCollectionResourceKit) {
		this.defaultCollectionResourceKit = defaultCollectionResourceKit;
	}

	/** Default constructor. */
	public AbstractMarmotSession() {
	}

	/** Updates the resource kit maps based upon the currently installed resources. This method synchronizes on the resource kit set. */
	protected void updateResourceKits() {
		synchronized(resourceKits) { //keep the update process from occurring while this thread is updating (the maps are individually thread-safe)
			classResourceKitsMap.clear(); //clear the maps
			contentTypeResourceKitsMap.clear();
			resourceTypeResourceKitsMap.clear();
			final Set<ContentType> supportedContentTypes = new HashSet<ContentType>(); //create a set of content types, even though content types use identity comparison
			for(final RK resourceKit : resourceKits) { //for each resource kit
				classResourceKitsMap.addItem(resourceKit.getClass(), resourceKit); //associate the resource kit instance with its type TODO register all its parent classes and interfaces up to but not including ResourceKit
				for(final ContentType contentType : resourceKit.getSupportedContentTypes()) { //for each content type this resource kit supports
					contentTypeResourceKitsMap.addItem(contentType.getBaseType(), resourceKit); //add this resource kit to the map			
					supportedContentTypes.add(contentType); //store the content type in our set
				}
				for(final URI resourceType : resourceKit.getSupportedResourceTypes()) { //for each resource type this resource kit supports
					resourceTypeResourceKitsMap.addItem(resourceType, resourceKit); //add this resource kit to the map			
				}
			}
			this.supportedContentTypes = unmodifiableSet(supportedContentTypes); //store the updated supported content types set
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #installResourceKit(ResourceKit, boolean, boolean)}.
	 * </p>
	 */
	@Override
	public void installResourceKit(final RK resourceKit) {
		installResourceKit(resourceKit, false, false); //install the resource kit, but not as the default resource kit
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #installResourceKit(ResourceKit, boolean, boolean)}.
	 * </p>
	 */
	@Override
	public void installDefaultResourceKit(final RK resourceKit) {
		installResourceKit(resourceKit, true, false); //install the resource kit as the default
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #installResourceKit(ResourceKit, boolean, boolean)}.
	 * </p>
	 */
	@Override
	public void installDefaultCollectionResourceKit(final RK resourceKit) {
		installResourceKit(resourceKit, false, true); //install the resource kit as the default for collections
	}

	/**
	 * Registers a resource kit with the session, specifying if the resource kit should be considered the default and/or the default collection resource kit. If
	 * this resource kit is specified as the default, it will replace any resource kit previously specified as the default.
	 * @param resourceKit The resource kit to register.
	 * @param isDefaultResourceKit Whether the resource kit should be the default.
	 * @param isDefaultCollectionResourceKit Whether the resource kit should be the default for collections.
	 * @throws IllegalStateException if the resource kit is already installed.
	 */
	protected void installResourceKit(final RK resourceKit, final boolean isDefaultResourceKit, final boolean isDefaultCollectionResourceKit) {
		if(resourceKit.getMarmotSession() != null) { //if the resource kit is already installed
			throw new IllegalStateException("Resource kit already intalled.");
		}
		assert !resourceKits.contains(resourceKit) : "Marmot contains unassigned resource kit.";
		resourceKit.setMarmotSession(this); //tell the resource kit its owner
		resourceKits.add(resourceKit); //add the resource kit
		if(isDefaultResourceKit) { //if this resource kit should be the default
			setDefaultResourceKit(resourceKit); //set the resource kit as the default
		}
		if(isDefaultCollectionResourceKit) { //if this resource kit should be the default for collections
			setDefaultCollectionResourceKit(resourceKit); //set the resource kit as the default for collections
		}
		updateResourceKits(); //update the resource kits
	}

	@Override
	public void uninstallResourceKit(final RK resourceKit) {
		if(resourceKit.getMarmotSession() != this) { //if the resource kit is not installed
			throw new IllegalStateException("Resource kit not intalled.");
		}
		assert resourceKits.contains(resourceKit) : "Marmot does not contain assigned resource kit.";
		if(getDefaultResourceKit() == resourceKit) { //if this is our default resource kit
			setDefaultResourceKit(null); //show that we have no default resource kit
		}
		if(getDefaultCollectionResourceKit() == resourceKit) { //if this is our default resource kit for collections
			setDefaultCollectionResourceKit(null); //show that we have no default collection resource kit
		}
		resourceKits.remove(resourceKit); //remove the resource kit
		resourceKit.setMarmotSession(null); //tell the resource kit it has no owner
		updateResourceKits(); //update the resource kits		
	}

	@Override
	public boolean hasResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities) {
		return getResourceKit(repository, resource, capabilities) != null; //determine if getting a resource kit would result in a resource kit
	}

	@Override
	public RK getResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities) {
		RK resourceKit = null;
		//step 1: try to match a resource kit by content type
		final ContentType contentType = determineContentType(resource); //get the content type of the resource
		if(contentType != null) { //if we know the content type of the resource
			resourceKit = getResourceKit(contentType, capabilities); //see if we have a resource kit registered for this content type and capabilities
		}
		//step 2: try to match a resource kit by resource type
		if(resourceKit == null) { //if we haven't yet found a resource kit, try to match a resource by resource type
			final Iterator<URFResource> typeIterator = resource.getTypes().iterator(); //get an iterator to all the types of this resource
			while(resourceKit == null && typeIterator.hasNext()) { //while there are more types and we haven't yet found a resource kit
				final URI typeURI = typeIterator.next().getURI(); //get the URI of the next type
				if(typeURI != null) { //if there is a type URI
					resourceKit = getResourceKit(typeURI, capabilities); //see if we have a resource kit registered for this resource type URI and capabilities
				}
			}
		}
		//step 3: ask each resource kit individually if it supports this resource
		/*TODO fix or del if not needed
				if(resourceKit==null) {	//if we haven't yet found a resource kit, ask each resource kit individually
					final Iterator resourceKitIterator=getRegisteredResourceKitIterator();	//get an iterator to the resource kits
					while(resourceKitIterator.hasNext()) {	//while there are more resource kits
						final ResourceKit currentResourceKit=(ResourceKit)resourceKitIterator.next();	//get the next resource kit
						if(currentResourceKit.supports(resource)) {	//if this resource kit supports the resource
							resourceKit=currentResourceKit;	//use this resource kit
							break;	//stop looking for a resource kit
						}
					}			
				}
		*/
		if(resourceKit == null) { //if we have exhausted all attempts to get a matching resource kit
			final URI resourceURI = resource.getURI(); //get the URI of the resource
			if(resourceURI != null && isCollectionURI(resourceURI)) { //if this is a collection URI
				final RK defaultCollectionResourceKit = getDefaultCollectionResourceKit(); //use the default collection resource kit, if there is one
				if(defaultCollectionResourceKit != null && defaultCollectionResourceKit.hasCapabilities(capabilities)) { //if there is a default collection resource kit that has the requested capabilities
					resourceKit = defaultCollectionResourceKit; //use the default collection resource kit
				}
			}
			if(resourceKit == null) { //if we didn't find an appropriate default collection resource kit
				final RK defaultResourceKit = getDefaultResourceKit(); //use the default resource kit, if there is one
				if(defaultResourceKit != null && defaultResourceKit.hasCapabilities(capabilities)) { //if there is a default resource kit that has the requested capabilities
					resourceKit = defaultResourceKit; //use the default resource kit
				}
			}
		}
		return resourceKit; //return whatever resource kit we found, if any
	}

	@Override
	public RK getResourceKit(final Repository repository, final URI resourceURI, ContentType contentType, final Capability... capabilities)
			throws ResourceIOException {
		if(contentType == null) { //if no content type was given, try to find one
			contentType = determineContentType(resourceURI); //try to determine the content type from the URI alone
		}
		RK resourceKit = null;
		if(contentType != null) { //if we found a content type
			resourceKit = getResourceKit(contentType, capabilities); //see if we have a resource kit registered for this content type and capabilities
		}
		if(resourceKit == null) { //if we still don't have a resource kit
			final URFResource resource = repository.getResourceDescription(resourceURI); //get the description of the resource from the repository
			resourceKit = getResourceKit(repository, resource, capabilities); //try to get a resource kit based upon the complete description
		}
		return resourceKit;
	}

	/**
	 * Retrieves a resource kit appropriate for the given resource based upon its type URI.
	 * @param typeURI The URI of the resource type.
	 * @param capabilities The capabilities required for the resource kit.
	 * @return A resource kit to handle the given resource type, or <code>null</code> if no appropriate resource kit is registered.
	 */
	protected RK getResourceKit(final URI typeURI, final Capability... capabilities) { //TODO iterate available resource kits
		final RK resourceKit = resourceTypeResourceKitsMap.getItem(typeURI); //get the resource kit, if any, registered for this resource type URI
		return resourceKit != null && resourceKit.hasCapabilities(capabilities) ? resourceKit : null; //return the resource kit if it has the given capabilities
	}

	@Override
	public RK getResourceKit(final String resourceName, ContentType contentType, final Capability... capabilities) {
		if(contentType == null) { //if no content type was given, try to find one
			contentType = determineContentType(resourceName); //try to determine the content type from the resource name alone
		}
		return contentType != null ? getResourceKit(contentType, capabilities) : null; //see if we have a resource kit registered for this content type and capabilities
	}

	@Override
	public RK getResourceKit(final ContentType contentType, final Capability... capabilities) { //TODO iterate available resource kits
		final RK resourceKit = contentTypeResourceKitsMap.getItem(contentType.getBaseType()); //get the resource kit, if any, registered for this content type
		return resourceKit != null && resourceKit.hasCapabilities(capabilities) ? resourceKit : null; //return the resource kit if it has the given capabilities
	}

	@Override
	public <RK2 extends RK> RK2 getResourceKit(final Class<RK2> resourceKitClass) {
		return resourceKitClass.cast(classResourceKitsMap.getItem(resourceKitClass)); //get the resource kit, if any, registered for this class
	}

	@Override
	public ContentType determineContentType(final URFResource resource) {
		ContentType contentType = Content.getContentType(resource); //see if the resource indicates a content type
		if(contentType == null) { //if the resource does not indicate a content type
			final URI resourceURI = resource.getURI(); //get the resource URI
			if(resourceURI != null) {
				contentType = determineContentType(resourceURI); //try to determine the content type from the URI alone
			}
		}
		return contentType;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation delegates to {@link #determineContentType(String)}.
	 * </p>
	 */
	@Override
	public ContentType determineContentType(final URI resourceURI) {
		final String resourceName = !isCollectionURI(resourceURI) ? URIs.getRawName(resourceURI) : null; //get the resource name, if any
		if(resourceName != null) { //if we have a resource name
			return determineContentType(resourceName); //get the registered content type
		}
		return null;
	}

	@Override
	public ContentType determineContentType(final String resourceName) {
		return getExtensionContentType(getNameExtension(resourceName)); //get the registered content type, if any, for the resource's extension (which may be null)
	}

	@Override
	public Charset determineCharset(final URFResource resource) {
		Charset charset = Content.getCharset(resource); //see if the resource indicates a charset
		if(charset == null) { //if the resource does not indicate a charset
			final ContentType contentType = determineContentType(resource); //try to determine a content type
			if(contentType != null) { //if we could determine a content type
				charset = getContentTypeCharset(contentType); //get the registered charset, if any, for the determined content type
			}
		}
		return charset;
	}
}

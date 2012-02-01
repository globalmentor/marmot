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

import java.nio.charset.Charset;
import java.util.Set;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.ResourceKit;
import com.globalmentor.marmot.resource.ResourceKit.Capability;
import com.globalmentor.marmot.security.MarmotSecurityManager;
import com.globalmentor.net.ContentType;
import com.globalmentor.urf.URFResource;
import com.globalmentor.urf.content.Content;

/**
 * Marmot session information.
 * @param <RK> The type of resource kits supported by this session.
 * @author Garret Wilson
 */
public interface MarmotSession<RK extends ResourceKit>
{

	/** @return The installed Marmot security manager. */
	public MarmotSecurityManager getSecurityManager();

	/**
	 * Registers a resource kit with the session.
	 * @param resourceKit The resource kit to register.
	 * @throws IllegalStateException if the resource kit is already installed.
	 */
	public void installResourceKit(final RK resourceKit);

	/**
	 * Registers a resource kit with the session, specifying it as the default resource kit. The resource kit will replaced any other resource kit designated as
	 * default.
	 * @param resourceKit The resource kit to register.
	 * @throws IllegalStateException if the resource kit is already installed.
	 */
	public void installDefaultResourceKit(final RK resourceKit);

	/**
	 * Registers a resource kit with the session, specifying it as the default resource kit for collections. The resource kit will replaced any other resource kit
	 * designated as default for collections.
	 * @param resourceKit The resource kit to register.
	 * @throws IllegalStateException if the resource kit is already installed.
	 */
	public void installDefaultCollectionResourceKit(final RK resourceKit);

	/**
	 * Unregisters a resource kit with the session. If this resource kit was previously set as the default, there will no longer be a default resource kit.
	 * @param resourceKit The resource kit to unregister.
	 * @throws IllegalStateException if the resource kit is not installed in this session.
	 */
	public void uninstallResourceKit(final RK resourceKit);

	/** @return The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit. */
	public RK getDefaultResourceKit();

	/**
	 * @return The default resource kit to use if a specific resource kit cannot be found for a collection, or <code>null</code> if there is no default collection
	 *         resource kit.
	 */
	public RK getDefaultCollectionResourceKit();

	/** @return The available resource kits. */
	public Iterable<RK> getResourceKits();

	/**
	 * Returns the content types supported by all the available resource kits.
	 * @return An immutable set of content types supported by all available resource kits.
	 * @see ResourceKit#getSupportedContentTypes()
	 */
	public Set<ContentType> getSupportedContentTypes();

	/**
	 * Determines if there exists resource kit appropriate for the given resource supporting the given capabilities.
	 * @param repository The repository in which the resource resides.
	 * @param resource The resource for which a resource kit should be returned.
	 * @param capabilities The capabilities required for the resource kit.
	 * @return <code>true</code> if there exists a resource kit to handle the given resource with the given capabilities, if any, in relation to the resource.
	 * @see #getResourceKit(Repository, URFResource, Capability...)
	 */
	public boolean hasResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities);

	/**
	 * Retrieves a resource kit appropriate for the given resource. This method locates a resource kit in the following priority:
	 * <ol>
	 * <li>The first resource kit supporting the resource content type determined by {@link #determineContentType(URFResource)}.</li>
	 * <li>The first resource kit supporting one of the resource types.</li>
	 * <li>If the resource has a collection URI, the default collection resource kit.</li>
	 * <li>The default resource kit.</li>
	 * </ol>
	 * @param repository The repository in which the resource resides.
	 * @param resource The resource for which a resource kit should be returned.
	 * @param capabilities The capabilities required for the resource kit.
	 * @return A resource kit to handle the given resource with the given capabilities, if any, in relation to the resource; or <code>null</code> if there is no
	 *         registered resource kit with the given capabilities in relation to the resource.
	 * @see #determineContentType(URFResource)
	 */
	public RK getResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities);

	/**
	 * Retrieves a resource kit appropriate for a MIME content type. This method should only be used for special-purpose functionality; when accessing resources
	 * {@link #getResourceKit(Repository, URFResource, Capability...)} should normally be used instead.
	 * @param contentType The type of content the resource contains.
	 * @param capabilities The capabilities required for the resource kit.
	 * @return A resource kit with the requested capabilities to handle the given content type, or <code>null</code> if no appropriate resource kit is registered.
	 */
	public RK getResourceKit(final ContentType contentType, final Capability... capabilities);

	/**
	 * Retrieves a resource kit previously registered that is an instance of the given class.
	 * @param resourceKitClass The class representing the type of registered resource kit to return.
	 * @return A resource kit previously registered that is an instance of the given class, or <code>null</code> if no resource class instance of the given class
	 *         is registered.
	 */
	public <RK2 extends RK> RK2 getResourceKit(final Class<RK2> resourceKitClass);

	/**
	 * Associates the given content type with the given extension, without regard to case.
	 * @param extension The URI name extension with which the content type should be associated, or <code>null</code> if the content type should be associated
	 *          with resources that have no extension.
	 * @param contentType The content type to associate with the given extension.
	 * @return The content type previously registered with the given extension, or <code>null</code> if no content type was previously registered.
	 * @throws NullPointerException if the given content type is <code>null</code>.
	 */
	public ContentType registerExtensionContentType(final String extension, final ContentType contentType);

	/**
	 * Returns the content type associated with the given extension, without regard to case.
	 * @param extension The URI name extension with which the content type is associated, or <code>null</code> if the content type is associated with resources
	 *          that have no extension.
	 * @return The content type associated with the given extension, or <code>null</code> if there is no content type associated with the given extension.
	 */
	public ContentType getExtensionContentType(final String extension);

	/**
	 * Associates the given charset with the base media type of the given content type. Any association will only override resources that do not explicitly have a
	 * charset specified. Any parameters of the given content type will be ignored.
	 * @param contentType The content type with which the charset should be associated.
	 * @param charset The charset to associate with the given content type.
	 * @return The charset previously registered with the given content type, or <code>null</code> if no charset was previously registered.
	 * @throws NullPointerException if the given content type and/or charset is <code>null</code>.
	 */
	public Charset registerContentTypeCharset(final ContentType contentType, final Charset charset);

	/**
	 * Returns the charset associated with the given content type. Any parameters of the given content type will be ignored.
	 * @param contentType The content type with which the charset is associated.
	 * @return The charset associated with the given content type, or <code>null</code> if there is no charset associated with the given content type.
	 * @throws NullPointerException if the given content type is <code>null</code>.
	 */
	public Charset getContentTypeCharset(final ContentType contentType);

	/**
	 * Determines the content type of a resource. The content type is determined in this order:
	 * <ol>
	 * <li>The value of the {@value Content#TYPE_PROPERTY_URI} property, if any.</li>
	 * <li>The registered content type for the resource extension, if any, returned by {@link #getExtensionContentType(String)}.</li>
	 * </ol>
	 * @param resource The resource for which a content type should be determined.
	 * @return The content type for the given resource, or <code>null</code> if no content type can be determined for the given resource.
	 */
	public ContentType determineContentType(final URFResource resource);

	/**
	 * Determines the charset of a resource. The charset is determined in this order:
	 * <ol>
	 * <li>The value of the {@value Content#CHARSET_PROPERTY_URI} property, if any.</li>
	 * <li>The registered charset for the determined content type, if any, returned by {@link #getContentTypeCharset(ContentType)}.</li>
	 * </ol>
	 * @param resource The resource for which a charset should be determined.
	 * @return The charset for the given resource, or <code>null</code> if no charset can be determined for the given resource.
	 * @see #determineContentType(URFResource)
	 */
	public Charset determineCharset(final URFResource resource);

}
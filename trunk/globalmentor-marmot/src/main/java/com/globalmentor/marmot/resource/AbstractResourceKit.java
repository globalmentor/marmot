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

package com.globalmentor.marmot.resource;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.urframework.*;
import org.urframework.content.Content;

import static java.util.Arrays.*;
import static java.util.Collections.*;
import static org.urframework.content.Content.*;
import static org.urframework.dcmi.DCMI.*;

import static com.globalmentor.java.Bytes.*;
import static com.globalmentor.java.Enums.*;
import static com.globalmentor.java.Objects.*;
import static com.globalmentor.net.URIs.*;

import com.globalmentor.iso.datetime.ISODateTime;
import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;
import com.globalmentor.net.ContentType;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.net.URIPath;

/**
 * Abstract implementation of a resource kit.
 * @author Garret Wilson
 */
public abstract class AbstractResourceKit implements ResourceKit
{

	private MarmotSession<?> marmotSession = null;

	@Override
	public MarmotSession<?> getMarmotSession()
	{
		return marmotSession;
	}

	@Override
	public void setMarmotSession(final MarmotSession<?> marmotSession)
	{
		final MarmotSession<?> oldMarmotSession = this.marmotSession; //get the current Marmot session
		if(marmotSession != null) //if this resource kit is being installed
		{
			if(oldMarmotSession != null && oldMarmotSession != this) //if we're already installed in another session
			{
				throw new IllegalStateException("Resource kit already installed in session " + oldMarmotSession);
			}
		}
		if(oldMarmotSession != marmotSession) //if the value is really changing (compare identity, because for sessions the instance matters)
		{
			this.marmotSession = marmotSession; //change the value
		}
	}

	/** {@inheritDoc} This version returns any of the supported content types, if there are any. */
	@Override
	public ContentType getDefaultContentType()
	{
		return !supportedContentTypes.isEmpty() ? supportedContentTypes.iterator().next() : null;
	}

	private final Set<ContentType> supportedContentTypes;

	@Override
	public Set<ContentType> getSupportedContentTypes()
	{
		return supportedContentTypes;
	}

	private final Set<URI> supportedResourceTypes;

	@Override
	public Set<URI> getSupportedResourceTypes()
	{
		return supportedResourceTypes;
	}

	private final String defaultNameExtension;

	@Override
	public String getDefaultNameExtension()
	{
		return defaultNameExtension;
	}

	private final Set<Capability> capabilities;

	@Override
	public Set<Capability> getCapabilities()
	{
		return capabilities;
	}

	@Override
	public boolean hasCapabilities(final Capability... capabilities)
	{
		for(final Capability capability : capabilities) //for each given capability
		{
			if(!this.capabilities.contains(capability)) ///if this capability isn't present
			{
				return false; //there is a capability not had
			}
		}
		return true; //this resource kit has all the given capabilities, if any
	}

	/**
	 * Capabilities constructor with no support for content type or types.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final Capability... capabilities)
	{
		this((String)null, capabilities); //construct the class with no default extension
	}

	/**
	 * Content types and capabilities constructor with no default extension.
	 * @param supportedContentType The content type supported by this resource kit.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported content type and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final ContentType supportedContentType, final Capability... capabilities)
	{
		this(supportedContentType, null, capabilities); //construct the class with no default extension
	}

	/**
	 * Content types and capabilities constructor with no default extension.
	 * @param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported content types array and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final Capability... capabilities)
	{
		this(supportedContentTypes, (String)null, capabilities); //construct the class with no default extension
	}

	/**
	 * Resource type and capabilities constructor with no default extension.
	 * @param supportedResourceType The URI for the resource type this resource kit supports.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported resource types and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final URI supportedResourceType, final Capability... capabilities)
	{
		this(supportedResourceType, null, capabilities); //construct the class with no default extension
	}

	/**
	 * Resource types and capabilities constructor with no default extension.
	 * @param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported resource types array and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this(supportedResourceTypes, null, capabilities); //construct the class with no default extension
	}

	/**
	 * Content types, resource types, and capabilities constructor with no default extension.
	 * @param presentation The presentation support for this resource kit.
	 * @param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	 * @param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	 */
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this(supportedContentTypes, supportedResourceTypes, null, capabilities); //construct the class with no default extension
	}

	/**
	 * Default extension and capabilities constructor with no support for content type or types.
	 * @param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an
	 *          extension.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[0], defaultNameExtension, capabilities);
	}

	/**
	 * Content types, default extension, and capabilities constructor.
	 * @param supportedContentType The content type supported by this resource kit.
	 * @param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an
	 *          extension.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported content type and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final ContentType supportedContentType, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[] { checkInstance(supportedContentType, "Supported content type cannot be null.") }, defaultNameExtension, capabilities);
	}

	/**
	 * Content types, default extension, and capabilities constructor.
	 * @param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	 * @param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an
	 *          extension.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported content types array and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		this(supportedContentTypes, new URI[] {}, defaultNameExtension, capabilities); //construct the class with no supported resource types
	}

	/**
	 * Resource type, default extension, and capabilities constructor.
	 * @param supportedResourceType The URI for the resource type this resource kit supports.
	 * @param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an
	 *          extension.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported resource types and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final URI supportedResourceType, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new URI[] { checkInstance(supportedResourceType, "Supported resource type cannot be null.") }, defaultNameExtension, capabilities);
	}

	/**
	 * Resource types, default extension, and capabilities constructor.
	 * @param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	 * @param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an
	 *          extension.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported resource types array and/or capabilities is <code>null</code>.
	 */
	public AbstractResourceKit(final URI[] supportedResourceTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[] {}, supportedResourceTypes, defaultNameExtension, capabilities); //construct the class with no supported content types
	}

	/**
	 * Content types, resource types, default extension, and capabilities constructor.
	 * @param presentation The presentation support for this resource kit.
	 * @param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	 * @param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	 * @param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an
	 *          extension.
	 * @param capabilities The capabilities provided by this resource kit.
	 * @throws NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	 */
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes, final String defaultNameExtension,
			final Capability... capabilities)
	{
		this.supportedContentTypes = unmodifiableSet(new HashSet<ContentType>(asList(checkInstance(supportedContentTypes,
				"Supported content types array cannot be null."))));
		this.supportedResourceTypes = unmodifiableSet(new HashSet<URI>(asList(checkInstance(supportedResourceTypes,
				"Supported resource types array cannot be null."))));
		this.defaultNameExtension = defaultNameExtension;
		this.capabilities = unmodifiableSet(createEnumSet(Capability.class, capabilities));
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version sets a default content type if the resource kit supports content types.
	 * </p>
	 * <p>
	 * This implementation sets the DCMI date to a floating representation of the current date/time.
	 * </p>
	 * @see Content#TYPE_PROPERTY_URI
	 * @see DCMI#DATE_PROPERTY_URI
	 */
	@Override
	public URFResource getDefaultResourceDescription(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		final URFResource resource = new DefaultURFResource(resourceURI); //create a new resource description
		final ContentType contentType = getDefaultContentType(); //get the default content type, if any
		if(contentType != null) //if there is a default content type
		{
			setContentType(resource, contentType); //set the content type
		}
		setDate(resource, new ISODateTime()); //set the date to the current date and time with no particular time zone
		return resource; //return the default resource
	}

	/** {@inheritDoc} This version does nothing. */
	@Override
	public void initializeResourceDescription(final Repository repository, final URFResource resource) throws ResourceIOException
	{
	}

	/** {@inheritDoc} This default version returns the collection of the parent resource. */
	@Override
	public URI getChildResourceCollectionURI(final Repository repository, final URI parentResourceURI) throws ResourceIOException
	{
		return repository.getCollectionURI(parentResourceURI); //return the collection URI of the parent resource
	}

	/**
	 * {@inheritDoc} This default version resolves the name against the parent resource URI and appends the resource kit's default extension, if any.
	 * @see #getDefaultNameExtension()
	 */
	@Override
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName) throws ResourceIOException
	{
		//TODO fix IllegalArgumentException by checking to ensure that the parent resource is within the repository
		final StringBuilder stringBuilder = new StringBuilder(URIPath.encodeSegment(resourceName)); //create a new string builder, starting with the encoded simple resource name
		final String defaultExtension = getDefaultNameExtension(); //get the default extension, if any
		if(defaultExtension != null) //if we have a default extension
		{
			stringBuilder.append(NAME_EXTENSION_SEPARATOR).append(defaultExtension); //append the default extension
		}
		return resolve(parentResourceURI, URIPath.createURIPathURI(stringBuilder.toString())); //resolve the encoded name against the parent resource URI; use the special URIPath method in case the name contains a colon character
	}

	/** {@inheritDoc} This implementation delegates to {@link #createResource(Repository, URI, URFResource)} with a default description. */
	@Override
	public final URFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		return createResource(repository, resourceURI, new DefaultURFResource(resourceURI)); //create the resource with a default description
	}

	/**
	 * {@inheritDoc} This version creates a resource then writes default content, if any, using
	 * {@link #writeDefaultResourceContent(Repository, URI, URFResource, OutputStream)}.
	 */
	@Override
	public URFResource createResource(final Repository repository, final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		if(hasDefaultResourceContent(repository, resourceURI)) //if there is default content for the resource
		{
			final OutputStream outputStream = new BufferedOutputStream(repository.createResource(resourceURI, resourceDescription)); //create a new resource
			try
			{
				try
				{
					if(hasDefaultResourceContent(repository, resourceURI)) //if there is default content for the resource
					{
						writeDefaultResourceContent(repository, resourceURI, resourceDescription, outputStream); //write default content to the output stream
					}
				}
				finally
				{
					outputStream.close(); //always close the output stream
				}
				return repository.getResourceDescription(resourceURI); //return the resource description
			}
			catch(final IOException ioException) //if an I/O exception occurs
			{
				throw ResourceIOException.toResourceIOException(ioException, resourceURI); //send a resource version of the exception
			}
		}
		else
		//if there is no default content for the resource
		{
			return repository.createResource(resourceURI, resourceDescription, NO_BYTES); //create a new resource with no content
		}
	}

	/** {@inheritDoc} This implementation delegates to {@link #writeDefaultResourceContent(Repository, URI, URFResource, OutputStream)}. */
	@Override
	public URFResource writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription)
			throws ResourceIOException
	{
		final OutputStream outputStream = new BufferedOutputStream(repository.getResourceOutputStream(resourceURI)); //create a new resource
		try
		{
			try
			{
				writeDefaultResourceContent(repository, resourceURI, resourceDescription, outputStream); //write default content to the output stream
			}
			finally
			{
				outputStream.close(); //always close the output stream
			}
		}
		catch(final IOException ioException) //if an I/O exception occurs
		{
			throw ResourceIOException.toResourceIOException(ioException, resourceURI); //send a resource version of the exception
		}
		return repository.getResourceDescription(resourceURI); //return the resource description
	}

	/** {@inheritDoc} This version returns <code>false</code>. */
	public boolean hasDefaultResourceContent(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		return false;
	}

	/** {@inheritDoc} This version writes no content. */
	@Override
	public void writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription,
			final OutputStream outputStream) throws ResourceIOException
	{
	}

	/** {@inheritDoc} This version returns <code>null</code>. */
	@Override
	public Class<? extends ResourceAspect> getAspectType()
	{
		return null;
	}

	/** {@inheritDoc} This version recognizes no aspects. */
	@Override
	public boolean isAspectAllowed(final ResourceAspect aspect, final PermissionType permissionType)
	{
		return false;
	}

	/** {@inheritDoc} This version recognizes no aspects. */
	@Override
	public ResourceContentFilter[] getAspectFilters(final ResourceAspect aspect)
	{
		return new ResourceContentFilter[0];
	}

	/**
	 * Determines the URI of a resource related to the given resource. This method is useful for determining a specified or default template or theme resource.
	 * First a related resource is attempted to be identified from the specified property, if any. Then, if there is no related resource explicitly identified, a
	 * related resource of the given name is searched for in the directory and optionally up the hierarchy. This implementation only supports relative path URIs.
	 * @param repository The repository in which the resource resides.
	 * @param resourceURI The URI of the resource for which a related resource URI should be retrieved.
	 * @param relatedResourcePathURIPropertyURI The URI of the property indicating an explicit related resource by its path URI, or <code>null</code> if there no
	 *          explicit related resource is allowed to be designated.
	 * @param defaultRelatedResourceName The name of a default related resource to be found in the same collection as the given resource, or <code>null</code> if
	 *          no default related resources are allowed.
	 * @param checkAncestors <code>true</code> if default related resources should be searched for up the ancestor hierarchy.
	 * @return The URI of the related resource, or <code>null</code> if no related resource could be located.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public static URI getRelatedResourceURI(final Repository repository, final URI resourceURI, final URI relatedResourcePathURIPropertyURI,
			final String defaultRelatedResourceName, final boolean checkAncestors) throws ResourceIOException
	{
		return getRelatedResourceURI(repository, repository.getResourceDescription(resourceURI), relatedResourcePathURIPropertyURI, defaultRelatedResourceName,
				checkAncestors); //get the description of the resource and look for a related resource
	}

	/**
	 * Determines the URI of a resource related to the given resource. This method is useful for determining a specified or default template or theme resource.
	 * First a related resource is attempted to be identified from the specified property, if any. Then, if there is no related resource explicitly identified, a
	 * related resource of the given name is searched for in the directory and optionally up the hierarchy. This implementation only supports relative path URIs.
	 * @param repository The repository in which the resource resides.
	 * @param resource The resource for which a related resource URI should be retrieved.
	 * @param relatedResourcePathURIPropertyURI The URI of the property indicating an explicit related resource by its path URI, or <code>null</code> if there no
	 *          explicit related resource is allowed to be designated.
	 * @param defaultRelatedResourceName The name of a default related resource to be found in the same collection as the given resource, or <code>null</code> if
	 *          no default related resources are allowed.
	 * @param checkAncestors <code>true</code> if default related resources should be searched for up the ancestor hierarchy.
	 * @return The URI of the related resource, or <code>null</code> if no related resource could be located.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public static URI getRelatedResourceURI(final Repository repository, final URFResource resource, final URI relatedResourcePathURIPropertyURI,
			final String defaultRelatedResourceName, final boolean checkAncestors) throws ResourceIOException
	{
		final URI resourceURI = resource.getURI(); //get the URI of the resource
		if(relatedResourcePathURIPropertyURI != null) //if we should check a property for an explicit related resource
		{
			final URI explicitRelatedResourceURI = URF.asURI(resource.getPropertyValue(relatedResourcePathURIPropertyURI)); //get the related resource URI property value, if any
			if(explicitRelatedResourceURI != null) //if there is a related resource URI specified
			{
				final URIPath relatedResourcePath = URIPath.asPathURIPath(explicitRelatedResourceURI); //see if this is a path: URI
				if(relatedResourcePath == null || !relatedResourcePath.isRelative()) //if this is not a relative path TODO determine how to handle absolute paths and general URIs appropriately; this will probably include making subrepositories be able to access root repositories
				{
					throw new ResourceIOException(resourceURI, "Specified related resource URI " + explicitRelatedResourceURI + " for resource " + resourceURI
							+ " currently must be a relative <path:...> URI.");
				}
				return resolve(resourceURI, relatedResourcePath.toURI()); //resolve the related resource path to the resource URI
			}
		}
		if(defaultRelatedResourceName != null) //if a default related resource name was given
		{
			URI collectionURI = getCurrentLevel(resourceURI); //start at the current collection level
			do
			{
				final URI relatedResourceURI = resolve(collectionURI, defaultRelatedResourceName); //get the URI of the related resource if it were to reside at this level
				if(repository.resourceExists(relatedResourceURI)) //if the related resource exists here
				{
					return relatedResourceURI; //return the URI of the template
				}
				collectionURI = repository.getParentResourceURI(collectionURI); //go up a level
			}
			while(checkAncestors && collectionURI != null); //keep going up the hierarchy until we run out of parent collections
		}
		return null; //indicate that we could find no template URI
	}
}

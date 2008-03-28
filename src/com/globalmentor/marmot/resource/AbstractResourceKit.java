package com.globalmentor.marmot.resource;

import java.net.URI;
import java.util.*;
import static java.util.Collections.*;

import javax.mail.internet.ContentType;

import static com.globalmentor.java.Enums.*;
import static com.globalmentor.java.Objects.*;
import static com.globalmentor.net.URIs.*;

import static com.globalmentor.java.Bytes.*;
import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.net.URIPath;
import com.globalmentor.urf.*;

import static com.globalmentor.urf.dcmi.DCMI.*;

/**Abstract implementation of a resource kit.
@author Garret Wilson
*/
public abstract class AbstractResourceKit implements ResourceKit
{

	/**The Marmot session with which this resource kit is associated.*/
	private MarmotSession<?> marmotSession=null;

		/**@return The Marmot session with which this resource kit is associated, or <code>null</code> if this resource kit has not yet been installed.*/
		public MarmotSession<?> getMarmotSession() {return marmotSession;}

		/**Sets the Marmot session with which this resource kit is associated.
		@param marmotSession The Marmot session with which the resource kit should be associated, or <code>null</code> if the resource kit is not installed.
		@exception IllegalStateException if this resource kit has already been installed in another Marmot session.
		*/
		public void setMarmotSession(final MarmotSession<?> marmotSession)
		{
			final MarmotSession<?> oldMarmotSession=this.marmotSession;	//get the current Marmot session
			if(marmotSession!=null)	//if this resource kit is being installed
			{
				if(oldMarmotSession!=null && oldMarmotSession!=this)	//if we're already installed in another session
				{
					throw new IllegalStateException("Resource kit already installed in session "+oldMarmotSession);
				}
			}
			if(oldMarmotSession!=marmotSession)	//if the value is really changing (compare identity, because for sessions the instance matters)
			{
				this.marmotSession=marmotSession;	//change the value
			}
		}

	/**A non-<code>null</code> array of the content types this resource kit supports.*/
	private final ContentType[] supportedContentTypes;

		/**Returns the content types supported.
		This is the primary method of determining which resource kit to use for a given resource.
		@return A non-<code>null</code> array of the content types this resource kit supports.
		*/
		public ContentType[] getSupportedContentTypes() {return supportedContentTypes;}

	/**A non-<code>null</code> array of the URIs for the resource types this resource kit supports.*/
	private final URI[] supportedResourceTypes;
	
		/**Returns the resource types supported.
		This is the secondary method of determining which resource kit to use for a given resource.
		@return A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
		*/
		public URI[] getSupportedResourceTypes() {return supportedResourceTypes;}

	/**The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.*/
	private final String defaultNameExtension;

		/**Returns the default name extension used for the resource URI.
		@return The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
		*/
		public String getDefaultNameExtension() {return defaultNameExtension;}

	/**The capabilities provided by this resource kit.*/
	private final Set<Capability> capabilities;
	
		/**Returns the capabilities of this resource kit.
		@return The capabilities provided by this resource kit.
		*/
		public Set<Capability> getCapabilities() {return capabilities;}

		/**Determines if the resource kit has all the given capabilities.
		@param capabilities The capabilities to check.
		@return <code>true</code> if the resource kit has all the given capabilities, if any.
		*/
		public boolean hasCapabilities(final Capability... capabilities)
		{
			for(final Capability capability:capabilities)	//for each given capability
			{
				if(!this.capabilities.contains(capability))	///if this capability isn't present
				{
					return false;	//there is a capability not had
				}
			}
			return true;	//this resource kit has all the given capabilities, if any
		}

	/**Capabilities constructor with no support for content type or types.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final Capability... capabilities)
	{
		this((String)null, capabilities);	//construct the class with no default extension
	}

	/**Content types and capabilities constructor with no default extension.
	@param supportedContentType The content type supported by this resource kit.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content type and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType supportedContentType, final Capability... capabilities)
	{
		this(supportedContentType, null, capabilities);	//construct the class with no default extension
	}

	/**Content types and capabilities constructor with no default extension.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final Capability... capabilities)
	{
		this(supportedContentTypes, (String)null, capabilities);	//construct the class with no default extension
	}

	/**Resource type and capabilities constructor with no default extension.
	@param supportedResourceType The URI for the resource type this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final URI supportedResourceType, final Capability... capabilities)
	{
		this(supportedResourceType, null, capabilities);	//construct the class with no default extension
	}

	/**Resource types and capabilities constructor with no default extension.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types array and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this(supportedResourceTypes, null, capabilities);	//construct the class with no default extension
	}

	/**Content types, resource types, and capabilities constructor with no default extension.
	@param presentation The presentation support for this resource kit.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this(supportedContentTypes, supportedResourceTypes, null, capabilities);	//construct the class with no default extension
	}
	
	/**Default extension and capabilities constructor with no support for content type or types.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[0], defaultNameExtension, capabilities);
	}

	/**Content types, default extension, and capabilities constructor.
	@param supportedContentType The content type supported by this resource kit.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content type and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType supportedContentType, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[]{checkInstance(supportedContentType, "Supported content type cannot be null.")}, defaultNameExtension, capabilities);
	}

	/**Content types, default extension, and capabilities constructor.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		this(supportedContentTypes, new URI[]{}, defaultNameExtension, capabilities);	//construct the class with no supported resource types
	}

	/**Resource type, default extension, and capabilities constructor.
	@param supportedResourceType The URI for the resource type this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final URI supportedResourceType, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new URI[]{checkInstance(supportedResourceType, "Supported resource type cannot be null.")}, defaultNameExtension, capabilities);
	}

	/**Resource types, default extension, and capabilities constructor.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types array and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final URI[] supportedResourceTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[]{}, supportedResourceTypes, defaultNameExtension, capabilities);	//construct the class with no supported content types
	}

	/**Content types, resource types, default extension, and capabilities constructor.
	@param presentation The presentation support for this resource kit.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		this.supportedContentTypes=checkInstance(supportedContentTypes, "Supported content types array cannot be null.");
		this.supportedResourceTypes=checkInstance(supportedResourceTypes, "Supported resource types array cannot be null.");
		this.defaultNameExtension=defaultNameExtension;
		this.capabilities=unmodifiableSet(createEnumSet(Capability.class, capabilities));
	}
	
	/**Retrieves a default resource description for a given resource, without regard to whether it exists.
	This implementation sets the DCMI date to a floating representation of the current date/time.
	@param repository The repository within which the resource would reside.
	@param resourceURI The URI of the resource for which a default resource description should be retrieved.
	@exception ResourceIOException if there is an error accessing the repository.
	@see DCMI#DATE_PROPERTY_URI
	*/
	public URFResource getDefaultResourceDescription(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		final URFResource resource=new DefaultURFResource(resourceURI);	//create a new resoruce description
		setDate(resource, new URFDateTime());	//set the date to the current date and time with no particular time zone
		return resource;	//return the default resource
	}

	/**Retrieves default resource contents for a given resource, without regard to whether it exists.
	This version returns an empty array of bytes.
	@param repository The repository within which the resource would reside.
	@param resource The description of the resource the default contents of which to retrieve.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public byte[] getDefaultResourceContents(final Repository repository, final URFResource resource) throws ResourceIOException
	{
		return NO_BYTES;	//default to an empty resource
	}

	/**Initializes a resource description, creating whatever properties are appropriate.
	This version does nothing.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception ResourceIOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final URFResource resource) throws ResourceIOException
	{
	}

	/**Returns the URI of the collection of a child resource.
	This is normally the collection URI of the parent resource URI.
	Some implementations may prefer for child resources by default to be placed within sub-collections.
	This default version returns the collection of the parent resource.
	@param repository The repository that contains the resource.
	@param parentResourceURI The URI to of the parent resource.
	@return The URI of the child resource collection
	@exception NullPointerException if the given repository and/or parent resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if there is an error accessing the repository.
	@see Repository#getCollectionURI(URI)
	*/
	public URI getChildResourceCollectionURI(final Repository repository, final URI parentResourceURI) throws ResourceIOException
	{
		return repository.getCollectionURI(parentResourceURI);	//return the collection URI of the parent resource
	}

	/**Returns the URI of a child resource with the given simple name within a parent resource.
	This is normally the simple name resolved against the child resource collection URI, although a resource kit for collections may append an ending path separator.
	The simple name will be encoded before being used to construct the URI.
	This default version resolves the name against the parent resource URI and appends the resource kit's default extension, if any.
	@param repository The repository that contains the resource.
	@param parentResourceURI The URI to of the parent resource.
	@param resourceName The unencoded simple name of the child resource.
	@return The URI of the child resource.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if there is an error accessing the repository.
	@see #getDefaultNameExtension()
	*/
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName) throws ResourceIOException
	{
		//TODO fix IllegalArgumentException by checking to ensure that the parent resource is within the repository
		final StringBuilder stringBuilder=new StringBuilder(URIPath.encodeSegment(resourceName));	//create a new string builder, starting with the encoded simple resource name
		final String defaultExtension=getDefaultNameExtension();	//get the default extension, if any
		if(defaultExtension!=null)	//if we have a default extension
		{
			stringBuilder.append(NAME_EXTENSION_SEPARATOR).append(defaultExtension);	//append the default extension
		}
		return parentResourceURI.resolve(URIPath.createURIPathURI(stringBuilder.toString()));	//resolve the encoded name against the parent resource URI; use the special URIPath method in case the name contains a colon character
	}

	/**Creates a new resource with the appropriate default contents for this resource type.
	If a resource already exists at the given URI it will be replaced.
	If the resource URI is a collection URI, a collection resource will be created.
	This implementation delegates to {@link #createResource(Repository, URI, URFResource)} with a default description.
	@param repository The repository that will contain the resource.
	@param resourceURI The reference URI to use to identify the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given repository, resource URI, and/or resource description is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the resource could not be created.
	*/
	public final URFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		return createResource(repository, resourceURI, new DefaultURFResource(resourceURI));	//create the resource with a default description
	}

	/**Creates a new resource with the given description and the appropriate default contents for this resource type.
	If a resource already exists at the given URI it will be replaced.
	If the resource URI is a collection URI, a collection resource will be created.
	This version creates a default resource or collection resource with the contents provided by {@link #getDefaultResourceContents(Repository, URFResource)}.
	@param repository The repository that will contain the resource.
	@param resourceURI The reference URI to use to identify the resource.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@return A description of the resource that was created.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final Repository repository, final URI resourceURI, final URFResource resourceDescription) throws ResourceIOException
	{
		if(isCollectionURI(resourceURI))	//if this is a collection URI
		{
			return repository.createCollection(resourceURI, resourceDescription);	//create a new collection
		}
		else	//if this is not a collection URI
		{
			return repository.createResource(resourceURI, resourceDescription, getDefaultResourceContents(repository, resourceDescription));	//create a new resource with the default contents
		}
	}

	/**Determines whether the given permission is appropriate for accessing the identified aspect.
	This prevents aspects from being accessed at lower permissions.
	For example, a rogue user may attempt to retrieve a preview-permission aspect such as a high-resolution image
	using a permission such as {@link PermissionType#EXECUTE} when a permission appropriate to the aspect, {@link PermissionType#PREVIEW},
	is not allowed to the user.
	This version recognizes no aspect IDs.
	@param aspectID The serialized form of the ID of the aspect to be accessed.
	@param permissionType The type of permission requested.
	@return <code>true</code> if access to the given aspect is allowed using the given permission, else <code>false</code>.
	@exception NullPointerException if the given aspect ID and/or permission type is <code>null</code>.
	*/
	public boolean isAspectAllowed(final String aspectID, final PermissionType permissionType)
	{
		throw new IllegalArgumentException(checkInstance(aspectID, "Aspect ID cannot be null."));
	}

	/**Returns the appropriate filters for accessing an identified aspect of the resource.
	This version recognizes no aspect IDs.
	@param aspectID The serialized form of the ID of the aspect to be accessed.
	@exception NullPointerException if the given aspect ID is <code>null</code>.
	@exception IllegalArgumentException if the given aspect ID does not represent a valid aspect.
	*/
	public ResourceFilter[] getAspectFilters(final String aspectID)
	{
		throw new IllegalArgumentException(checkInstance(aspectID, "Aspect ID cannot be null."));		
	}
}

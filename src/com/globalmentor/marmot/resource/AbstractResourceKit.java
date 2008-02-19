package com.globalmentor.marmot.resource;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import static java.util.Collections.*;

import javax.mail.internet.ContentType;

import com.garretwilson.net.ResourceIOException;
import com.garretwilson.net.URIPath;
import static com.garretwilson.net.URIs.*;

import static com.globalmentor.java.Enums.*;
import static com.globalmentor.java.Objects.*;

import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;
import com.globalmentor.urf.URFResource;

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
		this(new ContentType[0], capabilities);
	}

	/**Content types and capabilities constructor.
	@param supportedContentType The content type supported by this resource kit.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content type and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType supportedContentType, final Capability... capabilities)
	{
		this(new ContentType[]{checkInstance(supportedContentType, "Supported content type cannot be null.")}, capabilities);
	}

	/**Content types and capabilities constructor.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final Capability... capabilities)
	{
		this(supportedContentTypes, new URI[]{}, capabilities);	//construct the class with no supported resource types
	}

	/**Resource type and capabilities constructor.
	@param supportedResourceType The URI for the resource type this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final URI supportedResourceType, final Capability... capabilities)
	{
		this(new URI[]{checkInstance(supportedResourceType, "Supported resource type cannot be null.")}, capabilities);
	}

	/**Resource types and capabilities constructor.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types array and/or capabilities is <code>null</code>.
	*/
	public AbstractResourceKit(final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this(new ContentType[]{}, supportedResourceTypes, capabilities);	//construct the class with no supported content types
	}

	/**Content types and resource types constructor.
	@param presentation The presentation support for this resource kit.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this.supportedContentTypes=checkInstance(supportedContentTypes, "Supported content types array cannot be null.");
		this.supportedResourceTypes=checkInstance(supportedResourceTypes, "Supported resource types array cannot be null.");
		this.capabilities=unmodifiableSet(createEnumSet(Capability.class, capabilities));
	}

	/**Initializes a resource description, creating whatever properties are appropriate.
	This version does nothing.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception IOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final URFResource resource) throws IOException
	{
	}

	/**Returns the URI of a child resource with the given simple name within a parent resource.
	This is normally the simple name resolved against the parent resource URI, although a resource kit for collections may append an ending path separator.
	The simple name will be encoded before being used to construct the URI.
	This default version resolves the name against the parent resource URI.
	@param repository The repository that contains the resource.
	@param parentResourceURI The URI to of the parent resource.
	@param resourceName The unencoded simple name of the child resource.
	@return The URI of the child resource
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	*/
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName)
	{
		//TODO fix IllegalArgumentException by checking to ensure that the parent resource is within the repository
		return parentResourceURI.resolve(URIPath.createURIPathURI(URIPath.encodeSegment(resourceName)));	//encode the resource name and resolve it against the parent resource URI; use the special URIPath method in case the name contains a colon character
	}

	/**Creates a new resource with the appropriate default contents for this resource type.
	If a resource already exists at the given URI it will be replaced.
	If the resource URI is a collection URI, a collection resource will be created.
	This version creates a default resource or collection resource with content of zero bytes.
	@param repository The repository that will contain the resource.
	@param resourceURI The reference URI to use to identify the resource.
	@return A description of the resource that was created.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the resource could not be created.
	*/
	public URFResource createResource(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		if(isCollectionURI(resourceURI))	//if this is a collection URI
		{
			return repository.createCollection(resourceURI);	//create a new collection
		}
		else	//if this is not a collection URI
		{
			return repository.createResource(resourceURI, new byte[]{});	//create a new empty resource
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

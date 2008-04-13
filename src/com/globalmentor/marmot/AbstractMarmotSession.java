package com.globalmentor.marmot;

import java.net.*;
import java.util.*;
import static java.util.Collections.*;
import java.util.concurrent.*;

import javax.mail.internet.ContentType;

import static com.globalmentor.net.URIs.*;
import static com.globalmentor.urf.content.Content.*;

import com.globalmentor.marmot.repository.*;
import com.globalmentor.marmot.resource.*;
import com.globalmentor.marmot.resource.ResourceKit.Capability;
import com.globalmentor.marmot.security.*;
import com.globalmentor.urf.*;
import com.globalmentor.util.*;

/**A Marmot session with installed resource kits.
@param <RK> The type of resource kits supported by this session.
@author Garret Wilson
*/
public abstract class AbstractMarmotSession<RK extends ResourceKit> implements MarmotSession<RK>
{

	/**The installed Marmot security manager.*/
	private MarmotSecurityManager securityManager=new DefaultMarmotSecurityManager();

		/**@return The installed Marmot security manager.*/
		public MarmotSecurityManager getSecurityManager() {return securityManager;}

	/**The set of resource kits.*/
	private Set<RK> resourceKits=new CopyOnWriteArraySet<RK>();

		/**@return The available resource kits.*/
		public Iterable<RK> getResourceKits() {return unmodifiableSet(resourceKits);} 

	//TODO should we use set maps instead of list maps here?
	
	/**The map of resource kit lists, keyed to supported content type base types.*/
	private CollectionMap<String, RK, List<RK>> contentTypeResourceKitsMap=new CopyOnWriteArrayListConcurrentHashMap<String, RK>();

	/**The map of resource kit lists, keyed to supported resource type URIs.*/
	private CollectionMap<URI, RK, List<RK>> resourceTypeResourceKitsMap=new CopyOnWriteArrayListConcurrentHashMap<URI, RK>();

	/**The thread-safe immutable set of content types supported by all the available resource kits.
	<em>Caution: {@link ContentType} does not correctly support {@link ContentType#equals(Object)}, which means equality in a set will be determined by identity.</em>
	*/
	private Set<ContentType> supportedContentTypes=emptySet();
	
		/**Returns the content types supported by all the available resource kits.
		@return An immutable set of content types supported by all available resource kits.
		@see ResourceKit#getSupportedContentTypes()
		*/
		public Set<ContentType> getSupportedContentTypes() {return supportedContentTypes;}

	/**The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit.*/
	private RK defaultResourceKit=null;

		/**@return The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit.*/
		public RK getDefaultResourceKit() {return defaultResourceKit;}

		/**Sets the default resource kit.
		@param defaultResourceKit The default resource kit if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit.
		*/
		protected void setDefaultResourceKit(final RK defaultResourceKit) {this.defaultResourceKit=defaultResourceKit;}

	/**The default resource kit to use if a specific resource kit cannot be found for a collection, or <code>null</code> if there is no default collection resource kit.*/
	private RK defaultCollectionResourceKit=null;

		/**@return The default resource kit to use if a specific resource kit cannot be found for a collection, or <code>null</code> if there is no default collection resource kit.*/
		public RK getDefaultCollectionResourceKit() {return defaultCollectionResourceKit;}

		/**Sets the default collection resource kit.
		@param defaultResourceKit The default resource kit if a specific resource kit cannot be found for a collection, or <code>null</code> if there is no default resource kit.
		*/
		protected void setDefaultCollectionResourceKit(final RK defaultCollectionResourceKit) {this.defaultCollectionResourceKit=defaultCollectionResourceKit;}

	/**Default constructor.*/
	public AbstractMarmotSession()
	{
	}

	/**Updates the resource kit maps based upon the currently installed resources.
	This method synchronizes on the resource kit set.
	*/
	protected void updateResourceKits()
	{
		synchronized(resourceKits)	//keep the update process from occurring while this thread is updating (the maps are individually thread-safe)
		{
			contentTypeResourceKitsMap.clear();	//clear the maps
			resourceTypeResourceKitsMap.clear();
			final Set<ContentType> supportedContentTypes=new HashSet<ContentType>();	//create a set of content types, even though content types use identity comparison
			for(final RK resourceKit:resourceKits)	//for each resource kit
			{		
				for(final ContentType contentType:resourceKit.getSupportedContentTypes())	//for each content type this resource kit supports
				{
					contentTypeResourceKitsMap.addItem(contentType.getBaseType(), resourceKit);	//add this resource kit to the map			
					supportedContentTypes.add(contentType);	//store the content type in our set
				}
				for(final URI resourceType:resourceKit.getSupportedResourceTypes())	//for each resource type this resource kit supports
				{
					resourceTypeResourceKitsMap.addItem(resourceType, resourceKit);	//add this resource kit to the map			
				}
			}
			this.supportedContentTypes=unmodifiableSet(supportedContentTypes);	//store the updated supported content types set
		}
	}

	/**Registers a resource kit with the session.
	@param resourceKit The resource kit to register.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installResourceKit(final RK resourceKit)
	{
		installResourceKit(resourceKit, false, false);	//install the resource kit, but not as the default resource kit
	}

	/**Registers a resource kit with the session, specifying it as the default resource kit.
	The resource kit will replaced any other resource kit designated as default.
	@param resourceKit The resource kit to register.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installDefaultResourceKit(final RK resourceKit)
	{
		installResourceKit(resourceKit, true, false);	//install the resource kit as the default
	}

	/**Registers a resource kit with the session, specifying it as the default resource kit for collections.
	The resource kit will replaced any other resource kit designated as default for collections.
	@param resourceKit The resource kit to register.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	public void installDefaultCollectionResourceKit(final RK resourceKit)
	{
		installResourceKit(resourceKit, false, true);	//install the resource kit as the default for collections
	}

	/**Registers a resource kit with the session, specifying if the resource kit should be considered the default and/or the default collection resource kit.
	If this resource kit is specified as the default, it will replace any resource kit previously specified as the default.
	@param resourceKit The resource kit to register.
	@param isDefaultResourceKit Whether the resource kit should be the default.
	@param isDefaultCollectionResourceKit Whether the resource kit should be the default for collections.
	@exception IllegalStateException if the resource kit is already installed.
	*/
	protected void installResourceKit(final RK resourceKit, final boolean isDefaultResourceKit, final boolean isDefaultCollectionResourceKit)
	{
		if(resourceKit.getMarmotSession()!=null)	//if the resource kit is already installed
		{
			throw new IllegalStateException("Resource kit already intalled.");
		}
		assert !resourceKits.contains(resourceKit) : "Marmot contains unassigned resource kit.";
		resourceKit.setMarmotSession(this);	//tell the resource kit its owner
		resourceKits.add(resourceKit);	//add the resource kit
		if(isDefaultResourceKit)	//if this resource kit should be the default
		{
			setDefaultResourceKit(resourceKit);	//set the resource kit as the default
		}
		if(isDefaultCollectionResourceKit)	//if this resource kit should be the default for collections
		{
			setDefaultCollectionResourceKit(resourceKit);	//set the resource kit as the default for collections
		}
		updateResourceKits();	//update the resource kits
	}
	
	/**Unregisters a resource kit with the session.
	If this resource kit was previously set as the default, there will no longer be a default resource kit. 
	@param resourceKit The resource kit to unregister.
	@exception IllegalStateException if the resource kit is not installed in this session.
	*/
	public void uninstallResourceKit(final RK resourceKit)
	{
		if(resourceKit.getMarmotSession()!=this)	//if the resource kit is not installed
		{
			throw new IllegalStateException("Resource kit not intalled.");			
		}
		assert resourceKits.contains(resourceKit) : "Marmot does not contain assigned resource kit.";
		if(getDefaultResourceKit()==resourceKit)	//if this is our default resource kit
		{
			setDefaultResourceKit(null);	//show that we have no default resource kit
		}
		if(getDefaultCollectionResourceKit()==resourceKit)	//if this is our default resource kit for collections
		{
			setDefaultCollectionResourceKit(null);	//show that we have no default collection resource kit
		}
		resourceKits.remove(resourceKit);	//remove the resource kit
		resourceKit.setMarmotSession(null);	//tell the resource kit it has no owner
		updateResourceKits();	//update the resource kits		
	}

	/**Determines if there exists  resource kit appropriate for the given resource supporting the given capabilities.
	@param repository The repository in which the resource resides.
	@param resource The resource for which a resource kit should be returned.
	@param capabilities The capabilities required for the resource kit.
	@return <code>true</code> if there exists a resource kit to handle the given resource with the given capabilities, if any, in relation to the resource.
	@see #getResourceKit(Repository, URFResource, Capability...)
	*/
	public boolean hasResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities)
	{
		return getResourceKit(repository, resource, capabilities)!=null;	//determine if getting a resource kit would result in a resource kit
	}

	/**Retrieves a resource kit appropriate for the given resource.
	This method locates a resource kit in the following priority:
	<ol>
		<li>The first resource kit supporting the resource content type.</li>
		<li>The first resource kit supporting one of the resource types.</li>
		<li>If the resource has a collection URI, the default collection resource kit.</li>
		<li>The default resource kit.</li>
	</ol>
	@param repository The repository in which the resource resides.
	@param resource The resource for which a resource kit should be returned.
	@param capabilities The capabilities required for the resource kit.
	@return A resource kit to handle the given resource with the given capabilities, if any, in relation to the resource;
		or <code>null</code> if there is no registered resource kit with the given capabilities in relation to the resource.
	*/
	public RK getResourceKit(final Repository repository, final URFResource resource, final Capability... capabilities)
	{
		RK resourceKit=null;
			//step 1: try to match a resource kit by content type
		final ContentType contentType=getContentType(resource); //get the content type of the resource
		if(contentType!=null)	//if we know the content type of the resource
		{
			resourceKit=getResourceKit(contentType, capabilities);	//see if we have a resource kit registered for this media type and capabilities
		}
			//step 2: try to match a resource kit by resource type
		if(resourceKit==null)	//if we haven't yet found a resource kit, try to match a resource by resource type
		{
			final Iterator<URFResource> typeIterator=resource.getTypes().iterator();	//get an iterator to all the types of this resource
			while(resourceKit==null && typeIterator.hasNext())	//while there are more types and we haven't yet found a resource kit
			{
				final URI typeURI=typeIterator.next().getURI();	//get the URI of the next type
				if(typeURI!=null)	//if there is a type URI
				{
					resourceKit=getResourceKit(typeURI, capabilities);	//see if we have a resource kit registered for this resource type URI and capabilities
				}
			}
		}
			//step 3: ask each resource kit individually if it supports this resource
/*TODO fix or del if not needed
		if(resourceKit==null)	//if we haven't yet found a resource kit, ask each resource kit individually
		{
			final Iterator resourceKitIterator=getRegisteredResourceKitIterator();	//get an iterator to the resource kits
			while(resourceKitIterator.hasNext())	//while there are more resource kits
			{
				final ResourceKit currentResourceKit=(ResourceKit)resourceKitIterator.next();	//get the next resource kit
				if(currentResourceKit.supports(resource))	//if this resource kit supports the resource
				{
					resourceKit=currentResourceKit;	//use this resource kit
					break;	//stop looking for a resource kit
				}
			}			
		}
*/
		if(resourceKit==null)	//if we have exhausted all attempts to get a matching resource kit
		{
			final URI resourceURI=resource.getURI();	//get the URI of the resource
			if(resourceURI!=null && isCollectionURI(resourceURI))	//if this is a collection URI
			{
				final RK defaultCollectionResourceKit=getDefaultCollectionResourceKit();	//use the default collection resource kit, if there is one
				if(defaultCollectionResourceKit!=null && defaultCollectionResourceKit.hasCapabilities(capabilities))	//if there is a default collection resource kit that has the requested capabilities
				{
					resourceKit=defaultCollectionResourceKit;	//use the default collection resource kit
				}
			}
			if(resourceKit==null)	//if we didn't find an appropriate default collection resource kit
			{
				final RK defaultResourceKit=getDefaultResourceKit();	//use the default resource kit, if there is one
				if(defaultResourceKit!=null && defaultResourceKit.hasCapabilities(capabilities))	//if there is a default resource kit that has the requested capabilities
				{
					resourceKit=defaultResourceKit;	//use the default resource kit
				}
			}
		}
		return resourceKit;	//return whatever resource kit we found, if any
	}
	
	/**Retrieves a resource kit appropriate for the given resource information.
	@param resourceURI The reference URI used to identify the resource.
	@param typeURI The URI of the resource type, or <code>null</code> if no
		specific type is known.
	@param mediaType The type of content the resource contains, or
		<code>null</code> if no MIME content type is known.
	@return A resource kit to handle the given resource, or <code>null</code>
		if no appropriate resource kit is registered.
	*/
/*TODO fix
	public RK getResourceKit(final URI typeURI, final ContentType mediaType)
	{	
		RK resourceKit=null;	//start by assuming we won't find a resource kit
			//step 1: try to match a resource kit by media type
		resourceKit=getResourceKit(mediaType);	//see if we have a resource kit registered for this media type
			//step 2: try to match a resource kit by resource type
		if(resourceKit==null)	//if we haven't yet found a resource kit, try to match a resource by resource type
		{
			resourceKit=getResourceKit(typeURI);	//see if we have a resource kit registered for this resource type URI
		}
		return resourceKit;	//return whatever resource kit we found, if any
	}
*/

	/**Retrieves a resource kit appropriate for the given resource based upon its type URI.
	@param typeURI The URI of the resource type.
	@param capabilities The capabilities required for the resource kit.
	@return A resource kit to handle the given resource type, or <code>null</code> if no appropriate resource kit is registered.
	*/
	protected RK getResourceKit(final URI typeURI, final Capability... capabilities)
	{	
		final RK resourceKit=resourceTypeResourceKitsMap.getItem(typeURI);	//get the resource kit, if any, registered for this resource type URI
		return resourceKit!=null && resourceKit.hasCapabilities(capabilities) ? resourceKit : null;	//return the resource kit if it has the given capabilities
	}

	/**Retrieves a resource kit appropriate for a MIME content type.
	This method should only be used for special-purpose functionality;
	when accessing resources {@link #getResourceKit(Repository, URFResource, Capability...)} should normally be used instead.
	@param contentType The type of content the resource contains.
	@param capabilities The capabilities required for the resource kit.
	@return A resource kit with the requested capabilities to handle the given content type, or <code>null</code> if no appropriate resource kit is registered.
	*/
	public RK getResourceKit(final ContentType contentType, final Capability... capabilities)
	{
		final RK resourceKit=contentTypeResourceKitsMap.getItem(contentType.getBaseType());	//get the resource kit, if any, registered for this content type
		return resourceKit!=null && resourceKit.hasCapabilities(capabilities) ? resourceKit : null;	//return the resource kit if it has the given capabilities
	}

}

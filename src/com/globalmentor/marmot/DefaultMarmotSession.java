package com.globalmentor.marmot;

import java.net.*;
import java.util.*;
import static java.util.Collections.*;
import java.util.concurrent.*;

import javax.mail.internet.ContentType;
import com.garretwilson.rdf.*;
import com.garretwilson.rdf.xpackage.*;
import com.garretwilson.util.CollectionMap;
import com.garretwilson.util.CopyOnWriteArrayListConcurrentHashMap;
import com.garretwilson.util.CollectionMap;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.ResourceKit;

/**A Marmot session with installed resource kits.
@author Garret Wilson
*/
public class DefaultMarmotSession implements MarmotSession
{

	/**The set of resource kits.*/
	private Set<ResourceKit> resourceKits=new CopyOnWriteArraySet<ResourceKit>();
	
	
	//TODO should we use set maps instead of list maps here?
	
	/**The map of resource kit lists, keyed to supported content type base types.*/
	private CollectionMap<String, ResourceKit, List<ResourceKit>> contentTypeResourceKitsMap=new CopyOnWriteArrayListConcurrentHashMap<String, ResourceKit>();

	/**The map of resource kit lists, keyed to supported resource type URIs.*/
	private CollectionMap<URI, ResourceKit, List<ResourceKit>> resourceTypeResourceKitsMap=new CopyOnWriteArrayListConcurrentHashMap<URI, ResourceKit>();

	/**The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit..*/
	private final ResourceKit defaultResourceKit;

		/**@return The default resource kit to use if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit..*/
		protected ResourceKit getDefaultResourceKit() {return defaultResourceKit;}

	/**Default constructor.*/
	public DefaultMarmotSession()
	{
		this(null);	//construct the resource kit manager with no default resource kit
	}

	/**Default resource kit constructor.
	@param defaultResourceKit The default resource kit if a specific resource kit cannot be found, or <code>null</code> if there is no default resource kit.
	*/
	public DefaultMarmotSession(final ResourceKit defaultResourceKit)
	{
		this.defaultResourceKit=defaultResourceKit;	//save the default resource kit
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
			for(final ResourceKit resourceKit:resourceKits)	//for each resource kit
			{		
				for(final ContentType contentType:resourceKit.getSupportedContentTypes())	//for each content type this resource kit supports
				{
					contentTypeResourceKitsMap.addItem(contentType.getBaseType(), resourceKit);	//add this resource kit to the map			
				}
				for(final URI resourceType:resourceKit.getSupportedResourceTypes())	//for each resource type this resource kit supports
				{
					resourceTypeResourceKitsMap.addItem(resourceType, resourceKit);	//add this resource kit to the map			
				}
			}
		}
	}

	/**Registers a resource kit with the session. If the resource kit is already registered, no action occurs.
	@param resourceKit The resource kit to register.
	*/
	public void registerResourceKit(final ResourceKit resourceKit)
	{
		if(resourceKits.add(resourceKit))	//add the resource kit; if it was not already in the set
		{
			updateResourceKits();	//update the resource kits
		}
	}

	/**Unregisters a resource kit with the session. If the resource kit is not registered, no action is taken.
	@param resourceKit The resource kit to unregister.
	*/
	public void unregisterResourceKIt(final ResourceKit resourceKit)
	{
		if(resourceKits.remove(resourceKit))	//remove the resource kit from the set; if the resource kit was in the set
		{ 
			updateResourceKits();	//update the resource kits
		}
	}

	/**@return Access to the registered resource kits.*/
/*TODO del if not needed
	public Iterable<ResourceKit> getResourceKits()
	{
		return unmodifiableSet(resourceKits);	//return a read-only set of registered resource kits
	}
*/

	/**Returns an iterator to all resource kits that have the specified capabiliites. 
	@param capabilities The requisite capabilities, one or more
		<code>ResourceKit.XXX_CAPABILITY</code> constants logically ORed together.
	@return A read-only iterator of the registered resource kits with the
		specified capabilities.
	*/
/*TODO fix
	public Iterator<RK> getCapableResourceKitIterator(final long capabilities)
	{
		final List<RK> capableResourceKitList=new ArrayList<RK>(resourceKitSet.size());	//create a list that can hold the maximum number of resources we would need
		for(RK resourceKit:resourceKitSet)	//look at all the resource kits
		{
			if(resourceKit.isCapable(capabilities))	//if this resource kit has the requisite capabilities
				capableResourceKitList.add(resourceKit);	//add this resource kit to our list of capable resource kits	
		}			
		return Collections.unmodifiableList(capableResourceKitList).iterator();	//get an iterator to the capable resource kits
	}
*/

	/**Retrieves a resource kit with the requested capabilities appropriate for
		the given resource.
	@param burrow The burrow from which the resource would be retrieved.
	@param resource The resource for which a resource kit should be returned.
	@param capabilities The requisite capabilities, one or more
		<code>ResourceKit.XXX_CAPABILITY</code> constants logically ORed together.
	@return A resource kit to handle the given resource, or <code>null</code>
		if no appropriate resource kit is registered.
	*/
/*TODO fix
	public RK getResourceKit(final OldBurrow burrow, final RDFResource resource, final long capabilities)	//TODO search for all relevant resource kits and use the capable one; move code from getResourceKit(Burrow, RDFResource) to here
	{
		final RK resourceKit=getResourceKit(burrow, resource);	//get a resource kit registered for the given resource
			//return the resource kit if there is one and it has the requested capabilities for this resource
		return resourceKit!=null && resourceKit.isCapable(capabilities, burrow, resource) ? resourceKit : null;
	}
*/

	/**Retrieves a resource kit appropriate for the given resource.
	This method locates a resource kit in the following priority:
	<ol>
		<li>The first resource kit supporting the resource content type.</li>
		<li>The first resource kit supporting one of the resource types.</li>
		<li>The default resource kit.</li>
	</ol>
	@param repository The repository in which the resource resides.
	@param resource The resource for which a resource kit should be returned.
	@return A resource kit to handle the given resource.
	*/
	public ResourceKit getResourceKit(final Repository repository, final RDFResource resource)
	{
		ResourceKit resourceKit=null;
			//step 1: try to match a resource kit by content type
		final ContentType contentType=MIMEOntologyUtilities.getMediaType(resource); //get the content type of the resource
		if(contentType!=null)	//if we know the content type of the resource
		{
			resourceKit=getResourceKit(contentType);	//see if we have a resource kit registered for this media type
				//step 2: try to match a resource kit by resource type
			if(resourceKit==null)	//if we haven't yet found a resource kit, try to match a resource by resource type
			{
				final Iterator<RDFObject> typeIterator=RDFUtilities.getTypes(resource).iterator();	//get an iterator to all the types of this resource
				while(resourceKit==null && typeIterator.hasNext())	//while there are more types and we haven't yet found a resource kit
				{
					final RDFObject rdfObject=typeIterator.next();	//get the next type
					if(rdfObject instanceof RDFResource)	//if this is a type resource, as we expect
					{
						final RDFResource typeResource=(RDFResource)rdfObject;	//cast the object to an RDF resource
						resourceKit=getResourceKit(typeResource.getReferenceURI());	//see if we have a resource kit registered for this resource type URI
					}
				}
			}
				//step 3: ask each resource kit individually if it supports this resource
	/*G***fix or del if not needed
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
		}
		if(resourceKit==null)	//if we have exhausted all attempts to get a matching resource kit
		{
			resourceKit=getDefaultResourceKit();	//use the default resource kit, if there is one
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
	@return A resource kit to handle the given resource type, or <code>null</code> if no appropriate resource kit is registered.
	*/
	protected ResourceKit getResourceKit(final URI typeURI)
	{	
		return resourceTypeResourceKitsMap.getItem(typeURI);	//see if we have a resource kit registered for this resource type URI
	}

	/**Retrieves a resource kit appropriate for the given resource based upon its MIME content type.
	@param contentType The type of content the resource contains.
	@return A resource kit to handle the given content type, or <code>null</code> if no appropriate resource kit is registered.
	*/
	protected ResourceKit getResourceKit(final ContentType contentType)
	{
		return contentTypeResourceKitsMap.getItem(contentType.getBaseType());	//see if we have a resource kit registered for this content type
	}

}

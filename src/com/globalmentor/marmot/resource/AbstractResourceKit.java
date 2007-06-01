package com.globalmentor.marmot.resource;

import java.io.IOException;
import java.net.URI;

import javax.mail.internet.ContentType;

import static com.garretwilson.lang.ObjectUtilities.*;

import com.garretwilson.lang.ObjectUtilities;
import com.garretwilson.rdf.RDFResource;

import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.security.PermissionType;

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
	private URI[] supportedResourceTypes;
	
		/**Returns the resource types supported.
		This is the secondary method of determining which resource kit to use for a given resource.
		@return A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
		*/
		public URI[] getSupportedResourceTypes() {return supportedResourceTypes;}

		/**The map of installed filters, keyed to filter IDs.*/
//TODO del		private final Map<String, ResourceFilter> filterMap=new ConcurrentHashMap<String, ResourceFilter>();

		/**Returns this resource kit's installed filter based upon its ID.
		@param filterID The ID of the filter to return.
		@return The resource filter identified by the given ID.
		@exception IllegalArgumentException if there is no installed resource filter identified by the given ID.
		*/
/*TODO del
		public ResourceFilter getFilter(final String filterID) throws IllegalArgumentException
		{
			final ResourceFilter resourceFilter=filterMap.get(filterID);	//get the filter, if any, keyed to the given ID
			if(resourceFilter==null)	//if no such filter is installed
			{
				throw new IllegalArgumentException("No such filter installed: "+filterID);
			}
			return resourceFilter;	//return the filter we found
		}
*/

		/**Installs a filter into the resource kit.
		Any filter installed with the same ID will be removed.
		@param filterID The ID to use in locating the filter.
		@param filter The filter to install.
		*/
/*TODO del
		protected void installFilter(final String filterID, final ResourceFilter filter)
		{
			filterMap.put(filterID, filter);	//store the filter in the map
		}
*/

	/**Content types constructor.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@exception NullPointerException if the supported content types array is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType... supportedContentTypes)
	{
		this(supportedContentTypes, new URI[]{});	//construct the class with no supported resource types
	}

	/**Resource types constructor.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@exception NullPointerException if the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final URI... supportedResourceTypes)
	{
		this(new ContentType[]{}, supportedResourceTypes);	//construct the class with no supported content types
	}

	/**Content types and resource types constructor.
	@param presentation The presentation support for this resource kit.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@exception NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes)
	{
		this.supportedContentTypes=checkInstance(supportedContentTypes, "Supported content types array cannot be null.");
		this.supportedResourceTypes=checkInstance(supportedResourceTypes, "Supported resource types array cannot be null.");		
	}
	
	/**Initializes a resource description, creating whatever properties are appropriate.
	This version does nothing.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception IOException if there is an error accessing the repository.
	*/
	public void initializeResourceDescription(final Repository repository, final RDFResource resource) throws IOException
	{
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

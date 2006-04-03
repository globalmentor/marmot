package com.globalmentor.marmot.resource;

import java.io.IOException;
import java.net.URI;

import javax.mail.internet.ContentType;

import static com.garretwilson.lang.ObjectUtilities.*;
import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;

/**Abstract implementation of a resource kit.
@author Garret Wilson
*/
public abstract class AbstractResourceKit implements ResourceKit
{

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
	
	/**Returns the URI of an open icon representing the given resource.
	This version delegates to {@link #getLeafTreeNodeIconURI(Repository, RDFResource)}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for an open tree node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getOpenTreeNodeIconURI(final Repository repository, final RDFResource resource)
	{
		return getLeafTreeNodeIconURI(repository, resource);
	}

	/**Returns the resource key of an open icon representing the given resource.
	This version delegates to {@link #getLeafTreeNodeIconResourceKey(Repository, RDFResource)}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of an icon to display for an open tree node for this resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getOpenTreeNodeIconResourceKey(final Repository repository, final RDFResource resource)
	{
		return getLeafTreeNodeIconResourceKey(repository, resource);
	}

	/**Returns the URI of a closed icon representing the given resource.
	This version delegates to {@link #getLeafTreeNodeIconURI(Repository, RDFResource)}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for a closed tree node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getClosedTreeNodeIconURI(final Repository repository, final RDFResource resource)
	{
		return getLeafTreeNodeIconURI(repository, resource);
	}

	/**Returns the resource key of a closed icon representing the given resource.
	This version delegates to {@link #getLeafTreeNodeIconResourceKey(Repository, RDFResource)}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of an icon to display for a closed tree node for this resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getClosedTreeNodeIconResourceKey(final Repository repository, final RDFResource resource)
	{
		return getLeafTreeNodeIconResourceKey(repository, resource);
	}

	/**Returns the URI of a leaf icon representing the given resource.
	This version delegates to {@link #getIconURI(Repository, RDFResource)}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of an icon to display for a leaf node for this resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getLeafTreeNodeIconURI(final Repository repository, final RDFResource resource)
	{
		return getIconURI(repository, resource);
	}

	/**Returns the resource key of a leaf icon representing the given resource.
	This version delegates to {@link #getIconResourceKey(Repository, RDFResource)}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of an icon to display for a leaf node for this resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getLeafTreeNodeIconResourceKey(final Repository repository, final RDFResource resource)
	{
		return getIconResourceKey(repository, resource);
	}

	/**Returns the URI of a general icon representing the given resource.
	This version delegates to {@link #getIconURI()}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The URI of a general icon representing the given resource, or <code>null</code> if no icon URI is available.
	*/
	public URI getIconURI(final Repository repository, final RDFResource resource)
	{
		return getIconURI();
	}

	/**Returns the resource key of a general icon representing the given resource.
	This version delegates to {@link #getIconResourceKey()}.
	@param repository The repository in which the resource resides.
	@param resource The resource for which an icon should be returned.
	@return The resource key of a general icon representing the given resource, or <code>null</code> if no icon resource key is available.
	*/
	public String getIconResourceKey(final Repository repository, final RDFResource resource)
	{
		return getIconResourceKey();
	}

	/**The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.*/
	private final URI iconURI;
	
	/**Returns the URI of a general icon representing this resource kit.
	@return The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	*/
	public URI getIconURI() {return iconURI;}

	/**The resource key of a general icon representing this resource kit, or <code>null</code> if no icon resource key is available.*/
	private final String iconResourceKey;

	/**Returns the resource key of a general icon representing this resource kit.
	@return The resource key of a general icon representing this resource kit, or <code>null</code> if no icon resource key is available.
	*/
	public String getIconResourceKey() {return iconResourceKey;}

	/**Icon and content types constructor.
	Either an icon URI or an icon resource key must be provided.
	@param iconURI The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	@param iconResourceKey The resource key of a general icon representing this resource kit, or <code>null</code> if no icon resource key is available.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@exception NullPointerException if both the icon URI and the icon resource key is <code>null</code>, and/or if the supported content types array is <code>null</code>.
	*/
	public AbstractResourceKit(final URI iconURI, final String iconResourceKey, final ContentType... supportedContentTypes)
	{
		this(iconURI, iconResourceKey, supportedContentTypes, new URI[]{});	//construct the class with no supported resource types
	}

	/**Icon and resource types constructor.
	Either an icon URI or an icon resource key must be provided.
	@param iconURI The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	@param iconResourceKey The resource key of a general icon representing this resource kit, or <code>null</code> if no icon resource key is available.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@exception NullPointerException if both the icon URI and the icon resource key is <code>null</code>, and/or if the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final URI iconURI, final String iconResourceKey, final URI... supportedResourceTypes)
	{
		this(iconURI, iconResourceKey, new ContentType[]{}, supportedResourceTypes);	//construct the class with no supported content types
	}

	/**Icon, content types, and resource types constructor.
	Either an icon URI or an icon resource key must be provided.
	@param iconURI The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	@param iconResourceKey The resource key of a general icon representing this resource kit, or <code>null</code> if no icon resource key is available.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@exception NullPointerException if both the icon URI and the icon resource key is <code>null</code>, and/or if the supported content types array and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final URI iconURI, final String iconResourceKey, final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes)
	{
		if(iconURI==null && iconResourceKey==null)	//if no icon URI or resource key was given
		{
			throw new NullPointerException("Either an icon URI or an icon resource key must be given.");
		}
		this.iconURI=iconURI;
		this.iconResourceKey=iconResourceKey;
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

}

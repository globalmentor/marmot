package com.globalmentor.marmot.resource;

import java.io.IOException;
import java.net.URI;

import javax.mail.internet.ContentType;

import static com.garretwilson.lang.ObjectUtilities.*;
import com.garretwilson.rdf.RDFResource;
import com.globalmentor.marmot.repository.Repository;

/**Abstract implementation of a resource kit.
@param <P> The type of presentation supported by this resource kit.
@author Garret Wilson
*/
public abstract class AbstractResourceKit<P extends Presentation> implements ResourceKit<P>
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

	/**The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.*/
	private final URI iconURI;
	
	/**Returns the URI of a general icon representing this resource kit.
	@return The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	*/
	public URI getIconURI() {return iconURI;}

	/**Presentation, icon, and content types constructor.
	@param presentation The presentation support for this resource kit.
	@param iconURI The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@exception NullPointerException if the presentation, icon URI and/or the supported content types array is <code>null</code>.
	*/
	public AbstractResourceKit(final P presentation, final URI iconURI, final ContentType... supportedContentTypes)
	{
		this(presentation, iconURI, supportedContentTypes, new URI[]{});	//construct the class with no supported resource types
	}

	/**Presentation, icon, and resource types constructor.
	@param presentation The presentation support for this resource kit.
	@param iconURI The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@exception NullPointerException if the presentation, icon URI and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final P presentation, final URI iconURI, final URI... supportedResourceTypes)
	{
		this(presentation, iconURI, new ContentType[]{}, supportedResourceTypes);	//construct the class with no supported content types
	}

	/**Presentation, icon, content types, and resource types constructor.
	@param presentation The presentation support for this resource kit.
	@param iconURI The URI of a general icon representing this resource kit, or <code>null</code> if no icon URI is available.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@exception NullPointerException if the presentation, icon URI, the supported content types array, and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractResourceKit(final P presentation, final URI iconURI, final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes)
	{
		this.presentation=checkInstance(presentation, "Presentation cannot be null.");
		this.iconURI=checkInstance(iconURI, "Icon URI cannot be null.");
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

	/**The presentation implementation for supported resources.*/
	private final P presentation;
	
		/**@return The presentation implementation for supported resources.*/
		public P getPresentation() {throw new UnsupportedOperationException("Presentation not yet supported.");}

}

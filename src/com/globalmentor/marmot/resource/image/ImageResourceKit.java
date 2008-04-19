package com.globalmentor.marmot.resource.image;

import javax.mail.internet.ContentType;

import net.marmox.resource.image.ImageScaleFilter;

import static com.globalmentor.io.ContentTypeConstants.*;
import static com.globalmentor.java.Enums.*;
import static com.globalmentor.java.Objects.*;

import static com.globalmentor.io.ContentTypes.*;
import com.globalmentor.marmot.resource.*;
import com.globalmentor.marmot.security.PermissionType;

/**Resource kit for handling images.
<p>Supported media types:</p>
<ul>
	<li><code>image/gif</code></li>
	<li><code>image/jpeg</code></li>
	<li><code>image/png</code></li>
</ul>
@author Garret Wilson
*/
public class ImageResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public ImageResourceKit()
	{
		super(new ContentType[]{getContentTypeInstance(IMAGE_PRIMARY_TYPE, GIF_SUBTYPE), getContentTypeInstance(IMAGE_PRIMARY_TYPE, JPEG_SUBTYPE), getContentTypeInstance(IMAGE_PRIMARY_TYPE, PNG_SUBTYPE)});
	}

	/**Initializes a resource description, creating whatever properties are appropriate.
	This version adds appropriate XPackage icon properties.
	@param repository The repository to use to access the resource content, if needed.
	@param resource The resource description to initialize.
	@exception IOException if there is an error accessing the repository.
	*/
/*TODO fix
	public void initializeResourceDescription(final Repository repository, final RDFResource resource) throws IOException
	{
		super.initializeResourceDescription(repository, resource);	//do the default initialization
		if(getIcon(resource)==null)	//if this resource has no icon defined
		{
//TODO fix			setIcon(resource, iconURI);
		}
	}
*/

	/**Determines whether the given permission is appropriate for accessing the identified aspect.
	This prevents aspects from being accessed at lower permissions.
	For example, a rogue user may attempt to retrieve a preview-permission aspect such as a high-resolution image
	using a permission such as {@link PermissionType#EXECUTE} when a permission appropriate to the aspect, {@link PermissionType#PREVIEW},
	is not allowed to the user.
	@param aspectID The serialized form of the ID of the aspect to be accessed.
	@param permissionType The type of permission requested.
	@return <code>true</code> if access to the given aspect is allowed using the given permission, else <code>false</code>.
	@exception IllegalArgumentException if the given aspect ID does not represent a valid aspect.
	@exception NullPointerException if the given aspect ID and/or permission type is <code>null</code>.
	*/
	public boolean isAspectAllowed(final String aspectID, final PermissionType permissionType)
	{
		final ImageAspect imageAspect=ImageAspect.valueOf(aspectID);	//determine the image aspect from the ID to make sure it is non-null and valid
		return checkInstance(permissionType, "Permission type cannot be null")==PermissionType.PREVIEW;	//all image aspects can only be used in preview mode
}

	/**Returns the appropriate filters for accessing an identified aspect of the resource.
	@param aspectID The serialized form of the ID of the aspect to be accessed.
	@exception NullPointerException if the given aspect ID is <code>null</code>.
	@exception IllegalArgumentException if the given aspect ID does not represent a valid aspect.
	*/
	public ResourceContentFilter[] getAspectFilters(final String aspectID) throws IllegalArgumentException
	{
		final ImageAspect imageAspect=ImageAspect.valueOf(aspectID);	//determine the image aspect from the ID
		return new ResourceContentFilter[]{new ImageScaleFilter(imageAspect)};	//return the correct image aspect filter
	}

}

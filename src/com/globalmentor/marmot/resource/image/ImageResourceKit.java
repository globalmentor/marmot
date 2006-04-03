package com.globalmentor.marmot.resource.image;

import static com.garretwilson.io.ContentTypeConstants.*;
import static com.garretwilson.io.FileConstants.*;

import java.io.IOException;
import java.net.URI;

import javax.mail.internet.ContentType;

import com.garretwilson.rdf.RDFResource;
import static com.garretwilson.rdf.xpackage.XPackageUtilities.*;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.AbstractResourceKit;

import static com.guiseframework.GuiseResourceConstants.*;

public class ImageResourceKit extends AbstractResourceKit
{
	/**The default file extension.*/
//TODO del if not needed	protected final static String DEFAULT_EXTENSION=JPG_EXTENSION;

	/**The supported file extensions.*/
//TODO del if not needed	protected final static String[] SUPPORTED_EXTENSIONS=new String[] {JPG_EXTENSION, JPEG_EXTENSION};

	/**The "image/jpeg" content type.*/
//TODO del if not needed	protected final static ContentType IMAGE_JPEG_CONTENT_TYPE=new ContentType(IMAGE, JPEG_SUBTYPE, null);

	/**The image content type array.*/
//TODO del if not needed	protected final static ContentType[] SUPPORTED_CONTENT_TYPES=new ContentType[] {IMAGE_JPEG_CONTENT_TYPE};

	/**Returns the default file extensions used for the resource URI.
	@return The default file extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	*/
/*TODO del if not needed
	public String getDefaultExtension()
	{
		return DEFAULT_EXTENSION;	//return the default extension		
	}
*/

	/**Returns the file extensions supported for the resource URI.
	@return A non-<code>null</code> array of the extensions this resource kit supports.
	*/
/*TODO del if not needed
	public String[] getSupportedExtensions()
	{
		return SUPPORTED_EXTENSIONS;	//return our array of supported extensions		
	}
*/

	/**Returns the content types supported.
	This is the primary method of determining which resource kit to use for a given resource.
	@return A non-<code>null</code> array of the content types this resource kit supports.
	*/
/*TODO del if not needed
	public ContentType[] getSupportedContentTypes()
	{
		return SUPPORTED_CONTENT_TYPES;	//return our array of supported content types 		
	}
*/

	/**Returns the resource types supported.
	This is the secondary method of determining which resource kit to use for a given resource.
	@return A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	*/
/*TODO del if not needed
	public URI[] getSupportedResourceTypes()
	{
		return NO_RESOURCE_TYPES;	//return an empty array of URIs		
	}
*/

	/**Default constructor.*/
	public ImageResourceKit()
	{
		super(null, IMAGE_ICON_RESOURCE_KEY, new ContentType(IMAGE, JPEG_SUBTYPE, null));
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
}

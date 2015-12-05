/*
 * Copyright © 1996-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.resource.image;

import static com.globalmentor.net.ContentTypeConstants.*;

import com.globalmentor.marmot.resource.*;
import com.globalmentor.marmot.security.PermissionType;
import com.globalmentor.net.ContentType;

/**
 * Resource kit for handling images.
 * <p>
 * Supported media types:
 * </p>
 * <ul>
 * <li><code>image/gif</code></li>
 * <li><code>image/jpeg</code></li>
 * <li><code>image/png</code></li>
 * </ul>
 * @author Garret Wilson
 */
public class ImageResourceKit extends AbstractResourceKit {

	/** Default constructor. */
	public ImageResourceKit() {
		super(new ContentType[] { ContentType.create(ContentType.IMAGE_PRIMARY_TYPE, GIF_SUBTYPE),
				ContentType.create(ContentType.IMAGE_PRIMARY_TYPE, JPEG_SUBTYPE), ContentType.create(ContentType.IMAGE_PRIMARY_TYPE, PNG_SUBTYPE) });
	}

	/**
	 * Initializes a resource description, creating whatever properties are appropriate. This version adds appropriate XPackage icon properties.
	 * @param repository The repository to use to access the resource content, if needed.
	 * @param resource The resource description to initialize.
	 * @throws IOException if there is an error accessing the repository.
	 */
	/*TODO fix
		public void initializeResourceDescription(final Repository repository, final RDFResource resource) throws IOException
		{
			super.initializeResourceDescription(repository, resource);	//do the default initialization
			if(getIcon(resource)==null) {	//if this resource has no icon defined
	//TODO fix			setIcon(resource, iconURI);
			}
		}
	*/

	@Override
	public Class<? extends ResourceAspect> getAspectType() {
		return ImageAspect.class;
	};

	@Override
	public boolean isAspectAllowed(final ResourceAspect aspect, final PermissionType permissionType) {
		switch((ImageAspect)aspect) {
			case PREVIEW:
				return permissionType == PermissionType.PREVIEW || permissionType == PermissionType.READ;
			case FIGURE:
				return permissionType == PermissionType.PREVIEW || permissionType == PermissionType.READ || permissionType == PermissionType.UTILIZE;
			case THUMBNAIL:
				return permissionType == PermissionType.PREVIEW || permissionType == PermissionType.READ || permissionType == PermissionType.UTILIZE;
			default:
				throw new AssertionError("Unrecognized image aspect: " + aspect);
		}
	}

	@Override
	public ResourceContentFilter[] getAspectFilters(final ResourceAspect aspect) {
		return new ResourceContentFilter[] { new ImageScaleFilter((ImageAspect)aspect) }; //return the correct image aspect filter
	}
}

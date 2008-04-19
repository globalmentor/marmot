package com.globalmentor.marmot.resource.image;

import com.globalmentor.marmot.resource.ResourceAspect;

/**Available images aspects.
@author Garret Wilson
*/
public enum ImageAspect implements ResourceAspect
{
	/**Dimensions appropriate for presenting as a preview.*/
	preview,

	/**Dimensions appropriate for presenting as a thumbnail.*/
	thumbnail;
}
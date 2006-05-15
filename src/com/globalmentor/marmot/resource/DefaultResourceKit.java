package com.globalmentor.marmot.resource;

import javax.mail.internet.ContentType;

import com.globalmentor.marmot.resource.AbstractResourceKit;

import static com.guiseframework.theme.Theme.*;

/**A default resource kit which can be used for most resources.
@author Garret Wilson
*/
public class DefaultResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public DefaultResourceKit()
	{
		super(ICON_RESOURCE, new ContentType[]{});
	}

}

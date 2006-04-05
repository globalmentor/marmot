package com.globalmentor.marmot.resource;

import java.net.URI;

import javax.mail.internet.ContentType;

import com.globalmentor.marmot.resource.AbstractResourceKit;

/**A default resource kit which can be used for most resources.
@author Garret Wilson
*/
public class DefaultResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public DefaultResourceKit()
	{
		super(URI.create("guise/images/icons/resource.gif"), new ContentType[]{});	//TODO use a constant
	}

}

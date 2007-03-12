package com.globalmentor.marmot.resource;

import javax.mail.internet.ContentType;

import com.globalmentor.marmot.resource.AbstractResourceKit;

import static com.guiseframework.theme.Theme.*;

/**A default resource kit which can be used for most resources.
@param <P> The type of presentation supported by this resource kit.
@author Garret Wilson
*/
public class DefaultResourceKit<P extends Presentation> extends AbstractResourceKit<P>
{

	/**Presentation constructor.
	@param presentation The presentation implementation for supported resources.
	@exception NullPointerException if the given presentation is <code>null</code>.
	*/
	public DefaultResourceKit(final P presentation)
	{
		super(presentation, ICON_RESOURCE, new ContentType[]{});
	}

}

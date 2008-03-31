package com.globalmentor.marmot.resource.xhtml;

import static com.globalmentor.io.Files.*;

import com.globalmentor.text.xml.xhtml.XHTML;
import static com.globalmentor.text.xml.xhtml.XHTML.*;

/**Resource kit for handling XHTML resources.
<p>Supported media types:</p>
<ul>
	<li>{@value XHTML#XHTML_NAMESPACE_URI}</li>
</ul>
<p>Default extension:</p>
<ul>
	<li>{@value XHTML#XHTML_NAME_EXTENSION}</li>
</ul>
@author Garret Wilson
*/
public class XHTMLResourceKit extends AbstractXHTMLResourceKit
{

	/**The default simple name (i.e. the name without an extension) of an XHTML template.*/
	public final static String DEFAULT_TEMPLATE_SIMPLE_NAME="_";
	/**The default name an XHTML template.*/
	public final static String DEFAULT_TEMPLATE_NAME=addExtension(DEFAULT_TEMPLATE_SIMPLE_NAME, XHTML_NAME_EXTENSION);

	/**Default constructor.*/
	public XHTMLResourceKit()
	{
		super(XHTML_CONTENT_TYPE, XHTML_NAME_EXTENSION, Capability.CREATE, Capability.EDIT);
	}

}

package com.globalmentor.marmot.resource.xhtml;

import static com.garretwilson.text.xml.xhtml.XHTML.*;

import static com.garretwilson.io.Files.*;

import com.garretwilson.text.xml.XMLIO;
import com.globalmentor.marmot.resource.*;

/**Resource kit for handling XHTML resources.
<p>Supported media types:</p>
<ul>
	<li><code>application/xhtml+xml</code></li>
</ul>
@author Garret Wilson
*/
public class XHTMLResourceKit extends AbstractResourceKit
{

	/**The I/O implementation that reads and writes XHTML resources.*/
	private final static XMLIO xhtmlIO;

		/**@return The I/O implementation that reads and writes XHTML resources.*/
		public static XMLIO getXHTMLIO() {return xhtmlIO;}

	static
	{
		xhtmlIO=new XMLIO(true);	//create namespace-aware XML I/O
	}

	/**The default simple name (i.e. the name without an extension) of an XHTML template.*/
	public final static String DEFAULT_TEMPLATE_SIMPLE_NAME="_";
	/**The default name an XHTML template.*/
	public final static String DEFAULT_TEMPLATE_NAME=addExtension(DEFAULT_TEMPLATE_SIMPLE_NAME, XHTML_NAME_EXTENSION);

	/**Default constructor.*/
	public XHTMLResourceKit()
	{
		super(XHTML_CONTENT_TYPE, Capability.CREATE);
	}

}

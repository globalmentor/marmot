package com.globalmentor.marmot.resource.xhtml;

import static com.globalmentor.io.Files.*;

import com.globalmentor.marmot.resource.*;
import com.globalmentor.text.xml.XMLIO;
import static com.globalmentor.text.xml.XML.*;
import com.globalmentor.text.xml.xhtml.XHTML;
import static com.globalmentor.text.xml.xhtml.XHTML.*;

import org.w3c.dom.*;

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

	/**Retrieves an excerpt from the given document.
	The elements are not removed from the original document.
	This implementation uses the first paragraph encountered depth-first. 
	@param element The element for which an excerpt should be returned.
	@return A document fragment containing an excerpt of the given element, or <code>null</code> if no excerpt could be located.
	@see XHTML#ELEMENT_P
	*/
	public static DocumentFragment getExcerpt(final Document document)
	{
		return getExcerpt(document.getDocumentElement());	//return an excerpt from the document element
	}

	/**Retrieves an excerpt from the given element.
	The elements are not removed from the original document.
	This implementation uses the first paragraph encountered depth-first. 
	@param element The element for which an excerpt should be returned.
	@return A document fragment containing an excerpt of the given element, or <code>null</code> if no excerpt could be located.
	@see XHTML#ELEMENT_P
	*/
	public static DocumentFragment getExcerpt(final Element element)
	{
		if(XHTML_NAMESPACE_URI.toString().equals(element.getNamespaceURI()) && ELEMENT_P.equals(element.getLocalName()))	//if this is <xhtml:p>
		{
			return extractNode(element, false);	//extract the paragraph without removing it
		}
		DocumentFragment excerpt=null; //process the children to try to find an excerpt
		final NodeList childNodeList=element.getChildNodes();	//get the list of child nodes
		final int childNodeCount=childNodeList.getLength();	//get the number of child nodes
		for(int i=0; i<childNodeCount && excerpt==null; ++i)	//look at each child node until find an excerpt
		{
			final Node childNode=childNodeList.item(i);	//get this child node
			if(childNode instanceof Element)	//if this is an element
			{
				excerpt=getExcerpt((Element)childNode);	//see if we can get an excerpt from this child node
			}
		}
		return excerpt;	//return the excerpt, if any, we found
	}

}

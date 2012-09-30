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

package com.globalmentor.marmot.resource.xhtml;

import java.io.*;
import java.net.URI;

import static com.globalmentor.java.CharSequences.*;
import static com.globalmentor.java.Objects.*;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.*;
import com.globalmentor.net.ContentType;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.text.xml.XMLIO;
import static com.globalmentor.text.xml.XML.*;
import static com.globalmentor.text.xml.xhtml.XHTML.*;
import static org.urframework.dcmi.DCMI.*;

import org.urframework.*;
import org.w3c.dom.*;

/**Abstract resource kit for basic handling XHTML resources.
@author Garret Wilson
*/
public class AbstractXHTMLResourceKit extends AbstractResourceKit
{

	/**The I/O implementation that reads and writes XHTML resources.*/
	private final static XMLIO xhtmlIO;

		/**@return The I/O implementation that reads and writes XHTML resources.*/
		public static XMLIO getXHTMLIO() {return xhtmlIO;}

	static
	{
		xhtmlIO=new XMLIO(true);	//create namespace-aware XML I/O
	}

	/**Capabilities constructor with no support for content type or types.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final Capability... capabilities)
	{
		this((String)null, capabilities);	//construct the class with no default extension
	}

	/**Content types and capabilities constructor with no default extension.
	@param supportedContentType The content type supported by this resource kit.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content type and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final ContentType supportedContentType, final Capability... capabilities)
	{
		this(supportedContentType, null, capabilities);	//construct the class with no default extension
	}

	/**Content types and capabilities constructor with no default extension.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final ContentType[] supportedContentTypes, final Capability... capabilities)
	{
		this(supportedContentTypes, (String)null, capabilities);	//construct the class with no default extension
	}

	/**Resource type and capabilities constructor with no default extension.
	@param supportedResourceType The URI for the resource type this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final URI supportedResourceType, final Capability... capabilities)
	{
		this(supportedResourceType, null, capabilities);	//construct the class with no default extension
	}

	/**Resource types and capabilities constructor with no default extension.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types array and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this(supportedResourceTypes, null, capabilities);	//construct the class with no default extension
	}

	/**Content types, resource types, and capabilities constructor with no default extension.
	@param presentation The presentation support for this resource kit.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes, final Capability... capabilities)
	{
		this(supportedContentTypes, supportedResourceTypes, null, capabilities);	//construct the class with no default extension
	}
	
	/**Default extension and capabilities constructor with no support for content type or types.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[0], defaultNameExtension, capabilities);
	}

	/**Content types, default extension, and capabilities constructor.
	@param supportedContentType The content type supported by this resource kit.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content type and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final ContentType supportedContentType, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[]{checkInstance(supportedContentType, "Supported content type cannot be null.")}, defaultNameExtension, capabilities);
	}

	/**Content types, default extension, and capabilities constructor.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final ContentType[] supportedContentTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		this(supportedContentTypes, new URI[]{}, defaultNameExtension, capabilities);	//construct the class with no supported resource types
	}

	/**Resource type, default extension, and capabilities constructor.
	@param supportedResourceType The URI for the resource type this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final URI supportedResourceType, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new URI[]{checkInstance(supportedResourceType, "Supported resource type cannot be null.")}, defaultNameExtension, capabilities);
	}

	/**Resource types, default extension, and capabilities constructor.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported resource types array and/or capabilities is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final URI[] supportedResourceTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		this(new ContentType[]{}, supportedResourceTypes, defaultNameExtension, capabilities);	//construct the class with no supported content types
	}

	/**Content types, resource types, default extension, and capabilities constructor.
	@param presentation The presentation support for this resource kit.
	@param supportedContentTypes A non-<code>null</code> array of the content types this resource kit supports.
	@param supportedResourceTypes A non-<code>null</code> array of the URIs for the resource types this resource kit supports.
	@param defaultNameExtension The default name extension this resource kit uses, or <code>null</code> if by default this resource kit does not use an extension.
	@param capabilities The capabilities provided by this resource kit.
	@exception NullPointerException if the supported content types array and/or the supported resource types array is <code>null</code>.
	*/
	public AbstractXHTMLResourceKit(final ContentType[] supportedContentTypes, final URI[] supportedResourceTypes, final String defaultNameExtension, final Capability... capabilities)
	{
		super(supportedContentTypes, supportedResourceTypes, defaultNameExtension, capabilities);	//construct the parent class
	}

	/**Indicates whether this resource has default resource content.
	This version returns <code>true</code>.
	@param repository The repository that contains the resource.
	@param resourceURI The reference URI to use to identify the resource, which may not exist.
	@exception NullPointerException if the given repository, resource URI, resource description, and/or output stream is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the default resource content could not be written.
	@see #writeDefaultResourceContent(Repository, URI, URFResource)
	@see #writeDefaultResourceContent(Repository, URI, URFResource, OutputStream)
	*/
	public boolean hasDefaultResourceContent(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		return true;
	}

	/**Writes default resource content to the given output stream.
	@param repository The repository that contains the resource.
	@param resourceURI The reference URI to use to identify the resource, which may not exist.
	@param resourceDescription A description of the resource; the resource URI is ignored.
	@exception NullPointerException if the given repository, resource URI, resource description, and/or output stream is <code>null</code>.
	@exception IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	@exception ResourceIOException if the default resource content could not be written.
	*/
	public void writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription, final OutputStream outputStream) throws ResourceIOException
	{
		final String title=getTitle(resourceDescription);	//see if there is a title
		final Document document=createXHTMLDocument(title!=null ? title : "", true, true);	//create an XHTML document with a doctype and the correct title, if any
		try
		{
			getXHTMLIO().write(outputStream, resourceURI, document);	//write the default document				
		}
		catch(final IOException ioException)	//if an I/O exception occurs
		{
			throw ResourceIOException.toResourceIOException(ioException, resourceURI);	//send a resource version of the exception
		}
	}

	/**Retrieves an excerpt from the given document.
	The elements are not removed from the original document.
	This implementation uses the first non-empty paragraph encountered depth-first. 
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
	This implementation uses the first non-empty paragraph encountered depth-first. 
	@param element The element for which an excerpt should be returned.
	@return A document fragment containing an excerpt of the given element, or <code>null</code> if no excerpt could be located.
	@see XHTML#ELEMENT_P
	*/
	public static DocumentFragment getExcerpt(final Element element)
	{
		if(XHTML_NAMESPACE_URI.toString().equals(element.getNamespaceURI()) && ELEMENT_P.equals(element.getLocalName()))	//if this is <xhtml:p>
		{
			if(containsNonTrim(element.getTextContent()))	//if this paragraph isn't empty
			{
				return extractNode(element, false);	//extract the paragraph without removing it
			}
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

package com.globalmentor.marmot.resource.xhtml;

import static com.globalmentor.io.Files.*;

import java.io.IOException;
import java.net.URI;

import org.w3c.dom.Document;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.ResourceIOException;

import static com.globalmentor.net.URIs.*;

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

	/**Determines the URI of the template to use for the resource identified by the given URI.
	@param repository The repository in which the resource resides.
	@param resourceURI The URI of the resource.
	@return The URI of the template to use for the resource, or <code>null</code> if no template could be located.
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public static URI getResourceTemplateURI(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		URI collectionURI=getCurrentLevel(resourceURI);	//start at the current collection level
		do
		{
			final URI templateURI=collectionURI.resolve(DEFAULT_TEMPLATE_NAME);	//get the URI of the template if it were to reside at this level
			if(repository.resourceExists(templateURI))	//if the template exists here
			{
				return templateURI;	//return the URI of the template
			}
			collectionURI=repository.getParentResourceURI(collectionURI);	//go up a level
		}
		while(collectionURI!=null);	//keep going up the hierarchy until we run out of parent collections
		return null;	//indicate that we could find no template URI
	}

	/**Loads the template document for the identified resource.
	@param repository The repository in which the resource resides.
	@param resourceURI The URI of the resource.
	@return The template document for the given resource, or <code>null</code> if no template could be found for the identified resource.
	@throws ResourceIOException if there is an error accessing the repository.
	@see #getResourceTemplateURI(Repository, URI)
	*/
/*TODO del if not needed
	public static Document getResourceTemplate(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		final URI resourceTemplateURI=getResourceTemplateURI(repository, resourceURI);	//get the URI of the resource template, if there is one
		if(resourceTemplateURI!=null)	//if there is a template
		{
			try
			{
				return getXHTMLIO().read(repository.getResourceInputStream(resourceTemplateURI), resourceURI);	//read the template and return it
			}
			catch(final IOException ioException)	//if there is an I/O error
			{
				throw ResourceIOException.toResourceIOException(ioException, resourceTemplateURI);
			}
		}
		else	//if there is no template
		{
			return null;
		}
	}
*/
}

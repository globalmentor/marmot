package com.globalmentor.marmot.resource.xhtml;

import java.io.IOException;
import java.net.URI;

import net.marmox.resource.*;
import net.marmox.resource.xhtml.XHTMLMenuWidget;

import static com.globalmentor.io.Files.*;
import static com.globalmentor.java.Classes.*;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.net.URIPath;

import static com.globalmentor.net.URIs.*;
import static com.globalmentor.text.xml.XML.*;
import com.globalmentor.text.xml.xhtml.XHTML;
import com.globalmentor.urf.URFResource;
import static com.globalmentor.urf.URF.*;

import static com.globalmentor.text.xml.xhtml.XHTML.*;

import org.w3c.dom.*;

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
	/**The extension for XHTML template resource names.*/
	public final static String XHTML_TEMPLATE_NAME_EXTENSION="_xhtml";	//TODO create content type for _xhtml file
	/**The default name an XHTML template.*/
	public final static String DEFAULT_TEMPLATE_NAME=addExtension(DEFAULT_TEMPLATE_SIMPLE_NAME, XHTML_TEMPLATE_NAME_EXTENSION);

	/**The URI of the Marmot XHTML namespace.*/
	public final static URI MARMOT_XHTML_NAMESPACE_URI=URI.create("http://globalmentor.com/marmot/resource/xhtml");
		//properties
	/**Specifies a template of resource by its URI, which may be a path URI relative to the repository.*/
	public final static URI TEMPLATE_URI_PROPERTY_URI=createResourceURI(MARMOT_XHTML_NAMESPACE_URI, "templateURI");

	/**Default constructor.*/
	public XHTMLResourceKit()
	{
		super(XHTML_CONTENT_TYPE, XHTML_NAME_EXTENSION, Capability.CREATE, Capability.EDIT);
	}

	/**Determines the URI of the template to use for the resource identified by the given URI.
	First a template is attempted to be identified from the {@value XHTMLResourceKit#TEMPLATE_URI_PROPERTY_URI} property.
	Then, if there is no template explicitly identified, a template named {@value #DEFAULT_TEMPLATE_NAME} is searched for up the hierarchy.
	@param repository The repository in which the resource resides.
	@param resourceURI The URI of the resource.
	@return The URI of the template to use for the resource, or <code>null</code> if no template could be located.
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public static URI getResourceTemplateURI(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		return getResourceTemplateURI(repository, repository.getResourceDescription(resourceURI));	//get the description of the resource and look for a template
	}

	/**Determines the URI of the template to use for the given resource.
	First a template is attempted to be identified from the {@value XHTMLResourceKit#TEMPLATE_URI_PROPERTY_URI} property.
	Then, if there is no template explicitly identified, a template named {@value #DEFAULT_TEMPLATE_NAME} is searched for up the hierarchy.
	@param repository The repository in which the resource resides.
	@param resource The resource for which a template URI should be retrieved.
	@return The URI of the template to use for the resource, or <code>null</code> if no template could be located.
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public static URI getResourceTemplateURI(final Repository repository, final URFResource resource) throws ResourceIOException
	{
		final URI resourceURI=resource.getURI();	//get the URI of the resource
		final URI explicitTemplateURI=XHTMLResourceKit.getTemplateURI(resource);	//get the template URI property, if any
		if(explicitTemplateURI!=null)	//if there is a template URI specified
		{
			final URIPath templatePath=URIPath.asPathURIPath(explicitTemplateURI);	//see if this is a path: URI
			if(templatePath==null || !templatePath.isRelative())	//if this is not a relative path TODO determine how to handle absolute paths and general URIs appropriately; this will probably include making subrepositories be able to access root repositories
			{
				throw new ResourceIOException(resourceURI, "Specified template URI "+explicitTemplateURI+" for resource "+resourceURI+" currently must be a relative <path:...> URI.");
			}
			return resourceURI.resolve(templatePath.toURI());	//resolve the template path to the resource URI
		}
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

	/**Retrieves a default template document for the identified resource.
	@param repository The repository in which the resource resides.
	@param resourceURI The URI of the resource.
	@return A default template document for the given resource.
	@throws ResourceIOException if there is an error accessing the repository.
	*/
	public static Document getDefaultResourceTemplate(final Repository repository, final URI resourceURI) throws ResourceIOException
	{
		final Document document=createXHTMLDocument("Template");	//create a new template document TODO use a constant
		final Element bodyElement=getBodyElement(document);	//get the body element of the document
		assert bodyElement!=null : "XHTML documents should always have a body.";
		final Element headerElement=document.createElementNS(XHTML_NAMESPACE_URI.toString(), ELEMENT_HEADER);	//<header>
		bodyElement.appendChild(headerElement);
		appendElementNS(headerElement, createJavaURI(BreadcrumbWidget.class.getPackage()).toString(), getLocalName(BreadcrumbWidget.class));	//<BreadcrumbWidget>
		appendElementNS(headerElement, createJavaURI(ResourceLabelHeadingWidget.class.getPackage()).toString(), getLocalName(ResourceLabelHeadingWidget.class));	//<ResourceLabelHeadingWidget>
		final Element asideElement=document.createElementNS(XHTML_NAMESPACE_URI.toString(), ELEMENT_ASIDE);	//<aside>
		bodyElement.appendChild(asideElement);
		appendElementNS(asideElement, createJavaURI(XHTMLMenuWidget.class.getPackage()).toString(), getLocalName(XHTMLMenuWidget.class));	//<XHTMLMenuWidget>
		return document;	//return the template document we constructed
	}

	/**Returns the template URI of the resource
	@param resource The resource the property of which should be located.
	@return The URI value of the property, or <code>null</code> if there is no such property or the property value is not a URI.
	@see #TEMPLATE_URI_PROPERTY_URI
	*/
	public static URI getTemplateURI(final URFResource resource)
	{
		return asURI(resource.getPropertyValue(TEMPLATE_URI_PROPERTY_URI));
	}

	/**Sets the template URI of the resource.
	@param resource The resource of which the property should be set.
	@param value The property value to set.
	@see #TEMPLATE_URI_PROPERTY_URI
	*/
	public static void setTemplateURI(final URFResource resource, final URI value)
	{
		resource.setPropertyValue(TEMPLATE_URI_PROPERTY_URI, value);
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

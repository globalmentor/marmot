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

import java.net.URI;

import static com.globalmentor.io.Files.*;
import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.net.*;
import com.globalmentor.w3c.spec.HTML;
import com.globalmentor.xml.xhtml.XHTML;

import static com.globalmentor.net.URIs.*;
import static com.globalmentor.w3c.spec.HTML.*;
import static com.globalmentor.xml.XML.*;
import static org.urframework.URF.*;

import org.urframework.URFResource;
import org.w3c.dom.*;

/**
 * Resource kit for handling XHTML resources.
 * <p>
 * Supported media types:
 * </p>
 * <ul>
 * <li>{@link HTML#XHTML_CONTENT_TYPE}</li>
 * </ul>
 * <p>
 * Default extension:
 * </p>
 * <ul>
 * <li>{@value HTML#XHTML_NAME_EXTENSION}</li>
 * </ul>
 * @author Garret Wilson
 */
public class XHTMLResourceKit extends AbstractXHTMLResourceKit {

	/** The default simple name (i.e. the name without an extension) of an XHTML template. */
	public static final String DEFAULT_TEMPLATE_SIMPLE_NAME = "_";
	/** The extension for XHTML template resource names. */
	public static final String XHTML_TEMPLATE_NAME_EXTENSION = "_xhtml"; //TODO create content type for _xhtml file
	/** The default name an XHTML template. */
	public static final String DEFAULT_TEMPLATE_NAME = addExtension(DEFAULT_TEMPLATE_SIMPLE_NAME, XHTML_TEMPLATE_NAME_EXTENSION);

	/** The URI of the Marmot XHTML namespace. */
	public static final URI MARMOT_XHTML_NAMESPACE_URI = URI.create("http://globalmentor.com/marmot/resource/xhtml/");
	//properties
	/** Specifies a template of resource by its URI, which may be a path URI relative to the repository. */
	public static final URI TEMPLATE_URI_PROPERTY_URI = createResourceURI(MARMOT_XHTML_NAMESPACE_URI, "templateURI");

	/** Default constructor. */
	public XHTMLResourceKit() {
		super(XHTML_CONTENT_TYPE, XHTML_NAME_EXTENSION, Capability.CREATE, Capability.EDIT);
	}

	/**
	 * Determines the URI of the template to use for the resource identified by the given URI. First a template is attempted to be identified from the
	 * {@link XHTMLResourceKit#TEMPLATE_URI_PROPERTY_URI} property. Then, if there is no template explicitly identified, a template named
	 * {@link #DEFAULT_TEMPLATE_NAME} is searched for up the hierarchy.
	 * @param repository The repository in which the resource resides.
	 * @param resourceURI The URI of the resource.
	 * @return The URI of the template to use for the resource, or <code>null</code> if no template could be located.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public static URI getResourceTemplateURI(final Repository repository, final URI resourceURI) throws ResourceIOException {
		return getResourceTemplateURI(repository, repository.getResourceDescription(resourceURI)); //get the description of the resource and look for a template
	}

	/**
	 * Determines the URI of the template to use for the given resource. First a template is attempted to be identified from the
	 * {@link XHTMLResourceKit#TEMPLATE_URI_PROPERTY_URI} property. Then, if there is no template explicitly identified, a template named
	 * {@link #DEFAULT_TEMPLATE_NAME} is searched for up the hierarchy.
	 * @param repository The repository in which the resource resides.
	 * @param resource The resource for which a template URI should be retrieved.
	 * @return The URI of the template to use for the resource, or <code>null</code> if no template could be located.
	 * @throws ResourceIOException if there is an error accessing the repository.
	 */
	public static URI getResourceTemplateURI(final Repository repository, final URFResource resource) throws ResourceIOException {
		return getRelatedResourceURI(repository, resource, TEMPLATE_URI_PROPERTY_URI, DEFAULT_TEMPLATE_NAME, true);
		/*TODO del when works
				final URI resourceURI=resource.getURI();	//get the URI of the resource
				final URI explicitTemplateURI=XHTMLResourceKit.getTemplateURI(resource);	//get the template URI property, if any
				if(explicitTemplateURI!=null) {	//if there is a template URI specified
					final URIPath templatePath=URIPath.asPathURIPath(explicitTemplateURI);	//see if this is a path: URI
					if(templatePath==null || !templatePath.isRelative()) {	//if this is not a relative path TODO determine how to handle absolute paths and general URIs appropriately; this will probably include making subrepositories be able to access root repositories
						throw new ResourceIOException(resourceURI, "Specified template URI "+explicitTemplateURI+" for resource "+resourceURI+" currently must be a relative <path:...> URI.");
					}
					return resolve(resourceURI, templatePath.toURI());	//resolve the template path to the resource URI
				}
				URI collectionURI=getCurrentLevel(resourceURI);	//start at the current collection level
				do
				{
					final URI templateURI=resolve(collectionURI, DEFAULT_TEMPLATE_NAME);	//get the URI of the template if it were to reside at this level
					if(repository.resourceExists(templateURI)) {	//if the template exists here
						return templateURI;	//return the URI of the template
					}
					collectionURI=repository.getParentResourceURI(collectionURI);	//go up a level
				}
				while(collectionURI!=null);	//keep going up the hierarchy until we run out of parent collections
				return null;	//indicate that we could find no template URI
		*/
	}

	/**
	 * Returns the template URI of the resource
	 * @param resource The resource the property of which should be located.
	 * @return The URI value of the property, or <code>null</code> if there is no such property or the property value is not a URI.
	 * @see #TEMPLATE_URI_PROPERTY_URI
	 */
	public static URI getTemplateURI(final URFResource resource) {
		return asURI(resource.getPropertyValue(TEMPLATE_URI_PROPERTY_URI));
	}

	/**
	 * Sets the template URI of the resource.
	 * @param resource The resource of which the property should be set.
	 * @param value The property value to set.
	 * @see #TEMPLATE_URI_PROPERTY_URI
	 */
	public static void setTemplateURI(final URFResource resource, final URI value) {
		resource.setPropertyValue(TEMPLATE_URI_PROPERTY_URI, value);
	}

	//	/**
	//	 * Loads the template document for the identified resource.
	//	 * @param repository The repository in which the resource resides.
	//	 * @param resourceURI The URI of the resource.
	//	 * @return The template document for the given resource, or <code>null</code> if no template could be found for the identified resource.
	//	 * @throws ResourceIOException if there is an error accessing the repository.
	//	 * @see #getResourceTemplateURI(Repository, URI)
	//	 */
	/*TODO del if not needed
		public static Document getResourceTemplate(final Repository repository, final URI resourceURI) throws ResourceIOException
		{
			final URI resourceTemplateURI=getResourceTemplateURI(repository, resourceURI);	//get the URI of the resource template, if there is one
			if(resourceTemplateURI!=null) {	//if there is a template
				try
				{
					return getXHTMLIO().read(repository.getResourceInputStream(resourceTemplateURI), resourceURI);	//read the template and return it
				}
				catch(final IOException ioException) {	//if there is an I/O error
					throw ResourceIOException.toResourceIOException(ioException, resourceTemplateURI);
				}
			}
			else {	//if there is no template
				return null;
			}
		}
	*/
}

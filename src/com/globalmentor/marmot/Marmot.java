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

package com.globalmentor.marmot;

import static com.globalmentor.urf.URF.*;

import java.net.URI;

import com.globalmentor.config.Configurator;
import com.globalmentor.urf.URFResource;

/**Constant values and utilities used by Marmot.
@author Garret Wilson
*/
public class Marmot
{

	static	//configure the default Marmot configuration to return a default Marmot resource cache
	{
		Configurator.setDefaultConfiguration(new MarmotConfiguration()
			{
				private final MarmotResourceCache<?> resourceCache=new DefaultMarmotResourceCache();
				@Override public MarmotResourceCache<?> getResourceCache() {return resourceCache;}
			});
	}

		//predefined users
	/**The principal wildcard character, '*'.*/
//TODO del	public final static char WILDCARD_PRINCIPAL_CHAR='*';
	/**The predefined "all principals at localhost" <code>mailto</code> URI.*/
//TODO del	public final static URI ALL_LOCALHOST_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), LOCALHOST_DOMAIN);
	/**The predefined "all principals" <code>mailto</code> URI.*/
//TODO del	public final static URI ALL_PRINCIPALS_URI=createMailtoURI(String.valueOf(WILDCARD_PRINCIPAL_CHAR), String.valueOf(WILDCARD_PRINCIPAL_CHAR));

	/**The URI to the Marmot namespace.*/
	public final static URI MARMOT_NAMESPACE_URI=URI.create("http://globalmentor.com/marmot/");

		//Marmot properties
	/**Provides an annotation of the resource.*/
	public final static URI ANNOTATION_PROPERTY_URI=createResourceURI(MARMOT_NAMESPACE_URI, "annotation");

	/**Returns the annotations of the resource.
	@param resource The resource the property of which should be located.
	@return The annotation values of the property.
	@see #ANNOTATION_PROPERTY_URI
	*/
	public static Iterable<Annotation> getAnnotations(final URFResource resource)
	{
		return getAnnotations(resource, Annotation.class);
	}

	/**Returns the annotations of the resource.
	@param <A> The type of annotation to be returned.
	@param resource The resource the property of which should be located.
	@return The annotation values of the property.
	@see #ANNOTATION_PROPERTY_URI
	*/
	public static <A extends Annotation> Iterable<A> getAnnotations(final URFResource resource, final Class<A> annotationClass)
	{
		return resource.getPropertyValues(ANNOTATION_PROPERTY_URI, annotationClass);
	}

	/**Adds an annotation to the resource.
	@param resource The resource of which the property should be set.
	@param value The property value to set.
	@see #ANNOTATION_PROPERTY_URI
	*/
	public static void addAnnotation(final URFResource resource, final Annotation value)
	{
		resource.addPropertyValue(ANNOTATION_PROPERTY_URI, value);
	}

		//deprecated
	
	/**The URI of the Marmot RDF namespace.*/
	public final static URI MARMOT_RDF_NAMESPACE_URI=URI.create("http://globalmentor.com/namespaces/marmot#");	//TODO del; used by old code

	/**A resource that is a collection of other resources.*/
	public final static String COLLECTION_CLASS_NAME="Collection";	//TODO del; used by old code

}

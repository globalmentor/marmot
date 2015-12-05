/*
 * Copyright © 2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import java.net.URI;

import org.urframework.content.AbstractContentResource;

/**
 * The abstract base class of a resource annotation.
 * @author Garret Wilson
 */
public abstract class AbstractAnnotation extends AbstractContentResource implements Annotation {

	/**
	 * URI and type namespace URI constructor.
	 * @param uri The URI for the resource, or <code>null</code> if the resource should have no URI.
	 * @param typeNamespaceURI The namespace URI of the URI of the type to be added.
	 * @throws NullPointerException if the given type type namespace URI is <code>null</code>.
	 */
	public AbstractAnnotation(final URI uri, final URI typeNamespaceURI) {
		super(uri, typeNamespaceURI); //construct the parent class
	}

}

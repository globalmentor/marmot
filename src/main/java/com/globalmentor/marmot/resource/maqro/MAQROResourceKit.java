/*
 * Copyright © 2003-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.resource.maqro;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import org.urframework.URFResource;
import org.urframework.maqro.MAQRO;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.*;
import com.globalmentor.net.ContentType;
import com.globalmentor.net.ResourceIOException;

import static org.urframework.maqro.MAQRO.*;

/**
 * Resource kit for handling mentoring activities and interactions.
 * <p>
 * Supported media types:
 * </p>
 * <ul>
 * <li>{@value MAQRO#MENTOR_ACTIVITY_CONTENT_TYPE}</li>
 * </ul>
 * @author Garret Wilson
 */
public class MAQROResourceKit extends AbstractResourceKit {

	/** Default constructor. */
	public MAQROResourceKit() {
		super(new ContentType[] { MENTOR_ACTIVITY_CONTENT_TYPE });
	}

	/**
	 * Writes default resource content to the given output stream.
	 * @param repository The repository that contains the resource.
	 * @param resourceURI The reference URI to use to identify the resource, which may not exist.
	 * @param resourceDescription A description of the resource; the resource URI is ignored.
	 * @throws NullPointerException if the given repository, resource URI, resource description, and/or output stream is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @throws ResourceIOException if the default resource content could not be written.
	 */
	public void writeDefaultResourceContent(final Repository repository, final URI resourceURI, final URFResource resourceDescription,
			final OutputStream outputStream) throws ResourceIOException {
		/*TODO fix for MAQRO
				final String title=getTitle(resourceDescription);	//see if there is a title
				final Document document=createXHTMLDocument(title!=null ? title : "", true, true);	//create an XHTML document with a doctype and the correct title, if any
				try
				{
					getXHTMLIO().write(outputStream, resourceURI, document);	//write the default document				
				}
				catch(final IOException ioException) {	//if an I/O exception occurs
					throw ResourceIOException.toResourceIOException(ioException, resourceURI);	//send a resource version of the exception
				}
		*/
	}

}

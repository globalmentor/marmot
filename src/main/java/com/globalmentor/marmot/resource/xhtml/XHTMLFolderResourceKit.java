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

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.xhtml.AbstractXHTMLResourceKit;
import com.globalmentor.net.*;

import static com.globalmentor.net.URIs.*;
import static com.globalmentor.w3c.spec.HTML.*;

/**
 * Resource kit for handling folders with XHTML content
 * @author Garret Wilson
 */
public class XHTMLFolderResourceKit extends AbstractXHTMLResourceKit {

	/** Default constructor. */
	public XHTMLFolderResourceKit() {
		super(Capability.CREATE);
	}

	@Override
	public ContentType getDefaultContentType() {
		return XHTML_CONTENT_TYPE;
	} //TODO improve so that we can add this to the supported list, allowing a resource to indicate it allows general capabilities or not, which will obviate this method

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version first appends an ending path separator before resolving the name against the child resource collection URI.
	 * </p>
	 * @throws NullPointerException if the given repository and/or resource URI is <code>null</code>.
	 * @throws IllegalArgumentException if the given URI designates a resource that does not reside inside this repository.
	 * @see #getDefaultNameExtension()
	 */
	@Override
	public URI getChildResourceURI(final Repository repository, final URI parentResourceURI, final String resourceName) throws ResourceIOException {
		//TODO fix IllegalArgumentException by checking to ensure that the parent resource is within the repository
		return resolve(parentResourceURI, URIPath.createURIPathURI(URIPath.encodeSegment(resourceName) + PATH_SEPARATOR)); //encode the resource name, append a path separator, and resolve it against the child resource collection URI; use the special URIPath method in case the name contains a colon character
	}
}

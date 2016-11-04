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

package com.globalmentor.marmot.resource.folder;

import java.net.URI;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.resource.AbstractResourceKit;
import com.globalmentor.net.ResourceIOException;
import com.globalmentor.net.URIPath;
import static com.globalmentor.net.URIs.*;

/**
 * Resource kit for handling collections as folder resources that have no content but can contain children.
 * @author Garret Wilson
 */
public class FolderResourceKit extends AbstractResourceKit {

	/** Default constructor. */
	public FolderResourceKit() {
		super(Capability.CREATE);
	}

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

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

package com.globalmentor.marmot.security;

import java.net.URI;

import org.urframework.AbstractClassTypedURFResource;

import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**
 * An abstract resource in the Marmot security namespace, {@value MarmotSecurity#MARMOT_SECURITY_NAMESPACE_URI}.
 * @author Garret Wilson
 */
public abstract class AbstractMarmotSecurityResource extends AbstractClassTypedURFResource {

	/** Default constructor. */
	public AbstractMarmotSecurityResource() {
		this(null); //construct the class with no URI
	}

	/**
	 * URI constructor.
	 * @param uri The URI for the new resource.
	 */
	public AbstractMarmotSecurityResource(final URI uri) {
		super(uri, MARMOT_SECURITY_NAMESPACE_URI); //construct the parent class
	}
}
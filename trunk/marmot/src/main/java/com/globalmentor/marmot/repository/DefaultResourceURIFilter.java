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

package com.globalmentor.marmot.repository;

import java.net.URI;

import org.urframework.URFResource;

/**
 * A resource filter that filters on resource URI. This class automatically filters on collection and non-collection status. A subclass may override
 * {@link #isPass(URI)} for more specific URI filtering, but should first call the parent version and return <code>false</code> if the URI does not pass. This
 * implementation of {@link #isPass(URFResource)} does no filtering at all, as filtering on URI will already have occurred in {@link #isPass(URI)}.
 * @author Garret Wilson
 */
public class DefaultResourceURIFilter extends AbstractResourceFilter {

	/** Default constructor which passes both collections and non-collections. */
	public DefaultResourceURIFilter() {
		this(true); //allow collections
	}

	/**
	 * Collection pass constructor which passes non-collections.
	 * @param collectionPass Whether collection resources are allowed to pass.
	 */
	public DefaultResourceURIFilter(final boolean collectionPass) {
		this(collectionPass, true); //allow non-collections
	}

	/**
	 * Collection and non-collection pass constructor.
	 * @param collectionPass Whether collection resources are allowed to pass.
	 * @param nonCollectionPass Whether non collection resources are allowed to pass.
	 */
	public DefaultResourceURIFilter(final boolean collectionPass, final boolean nonCollectionPass) {
		super(collectionPass, nonCollectionPass); //construct the parent class
	}

	/**
	 * Determines whether a given resource should pass through the filter or be filtered out. This implementation unconditionally returns <code>true</code>.
	 * @param resource The resource to filter.
	 * @return <code>true</code> if the resource should pass through the filter, else <code>false</code> if the resource should be filtered out.
	 */
	public boolean isPass(final URFResource resource) {
		return true;
	}

}

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

import static com.globalmentor.net.URIs.*;

/**
 * An abstract implementation of a resource filter that provides functionality for determining if collections and non-collections should be passed.
 * @author Garret Wilson
 */
public abstract class AbstractResourceFilter implements ResourceFilter {

	/** Whether collection resources are allowed to pass. */
	private final boolean collectionPass;

	/** @return Whether collection resources are allowed to pass. */
	public boolean isCollectionPass() {
		return collectionPass;
	}

	/** Whether non collection resources are allowed to pass. */
	private final boolean nonCollectionPass;

	/** @return Whether non collection resources are allowed to pass. */
	public boolean isNonCollectionPass() {
		return nonCollectionPass;
	}

	/** Default constructor which passes both collections and non-collections. */
	public AbstractResourceFilter() {
		this(true); //allow collections
	}

	/**
	 * Collection pass constructor which passes non-collections.
	 * @param collectionPass Whether collection resources are allowed to pass.
	 */
	public AbstractResourceFilter(final boolean collectionPass) {
		this(collectionPass, true); //allow non-collections
	}

	/**
	 * Collection and non-collection pass constructor.
	 * @param collectionPass Whether collection resources are allowed to pass.
	 * @param nonCollectionPass Whether non collection resources are allowed to pass.
	 */
	public AbstractResourceFilter(final boolean collectionPass, final boolean nonCollectionPass) {
		this.collectionPass = collectionPass;
		this.nonCollectionPass = nonCollectionPass;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This version rejects a resource if its collection or non-collection status is not allowed.
	 * </p>
	 * @see #isCollectionPass()
	 * @see #isNonCollectionPass()
	 */
	@Override
	public boolean isPass(final URI resourceURI) {
		if(isCollectionURI(resourceURI)) { //if this is a collection URI
			if(!isCollectionPass()) { //if collections shouldn't pass
				return false;
			}
		} else { //if this is a non-collection URI
			if(!isNonCollectionPass()) { //if non-collections shouldn't pass
				return false;
			}
		}
		return true; //this resource met all the criteria
	}

}

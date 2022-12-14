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

import static java.util.Objects.*;

import org.urframework.URFResource;

import static com.globalmentor.java.Arrays.*;
import static com.globalmentor.java.Objects.*;
import com.globalmentor.marmot.MarmotSession;
import com.globalmentor.net.ContentType;

/**
 * A resource filter that filters on resource content type. This class automatically filters on collection and non-collection status. A subclass may override
 * {@link #isPass(URI)} for more specific URI filtering, but should first call the parent version and return <code>false</code> if the URI does not pass.
 * Constructing this filter with no specified content types will not pass any resources. This filter does not pass resources with no content type determined.
 * @author Garret Wilson
 * @see MarmotSession#determineContentType(URFResource)
 * @see ContentType#hasBaseType(ContentType)
 */
public class ResourceContentTypeFilter extends AbstractResourceFilter {

	/** The Marmot session to determine the content types. */
	private final MarmotSession<?> marmotSession;

	/** @return The Marmot session to determine the content types. */
	public MarmotSession<?> getMarmotSession() {
		return marmotSession;
	}

	/** The allowed content types. */
	private final ContentType[] contentTypes;

	/** @return The allowed content types. */
	public ContentType[] getContentTypes() {
		return contentTypes.clone();
	}

	/**
	 * Content types constructor which passes both collections and non-collections.
	 * @param marmotSession The Marmot session to determine the content types.
	 * @param contentTypes The passed content types.
	 * @throws NullPointerException if the given Marmos session, content types, and/or any content type is <code>null</code>.
	 */
	public ResourceContentTypeFilter(final MarmotSession<?> marmotSession, final ContentType... contentTypes) {
		this(marmotSession, true, contentTypes); //allow collections
	}

	/**
	 * Collection pass and content types constructor which passes non-collections.
	 * @param marmotSession The Marmot session to determine the content types.
	 * @param collectionPass Whether collection resources are allowed to pass.
	 * @param contentTypes The passed content types.
	 * @throws NullPointerException if the given Marmos session, content types, and/or any content type is <code>null</code>.
	 */
	public ResourceContentTypeFilter(final MarmotSession<?> marmotSession, final boolean collectionPass, final ContentType... contentTypes) {
		this(marmotSession, collectionPass, true, contentTypes); //allow non-collections
	}

	/**
	 * Collection pass, non-collection pass, and content types constructor.
	 * @param marmotSession The Marmot session to determine the content types.
	 * @param collectionPass Whether collection resources are allowed to pass.
	 * @param nonCollectionPass Whether non collection resources are allowed to pass.
	 * @param contentTypes The passed content types.
	 * @throws NullPointerException if the given Marmot session, content types, and/or any content type is <code>null</code>.
	 */
	public ResourceContentTypeFilter(final MarmotSession<?> marmotSession, final boolean collectionPass, final boolean nonCollectionPass,
			final ContentType... contentTypes) {
		super(collectionPass, nonCollectionPass); //construct the parent class
		this.marmotSession = requireNonNull(marmotSession, "Marmot session cannot be null.");
		this.contentTypes = requireNonNull(contentTypes).clone(); //create a copy of content types to pass
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This implementation only passes resources with a content type that matches at least one of the provided content types.
	 * </p>
	 */
	@Override
	public boolean isPass(final URFResource resource) {
		boolean contentTypePass = false;
		final ContentType contentType = getMarmotSession().determineContentType(resource); //get the content type of the resource
		if(contentType != null) { //if the resource has a content type
			for(final ContentType passContentType : contentTypes) { //for each content type to pass
				if(passContentType.hasBaseType(contentType)) { //if the content type matches
					contentTypePass = true; //we found a matching content type
					break; //stop checking
				}
			}
		}
		return contentTypePass; //return whether at least one content type passed 
	}

}

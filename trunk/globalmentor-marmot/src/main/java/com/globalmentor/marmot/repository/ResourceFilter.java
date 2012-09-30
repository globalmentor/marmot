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

import com.globalmentor.model.Filter;
import com.globalmentor.net.URIs;

/**A resource filter that allows two levels of filtering: one on the resource URI, and one on the resource description in general.
An operation using the filter will typically filter based upon the resource URI using {@link #isPass(URI)}
before filtering on the resource description using {@link #isPass(URFResource)}.
Implementations of this interface may assume that {@link #isPass(URI)} is called before {@link #isPass(URFResource)}
and need not provide URI-based filtering inside {@link #isPass(URFResource)} if that has been done in {@link #isPass(URI)}.
@author Garret Wilson
*/
public interface ResourceFilter extends Filter<URFResource>
{

	/**The resource filter that only passes collection resource URIs.
	@see URIs#isCollectionURI(URI)
	*/
	public final static ResourceFilter COLLECTION_RESOURCE_URI_FILTER=new DefaultResourceURIFilter(true, false);
	
	/**The resource filter that only passes non-collection resource URIs.
	@see URIs#isCollectionURI(URI)
	*/
	public final static ResourceFilter NON_COLLECTION_RESOURCE_URI_FILTER=new DefaultResourceURIFilter(false, true);
	
	/**Determines whether a given resource should pass through the filter or be filtered out based upon its URI.
	@param resourceURI The resource URI to filter.
	@return <code>true</code> if the resource should pass through the filter based upon its URI, else
		<code>false</code> if the resource should be filtered out.
	*/
	public boolean isPass(final URI resourceURI);

}

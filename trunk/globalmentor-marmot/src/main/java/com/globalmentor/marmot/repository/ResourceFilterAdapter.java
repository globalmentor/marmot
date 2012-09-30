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


/**A filter adapter which allows all resources.
Child implementations should override {@link #isPass(URI)} or {@link #isPass(URFResource)} to provide some filtering.
@author Garret Wilson
*/
public class ResourceFilterAdapter implements ResourceFilter
{

	/**Determines whether a given resource should pass through the filter or be filtered out based upon its URI.
	This version unconditionally returns <code>true</code>.
	@param resourceURI The resource URI to filter.
	@return <code>true</code> if the resource should pass through the filter based upon its URI, else
		<code>false</code> if the resource should be filtered out.
	*/
	public boolean isPass(final URI resourceURI)
	{
		return true;
	}

	/**Determines whether a given resource should pass through the filter or be filtered out.
	This version unconditionally returns <code>true</code>.
	@param resource The resource to filter.
	@return <code>true</code> if the resource should pass through the filter, else
		<code>false</code> if the resource should be filtered out.
	*/
	public boolean isPass(final URFResource resource)
	{
		return true;
	}

}

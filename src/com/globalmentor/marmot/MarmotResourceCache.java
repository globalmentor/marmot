/*
 * Copyright © 1996-2009 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import java.io.*;
import java.net.URI;

import com.globalmentor.cache.Cache;
import com.globalmentor.marmot.repository.Repository;

/**A manager of cached Marmot resources.
@param <Q> The type of query used to request data from the cache.
@author Garret Wilson
*/
public interface MarmotResourceCache<Q> extends Cache<Q, File>
{
	
	/**Retrieves a value from the cache.
	Values are fetched from the backing store if needed, and this method blocks until the data is fetched.
	@param repository The repository in which the resource is stored.
	@param resourceURI The URI of the resource.
	@return The cached value.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IOException if there was an error fetching the value from the backing store.
	@see #get(Object)
	*/
	public File get(final Repository repository, final URI resourceURI) throws IOException;
	
	/**Retrieves a value from the cache.
	Values are fetched from the backing store if needed, with fetching optionally deferred until later.
	@param repository The repository in which the resource is stored.
	@param resourceURI The URI of the resource.
	@param deferFetch Whether fetching, if needed, should be deffered and performed in an asynchronous thread.
	@return The cached value, or <code>null</code> if fetching was deferred.
	@exception NullPointerException if the given repository and/or resource URI is <code>null</code>.
	@exception IOException if there was an error fetching the value from the backing store.
	@see #get(Object, boolean)
	*/
	public File get(final Repository repository, final URI resourceURI, final boolean deferFetch) throws IOException;

}

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

package com.globalmentor.marmot.resource;

import java.io.*;

import org.urframework.URFResource;

import com.globalmentor.net.ResourceIOException;

/**A resource filter that also provides a file-based filtering method for more effecient filtering.
Resource file filters must still support stream-based filtering.
@author Garret Wilson
*/
public interface ResourceFileContentFilter extends ResourceContentFilter
{

	/**Performs a filtering operation on a resource.
	@param resource The description of the resource.
	@param inputFile The source of the resource content to filter.
	@param outputFile The destination of the filtered resource content.
	@return The new description of the filtered resource content.
	@throws ResourceIOException if there is an error filtering the resource.
	*/
	public URFResource filter(final URFResource resource, final File inputFile, final File outputFile) throws ResourceIOException;
}

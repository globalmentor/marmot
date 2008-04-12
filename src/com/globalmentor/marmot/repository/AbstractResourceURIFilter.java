package com.globalmentor.marmot.repository;

import java.net.URI;

import com.globalmentor.urf.URFResource;

/**A resource filter that filters on resource URI.
This implementation of {@link #isPass(URFResource)} does no filtering at all, as filtering on URI will already have occurred in {@link #isPass(URI)}.
@author Garret Wilson
*/
public abstract class AbstractResourceURIFilter implements ResourceFilter
{

	/**Determines whether a given resource should pass through the filter or be filtered out.
	This implementation unconditionally returns <code>true</code>.
	@param resource The resource to filter.
	@return <code>true</code> if the resource should pass through the filter, else
		<code>false</code> if the resource should be filtered out.
	*/
	public boolean isPass(final URFResource resource)
	{
		return true;
	}

}

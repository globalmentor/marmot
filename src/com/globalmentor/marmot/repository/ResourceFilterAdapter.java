package com.globalmentor.marmot.repository;

import java.net.URI;

import com.globalmentor.urf.URFResource;

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

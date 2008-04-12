package com.globalmentor.marmot.repository;

import java.net.URI;

import static com.globalmentor.net.URIs.*;

import com.globalmentor.net.URIs;
import com.globalmentor.urf.URFResource;
import com.globalmentor.util.Filter;

/**A resource filter that allows two levels of filtering: one on the resource URI, and one on the resource description in general.
An operation using the filter will typically filter based upon the resource URI using {@link #isPass(URI)}
before filtering on the resource description using {@link #isPass(URFResource)}.
Implementations of this interface may assume that {@link #isPass(URI)} is called before {@link #isPass(URFResource)}
and need not provide URI-based filtering inside {@link #isPass(URFResource)} if that has been done in {@link #isPass(URI)}.
@author Garret Wilson
*/
public interface ResourceFilter extends Filter<URFResource>
{

	/**The filter that only allows collection resource URIs.
	@see URIs#isCollectionURI(URI)
	*/
	public final static ResourceFilter COLLECTION_RESOURCE_URI_FILTER=new AbstractResourceURIFilter()
		{
			public boolean isPass(final URI resourceURI) {return isCollectionURI(resourceURI);}			
		};
	
	/**The filter that only allows non-collection resource URIs.
	@see URIs#isCollectionURI(URI)
	*/
	public final static ResourceFilter NON_COLLECTION_RESOURCE_URI_FILTER=new AbstractResourceURIFilter()
		{
			public boolean isPass(final URI resourceURI) {return !isCollectionURI(resourceURI);}			
		};
	
	/**Determines whether a given resource should pass through the filter or be filtered out based upon its URI.
	@param resourceURI The resource URI to filter.
	@return <code>true</code> if the resource should pass through the filter based upon its URI, else
		<code>false</code> if the resource should be filtered out.
	*/
	public boolean isPass(final URI resourceURI);

}

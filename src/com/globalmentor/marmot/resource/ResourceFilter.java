package com.globalmentor.marmot.resource;

import java.io.*;

import com.garretwilson.net.ResourceIOException;
import com.globalmentor.urf.URFResource;

/**An object that can filter resource descriptions and content.
@author Garret Wilson
*/
public interface ResourceFilter
{

	/**Performs a filtering operation on a resource.
	@param description The description of the resource.
	@param inputStream The source of the resource content to filter.
	@param outputStream The destination of the filtered resource content.
	@return The new description of the filtered resource content.
	@exception ResourceIOException if there is an error filtering the resource.
	*/
	public URFResource filter(final URFResource description, final InputStream inputStream, final OutputStream outputStream) throws IOException;
}

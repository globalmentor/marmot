package com.globalmentor.marmot.resource;

import java.io.*;

import com.globalmentor.net.ResourceIOException;
import com.globalmentor.urf.URFResource;

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
	@exception ResourceIOException if there is an error filtering the resource.
	*/
	public URFResource filter(final URFResource resource, final File inputFile, final File outputFile) throws ResourceIOException;
}

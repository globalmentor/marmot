package com.globalmentor.marmot.repository;

import java.net.URI;

import javax.mail.internet.ContentType;

import com.globalmentor.urf.URFResource;
import com.globalmentor.urf.content.Content;
import static com.globalmentor.util.Arrays.*;

/**A resource filter that filters on resource content type.
This class automatically filters on collection and non-collection status.
A subclass may override {@link #isPass(URI)} for more specific URI filtering, but should first call the parent version and return <code>false</code> if the URI does not pass.
Constructing this filter with no specified content types will not pass any resources.
This filter does not pass resources with no content type specified.
@author Garret Wilson
@see Content#getContentType(URFResource)
@see ContentType#match(ContentType)
*/
public class ResourceContentTypeFilter extends AbstractResourceFilter
{

	/**The allowed content types.*/
	private final ContentType[] contentTypes;

		/**@return The allowed content types.*/
		public ContentType[] getContentTypes() {return contentTypes.clone();}

	/**Content types constructor which passes both collections and non-collections.
	@param contentTypes The passed content types.
	@throws NullPointerException if the given content types or any content type is <code>null</code>.
	*/
	public ResourceContentTypeFilter(final ContentType... contentTypes)
	{
		this(true, contentTypes);	//allow collections
	}

	/**Collection pass and content types constructor which passes non-collections.
	@param collectionPass Whether collection resources are allowed to pass.
	@param contentTypes The passed content types.
	@throws NullPointerException if the given content types or any content type is <code>null</code>.
	*/
	public ResourceContentTypeFilter(final boolean collectionPass, final ContentType... contentTypes)
	{
		this(collectionPass, true, contentTypes);	//allow non-collections
	}

	/**Collection pass, non-collection pass, and content types constructor.
	@param collectionPass Whether collection resources are allowed to pass.
	@param nonCollectionPass Whether non collection resources are allowed to pass.
	@param contentTypes The passed content types.
	@throws NullPointerException if the given content types or any content type is <code>null</code>.
	*/
	public ResourceContentTypeFilter(final boolean collectionPass, final boolean nonCollectionPass, final ContentType... contentTypes)
	{
		super(collectionPass, nonCollectionPass);	//construct the parent class
		this.contentTypes=checkInstances(contentTypes).clone();	//create a copy of content types to pass
	}

	/**Determines whether a given resource should pass through the filter or be filtered out.
	This implementation only passes resources with a content type that matches at least one of the provided content types.
	@param resource The resource to filter.
	@return <code>true</code> if the resource should pass through the filter, else
		<code>false</code> if the resource should be filtered out.
	*/
	public boolean isPass(final URFResource resource)
	{
		boolean contentTypePass=false;
		final ContentType contentType=Content.getContentType(resource);	//get the content type of the resource
		if(contentType!=null)	//if the resource has a content type
		{
			for(final ContentType passContentType:contentTypes)	//for each content type to pass
			{
				if(passContentType.match(contentType))	//if the content type matches
				{
					contentTypePass=true;	//we found a matching content type
					break;	//stop checking
				}
			}
		}
		return contentTypePass;	//return whether at least one content type passed 
	}

}

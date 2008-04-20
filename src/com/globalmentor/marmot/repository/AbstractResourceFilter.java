package com.globalmentor.marmot.repository;

import java.net.URI;

import static com.globalmentor.net.URIs.*;

/**An abstract implementation of a resource filter that provides functionality for determining if collections and non-collections should be passed.
@author Garret Wilson
*/
public abstract class AbstractResourceFilter implements ResourceFilter
{

	/**Whether collection resources are allowed to pass.*/
	private final boolean collectionPass;

		/**@return Whether collection resources are allowed to pass.*/
		public boolean isCollectionPass() {return collectionPass;}

	/**Whether non collection resources are allowed to pass.*/
	private final boolean nonCollectionPass;

		/**@return Whether non collection resources are allowed to pass.*/
		public boolean isNonCollectionPass() {return nonCollectionPass;}

	/**Default constructor which passes both collections and non-collections.*/
	public AbstractResourceFilter()
	{
		this(true);	//allow collections
	}

	/**Collection pass constructor which passes non-collections.
	@param collectionPass Whether collection resources are allowed to pass.
	*/
	public AbstractResourceFilter(final boolean collectionPass)
	{
		this(collectionPass, true);	//allow non-collections
	}

	/**Collection and non-collection pass constructor.
	@param collectionPass Whether collection resources are allowed to pass.
	@param nonCollectionPass Whether non collection resources are allowed to pass.
	*/
	public AbstractResourceFilter(final boolean collectionPass, final boolean nonCollectionPass)
	{
		this.collectionPass=collectionPass;
		this.nonCollectionPass=nonCollectionPass;
	}

	/**Determines whether a given resource should pass through the filter or be filtered out based upon its URI.
	This version rejects a resource if its collection or non-collection status is not allowed.
	@param resourceURI The resource URI to filter.
	@return <code>true</code> if the resource should pass through the filter based upon its URI, else
		<code>false</code> if the resource should be filtered out.
	@see #isCollectionPass()
	@see #isNonCollectionPass()
	*/
	public boolean isPass(final URI resourceURI)
	{
		if(isCollectionURI(resourceURI))	//if this is a collection URI
		{
			if(!isCollectionPass())	//if collections shouldn't pass
			{
				return false;
			}
		}
		else	//if this is a non-collection URI
		{
			if(!isNonCollectionPass())	//if non-collections shouldn't pass
			{
				return false;
			}
		}
		return true;	//this resource met all the criteria
	}

}

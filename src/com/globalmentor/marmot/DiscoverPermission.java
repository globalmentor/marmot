package com.globalmentor.marmot;

import java.net.URI;

/**The discover permission.
@author Garret Wilson
*/
public class DiscoverPermission extends AbstractPermission
{

	/**Default constructor.*/
	public DiscoverPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public DiscoverPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
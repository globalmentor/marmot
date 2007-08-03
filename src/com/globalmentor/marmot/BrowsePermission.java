package com.globalmentor.marmot;

import java.net.URI;

/**The browse permission.
@author Garret Wilson
*/
public class BrowsePermission extends AbstractPermission
{

	/**Default constructor.*/
	public BrowsePermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public BrowsePermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
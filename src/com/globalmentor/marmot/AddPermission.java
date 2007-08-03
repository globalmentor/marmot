package com.globalmentor.marmot;

import java.net.URI;

/**The add permission.
@author Garret Wilson
*/
public class AddPermission extends AbstractPermission
{

	/**Default constructor.*/
	public AddPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public AddPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
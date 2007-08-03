package com.globalmentor.marmot;

import java.net.URI;

/**The subtract permission.
@author Garret Wilson
*/
public class SubtractPermission extends AbstractPermission
{

	/**Default constructor.*/
	public SubtractPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public SubtractPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
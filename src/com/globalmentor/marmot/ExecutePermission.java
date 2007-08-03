package com.globalmentor.marmot;

import java.net.URI;

/**The execute permission.
@author Garret Wilson
*/
public class ExecutePermission extends AbstractPermission
{

	/**Default constructor.*/
	public ExecutePermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public ExecutePermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
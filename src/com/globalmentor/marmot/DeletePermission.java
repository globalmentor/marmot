package com.globalmentor.marmot;

import java.net.URI;

/**The delete permission.
@author Garret Wilson
*/
public class DeletePermission extends AbstractPermission
{

	/**Default constructor.*/
	public DeletePermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public DeletePermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
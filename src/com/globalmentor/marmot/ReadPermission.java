package com.globalmentor.marmot;

import java.net.URI;

/**The read permission.
@author Garret Wilson
*/
public class ReadPermission extends AbstractPermission
{

	/**Default constructor.*/
	public ReadPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public ReadPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
package com.globalmentor.marmot;

import java.net.URI;

/**The rename permission.
@author Garret Wilson
*/
public class RenamePermission extends AbstractPermission
{

	/**Default constructor.*/
	public RenamePermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public RenamePermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
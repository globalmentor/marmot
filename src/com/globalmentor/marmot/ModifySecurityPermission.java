package com.globalmentor.marmot;

import java.net.URI;

/**The modify security permission.
@author Garret Wilson
*/
public class ModifySecurityPermission extends AbstractPermission
{

	/**Default constructor.*/
	public ModifySecurityPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public ModifySecurityPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
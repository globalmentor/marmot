package com.globalmentor.marmot;

import java.net.URI;

/**The modify properties permission.
@author Garret Wilson
*/
public class ModifyPropertiesPermission extends AbstractPermission
{

	/**Default constructor.*/
	public ModifyPropertiesPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public ModifyPropertiesPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
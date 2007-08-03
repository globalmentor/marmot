package com.globalmentor.marmot;

import java.net.URI;

/**The preview permission.
@author Garret Wilson
*/
public class PreviewPermission extends AbstractPermission
{

	/**Default constructor.*/
	public PreviewPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public PreviewPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
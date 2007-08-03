package com.globalmentor.marmot;

import java.net.URI;

/**The preview access level.
@author Garret Wilson
*/
public class PreviewAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public PreviewAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public PreviewAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
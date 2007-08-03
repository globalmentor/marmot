package com.globalmentor.marmot;

import java.net.URI;

/**The edit access level.
@author Garret Wilson
*/
public class EditAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public EditAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public EditAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
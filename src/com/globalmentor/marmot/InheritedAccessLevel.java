package com.globalmentor.marmot;

import java.net.URI;

/**The inherited access level.
@author Garret Wilson
*/
public class InheritedAccessLevel extends AbstractAccessLevel
{

	/**Default constructor.*/
	public InheritedAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public InheritedAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
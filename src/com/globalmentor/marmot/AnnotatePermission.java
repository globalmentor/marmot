package com.globalmentor.marmot;

import java.net.URI;

/**The annotate permission.
@author Garret Wilson
*/
public class AnnotatePermission extends AbstractPermission
{

	/**Default constructor.*/
	public AnnotatePermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public AnnotatePermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}
}
package com.globalmentor.marmot.security;

import java.net.URI;

/**The annotate permission.
@author Garret Wilson
*/
public class AnnotatePermission extends AbstractPermission
{

	/**Default constructor.*/
	public AnnotatePermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public AnnotatePermission(final URI uri)
	{
		super(uri);  //construct the parent class
	}
}
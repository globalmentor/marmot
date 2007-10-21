package com.globalmentor.marmot.security;

import java.net.URI;

import com.garretwilson.urf.AbstractClassTypedURFResource;

import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**An abstract resource in the Marmot security namespace, {@value MarmotSecurity#MARMOT_SECURITY_NAMESPACE_URI}.
@author Garret Wilson
*/
public abstract class AbstractMarmotSecurityResource extends AbstractClassTypedURFResource
{

	/**Default constructor.*/
	public AbstractMarmotSecurityResource()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	*/
	public AbstractMarmotSecurityResource(final URI uri)
	{
		super(uri, MARMOT_SECURITY_NAMESPACE_URI);  //construct the parent class
	}
}
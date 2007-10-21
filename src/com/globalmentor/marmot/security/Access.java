package com.globalmentor.marmot.security;

import java.net.URI;
import java.util.Collection;

import com.garretwilson.urf.*;
import static com.garretwilson.urf.URF.*;

import static com.globalmentor.marmot.security.MarmotSecurity.*;

/**Specifies access for a resource.
@author Garret Wilson
*/
public class Access extends AbstractMarmotSecurityResource
{

	/**Default constructor.*/
	public Access()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public Access(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}

	/**Returns this access description's access rules.
	@return This access description's access rules, or <code>null</code> if there is no access rules property or the value is not an {@link URFListResource}.
	@see MarmotSecurity#ACCESS_RULES_PROPERTY_URI
	*/
	public URFListResource<AccessRule> getAccessRules()
	{
		return asListInstance(getPropertyValue(ACCESS_RULES_PROPERTY_URI));	//return the security:accessRules property
	}

	/**Sets the access rules.
	@param accessRules The access rules to set.
	@see MarmotSecurity#ACCESS_RULES_PROPERTY_URI
	*/
	public void setAccessRules(final Collection<AccessRule> accessRules)
	{
		setPropertyValue(ACCESS_RULES_PROPERTY_URI, URFListResource.toListResource(accessRules));	//set the security:accessRules property
	}
	
}
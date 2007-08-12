package com.globalmentor.marmot;

import java.net.URI;
import java.util.Collection;

import com.garretwilson.rdf.RDFListResource;

import static com.globalmentor.marmot.Marmot.*;

/**Specifies access for a resource.
@author Garret Wilson
*/
public class Access extends AbstractMarmotResource
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

	/**@return This access description's access rules.
	@exception ClassCastException if the value of <code>marmot:accessRules</code> is not an {@link RDFListResource}.*/
	public RDFListResource getAccessRules()	//TODO do something better to allow the list resource to be generically correct
	{
		return (RDFListResource)getPropertyValue(MARMOT_NAMESPACE_URI, ACCESS_RULES_PROPERTY_NAME);	//return the marmot:accessRules
	}

	/**Sets the access rules.
	@param accessRules The access rules to set.
	@return The access rule list used as the set value.
	*/
	public RDFListResource setAccessRules(final Collection<AccessRule> accessRules)
	{
		final RDFListResource accessRuleList=new RDFListResource();	//create a new RDF list resource
		accessRuleList.addAll(accessRules);	//add all the access rules to the list
		setProperty(MARMOT_NAMESPACE_URI, ACCESS_RULES_PROPERTY_NAME, accessRuleList);	//set the marmot:accessRules
		return accessRuleList;	//return the access rule list used
	}
	
}
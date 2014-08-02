/*
 * Copyright © 1996-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.marmot.security;

import java.net.URI;
import java.util.Collection;

import org.urframework.*;
import org.urframework.select.UniversalSelector;



import static com.globalmentor.java.Classes.*;
import static com.globalmentor.marmot.security.MarmotSecurity.*;
import static org.urframework.URF.*;

/**Specifies access for a resource.
@author Garret Wilson
*/
public class Access extends URFListResource<AccessRule>
{

	/**Default constructor.*/
	public Access()
	{
		this((URI)null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public Access(final URI referenceURI)
	{
		super(referenceURI, createResourceURI(MARMOT_SECURITY_NAMESPACE_URI, getLocalName(Access.class)));  //construct the parent class, using a type based upon the name of this class
	}

	/**Collection constructor with no URI.
	The elements of the specified collection will be added to this list in the order they are returned by the collection's iterator.
	@param collection The collection whose elements are to be placed into this list.
	@throws NullPointerException if the specified collection is <code>null</code>.
	*/
	public Access(final Collection<? extends AccessRule> collection)
	{
		this(null, collection);	//construct the class with no URI
	}

	/**URI and collection constructor.
	The elements of the specified collection will be added to this list in the order they are returned by the collection's iterator.
	@param uri The URI for the resource, or <code>null</code> if the resource should have no URI.
	@param collection The collection whose elements are to be placed into this list.
	@throws NullPointerException if the specified collection is <code>null</code>.
	*/
	public Access(final URI uri, final Collection<? extends AccessRule> collection)
	{
		this(uri);	//construct the class with the URI
		addAll(collection);	//add all the collection elements to the list
	}

	/**Returns an access level type that represents a summary of the access.
	If there is no access rule, an access level type of {@link AccessLevelType#INHERITED} will be returned.
	If there is a single access rule with a universal selector and an access level, its access level type will be returned.
	Otherwise, {@link AccessLevelType#CUSTOM} will be returned. 
	@return an access level type representing a summary of the access.
	*/
	public AccessLevelType getSummaryAccessLevelType()
	{
		if(isEmpty())	//if there are no access rules
		{
			return AccessLevelType.INHERITED;
		}
		else if(size()==1)	//if there is only one access rule
		{
			final AccessRule accessRule=get(0);	//get the access rule
			if(accessRule.getSelector() instanceof UniversalSelector)	//if this is the universal selector
			{
				final AccessLevel accessLevel=accessRule.getAccessLevel();	//get the access level
				if(accessLevel!=null)	//if there is an access level
				{
					return accessLevel.getAccessLevelType();	//return the access level type
				}
			}
		}
		return AccessLevelType.CUSTOM;	//anything else is considered a custom acces level
	}

}
package com.globalmentor.marmot.security;

import java.net.URI;
import java.util.Collection;

import static com.garretwilson.lang.ClassUtilities.*;
import com.garretwilson.urf.*;
import static com.garretwilson.urf.URF.*;

import static com.globalmentor.marmot.security.MarmotSecurity.*;

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
	@exception NullPointerException if the specified collection is <code>null</code>.
	*/
	public Access(final Collection<? extends AccessRule> collection)
	{
		this(null, collection);	//construct the class with no URI
	}

	/**URI and collection constructor.
	The elements of the specified collection will be added to this list in the order they are returned by the collection's iterator.
	@param uri The URI for the resource, or <code>null</code> if the resource should have no URI.
	@param collection The collection whose elements are to be placed into this list.
	@exception NullPointerException if the specified collection is <code>null</code>.
	*/
	public Access(final URI uri, final Collection<? extends AccessRule> collection)
	{
		this(uri);	//construct the class with the URI
		addAll(collection);	//add all the collection elements to the list
	}

}
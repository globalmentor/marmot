package com.globalmentor.marmot;

import java.net.URI;

import static com.garretwilson.rdf.RDFUtilities.*;

import com.globalmentor.marmot.security.PermissionType;

/**An abstract implementation of a permission.
@author Garret Wilson
*/
public abstract class AbstractPermission extends AbstractMarmotResource implements Permission
{

	/**The permission type this permission represents.*/
	private final PermissionType permissionType;

		/**The permission type this permission represents.*/
		public PermissionType getPermissionType() {return permissionType;}

	/**Default constructor.*/
	public AbstractPermission()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	@exception IllegalArgumentException if this class does not correspond to an existing {@link PermissionType}.
	*/
	public AbstractPermission(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
		this.permissionType=PermissionType.getPermissionType(getType(this).getURI());	//determine the permission type
	}
}
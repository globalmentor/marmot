package com.globalmentor.marmot.security;

import java.net.URI;

/**An abstract implementation of a permission.
@author Garret Wilson
*/
public abstract class AbstractPermission extends AbstractMarmotSecurityResource implements Permission
{

	/**The permission type this permission represents.*/
	private final PermissionType permissionType;

		/**The permission type this permission represents.*/
		public PermissionType getPermissionType() {return permissionType;}

	/**Default constructor.*/
	public AbstractPermission()
	{
		this(null);	//construct the class with no URI
	}

	/**URI constructor.
	@param uri The URI for the new resource.
	@exception IllegalArgumentException if this class does not correspond to an existing {@link PermissionType}.
	*/
	public AbstractPermission(final URI uri)
	{
		super(uri);  //construct the parent class
		this.permissionType=PermissionType.getPermissionType(getTypeURI());	//determine the permission type
	}
}
package com.globalmentor.marmot;

import java.net.URI;
import java.util.Set;

import static com.garretwilson.rdf.RDFUtilities.*;

import static com.globalmentor.marmot.MarmotConstants.*;

import com.globalmentor.marmot.security.AccessLevelType;
import com.globalmentor.marmot.security.PermissionType;

/**An abstract implementation of an access level.
@author Garret Wilson
*/
public abstract class AbstractAccessLevel extends AbstractMarmotResource implements AccessLevel
{

	/**The access level type this access level represents.*/
	private final AccessLevelType accessLevelType;

		/**The access level type this access level represents.*/
		public AccessLevelType getAccessLevelType() {return accessLevelType;}

	/**Default constructor.*/
	public AbstractAccessLevel()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	@exception IllegalArgumentException if this class does not correspond to an existing {@link AccessLevelType}.
	*/
	public AbstractAccessLevel(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
		this.accessLevelType=AccessLevelType.getAccessLevelType(getType(this).getReferenceURI());	//determine the access level type
	}

	/**@return This access level's allowed permissions.*/
	public Iterable<Permission> getAllows()
	{
		return getPropertyValues(MARMOT_NAMESPACE_URI, ALLOW_PROPERTY_NAME, Permission.class);	//return the marmot:allow values
	}

	/**Adds a particular permission as allowed.
	@param permission The permission to be allowed.
	*/
	public void addAllow(final Permission permission)
	{
		addProperty(MARMOT_NAMESPACE_URI, ALLOW_PROPERTY_NAME, permission);	//add the marmot:allow property value
	}

	/**Adds a particular permission as allowed.
	@param permissionType The type of permission to be allowed.
	*/
	public void addAllow(final PermissionType permissionType)
	{
		addAllow(createPermission(permissionType));	//create a permission and allow it
	}

	/**Sets the allowed permissions to those specified.
	@param permissions The permissions that should be allowed.
	*/
	public void setAllowedPermissions(final Set<Permission> permissions)
	{
		removeProperties(MARMOT_NAMESPACE_URI, ALLOW_PROPERTY_NAME);	//remove all the "allow" properties
		for(final Permission permission:permissions)	//for each permission
		{
			addAllow(permission);	//allow this permission
		}
	}

	/**Sets the allowed permissions to those specified.
	@param permissionTypes The permission types that should be allowed.
	*/
	public void setAllowedPermissionTypes(final Set<PermissionType> permissionTypes)
	{
		removeProperties(MARMOT_NAMESPACE_URI, ALLOW_PROPERTY_NAME);	//remove all the "allow" properties
		for(final PermissionType permissionType:permissionTypes)	//for each permission
		{
			addAllow(createPermission(permissionType));	//create a permission and allow it
		}
	}

	/**@return This access level's denied permissions.*/
	public Iterable<Permission> getDenies()
	{
		return getPropertyValues(MARMOT_NAMESPACE_URI, DENY_PROPERTY_NAME, Permission.class);	//return the marmot:deny values
	}

	/**Adds a particular permission as denied.
	@param permission The permission to be denied.
	*/
	public void addDeny(final Permission permission)
	{
		addProperty(MARMOT_NAMESPACE_URI, DENY_PROPERTY_NAME, permission);	//add the marmot:deny property value
	}

	/**Adds a particular permission as denied.
	@param permissionType The type of permission to be denied.
	*/
	public void addDeny(final PermissionType permissionType)
	{
		addDeny(createPermission(permissionType));	//create a permission and deny it
	}

	/**Sets the denied permissions to those specified.
	@param permissions The permissions that should be denied.
	*/
	public void setDeniedPermissions(final Set<Permission> permissions)
	{
		removeProperties(MARMOT_NAMESPACE_URI, DENY_PROPERTY_NAME);	//remove all the "deny" properties
		for(final Permission permission:permissions)	//for each permission
		{
			addDeny(permission);	//deny this permission
		}
	}

	/**Sets the denied permissions to those specified.
	@param permissionTypes The permission types that should be denied.
	*/
	public void setDeniedPermissionTypes(final Set<PermissionType> permissionTypes)
	{
		removeProperties(MARMOT_NAMESPACE_URI, DENY_PROPERTY_NAME);	//remove all the "deny" properties
		for(final PermissionType permissionType:permissionTypes)	//for each permission
		{
			addDeny(createPermission(permissionType));	//create a permission and deny it
		}
	}

	/**Creates an permission based upon an permission type.
	@param permissionType The permission type for which an permission should be created.
	@return A new permission for the given access type.
	@exception NullPointerException if the given access type is <code>null</code>.
	*/
	public static Permission createPermission(final PermissionType permissionType)
	{
		switch(permissionType)	//see which permission type this is, and return the correct permission instance
		{
			case DISCOVER:
				return new DiscoverPermission();
			case BROWSE:
				return new BrowsePermission();
			case ANNOTATE:
				return new AnnotatePermission();
			case PREVIEW:
				return new PreviewPermission();
			case EXECUTE:
				return new ExecutePermission();
			case READ:
				return new ReadPermission();
			case MODIFY_PROPERTIES:
				return new ModifyPropertiesPermission();
			case MODIFY_SECURITY:
				return new ModifySecurityPermission();
			case RENAME:
				return new RenamePermission();
			case ADD:
				return new AddPermission();
			case SUBTRACT:
				return new SubtractPermission();
			case DELETE:
				return new DeletePermission();
			default:
				throw new AssertionError("Unrecognized access level type: "+permissionType);
		}
	}

}
package com.globalmentor.marmot;

import java.net.URI;

import com.globalmentor.marmot.security.AccessLevelType;

import static com.globalmentor.marmot.MarmotConstants.*;

/**Specifies a rule for access to a resource.
@author Garret Wilson
*/
public class AccessRule extends AbstractMarmotResource
{

	/**Default constructor.*/
	public AccessRule()
	{
		this(null);	//construct the class with no reference URI
	}

	/**Reference URI constructor.
	@param referenceURI The reference URI for the new resource.
	*/
	public AccessRule(final URI referenceURI)
	{
		super(referenceURI);  //construct the parent class
	}

	/**@return This rule's select declaration, or <code>null</code> if this rule has no <code>marmot:select</code> property.
	@exception ClassCastException if the value of the <code>marmot:select</code> property is not a {@link Selector}.
	*/
	public Selector getSelect() throws ClassCastException
	{
		return (Selector)getPropertyValue(MARMOT_NAMESPACE_URI, SELECT_PROPERTY_NAME);	//return the marmot:select value
	}

	/**Sets this rule's select declaration.
	@param selector This rule's select declaration, or <code>null</code> if this rule should have no <code>marmot:select</code> property.
	*/
	public void setSelect(final Selector selector)
	{
		setProperty(MARMOT_NAMESPACE_URI, SELECT_PROPERTY_NAME, selector);	//set the marmot:select property
	}

	/**@return This rule's access level, or <code>null</code> if this rule has no <code>marmot:accessLevel</code> property.
	@exception ClassCastException if the value of the <code>marmot:accessLevel</code> property is not an {@link AccessLevel}.
	*/
	public AccessLevel getAccessLevel() throws ClassCastException
	{
		return (AccessLevel)getPropertyValue(MARMOT_NAMESPACE_URI, ACCESS_LEVEL_PROPERTY_NAME);	//return the marmot:accessLevel value
	}

	/**Sets this rule's access level.
	@param accessLevel This rule's access level, or <code>null</code> if this rule should have no <code>marmot:accessLevel</code> property.
	*/
	public void setAccessLevel(final AccessLevel accessLevel)
	{
		setProperty(MARMOT_NAMESPACE_URI, ACCESS_LEVEL_PROPERTY_NAME, accessLevel);	//set the marmot:accessLevel property
	}

	/**Sets this rule's access level based upon an access level type.
	@param accessLevelType This rule's access level, or <code>null</code> if this rule should have no <code>marmot:accessLevel</code> property.
	@return The access level value used.
	*/
	public AccessLevel setAccessLevel(final AccessLevelType accessLevelType)
	{
		final AccessLevel accessLevel=accessLevelType!=null ? createAccessLevel(accessLevelType) : null;	//create the appropriate type of access level
		setAccessLevel(accessLevel);	//set the access level
		return accessLevel;	//return the access level we used
	}

	/**Creates an access level based upon an access level type.
	@param accessLevelType The access level type for which an access level should be created.
	@return A new access level for the given access level type.
	@exception NullPointerException if the given access level type is <code>null</code>.
	*/
	public static AccessLevel createAccessLevel(final AccessLevelType accessLevelType)
	{
		switch(accessLevelType)	//see which access level type this is, and return the correct access level instance
		{
			case INHERITED:
				return new InheritedAccessLevel();
			case CUSTOM:
				return new CustomAccessLevel();
			case PRIVATE:
				return new PrivateAccessLevel();
			case STEALTH:
				return new StealthAccessLevel();
			case PREVIEW:
				return new PreviewAccessLevel();
			case USE:
				return new UseAccessLevel();
			case RETRIEVE:
				return new RetrieveAccessLevel();
			case EDIT:
				return new EditAccessLevel();
			case FULL:
				return new FullAccessLevel();
			default:
				throw new AssertionError("Unrecognized access level type: "+accessLevelType);
		}
	}

}
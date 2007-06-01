package com.globalmentor.marmot;

import com.globalmentor.marmot.resource.*;

/**A default Marmot session
@param <RK> The type of resource kits supported by this session.
@author Garret Wilson
*/
public class DefaultMarmotSession<RK extends ResourceKit> extends AbstractMarmotSession<RK>
{

	/**Default constructor.*/
	public DefaultMarmotSession()
	{
	}

}

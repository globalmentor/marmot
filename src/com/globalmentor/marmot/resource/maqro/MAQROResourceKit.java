/*
 * Copyright © 2003-2008 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.resource.maqro;

import javax.mail.internet.ContentType;

import com.globalmentor.marmot.resource.*;
import com.globalmentor.urf.maqro.MAQRO;
import static com.globalmentor.urf.maqro.MAQRO.*;

/**Resource kit for handling mentoring activities and interactions.
<p>Supported media types:</p>
<ul>
	<li>{@value MAQRO#MENTOR_ACTIVITY_CONTENT_TYPE}</li>
</ul>
@author Garret Wilson
*/
public class MAQROResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public MAQROResourceKit()
	{
		super(new ContentType[]{MENTOR_ACTIVITY_CONTENT_TYPE});
	}

}

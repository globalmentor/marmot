/*
 * Copyright © 2009 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.resource.jnlp;

import com.globalmentor.jnlp.JNLP;
import com.globalmentor.marmot.resource.*;

/**Resource kit for handling Java applications.
<p>Supported media types:</p>
<ul>
	<li><code>application/x-java-jnlp-file</code></li>
</ul>
@author Garret Wilson
*/
public class JNLPResourceKit extends AbstractResourceKit
{

	/**Default constructor.*/
	public JNLPResourceKit()
	{
		super(JNLP.CONTENT_TYPE);
	}

}

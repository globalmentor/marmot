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

package com.globalmentor.marmot.resource;

import com.globalmentor.marmot.resource.AbstractResourceKit;
import com.globalmentor.net.ContentType;

/**
 * A default resource kit which can be used for most resources.
 * @author Garret Wilson
 */
public class DefaultResourceKit extends AbstractResourceKit {

	/** Default constructor. */
	public DefaultResourceKit() {
		super(new ContentType[] {});
	}

}

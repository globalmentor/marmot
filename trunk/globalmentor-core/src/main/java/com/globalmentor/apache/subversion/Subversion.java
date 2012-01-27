/*
 * Copyright © 2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.apache.subversion;

import java.util.regex.Pattern;

/**
 * Constants and utilities for working with Apache Subversion.
 * 
 * @author Garret Wilson
 * 
 *         <a href="http://subversion.apache.org/">Apache Subversion</a>
 */
public class Subversion
{

	/**
	 * The pattern describing a valid Subversion property name as per <code>svn_prop_name_is_valid()</code>: (alpha/colon/underscore) character followed by any
	 * number of (alpha/digit/minus/dot/colon/underscore) characters.
	 */
	public final Pattern PROPERTY_NAME_PATTERN = Pattern.compile("^[\\p{Alpha}:_][\\p{Alpha}\\p{Digit}\\-.:_]*$");

}

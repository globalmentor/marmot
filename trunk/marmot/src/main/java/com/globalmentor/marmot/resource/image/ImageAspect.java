/*
 * Copyright © 1996-2012 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.resource.image;

import com.globalmentor.marmot.resource.ResourceAspect;

/**
 * Available images aspects.
 * @author Garret Wilson
 */
public enum ImageAspect implements ResourceAspect {
	/** Dimensions appropriate for presenting as a figure. */
	FIGURE,

	/** Dimensions appropriate for presenting as a preview. */
	PREVIEW,

	/** Dimensions appropriate for presenting as a thumbnail. */
	THUMBNAIL;
}
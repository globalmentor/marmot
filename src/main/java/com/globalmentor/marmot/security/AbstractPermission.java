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

package com.globalmentor.marmot.security;

import java.net.URI;

/**
 * An abstract implementation of a permission.
 * @author Garret Wilson
 */
public abstract class AbstractPermission extends AbstractMarmotSecurityResource implements Permission {

	/** The permission type this permission represents. */
	private final PermissionType permissionType;

	@Override
	public PermissionType getPermissionType() {
		return permissionType;
	}

	/** Default constructor. */
	public AbstractPermission() {
		this(null); //construct the class with no URI
	}

	/**
	 * URI constructor.
	 * @param uri The URI for the new resource.
	 * @throws IllegalArgumentException if this class does not correspond to an existing {@link PermissionType}.
	 */
	public AbstractPermission(final URI uri) {
		super(uri); //construct the parent class
		this.permissionType = PermissionType.getPermissionType(getTypeURI()); //determine the permission type
	}
}
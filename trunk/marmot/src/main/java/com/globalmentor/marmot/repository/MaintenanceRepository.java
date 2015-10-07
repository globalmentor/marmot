/*
 * Copyright © 1996-2013 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository;

/**
 * An implementation of a repository that supports maintenance activities.
 * <p>
 * These methods are optional; a maintenance repository may choose which methods to implement, throwing {@link UnsupportedOperationException} for those methods
 * not implemented.
 * </p>
 * @author Garret Wilson
 */
public interface MaintenanceRepository extends Repository {

	/**
	 * Whether this repository will rewrite every resource description it reads, based upon the underlying repository implementation.
	 * <p>
	 * This setting defaults to <code>false</code>.
	 * </p>
	 * @return Whether resource descriptions will be immediately rewritten when read.
	 */
	public boolean isRewriteResourceDescriptions();

	/**
	 * Sets whether this repository will rewrite every resource description it reads, based upon the underlying repository implementation.
	 * @param rewriteResourceDescriptions <code>true</code> if, upon each read of a resource description, the resource description should immediately be
	 *          rewritten.
	 */
	public void setRewriteResourceDescriptions(final boolean rewriteResourceDescriptions);
}

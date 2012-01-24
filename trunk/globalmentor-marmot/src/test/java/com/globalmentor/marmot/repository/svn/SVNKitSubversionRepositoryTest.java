/*
 * Copyright © 2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.marmot.repository.svn;

import static com.globalmentor.java.Conditions.*;

import java.io.File;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;

import com.globalmentor.marmot.repository.Repository;
import com.globalmentor.marmot.repository.file.AbstractFileRepositoryTest;
import com.globalmentor.marmot.repository.svn.svnkit.SVNKitSubversionRepository;

/**
 * Tests repositories using an SVNKit-based Subversion repository.
 * 
 * @author Garret Wilson
 * @see SVNKitSubversionRepository
 */
public class SVNKitSubversionRepositoryTest extends AbstractFileRepositoryTest
{

	/**
	 * {@inheritDoc}
	 * @see #getTempDirectory()
	 */
	@Override
	protected Repository createRepository()
	{
		final File tempDirectory = getTempDirectory();
		try
		{
			SVNRepositoryFactory.createLocalRepository(tempDirectory, true, false); //create the physical repository
		}
		catch(final SVNException svnException)
		{
			throw unexpected(svnException);
		}
		return new SVNKitSubversionRepository(tempDirectory); //return a repository object
	}

}

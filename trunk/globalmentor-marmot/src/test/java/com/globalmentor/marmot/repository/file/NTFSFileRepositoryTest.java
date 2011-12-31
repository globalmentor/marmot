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

package com.globalmentor.marmot.repository.file;

import static com.globalmentor.java.OperatingSystem.*;
import static org.junit.Assume.*;

import com.globalmentor.marmot.repository.Repository;

/**
 * Tests repositories using the NTFS file system.
 * 
 * @author Garret Wilson
 */
public class NTFSFileRepositoryTest extends AbstractFileRepositoryTest
{

	@Override
	public void before()
	{
		assumeTrue(isWindowsOS()); //only run this test on Windows TODO improve to actually check for the presence of an NTFS file system, which is more correct
		super.before();
	}

	/**
	 * {@inheritDoc}
	 * @see #getTempDirectory()
	 */
	@Override
	protected Repository createRepository()
	{
		return new NTFSFileRepository(getTempDirectory());
	}

}

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

import java.io.File;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.globalmentor.marmot.repository.AbstractRepositoryTest;

/**
 * Abstract base class for testing repositories based on files.
 * 
 * @author Garret Wilson
 */
public abstract class AbstractFileRepositoryTest extends AbstractRepositoryTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	/** @return A temporary directory valid for the current test. */
	protected File getTempDirectory() {
		return temporaryFolder.getRoot();
	}

}

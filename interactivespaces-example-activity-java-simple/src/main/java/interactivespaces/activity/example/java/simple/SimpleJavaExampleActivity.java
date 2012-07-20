/*
 * Copyright (C) 2012 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package interactivespaces.activity.example.java.simple;

import interactivespaces.activity.impl.BaseActivity;

/**
 * A sample Interactive Spaces Java-based activity.
 * 
 * @author Keith M. Hughes
 * @since Ap4 4, 2012
 */
public class SimpleJavaExampleActivity extends BaseActivity {


	@Override
	public void onActivityStartup() {
		getLog().info("Simple Java Example Activity startup");
	}

	@Override
	public void onActivityActivate() {
		getLog().info("Simple Java Example Activity activate");
	}

	@Override
	public void onActivityDeactivate() {
		getLog().info("Simple Java Example Activity deactivate");
	}

	@Override
	public void onActivityShutdown() {
		getLog().info("Simple Java Example Activity shutdown");
	}
}
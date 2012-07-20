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

package interactivespaces.workbench.activity.project.packager;

import interactivespaces.InteractiveSpacesException;
import interactivespaces.domain.basic.pojo.SimpleActivity;
import interactivespaces.workbench.activity.project.ActivityProject;
import interactivespaces.workbench.activity.project.ActivityProjectBuildContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A basic implementation of a {@link ActivityProjectPackager}.
 * 
 * @author Keith M. Hughes
 */
public class ActivityProjectPackagerImpl implements ActivityProjectPackager {

	private static final String BUILD_DIRECTORY = "build";
	private static final String PROJECT_BUILD_FILE_EXTENSION = "zip";

	@Override
	public void packageActivityProject(ActivityProject project,
			ActivityProjectBuildContext context) {
		// Create a buffer for reading the files
		byte[] buf = new byte[1024];

		ZipOutputStream out = null;
		try {
			// Create the ZIP file
			out = new ZipOutputStream(new FileOutputStream(
					getBuildDestinationFile(project)));

			writeZipEntry(buf, out, "", project.getActivityDescriptionFile());
			writeDistributionFile(project.getActivitySourceFolder(), buf, out,
					"");

			addArtifacts(context, buf, out);

			// Complete the ZIP file
			out.flush();
		} catch (IOException e) {
			throw new InteractiveSpacesException(
					"Failed writing Activity Build file", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Don't care
				}
			}
		}
	}

	/**
	 * Get all artifacts into the final file.
	 * 
	 * @param context
	 *            the build context
	 * @param buf
	 *            the shared buffer
	 * @param out
	 *            the distribution file stream being written to
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void addArtifacts(ActivityProjectBuildContext context,
			byte[] buf, ZipOutputStream out) throws FileNotFoundException,
			IOException {
		for (File artifact : context.getArtifactsToAdd()) {
			writeZipEntry(buf, out, "", artifact);
		}
	}

	/**
	 * Write out the contents of the folder to the distribution file.
	 * 
	 * @param activityFolder
	 *            folder being written to the build
	 * @param buf
	 *            a buffer for caching info
	 * @param packageOutputStream
	 *            the stream where components are being written
	 * @param parentPath
	 *            path up to this point
	 * @throws IOException
	 */
	private void writeDistributionFile(File activityFolder, byte[] buf,
			ZipOutputStream packageOutputStream, String parentPath)
			throws IOException {
		for (File file : activityFolder.listFiles()) {
			if (file.isDirectory()) {
				writeDistributionFile(file, buf, packageOutputStream,
						parentPath + file.getName() + "/");
			} else {
				writeZipEntry(buf, packageOutputStream, parentPath, file);
			}
		}
	}

	/**
	 * @param buf
	 * @param packageOutputStream
	 * @param parentPath
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	protected void writeZipEntry(byte[] buf,
			ZipOutputStream packageOutputStream, String parentPath, File file)
			throws FileNotFoundException, IOException {
		FileInputStream in = new FileInputStream(file);

		// Add ZIP entry to output stream.
		packageOutputStream.putNextEntry(new ZipEntry(parentPath
				+ file.getName()));

		// Transfer bytes from the file to the ZIP file
		int len;
		while ((len = in.read(buf)) > 0) {
			packageOutputStream.write(buf, 0, len);
		}

		// Complete the entry
		packageOutputStream.closeEntry();
		in.close();
	}

	/**
	 * Get the build destination file.
	 * 
	 * <p>
	 * Any subdirectories needed will be created.
	 * 
	 * @param project
	 *            the project being built
	 * 
	 * @return the file where the build should be written
	 */
	private File getBuildDestinationFile(ActivityProject project) {
		File buildFolder = new File(project.getBaseDirectory(), BUILD_DIRECTORY);

		if (!buildFolder.exists()) {
			if (!buildFolder.mkdirs()) {
				throw new InteractiveSpacesException(String.format(
						"Cannot create folder %s",
						buildFolder.getAbsolutePath()));
			}
		}

		SimpleActivity activity = project.getActivity();
		return new File(buildFolder, activity.getIdentifyingName() + "-"
				+ activity.getVersion() + "." + PROJECT_BUILD_FILE_EXTENSION);
	}
}
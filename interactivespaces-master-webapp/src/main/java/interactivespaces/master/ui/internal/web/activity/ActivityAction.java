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

package interactivespaces.master.ui.internal.web.activity;

import interactivespaces.InteractiveSpacesException;
import interactivespaces.SimpleInteractiveSpacesException;
import interactivespaces.domain.basic.pojo.SimpleActivity;
import interactivespaces.master.api.master.MasterApiActivityManager;
import interactivespaces.master.api.messages.MasterApiMessageSupport;
import interactivespaces.master.api.messages.MasterApiMessages;
import interactivespaces.master.ui.internal.web.BaseSpaceMasterController;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.execution.RequestContext;

import java.io.Serializable;
import java.util.Map;

/**
 * The webflow action for activity upload.
 *
 * @author Keith M. Hughes
 */
public class ActivityAction extends BaseSpaceMasterController {

  /**
   * Manager for UI operations on activities.
   */
  private MasterApiActivityManager masterApiActivityManager;

  /**
   * Get a new activity model.
   *
   * @return new activity form
   */
  public ActivityForm newActivity() {
    return new ActivityForm();
  }

  /**
   * Add entities to the flow context needed by the new entity page.
   *
   * @param context
   *          The Webflow context.
   */
  public void addNeededEntities(RequestContext context) {
    MutableAttributeMap viewScope = context.getViewScope();
    addGlobalModelItems(viewScope);
  }

  /**
   * Save the new activity.
   *
   * @param form
   *          activity form context
   *
   * @return status result
   */
  public String saveActivity(ActivityForm form) {
    try {
      Map<String, Object> activityResponse =
          masterApiActivityManager.saveActivity(form.getActivity(), form.getActivityFile().getInputStream());

      // So the ID gets copied out of the flow.
      if (MasterApiMessageSupport.isSuccessResponse(activityResponse)) {

        form.getActivity().setId(
            (String) MasterApiMessageSupport.getResponseDataMap(activityResponse).get(
                MasterApiMessages.MASTER_API_PARAMETER_NAME_ENTITY_ID));
        return "success";
      } else {
        return handleError(form, MasterApiMessageSupport.getResponseDetail(activityResponse));
      }
    } catch (Throwable e) {
      String message =
          (e instanceof SimpleInteractiveSpacesException) ? ((SimpleInteractiveSpacesException) e)
              .getCompoundMessage() : InteractiveSpacesException.getStackTrace(e);

      spaceEnvironment.getLog().error("Could not get uploaded activity file\n" + message);

      return handleError(form, message);
    }
  }

  /**
   * handle an error from an activity upload attempt.
   *
   * @param form
   *          the submission form
   * @param responseDetail
   *          the detail of the error response
   *
   * @return the key for webflow for the error handling
   */
  private String handleError(ActivityForm form, String responseDetail) {
    // On an error, need to clear the activity file else flow serialization fails.
    form.setActivityFile(null);
    form.setActivityError(responseDetail);

    return "error";
  }

  /**
   * @param masterApiActivityManager
   *          the masterApiActivityManager to set
   */
  public void setMasterApiActivityManager(MasterApiActivityManager masterApiActivityManager) {
    this.masterApiActivityManager = masterApiActivityManager;
  }

  /**
   * Form bean for activity objects.
   *
   * @author Keith M. Hughes
   */
  public static class ActivityForm implements Serializable {

    /**
     * Form data for activity.
     */
    private SimpleActivity activity = new SimpleActivity();

    /**
     * The activity file.
     */
    private MultipartFile activityFile;

    /**
     * The activity error description.
     */
    private String activityError;

    /**
     * @return the activity
     */
    public SimpleActivity getActivity() {
      return activity;
    }

    /**
     * @param activity
     *          the activity to set
     */
    public void setActivity(SimpleActivity activity) {
      this.activity = activity;
    }

    /**
     * Get the uploaded activity file.
     *
     * @return the uploaded file
     */
    public MultipartFile getActivityFile() {
      return activityFile;
    }

    /**
     * Set the uploaded activity file.
     *
     * @param activityFile
     *          the uploaded file
     */
    public void setActivityFile(MultipartFile activityFile) {
      this.activityFile = activityFile;
    }

    /**
     * @return the activity error
     */
    public String getActivityError() {
      return activityError;
    }

    /**
     * Set an activity error message.
     *
     * @param activityError
     *          activity error message
     */
    public void setActivityError(String activityError) {
      this.activityError = activityError;
    }
  }
}

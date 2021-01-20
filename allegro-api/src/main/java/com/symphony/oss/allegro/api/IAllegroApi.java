/*
 * Copyright 2019 Symphony Communication Services, LLC.
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

package com.symphony.oss.allegro.api;

import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectBuilder;
import com.symphony.oss.allegro.api.AllegroApi.ApplicationObjectUpdater;
import com.symphony.oss.allegro.api.AllegroApi.EncryptedApplicationPayloadAndHeaderBuilder;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;
import com.symphony.oss.models.pod.canon.IUserV2;
import com.symphony.oss.models.pod.canon.IV2UserList;

/**
 * The public interface of the Allegro API.
 * <p>
 * The full version of AllegroApi supports encryption and decryption capabilities and
 * requires authentication to a pod. For cases where a multi-tenant service wishes to use a subset of
 * the full API avoiding the need to authenticate to a pod see {@link IAllegroMultiTenantApi}.
 * 
 * @author Bruce Skingle
 *
 */
public interface IAllegroApi extends IAllegroMultiTenantApi, IAllegroDelegatedPodApi
{
  /** Resource path for Symphony dev/QA root certificate */
  static final String SYMPHONY_DEV_QA_ROOT_CERT         = "/certs/symphony/devQaRoot.pem";
  /** Resource path for Symphony dev/QA intermediate certificate */
  static final String SYMPHONY_DEV_QA_INTERMEDIATE_CERT = "/certs/symphony/devQaIntermediate.pem";
  /** Resource path for Symphony dev certificate */
  static final String SYMPHONY_DEV_CERT                 = "/certs/symphony/dev.pem";

  
  /**
   * Create a new EncryptedApplicationPayloadAndHeaderBuilder.
   * 
   * This can be used to build an encrypted payload and header which can be sent to a server end point to be stored in the object store 
   * or elsewhere
   * 
   * @return A new EncryptedApplicationPayloadAndHeaderBuilder.
   */
  EncryptedApplicationPayloadAndHeaderBuilder newEncryptedApplicationPayloadAndHeaderBuilder();
  
  /**
   * Create a new ApplicationObjectBuilder.
   * 
   * @return A new ApplicationObjectBuilder.
   */
  ApplicationObjectBuilder newApplicationObjectBuilder();
  
  /**
   * Create a new ApplicationObjectBuilder to create a new version of the given object.
   * 
   * When an application object payload is returned from fetch operations it has a reference to the IStoredApplicationObject
   * which contains it and this can be retrieved by calling the <code>getStoredApplicationObject()</code>
   * method. In these cases (such as when writing a Consumer method) you can generate an updater from the
   * application object payload directly by calling
   * <code>newApplicationObjectUpdater(applicationObjectPayload.getStoredApplicationObject())</code>.
   * 
   * In cases where the application object was created (rather than having been retrieved from the object store)
   * this will not work, since the ApplicationObjectPayload must be created before the StoredApplicationObject.
   * In order to update an object from a created ApplicationObjectPayload it is necessary to retain a separate
   * reference to the StoredApplicationObject which is returned by the
   * <code>store(IAbstractStoredApplicationObject object)</code> method.
   * 
   * @param existingObject An existing application object for which a new version is to be created.
   * 
   * @return A new ApplicationObjectBuilder to create a new version of the given object.
   */
  ApplicationObjectUpdater newApplicationObjectUpdater(IStoredApplicationObject existingObject);
  
  /**
   * Create a new ApplicationObjectBuilder to create a new version of the given object.
   * 
   * This method only works with an IApplicationObjectPayload which was retrieved from the object store, if it
   * is passed a locally created IApplicationObjectPayload then a <code>NullPointerException</code> will be thrown.
   * 
   * @param existingObject An existing application object for which a new version is to be created.
   * 
   * @deprecated Use <code>newApplicationObjectUpdater(IStoredApplicationObject existingObject)</code> instead.
   * 
   * @return A new ApplicationObjectBuilder to create a new version of the given object.
   */
  @Deprecated
  default ApplicationObjectUpdater newApplicationObjectUpdater(IApplicationObjectPayload existingObject)
  {
    return newApplicationObjectUpdater(existingObject.getStoredApplicationObject());
  }
  
  /**
   * Fetch information about a user given a user (login) name.
   * 
   * @param userName  The userName with which the required user logs in.
   * 
   * @return The user object for the required user.
   * 
   * @throws NotFoundException If the given userName cannot be found.
   * 
   * @deprecated Use fetchUserByName()
   */
  @Deprecated
  IUserV2 getUserByName(String userName) throws NotFoundException;

  /**
   * Fetch information about one or more users given a user (login) name.
   * 
   * @param userNames  The userName with which the required users log in.
   * 
   * @return A list of responses and errors.
   * 
   * @deprecated Use fetchUsersByName()
   */
  @Deprecated
  IV2UserList getUsersByName(String... userNames);
}

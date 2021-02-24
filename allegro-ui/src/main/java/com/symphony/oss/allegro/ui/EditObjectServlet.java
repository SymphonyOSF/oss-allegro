/*
 *
 *
 * Copyright 2020 Symphony Communication Services, LLC.
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

package com.symphony.oss.allegro.ui;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableSet;
import com.symphony.oss.allegro.api.EncryptedApplicationObjectUpdater;
import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.IAllegroMultiTenantApi;
import com.symphony.oss.canon.runtime.exception.BadRequestException;
import com.symphony.oss.canon.runtime.exception.CanonException;
import com.symphony.oss.commons.dom.json.JsonString;
import com.symphony.oss.commons.dom.json.MutableJsonObject;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.IEncryptedApplicationPayload;
import com.symphony.oss.models.object.canon.facade.ApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

class EditObjectServlet extends AbstractObjectServlet
{
  private static final long            serialVersionUID = 1L;
  
  static final ImmutableSet<String> reservedAttributes_ = new ImmutableSet.Builder<String>()
      .add("absoluteHash")
      .add("baseHash")
      .add("prevHash")
      .add("sortKey")
      .add("partitionHash")
      .add("headerType")
      .add("payloadType")
      .build();

  private final IAllegroMultiTenantApi accessApi_;
  private final IAllegroApi            userApi_;

  EditObjectServlet(IAllegroMultiTenantApi accessApi, IAllegroApi userApi)
  {
    accessApi_ = accessApi;
    userApi_ = userApi;
  }

  @Override
  public String getUrlPath()
  {
    return "/edit";
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException
  {
    try
    {
      handle(req, resp);
    }
    catch(CanonException e)
    {
      resp.sendError(e.getHttpStatusCode(), e.getLocalizedMessage());
    }
    catch(RuntimeException e)
    {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
    }
  }

  private void handle(HttpServletRequest req, HttpServletResponse resp) throws IOException
  {
    Map<String, String[]> params = req.getParameterMap();

    System.out.println("params " + params);
    
    Hash absoluteHash = getHash(req, Projection.ATTRIBUTEID_ABSOLUTE_HASH);

    IAbstractStoredApplicationObject storedObject = accessApi_.fetchAbsolute(absoluteHash);
    
    if(storedObject instanceof IStoredApplicationObject)
    {
      IStoredApplicationObject existingObject = (IStoredApplicationObject) storedObject;
      
      IApplicationObjectPayload existingPayload = userApi_.decryptObject(existingObject);
      
      System.out.println("existingPayload " + existingPayload);
      
      MutableJsonObject newJson = existingPayload.getJsonObject().newMutableCopy();
      
      for(String name : params.keySet())
      {
        if(!name.startsWith("_") && !reservedAttributes_.contains(name))
          newJson.add(name, new JsonString(params.get(name)[0]));
      }
      
      IApplicationObjectPayload newPayload = new ApplicationObjectPayload(newJson.immutify().toString());
      
      System.out.println("newPayload " + newPayload);
      
      IEncryptedApplicationPayload newEncryptedPayload = userApi_.newEncryptedApplicationPayloadBuilder()
        .withPayload(newPayload)
        .withThreadId(existingObject.getThreadId())
        .build();

      EncryptedApplicationObjectUpdater updater = userApi_.newEncryptedApplicationObjectUpdater(existingObject)
        .withEncryptedPayload(newEncryptedPayload)
        ;
      
      if(params.containsKey(Projection.ATTRIBUTEID_SORT_KEY))
      {
        updater.withSortKey(params.get(Projection.ATTRIBUTEID_SORT_KEY)[0]);
      }
      
      IStoredApplicationObject newStoredObject = updater.build();
      
      System.out.println("newStoredObject " + newStoredObject);
      
      accessApi_.store(newStoredObject);
      
      resp.setStatus(HttpServletResponse.SC_OK);
      
      resp.getWriter().println(newStoredObject.serialize());
    }
    else
      throw new BadRequestException("You can't update an object of type " + storedObject.getCanonType());
  }
}
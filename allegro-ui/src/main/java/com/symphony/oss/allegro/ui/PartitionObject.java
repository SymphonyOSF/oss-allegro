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

import com.symphony.oss.allegro.objectstore.IAllegroApi;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

class PartitionObject<T extends IAbstractStoredApplicationObject>
{
  private T                         storedObject_;
  private IApplicationObjectPayload payload_;
  private RuntimeException          payloadException_;

  PartitionObject(T abstracttoredObject, IAllegroApi userApi)
  {
    storedObject_ = abstracttoredObject;
    
    if(abstracttoredObject instanceof IStoredApplicationObject)
    {
      IStoredApplicationObject storedObject = (IStoredApplicationObject)abstracttoredObject;
      
      if(storedObject.getEncryptedPayload() == null)
      {
        payload_ = null;
        payloadException_ = null;
      }
      else
      {
        try
        {
          payload_ = userApi.decryptObject(storedObject);
          payloadException_ = null;
        }
        catch(RuntimeException e)
        {
          payload_ = null;
          payloadException_ = e;
        }
      }
    }
    else
    {
      payload_ = null;
      payloadException_ = null;
    }
  }

  T getStoredObject()
  {
    return storedObject_;
  }

  IApplicationObjectPayload getPayload()
  {
    if(payloadException_ != null)
      throw payloadException_;
    
    return payload_;
  }

  IApplicationObjectPayload getPayloadUnchecked()
  {
    return payload_;
  }

  RuntimeException getPayloadException()
  {
    return payloadException_;
  }
}

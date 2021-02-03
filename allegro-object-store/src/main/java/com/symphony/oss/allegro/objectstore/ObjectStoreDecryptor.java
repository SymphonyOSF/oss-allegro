/*
 * Copyright 2021 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package com.symphony.oss.allegro.objectstore;

import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

abstract class ObjectStoreDecryptor
{

  /**
   * Deserialize and decrypt the given object.
   * 
   * @param encryptedApplicationPayload An encrypted object.
   * 
   * @return The decrypted object.
   */
  abstract IApplicationObjectPayload decryptObject(IStoredApplicationObject encryptedApplicationPayload);
}

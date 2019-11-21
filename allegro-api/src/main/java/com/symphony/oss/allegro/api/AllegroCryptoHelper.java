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

import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

import com.symphony.oss.models.crypto.canon.Base64SecretKey;
import com.symphony.oss.models.crypto.canon.CipherSuiteId;
import com.symphony.oss.models.crypto.canon.facade.WrappedKey;
import com.symphony.oss.models.crypto.cipher.CipherSuite;
import com.symphony.oss.models.crypto.cipher.ICipherSuite;
import com.symphony.security.exceptions.CiphertextTransportIsEmptyException;
import com.symphony.security.exceptions.CiphertextTransportVersionException;
import com.symphony.security.exceptions.InvalidDataException;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;
import com.symphony.security.helper.CDecryptionHelper;

public class AllegroCryptoHelper
{
  private static final ICipherSuite cipherSuite_ = CipherSuite.get(CipherSuiteId.RSA2048_AES256);
  private final CDecryptionHelper   cDecryptionHelper_;
  private final Base64SecretKey     encodedKey_;
  private final SecretKey           secretKey_;
  private final WrappedKey          encryptedKey_;

  public AllegroCryptoHelper(byte[] contentKey, WrappedKey encryptedKey)
  {
    cDecryptionHelper_  = new CDecryptionHelper(contentKey);
    encodedKey_         = Base64SecretKey.newBuilder().build(Base64.encodeBase64String(contentKey));
    secretKey_          = cipherSuite_.secretKeyFromBase64(encodedKey_);
    encryptedKey_       = encryptedKey;
  }

  public static ICipherSuite getCiphersuite()
  {
    return cipherSuite_;
  }

  public Base64SecretKey getEncodedKey()
  {
    return encodedKey_;
  }

  public SecretKey getSecretKey()
  {
    return secretKey_;
  }

  public WrappedKey getEncryptedKey()
  {
    return encryptedKey_;
  }

  public String decrypt(String message)
  {
    try
    {
      return cDecryptionHelper_.decrypt(message);
    }
    catch (SymphonyEncryptionException | InvalidDataException | CiphertextTransportVersionException
        | CiphertextTransportIsEmptyException | SymphonyInputException e)
    {
      throw new IllegalStateException(e);
    }
  }

  public byte[] decrypt(byte[] message)
  {
    try
    {
      return cDecryptionHelper_.decrypt(message);
    }
    catch (SymphonyEncryptionException | SymphonyInputException | InvalidDataException
        | CiphertextTransportIsEmptyException | CiphertextTransportVersionException e)
    {
      throw new IllegalStateException(e);
    }
  }

  public String encrypt(String message, int podId, long rotationId)
  {
    try
    {
      return cDecryptionHelper_.encrypt(message, getDefaultMessageVersion(), podId, rotationId);
    }
    catch (SymphonyEncryptionException e)
    {
      throw new IllegalStateException(e);
    }
  }

  public byte[] encrypt(byte[] message, int podId, long rotationId)
  {
    try
    {
      return cDecryptionHelper_.encrypt(message, getDefaultMessageVersion(), podId, rotationId);
    }
    catch (SymphonyEncryptionException e)
    {
      throw new IllegalStateException(e);
    }
  }

  public byte[] encrypt(byte[] message, int podId, long rotationId, byte mode)
  {
    try
    {
      return cDecryptionHelper_.encrypt(message, getDefaultMessageVersion(), podId, rotationId, mode);
    }
    catch (SymphonyEncryptionException e)
    {
      throw new IllegalStateException(e);
    }
  }
  
  private int getDefaultMessageVersion() {
    // The parameter "MSG_VERSION" is never actually used in Security-Lib/CDecryptionHelper.
    // If an updated version of the library starts requiring it, it can be retrieved from
    // SymphonyClient.getMessageOnTheWireVersion(AuthProvider)
    return 1;
  }
}

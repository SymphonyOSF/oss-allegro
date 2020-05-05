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

import org.apache.http.impl.client.CloseableHttpClient;

import com.symphony.oss.canon.runtime.IEntity;

//import com.symphony.oss.models.fundamental.FundamentalModelRegistry;
//import com.symphony.oss.models.fundamental.canon.facade.IExchangeKey;
//import com.symphony.oss.models.fundamental.canon.facade.IFundamentalObject;
//import com.symphony.oss.models.fundamental.canon.facade.IOpenExchangeKey;
//import com.symphony.oss.models.fundamental.canon.facade.IOpenPrincipalCredential;
//import com.symphony.oss.models.fundamental.canon.facade.IOpenSigningKey;
//import com.symphony.oss.models.fundamental.canon.facade.ISigningKey;
//import com.symphony.oss.models.fundamental.canon.facade.OpenExchangeKey;
//import com.symphony.oss.models.fundamental.canon.facade.OpenSigningKey;
//import com.symphony.oss.models.internal.km.canon.facade.IUserKeys;
//import com.symphony.oss.models.system.canon.facade.IPrincipal;
//import com.symphony.oss.models.system.canon.EstablishPrincipalRequest;
//import com.symphony.oss.models.system.canon.IEstablishPrincipalRequest;
//import com.symphony.oss.models.system.canon.IEstablishPrincipalResponse;
//import com.symphony.oss.models.system.canon.SystemHttpModelClient;

@Deprecated
class PrincipalSupplier
{
//  private final FundamentalModelRegistry modelRegistry_;
//  private final SystemHttpModelClient    systemApiClient_;
//  private final CloseableHttpClient      httpclient_;
//  private final IUserKeys                userKeys_;
//
//  private IPrincipal                     principal_;
//  private IOpenExchangeKey               exchangeKey_;
//  private IOpenSigningKey                signingKey_;
//  
//  PrincipalSupplier(FundamentalModelRegistry modelRegistry, SystemHttpModelClient systemApiClient, CloseableHttpClient httpclient, IUserKeys userKeys)
//  {
//    modelRegistry_ = modelRegistry;
//    systemApiClient_ = systemApiClient;
//    httpclient_ = httpclient;
//    userKeys_ = userKeys;
//  }
//  
//  private synchronized void fetch()
//  {
//    if(principal_ == null)
//    {
//      IEstablishPrincipalRequest request = new EstablishPrincipalRequest.Builder()
//          .withCipherSuiteId(AllegroCryptoClient.ThreadSeurityContextCipherSuiteId)
//          .withEncodedExchangeKey(userKeys_.getPublicKey())
//          .withEncodedSigningKey(userKeys_.getPublicKey())
//        .build();
//      
//      IEstablishPrincipalResponse establishPrincipalResponse = systemApiClient_.newPrincipalsEstsablishPostHttpRequestBuilder()
//        .withCanonPayload(request)
//        .build()
//        .execute(httpclient_);
//
//      principal_ = establishPrincipalResponse.getPrincipal();
//    
//      exchangeKey_ = OpenExchangeKey.deserialize(
//          (IExchangeKey) open(establishPrincipalResponse.getExchangeKey()),
//          userKeys_.getKeyPair().getPrivate());
//      
//      signingKey_ = OpenSigningKey.deserialize(
//          (ISigningKey) open(establishPrincipalResponse.getSigningKey()),
//          userKeys_.getKeyPair().getPrivate());
//    }
//  }
//
//  
//  private IEntity open(IFundamentalObject object)
//  {
//    return modelRegistry_.open(object, (IOpenPrincipalCredential)null);
//  }
//
//  IPrincipal getPrincipal()
//  {
//    fetch();
//    
//    return principal_;
//  }
//
//  IOpenExchangeKey getExchangeKey()
//  {
//    fetch();
//    
//    return exchangeKey_;
//  }
//
//  IOpenSigningKey getSigningKey()
//  {
//    fetch();
//    
//    return signingKey_;
//  }
}

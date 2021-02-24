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

package com.symphony.oss.allegro.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.fugue.aws.sqs.SqsAction;
import com.symphony.oss.fugue.aws.sqs.SqsMessageParser;
import com.symphony.oss.fugue.aws.sqs.SqsResponseMessage;

/**
 * @author Geremia Longobardo
 * 
 * Class used to build http requests to fetch records directly from SQS endpoint
 */
public class AllegroSqsRequestBuilder
{
  private static final Logger log_   = LoggerFactory.getLogger(AllegroSqsRequestBuilder.class);
  
  private RequestBuilder req;
  private URIBuilder uri = new URIBuilder();
  
  /**
   * @param allegro AllegroApi is needed for configuration.
   * @param path  endpoint path configured in API Gateway.
   */
  public AllegroSqsRequestBuilder(IAllegroMultiTenantApi allegro , String path)
  {
    uri.setScheme("https")
    .setHost(allegro.getConfiguration().getApiUrl().toString().split("//")[1])
    .setPath(path);

    req = RequestBuilder.post().addHeader("Authorization", "Bearer " + allegro.getApiAuthorizationToken());
  }

  /**
   * @param feedHash Hash of the feed.
   * @return self
   */
  public AllegroSqsRequestBuilder withFeedHash(String feedHash)
  {
    uri.addParameter("feedHash", feedHash);

    return self();
  }

  /**
   * @param MaxNumberOfMessages the Max number of messages to request (less than or equal to 10)
   * @return self
   */
  public AllegroSqsRequestBuilder withMaxNumberOfMessages(Integer MaxNumberOfMessages)
  {
    uri.addParameter("MaxNumberOfMessages", MaxNumberOfMessages.toString());

    return self();
  }

  /**
   * @param action The SQS Action to perform
   * @return self
   */
  public AllegroSqsRequestBuilder withAction(SqsAction action)
  {
    String s;
    switch(action) {
      case RECEIVE: s = "ReceiveMessage"; break;
      case DELETE:  s = "DeleteMessage"; break;
      case EXTEND:  s = "ChangeMessageVisibility"; break;
      default: throw new IllegalStateException("SQS Action not allowed:" + action);
    }
    uri.addParameter("Action", s);

    return self();
  }

  /**
   * @param WaitTimeSeconds The maximum number of seconds to wait
   * @return self
   */
  public AllegroSqsRequestBuilder withWaitTimeSeconds(Integer WaitTimeSeconds)
  {
    uri.addParameter("WaitTimeSeconds", WaitTimeSeconds == null? "0" : WaitTimeSeconds.toString());

    return self();
  }
  
  /**
   * @param ReceiptHandle A String used as reference to the original message
   * @return self
   */
  public AllegroSqsRequestBuilder withReceiptHandle(String ReceiptHandle) 
  {
    uri.addParameter("ReceiptHandle", ReceiptHandle);

    return self();
  }
  
  /**
   * @param VisibilityTimeout The number of seconds to extend visibility of a message.
   * @return self
   */
  public AllegroSqsRequestBuilder withVisibilityTimeout(Integer VisibilityTimeout)
  {
    uri.addParameter("VisibilityTimeout", VisibilityTimeout.toString());

    return self();
  }

  /**
   * @param httpClient The client used to execute the request
   * @return The SQS messages
   */
  public List<SqsResponseMessage> execute(CloseableHttpClient httpClient)
  {
    HttpUriRequest request = null;
    try
    {
      request = req.setUri(uri.build()).build();
    }
    catch (URISyntaxException e1)
    {
      throw new IllegalStateException(e1);
    }

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try
    {
      CloseableHttpResponse response = httpClient.execute(request);
        

      InputStream in = response.getEntity().getContent();
      
 
      byte[] buf = new byte[1024];
      int read = 0;
      try
      {
        while ((read = in.read(buf)) != -1)
        {
          buffer.write(buf, 0, read);
        }
      }
      finally
      {
        in.close();
      }
      buffer.flush();
      

      if(response.getStatusLine().getStatusCode() != 200)
      {
        log_.error("response " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase() + "\n" + buffer.toString());
        throw new IllegalStateException("SQS response " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
      }

    }
    catch (IOException e)
    {
      throw new IllegalStateException(e);
    }
    
   return SqsMessageParser.parse(buffer.toString());
  }

  private AllegroSqsRequestBuilder self()
  {
    return this;
  }

}

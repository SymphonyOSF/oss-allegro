/*
 *
 *
 * Copyright 2018 Symphony Communication Services, LLC.
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.symphony.oss.allegro.runtime.aws;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.exception.NotImplementedException;
import com.symphony.oss.canon.runtime.http.AbstractRequestContext;
import com.symphony.oss.canon.runtime.http.HttpMethod;
import com.symphony.oss.canon.runtime.http.IRequestContext;
import com.symphony.oss.fugue.aws.lambda.AwsLambdaRequest;
import com.symphony.oss.fugue.aws.lambda.AwsLambdaResponse;
import com.symphony.oss.fugue.trace.ITraceContext;

public class AwsLambdaRequestContext extends AbstractRequestContext implements IRequestContext
{
  private static final Logger log_ = LoggerFactory.getLogger(AwsLambdaRequestContext.class);
  
  private final AwsLambdaRequest  request_;
  private final AwsLambdaResponse response_ = new AwsLambdaResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No response from handler");;

  private Map<String, Cookie>    cookieMap_;
  private Map<String, String>    pathMap_;
  private String                 pathInfo_;
  
  private OutputStream            os_;
  private boolean                 streaming_;


  public AwsLambdaRequestContext(ITraceContext trace, IModelRegistry modelRegistry, AwsLambdaRequest request, OutputStream outputStream)
  {
    super(getHttpMethod(request.getHttpMethod()), trace, modelRegistry);
    this.os_ = outputStream;
    request_ = request;
  }
  
  private static HttpMethod getHttpMethod(String httpMethod)
  {
    for(HttpMethod method : HttpMethod.values())
    {
      if(method.toString().equalsIgnoreCase(httpMethod))
        return method;
    }

    throw new IllegalArgumentException("Invalid HTTP method name " + httpMethod);
  }

  @Override
  protected synchronized String getCookie(String name)
  {
    // TODO: implement me
//    if(cookieMap_ == null)
//    {
//      cookieMap_ = new HashMap<>();
//      
//      for(Cookie cookie : request_.getCookies())
//        cookieMap_.put(cookie.getName(), cookie);
//    }
//    return cookieMap_.get(name).getValue();
    throw new NotImplementedException();
  }

  @Override
  protected String getHeader(String name)
  {
    return request_.getHeader(name);
  }

  @Override
  public String getPathInfo()
  {
    return request_.getPath();
  }

  @Override
  protected String getParameter(String name)
  {
    return request_.getParameter(name);
  }
  
  @Override
  public BufferedReader getReader() throws IOException
  {
    return request_.getReader();
  }

  @Override
  public PrintWriter getWriter() throws IOException
  {
    return response_.getWriter();
  }

  @Override
  public OutputStream getOutputStream() throws IOException
  {
    return response_.getOutputStream();
  }

  @Override
  public void setContentType(String contentType)
  {
    response_.setContentType(contentType);
  }

  @Override
  public void setStatus(int sc)
  {
    response_.setStatus(sc);
  }
  
  @Override
  public void setHeader(String header, String value)
  {
    response_.setHeader(header, value);
  }

  @Override
  public void sendError(int sc, String msg) //throws IOException
  {
    response_.setStatus(sc);
    response_.setMessage(msg);
  }

  public void sendResponse(OutputStream outputStream)
  {
    try
    {
      response_.write(outputStream);
    }
    catch (IOException e)
    {
      log_.error("Failed to write response.", e);
    }
  }

  @Override
  public Writer startStreaming()
  {
    streaming_ = true;
    return new AwsLambdaWriter(os_);
  }
  
  @Override
  public Map<String, String> getStageVariables()
  {
    return request_.getStageVariables();
  }
  
}

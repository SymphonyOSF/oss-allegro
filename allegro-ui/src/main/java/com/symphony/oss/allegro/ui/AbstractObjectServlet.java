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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.joda.time.Instant;

import com.symphony.oss.canon.runtime.exception.BadRequestException;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.server.http.IUrlPathServlet;
import com.symphony.oss.models.core.canon.facade.ThreadId;

abstract class AbstractObjectServlet extends HttpServlet implements IUrlPathServlet
{
  private static final long            serialVersionUID = 1L;

  Hash getHash(HttpServletRequest req, String name)
  {
    String s = req.getParameter(name);
    
    if(s == null)
      throw new BadRequestException("Hash parameter \"" + name + "\" is required.");
    
    try
    {
      return Hash.ofBase64String(s);
    }
    catch(IllegalArgumentException e)
    {
      throw new BadRequestException("Parameter \"" + name + "\" is not a valid Hash value.");
    }
  }
  
  ThreadId getThreadId(HttpServletRequest req, String name)
  {
    String s = req.getParameter(name);
    
    if(s == null)
      throw new BadRequestException("ThreadId parameter \"" + name + "\" is required.");
    
    try
    {
      return ThreadId.newBuilder().build(s);
    }
    catch(IllegalArgumentException e)
    {
      throw new BadRequestException("Parameter \"" + name + "\" is not a valid ThreadId value.");
    }
  }
  
  String getString(HttpServletRequest req, String name)
  {
    String s = req.getParameter(name);
    
    if(s == null)
      throw new BadRequestException("String parameter \"" + name + "\" is required.");
    
    return s;
  }
  
  String getSortKey(HttpServletRequest req, String name)
  {
    String s = req.getParameter(name);
    
    if(s == null || s.trim().length()==0)
      return Instant.now().toString();
    
    return s;
  }
}
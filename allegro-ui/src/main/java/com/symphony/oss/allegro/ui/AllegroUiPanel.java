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

import java.io.StringReader;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.IAllegroMultiTenantApi;
import com.symphony.oss.canon.runtime.IEntity;
import com.symphony.oss.commons.dom.json.IImmutableJsonDomNode;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.server.http.ui.servlet.IUIPanel;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;
import com.symphony.oss.fugue.server.http.ui.servlet.UIPanel;
import com.symphony.oss.models.object.canon.INamedUserIdObject;
import com.symphony.oss.models.object.canon.IUserIdObject;

abstract class AllegroUiPanel extends UIPanel implements IUIPanel
{
  static final String CODE_CLASS = "sym-codespan";
  static final String ROWACTION_CLASS = "rowAction";
  static final String ERROR_CLASS = "error";
  static final String CLASS_DROP_BUTTON = "dropbtn";
  
  static final String PARTITION_HASH = "partitionHash";
  static final String PARTITION_ID   = "partitionId";
  static final String PARTITION_NAME = "partitionName";
  static final String SORT_KEY       = "sortKey";
  static final String ABSOLUTE_HASH  = "absoluteHash";
  static final String BASE_HASH      = "baseHash";
  static final String LOAD_AFTER     = "loadAfter";
  
  IAllegroApi userApi_;
  IAllegroMultiTenantApi accessApi_;
  
  public AllegroUiPanel(String id, String name, IAllegroMultiTenantApi accessApi, IAllegroApi userApi)
  {
    super(id, name);
    accessApi_ = accessApi;
    userApi_ = userApi;
  }

  public AllegroUiPanel(String name, IAllegroMultiTenantApi accessApi, IAllegroApi userApi)
  {
    super(name);
    accessApi_ = accessApi;
    userApi_ = userApi;
  }

  Hash getPartitionHash(HttpServletRequest req, UIHtmlWriter out)
  {
    String        name          = req.getParameter(PARTITION_HASH);
    
    if(name == null)
      return null;
    
    try
    {
      return Hash.ofBase64String(name);
    }
    catch(IllegalArgumentException e)
    {
      out.printError("Invalid Partition Hash", e);
      return null;
    }
  }

  Hash getAbsoluteHash(HttpServletRequest req, UIHtmlWriter out)
  {
    String        name          = req.getParameter(ABSOLUTE_HASH);
    
    if(name == null)
      return null;
    
    try
    {
      return Hash.ofBase64String(name);
    }
    catch(IllegalArgumentException e)
    {
      out.printError("Invalid absolute Hash", e);
      return null;
    }
  }

  Hash getBaseHash(HttpServletRequest req, UIHtmlWriter out)
  {
    String        name          = req.getParameter(BASE_HASH);
    
    if(name == null)
      return null;
    
    try
    {
      return Hash.ofBase64String(name);
    }
    catch(IllegalArgumentException e)
    {
      out.printError("Invalid base Hash", e);
      return null;
    }
  }
  
  INamedUserIdObject getPartitionId(HttpServletRequest req, UIHtmlWriter out)
  {
    String name = req.getParameter(PARTITION_ID);
    
    if(name == null)
      return null;
    
    IEntity id = userApi_.getModelRegistry().parseOne(new StringReader(name));
    
    if(id instanceof INamedUserIdObject)
      return (INamedUserIdObject)id;
    
    out.printError("Invalid Partition ID");

    return null;
  }

  void printObject(UIHtmlWriter out, String title, IEntity entity)
  {
    if(title == null)
      out.printElement("p");
    else
      out.printElement("h3", title);
    
    out.openElement("table", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");
    
    Iterator<String> it = entity.getJsonObject().getNameIterator();
    
    while(it.hasNext())
    {
      String name = it.next();
      IImmutableJsonDomNode value = entity.getJsonObject().get(name);
      
      print(out, name, value);
    }
    

    out.closeElement(); // table
  }

  void print(UIHtmlWriter out, String label, Object value)
  {
    out.openElement("tr");
    out.printElement("td", label);
    out.printElement("td", value);
    out.closeElement(); //tr
  }
}

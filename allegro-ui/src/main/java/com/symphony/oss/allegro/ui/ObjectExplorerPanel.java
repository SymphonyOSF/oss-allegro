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

import javax.servlet.http.HttpServletRequest;

import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.IAllegroMultiTenantApi;
import com.symphony.oss.canon.runtime.exception.BadRequestException;
import com.symphony.oss.canon.runtime.exception.NotFoundException;
import com.symphony.oss.canon.runtime.exception.PermissionDeniedException;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

class ObjectExplorerPanel extends RenderingPanel
{
  private static final String PANEL_NAME = "Object Explorer";
  public static final String PANEL_ID = "objectExplorer";

  ObjectExplorerPanel(ProjectorManager projectorManager, IAllegroMultiTenantApi accessApi, IAllegroApi userApi)
  {
    super(PANEL_ID, PANEL_NAME, accessApi, userApi, projectorManager, true);
  }
  
  @Override
  public void handleContent(HttpServletRequest req, UIHtmlWriter out)
  {
    Hash                              partitionHash = getPartitionHash(req, out);
    Hash                              absoluteHash = getAbsoluteHash(req, out);
    String                            sortKey = req.getParameter(SORT_KEY);
    IAbstractStoredApplicationObject  object = null;

    try
    {
      if(absoluteHash != null)
      {
        object = accessApi_.fetchAbsolute(absoluteHash);
        partitionHash = object.getPartitionHash();
        sortKey = object.getSortKey().asString();
      }
      else if(partitionHash != null && sortKey != null)
      {
        object = accessApi_.fetchObject(partitionHash, sortKey);
        absoluteHash = object.getAbsoluteHash();
      }
    }
    catch(PermissionDeniedException e)
    {
      out.printError("Object Access Denied");
    }
    catch(NotFoundException e)
    {
      out.printError("Object Does Not Exist");
    }
    catch(BadRequestException e)
    {
      out.printError("Unable to read Object: " + e.getLocalizedMessage());
    }
    catch(RuntimeException e)
    {
      out.printError("Unable to read Object", e);
    }
    
    printHeader(out);
    
    out.printElement("h1", "Object Explorer");
    
    
    out.openElement("table", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");
    
    out.openElement("tr");
    out.printElement("td", "Absolute Hash");
    out.openElement("td");
    out.openElement("form", "method", "POST");
    out.printTextInput(ABSOLUTE_HASH, absoluteHash == null ? "" : absoluteHash.toStringBase64(), "size", "64", CLASS, CODE_CLASS);
    out.printElement("input", null, "type", "submit", "value", "Search");
    out.closeElement(); // form
    out.closeElement(); // td
    out.closeElement(); //tr 
    
    out.openElement("tr");
    out.printElement("td", "Partition Hash");
    out.openElement("td");
    out.openElement("form", "method", "POST");
    out.printTextInput(PARTITION_HASH, partitionHash == null ? "" : partitionHash.toStringBase64(), "size", "64", CLASS, CODE_CLASS);
    out.printElement("br");
    out.printTextInput(SORT_KEY, sortKey == null ? "" : sortKey, "size", "64");
    out.printElement("input", null, "type", "submit", "value", "Search");
    out.closeElement(); // form
    out.closeElement(); // td
    out.closeElement(); //tr 
    
    out.closeElement(); // table
    

    if(object != null)
    {
      out.printElement("h2", "Object");
      
      PartitionObject<IAbstractStoredApplicationObject> currentPartitionObject = new PartitionObject<>(object, userApi_);
      
      renderSummary(out, currentPartitionObject);
      
      printObject(out, "Stored Application Object", object);
      
      if(object instanceof IStoredApplicationObject)
      {
        IStoredApplicationObject storedObject = (IStoredApplicationObject) object;

        if(storedObject.getHeader() != null)
        {
          printObject(out, "Header", storedObject.getHeader());
        }
        
        if(currentPartitionObject.getPayloadUnchecked() != null)
        {
          printObject(out, "Decrypted Payload", currentPartitionObject.getPayloadUnchecked());
        }
      }
    }
  }
}

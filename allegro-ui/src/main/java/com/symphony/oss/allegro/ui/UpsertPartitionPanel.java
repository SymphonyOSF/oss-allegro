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
import com.symphony.oss.allegro.api.request.UpsertPartitionRequest;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;
import com.symphony.oss.models.object.canon.INamedUserIdObject;
import com.symphony.oss.models.object.canon.IUserIdObject;
import com.symphony.oss.models.object.canon.facade.IPartition;

class UpsertPartitionPanel extends AllegroUiPanel
{
  static final String PANEL_NAME = "Upsert Partition";
  static final String PANEL_ID = "upsertPartition";
  
  private final PartitionExplorerPanel partitionExplorer_;
  
  UpsertPartitionPanel(PartitionExplorerPanel partitionExplorer, IAllegroMultiTenantApi accessApi, IAllegroApi userApi)
  {
    super(PANEL_ID, PANEL_NAME, accessApi, userApi);
    
    partitionExplorer_ = partitionExplorer;
  }
  
  @Override
  public void handleContent(HttpServletRequest req, UIHtmlWriter out)
  {
    IPartition          partition     = null;
    Hash                partitionHash = null;
    String              partitionName = req.getParameter(PARTITION_NAME);
    INamedUserIdObject  partitionId   = getPartitionId(req, out);
    boolean             loadAfter     = false;
    
    UpsertPartitionRequest request = null;
    
    if(partitionId != null)
    {
      partitionName = partitionId.getName();
      
      request = new UpsertPartitionRequest.Builder()
        .withId(partitionId)
        .build();
    }
    
    if(request == null && partitionName != null)
    {
      request = new UpsertPartitionRequest.Builder()
          .withName(partitionName)
          .build();
    }
    
    if(request != null)
    {
      try
      {
        partition = accessApi_.upsertPartition(request);
        partitionHash = partition.getId().getHash();
        
      }
      catch(RuntimeException e)
      {
        out.printError("Failed to upsert partition: " + e.getLocalizedMessage());
      }
    }
        
    if(partition == null)
    {
      out.printElement("h1", "Upsert Partition");
      
      out.openElement("table", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");
      
      out.openElement("tr");
      out.printElement("td", "Partition Name");
      out.openElement("td");
      out.openElement("form", "method", "POST", "action", getPath());
      out.printTextInput(PARTITION_NAME, partitionName == null ? "" : partitionName, "size", "30");
      out.printElement("input", null, "type", "submit", "value", "Upsert");
      out.closeElement(); // form
      out.closeElement(); // td
      out.closeElement(); //tr
      
      out.openElement("tr");
      out.printElement("td", "Partition ID Object");
      out.openElement("td");
      out.openElement("form", "method", "POST", "action", getPath());
      out.printElement("textarea", partitionId == null ? "" : partitionId.toString(), "name", PARTITION_ID, "rows", "8", "cols", "64");
      out.printElement("input", null, "type", "submit", "value", "Upsert");
      out.closeElement(); // form
      out.closeElement(); // td
      out.closeElement(); // tr
      
      out.closeElement(); // table
    }
    else
    {
      partitionExplorer_.printPartition(out, partitionHash, partitionName, partition, loadAfter);
    }
  }
}

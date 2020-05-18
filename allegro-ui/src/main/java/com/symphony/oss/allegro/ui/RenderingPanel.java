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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.IAllegroMultiTenantApi;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;

abstract class RenderingPanel extends AllegroUiPanel
{
  private static final Logger log_ = LoggerFactory.getLogger(RenderingPanel.class);
  private static final ImmutableMap<String, Boolean> initialColumnMap_;

  private static final String COL_PREFIX = "COL_";

  private static final String HIDE = "Hide";
  private static final String SHOW = "Show";
  private static final String SCRIPT = "script";
  private static final String SRC = "src";

  
  private final ProjectorManager       projectorManager_;
  private final ListRendererManager    listRendererManager_;
  private final SummaryRendererManager summaryRendererManager_;

  
  
  static
  {
    initialColumnMap_ = new ImmutableMap.Builder<String, Boolean>()
        .put(Projection.ATTRIBUTE_SORT_KEY,       Boolean.TRUE)
        .put(Projection.ATTRIBUTE_ABSOLUTE_HASH,  Boolean.TRUE)
        .put(Projection.ATTRIBUTE_BASE_HASH,      Boolean.FALSE)
        .put(Projection.ATTRIBUTE_PARTITION_HASH, Boolean.FALSE)
        .put(Projection.ATTRIBUTE_HEADER_TYPE,    Boolean.TRUE)
        .put(Projection.ATTRIBUTE_PAYLOAD_TYPE,   Boolean.TRUE)
        .build();
  }
  
  RenderingPanel(String id, String name, IAllegroMultiTenantApi accessApi, IAllegroApi userApi, ProjectorManager projectorManager)
  {
    super(id, name, accessApi, userApi);
    
    projectorManager_ = projectorManager;
    listRendererManager_ = new ListRendererManager(this, projectorManager_);
    summaryRendererManager_ = new SummaryRendererManager(this, projectorManager_);
    
    
  }
  
  void printHeader(UIHtmlWriter out)
  {
    out.printElement("i", "", "id", "error", "class", "error");
  }

  void finishPage(UIHtmlWriter out)
  {
    for(Entry<String, Boolean> entry : initialColumnMap_.entrySet())
    {
      if(!entry.getValue())
        out.println("hideCol('" + entry.getKey() + "');");
    }

    out.closeElement(); // script
    out.flush();
  }

  void startTable(UIHtmlWriter out)
  {
    out.printElement(SCRIPT, "", SRC, "https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js");
    out.openElement("div", ID, "configure-menu", CLASS, "dropdown-content");
    out.openElement("ul", ID, "configure-menu-list");
    
    for(Entry<String, Boolean> entry : initialColumnMap_.entrySet())
    {
      out.openElement("li");
      if(entry.getValue())
        out.printElement("input", null, TYPE, CHECKBOX, ID, "CHECK_" + entry.getKey().replaceAll(" ", "_"),
          ON_CHANGE, "toggleCol('" + entry.getKey() + "');", CHECKED);
      else
        out.printElement("input", null, TYPE, CHECKBOX, ID, "CHECK_" + entry.getKey().replaceAll(" ", "_"),
            ON_CHANGE, "toggleCol('" + entry.getKey() + "');");
      out.println(entry.getKey());
      out.closeElement(); // li
    }

    out.closeElement(); // ul
    out.closeElement(); // nav
    
    out.printElement("button", "Configure Table", CLASS, CLASS_DROP_BUTTON, ON_CLICK, "showMenu('configure-menu')");
    out.openElement("table", "id", "blotter", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");

    out.openElement("tr", "id", "header");
    for(Entry<String, Boolean> entry : initialColumnMap_.entrySet())
    {
      out.printElement("th", entry.getKey(), ID, COL_PREFIX + entry.getKey().replaceAll(" ", "_"));
    }
    
    out.closeElement(); //tr 
  }

  void finishTable(UIHtmlWriter out)
  {
    out.closeElement(); // table
    
    out.openElement("script");
    
    try(InputStreamReader in = new InputStreamReader(getClass().getResourceAsStream("/blotterFunc.js")))
    {
      char[] buf = new char[1024];
      int nbytes;
      
      while((nbytes = in.read(buf))>0)
        out.write(buf, 0, nbytes);
    }
    catch (IOException e)
    {
      log_.error("Failed to write functions", e);
    }
    
    out.println();
  }
  
  void renderSummary(UIHtmlWriter out, PartitionObject<?> partitionObject)
  {
    summaryRendererManager_.render(out, partitionObject);
  }
  
  void render(UIHtmlWriter out, Object rowId, PartitionObject<?> partitionObject)
  {
    listRendererManager_.render(out, rowId, partitionObject);
  }
}

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.IAllegroMultiTenantApi;
import com.symphony.oss.commons.hash.Hash;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;

abstract class RenderingPanel extends AllegroUiPanel
{
  private static final Logger log_ = LoggerFactory.getLogger(RenderingPanel.class);

  private static final String COL_PREFIX = "COL_";

  private static final String HIDE = "Hide";
  private static final String SHOW = "Show";
  private static final String SCRIPT = "script";
  private static final String SRC = "src";

  
  private final ProjectorManager       projectorManager_;
  private final ListRendererManager    listRendererManager_;
  private final SummaryRendererManager summaryRendererManager_;
  private final boolean                absolute_;

  
  RenderingPanel(String id, String name, IAllegroMultiTenantApi accessApi, IAllegroApi userApi, ProjectorManager projectorManager, boolean absolute)
  {
    super(id, name, accessApi, userApi);
    
    projectorManager_ = projectorManager;
    listRendererManager_ = new ListRendererManager(this, projectorManager_);
    summaryRendererManager_ = new SummaryRendererManager(this, projectorManager_);
    absolute_ = absolute;
    
  }
  
  void printHeader(UIHtmlWriter out)
  {
    out.printElement("i", "", "id", "error", "class", "error");
  }

  void finishPage(UIHtmlWriter out)
  {
    out.closeElement(); // script
    out.flush();
  }

  void startTable(UIHtmlWriter out)
  {
    include(out, "/html/blotterTableStart.html");
  }
  
  void include(UIHtmlWriter out, String resourceName)
  {

    try(InputStreamReader in = new InputStreamReader(getClass().getResourceAsStream(resourceName)))
    {
      char[] buf = new char[1024];
      int nbytes;
      
      while((nbytes = in.read(buf))>0)
        out.write(buf, 0, nbytes);
    }
    catch (IOException e)
    {
      log_.error("Failed to include " + resourceName, e);
    }
    
    out.println();
  }

  void finishTable(UIHtmlWriter out, Hash partitionHash)
  {
    out.println("</table>"); // table
    
    out.printElement("div", "", STYLE, "height:100px"); // space for the edit button bar to appear in for a single row table. 
    out.printElement(SCRIPT, "", SRC, "/js/blotterFunc.js");
    out.openElement("script");
    out.println("var absolute = " + absolute_);
    out.println("$(\"#insertForm\").find('input[name=\"partitionHash\"]').val('" + partitionHash + "');");
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

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

import com.symphony.oss.allegro.objectstore.IAllegroObjectStoreApi;
import com.symphony.oss.allegro.objectstore.IBaseObjectStoreApi;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;

class PodPanel extends AllegroUiPanel
{
  PodPanel(IBaseObjectStoreApi accessApi, IAllegroObjectStoreApi userApi)
  {
    super("Pod", accessApi, userApi);
  }
  
  @Override
  public void handleContent(HttpServletRequest req, UIHtmlWriter out)
  {
    out.printElement("h1", "Pod Info");
    
    out.openElement("table", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");
    
    out.openElement("tr");
    out.printElement("td", "Name");
    out.printElement("td", "Value");
    out.closeElement(); //tr
    
    print(out, "PodID", userApi_.getPodId());
    print(out, "UserID", userApi_.getUserId());
    print(out, "Display Name", userApi_.getSessioninfo().getDisplayName());
    print(out, "Company Name", userApi_.getSessioninfo().getCompany());
    
    out.closeElement(); // table
    
    printObject(out, "Session Info", userApi_.getSessioninfo());
    
    out.openElement("table", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");
    
    out.printElement("h1", "Access Account Info");
    out.openElement("tr");
    out.printElement("td", "Name");
    out.printElement("td", "Value");
    out.closeElement(); //tr
    
    print(out, "UserID", accessApi_.getUserId());
    
    out.closeElement(); // table
  }
}

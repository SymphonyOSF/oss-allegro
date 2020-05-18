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

import com.symphony.oss.allegro.ui.Projection.AbstractAttribute;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;

class SummaryRendererManager extends BaseRendererManager
{
  SummaryRendererManager(RenderingPanel panel, ProjectorManager projectorManager)
  {
    super(projectorManager);
    
    with(Projection.Error.class, (out, projection) ->
    {
      out.printError(projection.getMessage());
    });
    
    with(Projection.AttributeSet.class, (out, projection) ->
    {
      for(AbstractAttribute<?,?> attr : projection.getAttributes())
      {
        IRenderer<Projection<?>> renderer = getConsumer(attr.getClass());
        
        if(renderer == null)
        {
          out.openElement("tr");
          out.printElement("td", attr.getName());
          out.printElement("td", attr.getValue());
          out.printElement("td", attr.getHoverText());
          out.closeElement();
        }
        else
          renderer.render(out, attr);
      }
    });
    
    with(Projection.Attribute.class, (out, projection) ->
    {
      out.openElement("tr");
      out.printElement("td", projection.getName());
      out.printElement("td", projection.getValue());
      out.printElement("td", projection.getHoverText());
      out.closeElement();
    });
    
    with(Projection.ErrorAttribute.class, (out, projection) ->
    {
      out.openElement("tr");
      out.printElement("td", projection.getName());
      out.printElement("td", projection.getValue(), RenderingPanel.CLASS, RenderingPanel.ERROR_CLASS);
      out.printElement("td", projection.getHoverText());
      out.closeElement();
    });
    
    with(Projection.AbsoluteHashAttribute.class, (out, projection) ->
    {
      out.openElement("tr");
      out.printElement("td", projection.getName());
      out.openElement("td");
      out.printElement("a", projection.getValue().toStringUrlSafeBase64(), "href", 
          panel.getPath(ObjectExplorerPanel.PANEL_ID) + "?" + RenderingPanel.ABSOLUTE_HASH + "=" + projection.getValue().toStringUrlSafeBase64(),
          RenderingPanel.CLASS, RenderingPanel.CODE_CLASS);
      out.closeElement();
      out.printElement("td", projection.getHoverText());
      out.closeElement();
    });
    
    with(Projection.BaseHashAttribute.class, (out, projection) ->
    {
      out.openElement("tr");
      out.printElement("td", projection.getName());
      out.openElement("td");
      out.printElement("a", projection.getValue().toStringUrlSafeBase64(), "href", 
          panel.getPath(ObjectVersionsPanel.PANEL_ID) + "?" + RenderingPanel.BASE_HASH + "=" + projection.getValue().toStringUrlSafeBase64(),
          RenderingPanel.CLASS, RenderingPanel.CODE_CLASS);
      out.closeElement();
      out.printElement("td", projection.getHoverText());
      out.closeElement();
    });
    
    with(Projection.PartitionHashAttribute.class, (out, projection) ->
    {
      out.openElement("tr");
      out.printElement("td", projection.getName());
      out.openElement("td");
      out.printElement("a", projection.getValue().toStringUrlSafeBase64(), "href", 
          panel.getPath(PartitionExplorerPanel.PANEL_ID) + "?" + RenderingPanel.PARTITION_HASH + "=" + projection.getValue().toStringUrlSafeBase64(),
          RenderingPanel.CLASS, RenderingPanel.CODE_CLASS);
      out.closeElement();
      out.printElement("td", projection.getHoverText());
      out.closeElement();
    });
  }

  void render(UIHtmlWriter out, PartitionObject<?> partitionObject)
  {
    Projection<?> projection = projectorManager_.project(partitionObject);
    IRenderer<Projection<?>> renderer = getConsumer(projection.getClass());
    
    if(renderer == null)
    {
      out.println("showError('Unknown Projection type " + projection.getClass() + "');");
    }
    else
    {
      out.openElement("table", "class", "w3-table w3-striped w3-bordered w3-border w3-hoverable w3-white");
      
      if(partitionObject.getPayloadException() != null)
      {
        out.openElement("tr");
        out.printElement("td", "Decryption Error");
        out.printElement("td", partitionObject.getPayloadException(), RenderingPanel.CLASS, RenderingPanel.ERROR_CLASS, "colspan", "2");
        out.closeElement(); // tr
      }
      renderer.render(out, projection);
      out.closeElement(); // table
    }
  }
}

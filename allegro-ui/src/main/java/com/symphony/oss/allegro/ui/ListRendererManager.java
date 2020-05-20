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

class ListRendererManager extends BaseRendererManager
{
  ListRendererManager(RenderingPanel panel, ProjectorManager projectorManager)
  {
    super(projectorManager);
    
    with(Projection.Error.class, (out, projection) ->
    {
      out.println("showError('" + projection.getMessage() + "');");
    });
    
    with(Projection.AttributeSet.class, (out, projection) ->
    {
      boolean first = true;
      
      for(AbstractAttribute<?,?> attr : projection.getAttributes())
      {
        if(first)
          first = false;
        else
          out.print(", ");
        
        IRenderer<Projection<?>> renderer = getConsumer(attr.getClass());
        
        if(renderer == null)
          out.print("\"" + attr.getName() + "\": \"" + attr.getValue() + "\"");
        else
          renderer.render(out, attr);
      }
    });
    
    with(Projection.Attribute.class, (out, projection) ->
    {
      out.print(toJson(projection));
    });
    
    with(Projection.ErrorAttribute.class, (out, projection) ->
    {
      out.print(toJson(projection, "<span class=\\\"" + RenderingPanel.ERROR_CLASS + "\\\">" + projection.getValue() + "</span>"));
    });
    
    with(Projection.AbsoluteHashAttribute.class, (out, projection) ->
    {
      out.print(toJson(projection, "<a href=\\\"" +
          panel.getPath(ObjectExplorerPanel.PANEL_ID) + "?" + RenderingPanel.ABSOLUTE_HASH + "=" + projection.getValue().toStringUrlSafeBase64() +
          "\\\", " + RenderingPanel.CLASS + "=\\\"" + RenderingPanel.CODE_CLASS + "\\\">" +
          projection.getValue().toStringBase64() + "</a>"));

    });
    
    with(Projection.BaseHashAttribute.class, (out, projection) ->
    {
      out.print(toJson(projection, "<a href=\\\"" +
          panel.getPath(ObjectVersionsPanel.PANEL_ID) + "?" + RenderingPanel.BASE_HASH + "=" + projection.getValue().toStringUrlSafeBase64() +
          "\\\", " + RenderingPanel.CLASS + "=\\\"" + RenderingPanel.CODE_CLASS + "\\\">" +
          projection.getValue().toStringBase64() + "</a>"));
    });
    
    with(Projection.PartitionHashAttribute.class, (out, projection) ->
    {
      out.print(toJson(projection, "<a href=\\\"" +
          panel.getPath(PartitionExplorerPanel.PANEL_ID) + "?" + RenderingPanel.PARTITION_HASH + "=" + projection.getValue().toStringUrlSafeBase64() +
          "\\\", " + RenderingPanel.CLASS + "=\\\"" + RenderingPanel.CODE_CLASS + "\\\">" +
          projection.getValue().toStringBase64() + "</a>"));
    });
  }

  void render(UIHtmlWriter out, Object rowId, PartitionObject<?> partitionObject)
  {
    Projection<?> projection = projectorManager_.project(partitionObject);
    IRenderer<Projection<?>> renderer = getConsumer(projection.getClass());
    
    if(renderer == null)
    {
      out.println("showError('Unknown Projection type " + projection.getClass() + "');");
    }
    else
    {
      out.print("render('" + rowId + "', {");
      renderer.render(out, projection);
      out.println("});");
    }
  }
}

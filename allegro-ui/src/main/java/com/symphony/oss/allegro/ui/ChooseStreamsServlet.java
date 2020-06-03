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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.collect.ImmutableList;
import com.symphony.oss.allegro.api.IAllegroApi;
import com.symphony.oss.allegro.api.request.FetchStreamsRequest;
import com.symphony.oss.canon.runtime.exception.CanonException;
import com.symphony.oss.fugue.server.http.ui.servlet.UIHtmlWriter;
import com.symphony.oss.models.core.canon.facade.PodAndUserId;
import com.symphony.oss.models.pod.canon.IConversationSpecificStreamAttributes;
import com.symphony.oss.models.pod.canon.IStreamAttributes;
import com.symphony.oss.models.pod.canon.IUserV2;

class ChooseStreamsServlet extends AbstractObjectServlet
{
  private static final long            serialVersionUID = 1L;
  
  private final IAllegroApi            userApi_;

  ChooseStreamsServlet(IAllegroApi userApi)
  {
    userApi_ = userApi;
  }

  @Override
  public String getUrlPath()
  {
    return "/chooseStreams";
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
  {
    try
    {
      handle(resp);
    }
    catch(CanonException e)
    {
      resp.sendError(e.getHttpStatusCode(), e.getLocalizedMessage());
    }
    catch(RuntimeException e)
    {
      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
    }
  }

  private void handle(HttpServletResponse resp) throws IOException
  {
    UIHtmlWriter out = new UIHtmlWriter(resp.getWriter());
    
    List<IStreamAttributes> streams = userApi_.fetchStreams(new FetchStreamsRequest.Builder()
        .build());

    out.openElement("tbody");
    for(IStreamAttributes stream : streams)
    {
       out.openElement("tr");
        out.openElement("td", RenderingPanel.CLASS, RenderingPanel.CLASS_CHOOSER_CONTROL);
        out.printRadioInput("result", stream.getId().toBase64String());
        out.closeElement(); // td
        
        out.printElement("td", stream.getStreamType().getType());
        
        switch(stream.getStreamType().getType())
        {
          case IM:
            out.printElement("td", members(stream.getStreamAttributes()));
            break;
            
          case MIM:
            out.printElement("td", members(stream.getStreamAttributes()));
            break;
            
          case POST:
            out.printElement("td", "Wall Post Thread");
            break;
            
          case ROOM:
            out.printElement("td", stream.getRoomAttributes().getName());
            break;
            
          default:
            out.printElement("td", "UNKNOWN TYPE");
            break;
          
        }
      out.closeElement(); // tr
    }
    out.closeElement(); // tbody
    
    out.flush();
    out.close();
  }

  private String members(IConversationSpecificStreamAttributes streamAttributes)
  {
    int memberLimit = 5;
    
    StringBuilder s = new StringBuilder("Conversation with ");
    
    List<PodAndUserId>  members = new ArrayList<>(streamAttributes.getMembers().getElements().size());
    
    for(Long member : streamAttributes.getMembers().getElements())
    {
      if(!member.equals(userApi_.getUserId().asLong()))
      {
        members.add(PodAndUserId.newBuilder().build(member));
      }
    }
    
    s.append(getUser(members.get(0)));
    
    if(members.size() == 1)
    {
      return s.toString();
    }
    
    for(int i=1 ; i<members.size() - 1 && i < memberLimit; i++)
    {
      s.append(", ");
      s.append(getUser(members.get(i)));
    }
    
    s.append(" and ");
    
    if(members.size() > memberLimit + 1)
    {
      s.append(String.valueOf(members.size() - memberLimit));
      s.append(" others");
    }
    else if(members.size() > memberLimit)
    {
      s.append(" one other");
    }
    else
    {
      s.append(getUser(members.get(members.size() - 1)));
    }
    return s.toString();
  }

  private String getUser(PodAndUserId podAndUserId)
  {
    IUserV2 user = userApi_.fetchUserById(podAndUserId);
    return user.getDisplayName();
  }
}
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

import java.util.HashMap;
import java.util.Map;

class BaseRendererManager
{
  final Map<Class<? extends Projection<?>>, IRenderer<?>> consumerMap_ = new HashMap<>();
  final ProjectorManager projectorManager_;
  
  BaseRendererManager(ProjectorManager projectorManager)
  {
    projectorManager_ = projectorManager;
  }
  
  <T extends Projection<?>> void with(Class<T> type, IRenderer<T> consumer)
  {
    consumerMap_.put(type, consumer);
  }
  
  @SuppressWarnings("unchecked")
  <T extends Projection<?>> IRenderer<Projection<?>> getConsumer(Class<T> type)
  {
    return (IRenderer<Projection<?>>) consumerMap_.get(type);
  }
  
  String toJson(Projection.AbstractAttribute<?, ?> projection)
  {
    return toJson(projection, projection.getValue().toString(), null);
  }
  
  String toJson(Projection.AbstractAttribute<?, ?> projection, String text)
  {
    return toJson(projection, text, projection.getValue().toString());
  }
  
  String toJson(Projection.AbstractAttribute<?, ?> projection, String text, String editText)
  {
    StringBuilder s = new StringBuilder();

    s.append('"');
    s.append(projection.getName());
    s.append("\": {\"text\": \"");
    s.append(text);
    s.append('"');
    
    s.append(", \"id\": \"");

    s.append(projection.getId());
    s.append('"');

    if(projection.getHoverText() != null)
    {
      s.append(", \"hoverText\": \"");

      s.append(projection.getHoverText());
      s.append('"');
    }
    
    if(projection.isEditable())
    {
      s.append(", \"editable\": true");
    }
    
    if(editText != null)
    {
      s.append(", \"editText\": \"");

      s.append(editText);
      s.append('"');
    }
    
    s.append('}');

    return s.toString();
  }
}

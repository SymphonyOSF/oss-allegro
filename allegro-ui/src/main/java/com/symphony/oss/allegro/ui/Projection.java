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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.symphony.oss.commons.fluent.Fluent;
import com.symphony.oss.commons.hash.Hash;

public class Projection<T extends Projection<T>> extends Fluent<T>
{
  public static final String ATTRIBUTE_SORT_KEY       = "Sort Key";
  public static final String ATTRIBUTE_HEADER_TYPE    = "Header Type";
  public static final String ATTRIBUTE_PAYLOAD_TYPE   = "Payload Type";
  public static final String ATTRIBUTE_ABSOLUTE_HASH  = "Absolute Hash";
  public static final String ATTRIBUTE_BASE_HASH      = "Base Hash";
  public static final String ATTRIBUTE_PARTITION_HASH = "Partition Hash";
  
  public Projection(Class<T> type)
  {
    super(type);
  }

  public static class Error extends Projection<Error>
  {
    private String message_;

    public Error(String message)
    {
      super(Error.class);
      
      message_ = message;
    }

    public String getMessage()
    {
      return message_;
    }
  }
  
  public static class AbstractAttribute<T extends AbstractAttribute<T,P>, P> extends Projection<T>
  {
    private String name_;
    private P value_;
    private String hoverText_;
    private boolean hiddenByDefault_;
    
    public AbstractAttribute(Class<T> type, String name, P value)
    {
      super(type);
      
      name_ = name;
      value_ = value;
    }

    public T withHoverText(String hoverText)
    {
      hoverText_ = hoverText;
      
      return self();
    }

    public T withHiddenByDefault(boolean hiddenByDefault)
    {
      hiddenByDefault_ = hiddenByDefault;
      
      return self();
    }

    public String getName()
    {
      return name_;
    }

    public P getValue()
    {
      return value_;
    }

    public String getHoverText()
    {
      return hoverText_;
    }
  }
  
  public static class Attribute extends AbstractAttribute<Attribute, String>
  {
    public Attribute(String name, String value)
    {
      super(Attribute.class, name, value);
    }
  }
  
  public static class ErrorAttribute extends Attribute
  {
    public ErrorAttribute(String name, String value)
    {
      super(name, value);
    }
  }
  
  public static class AbsoluteHashAttribute extends AbstractAttribute<AbsoluteHashAttribute, Hash>
  {
    public AbsoluteHashAttribute(Hash value)
    {
      super(AbsoluteHashAttribute.class, ATTRIBUTE_ABSOLUTE_HASH, value);
    }
  }
  
  public static class BaseHashAttribute extends AbstractAttribute<BaseHashAttribute, Hash>
  {
    public BaseHashAttribute(Hash value)
    {
      super(BaseHashAttribute.class, ATTRIBUTE_BASE_HASH, value);
    }
  }
  
  public static class PartitionHashAttribute extends AbstractAttribute<PartitionHashAttribute, Hash>
  {
    public PartitionHashAttribute(Hash value)
    {
      super(PartitionHashAttribute.class, ATTRIBUTE_PARTITION_HASH, value);
    }
  }
  
  public static class AttributeSet extends Projection<AttributeSet>
  {
    private Map<String, AbstractAttribute<?,?>> attributeMap_ = new HashMap<>();
    
    public AttributeSet()
    {
      super(AttributeSet.class);
    }
    
    public AttributeSet with(AbstractAttribute<?,?> attribute)
    {
      attributeMap_.put(attribute.getName(), attribute);
      
      return this;
    }
    
    public AttributeSet with(String name, Object value)
    {
      attributeMap_.put(name, new Attribute(name, value == null ? "" : value.toString()));
      
      return self();
    }

    public Map<String, AbstractAttribute<?,?>> getAttributeMap()
    {
      return attributeMap_;
    }

    public Collection<AbstractAttribute<?,?>> getAttributes()
    {
      return attributeMap_.values();
    }
  }
}

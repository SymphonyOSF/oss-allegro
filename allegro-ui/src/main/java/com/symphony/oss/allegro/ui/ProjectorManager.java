/*
 *
 *
 * Copyright 2019 Symphony Communication Services, LLC.
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.symphony.oss.allegro.ui.Projection.Attribute;
import com.symphony.oss.allegro.ui.Projection.AttributeSet;
import com.symphony.oss.commons.dom.json.IImmutableJsonDomNode;
import com.symphony.oss.commons.dom.json.ImmutableJsonList;
import com.symphony.oss.commons.dom.json.ImmutableJsonObject;
import com.symphony.oss.commons.dom.json.ImmutableJsonSet;
import com.symphony.oss.commons.fluent.BaseAbstractBuilder;
import com.symphony.oss.models.object.canon.IAbstractStoredApplicationObject;
import com.symphony.oss.models.object.canon.facade.IApplicationObjectPayload;
import com.symphony.oss.models.object.canon.facade.IStoredApplicationObject;

/**
 * Manager of Projectors.
 * 
 * @author Bruce Skingle
 */
public class ProjectorManager
{
  static final ImmutableSet<String> reservedAttributes_ = new ImmutableSet.Builder<String>()
      .add("baseHash")
      .add("prevHash")
      .add("sortKey")
      .add("partitionHash")
      .add("headerType")
      .add("payloadType")
      .build();
  
  private final ImmutableMap<Class<?>, IProjector<?, ?>> projectorMap_;
  private final ImmutableList<Class<?>>                  projectorTypeList_;
  private final IProjector<Object, ?>                    defaultProjector_;
    
  ProjectorManager(AbstractBuilder<?,?> builder)
  {
    projectorMap_          = ImmutableMap.copyOf(builder.projectorMap_);
    projectorTypeList_     = ImmutableList.copyOf(builder.projectorTypeList_);
    defaultProjector_      = builder.defaultProjector_;
  }
  
  /**
   * Builder for RendererManager.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, ProjectorManager>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected ProjectorManager construct()
    {
      return new ProjectorManager(this);
    }
  }
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  public static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends ProjectorManager> extends BaseAbstractBuilder<T,B>
  {
    private Map<Class<?>, IProjector<?, ?>>                                projectorMap_      = new HashMap<>();
    private List<Class<?>>                                                 projectorTypeList_ = new LinkedList<>();
    private IProjector<IApplicationObjectPayload, Projection.AttributeSet> payloadProjector_;
    private IProjector<Object, ?>                                          defaultProjector_  = new IProjector<Object, Projection.Error>()
      {
        @Override
        public Projection.Error project(Object payload)
        {
          return new Projection.Error("No projector found for type " + payload.getClass());
        }
      };
    
    AbstractBuilder(Class<T> type)
    {
      super(type);
      
      IProjector<IAbstractStoredApplicationObject, Projection.AttributeSet> abstractStoredObjectProjector = addProjector(IAbstractStoredApplicationObject.class, (object) ->
      {
        return new Projection.AttributeSet()
            .with(Projection.ATTRIBUTE_SORT_KEY, Projection.ATTRIBUTEID_SORT_KEY, object.getSortKey())
            .with(new Projection.AbsoluteHashAttribute(object.getAbsoluteHash()))
            .with(new Projection.BaseHashAttribute(object.getBaseHash()))
            .with(new Projection.PartitionHashAttribute(object.getPartitionHash()))
            ;
      });
      
      IProjector<IStoredApplicationObject, Projection.AttributeSet> storedObjectProjector = addProjector(IStoredApplicationObject.class, (object) ->
      {
        Projection.AttributeSet projection = abstractStoredObjectProjector.project(object)
            ;
        
        if(object.getHeader() != null)
        {
          String  fullType  = object.getHeader().getCanonType();
          int     i         = fullType.lastIndexOf('.');
          
          if(i == -1)
            projection.with(Projection.ATTRIBUTE_HEADER_TYPE, Projection.ATTRIBUTEID_HEADER_TYPE, fullType);
          else
            projection.with(
                new Projection.Attribute(Projection.ATTRIBUTE_HEADER_TYPE, Projection.ATTRIBUTEID_HEADER_TYPE, fullType.substring(i+1))
                  .withHoverText(fullType)
                );
          
          if(object.getEncryptedPayload() != null)
            projection.with(new Projection.ErrorAttribute(Projection.ATTRIBUTE_PAYLOAD_TYPE, Projection.ATTRIBUTEID_PAYLOAD_TYPE, "Unable to Decrypt"));
        }
        
        return projection;
      });
      
      payloadProjector_ = new IProjector<IApplicationObjectPayload, Projection.AttributeSet>()
      {
        @Override
        public AttributeSet project(IApplicationObjectPayload object)
        {
          Projection.AttributeSet projection = storedObjectProjector.project(object.getStoredApplicationObject())
              ;
          
          String  fullType  = object.getCanonType();
          int     i         = fullType.lastIndexOf('.');
          
          if(i == -1)
            projection.with(Projection.ATTRIBUTE_PAYLOAD_TYPE, Projection.ATTRIBUTEID_PAYLOAD_TYPE, fullType);
          else
            projection.with(
                new Projection.Attribute(Projection.ATTRIBUTE_PAYLOAD_TYPE, Projection.ATTRIBUTEID_PAYLOAD_TYPE, fullType.substring(i+1))
                  .withHoverText(fullType)
                );
          
          return projection;
        }
    
      };
      
      withProjector(IApplicationObjectPayload.class, (projection, object) ->
      {
        
        projectJsonObject(projection, object.getJsonObject(), "");
        
      });
    }
    
    private void projectJsonObject(AttributeSet projection, ImmutableJsonObject json, String prefix)
    {
      Iterator<String> it = json.getNameIterator();
      
      while(it.hasNext())
      {
        String name = it.next();
        
        if(!name.startsWith("_"))
        {
          if(prefix.length() > 0 || !reservedAttributes_.contains(name))
            projectJsonItem(projection, json.get(name), prefix.length()==0 ? name : prefix + "." + name);
        }
      }
    }

    private void projectJsonItem(AttributeSet projection, IImmutableJsonDomNode item, String prefix)
    {
      if(item instanceof ImmutableJsonObject)
      {
        projectJsonObject(projection, (ImmutableJsonObject) item, prefix);
      }
      else if(item instanceof ImmutableJsonList)
      {
        projectJsonArray(projection, ((ImmutableJsonList) item).iterator(), prefix);
      }
      else if(item instanceof ImmutableJsonSet)
      {
        projectJsonArray(projection, ((ImmutableJsonSet) item).iterator(), prefix);
      }
      else
      {
        projection.with(new Projection.Attribute(prefix, prefix, item.toString())
            .withEditable(true));
      }
    }

    private void projectJsonArray(AttributeSet projection, Iterator<IImmutableJsonDomNode> it, String prefix)
    {
      int i=0;
      
      while(it.hasNext())
      {
        projectJsonItem(projection, it.next(), prefix.length()==0 ? String.valueOf(i) : prefix + "." + i);
        
        i++;
      }
    }
    
    class ProjectionAdaptor<P extends IApplicationObjectPayload> implements IProjector<P, Projection.AttributeSet>
    {
      private final IProjectionEnricher<P> enricher_;
      
      public ProjectionAdaptor(IProjectionEnricher<P> enricher)
      {
        enricher_ = enricher;
      }

      @Override
      public AttributeSet project(P payload)
      {
        AttributeSet projection = payloadProjector_.project(payload);
        
        enricher_.project(projection, payload);
        
        return projection;
      }
    }
    
    /**
     * Return the default projector which is used as a last resort if no matching projector is defined.
     * 
     * @return the default projector which is used as a last resort if no matching projector is defined.
     */
    public IProjector<Object,?> getDefaultProjector()
    {
      return defaultProjector_;
    }
    
    /**
     * Add the given projector.
     * 
     * @param <P>         The concrete type of the payload on which this projector operates.
     * @param type        The concrete type of the payload on which this projector operates.
     * @param projector   The projector.
     * 
     * @return This (fluent method).
     */
    public <P extends IApplicationObjectPayload> T withProjector(Class<P> type, IProjectionEnricher<P> projector)
    {
      projectorMap_.put(type, new ProjectionAdaptor<P>(projector));
      projectorTypeList_.add(type);
      
      return self();
    }

    /**
     * Add the given projector and return it.
     * 
     * @param <C>         The concrete type of the payload on which this projector operates.
     * @param <P>         The concrete type of the projection returned by this projector.
     * @param type        The concrete type of the payload on which this projector operates.
     * @param projector   The projector.
     * 
     * @return            The projector.
     */
    <C,P extends Projection<?>> IProjector<C,P> addProjector(Class<C> type, IProjector<C,P> projector)
    {
      projectorMap_.put(type, projector);
      projectorTypeList_.add(type);
      
      return projector;
    }
    
    /**
     * Set the default projector which is used as a last resort if no matching projector is defined.
     * 
     * @param defaultProjector the default projector which is used as a last resort if no matching projector is defined.
     * 
     * @return This (fluent method).
     */
    public T withDefaultProjector(IProjector<Object,?> defaultProjector)
    {
      defaultProjector_ = defaultProjector;
      
      return self();
    }
  }

  /**
   * Dispatch the given object to the most appropriate projector.
   * 
   * @param partitionObject An object to be consumed.
   * 
   * @return A Projection of the given object containing the attributes to be rendered.
   */
  public Projection<?> project(PartitionObject<?> partitionObject)
  {
    Projection<?> result =  null;
    IApplicationObjectPayload payload = partitionObject.getPayloadUnchecked();
    
    if(payload != null)
    {
      result = consume(payload);
    }
    
    if(result == null)
      result = consume(partitionObject.getStoredObject());
    
    if(result == null)
      result = defaultProjector_.project(partitionObject.getStoredObject());
    
    return result;
  }
  
  

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private Projection consume(Object object)
  {
    Class<? extends Object> type = object.getClass();
    IProjector projector = projectorMap_.get(type);
    
    if(projector != null)
    {
      return projector.project(object);
    }
    
    Class<?> bestType = null;
    
    for(Class<?> t : projectorTypeList_)
    {
      if(!t.isAssignableFrom(type))
        continue;
      
      if(bestType == null || bestType.isAssignableFrom(t))
        bestType = t;
      
    }
    
    if(bestType == null)
      return null;
    
    return ((IProjector)projectorMap_.get(bestType)).project(object);
  }
}

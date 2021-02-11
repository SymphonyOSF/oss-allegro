/*
 *
 *
 * Copyright 2021 Symphony Communication Services, LLC.
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

package com.symphony.oss.allegro2.mongo.api;

import org.bson.Document;

import com.symphony.oss.allegro2.api.AllegroConsumerManager;
import com.symphony.oss.canon.runtime.IModelRegistry;

/**
 * ConsumerManager implementation for Allegro2MongoApi.
 * 
 * @author Bruce Skingle
 *
 */
public class AllegroMongoConsumerManager extends AllegroConsumerManager
{
  private final Allegro2MongoApi allegroMongoDecryptor_;
  
  AllegroMongoConsumerManager(AbstractBuilder<?,?> builder)
  {
    super(builder);
    
    allegroMongoDecryptor_ = builder.allegroMongoDecryptor_;
  }
  
  /**
   * AbstractBuilder.
   * 
   * @author Bruce Skingle
   *
   * @param <T> Concrete type of the builder for fluent methods.
   * @param <B> Concrete type of the built object for fluent methods.
   */
  static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends AllegroMongoConsumerManager> extends AllegroConsumerManager.AbstractBuilder<T,B>
  {
    private final Allegro2MongoApi allegroMongoDecryptor_;

    AbstractBuilder(Class<T> type, Allegro2MongoApi allegroMongoDecryptor, IModelRegistry modelRegistry)
    {
      super(type, allegroMongoDecryptor, modelRegistry);
      
      allegroMongoDecryptor_ = allegroMongoDecryptor;
    }
  }
  
  static class Builder extends AbstractBuilder<Builder, AllegroMongoConsumerManager>
  {
    Builder(Allegro2MongoApi allegroMongoDecryptor, IModelRegistry modelRegistry)
    {
      super(Builder.class, allegroMongoDecryptor, modelRegistry);
    }

    @Override
    protected AllegroMongoConsumerManager construct()
    {
      return new AllegroMongoConsumerManager(this);
    }
  }

  /**
   * Iterate over the given input and handle each Document contained.
   *  
   * @param iterable A source of Documents to be handled.
   */
  public void accept(Iterable<Document> iterable)
  {
    for(Document doc : iterable)
    {
      accept(doc);
    }
  }

  /**
   * Handle the give Document.
   * 
   * @param doc A Document to be handled.
   */
  public void accept(Document doc)
  {
    accept(allegroMongoDecryptor_.parse(doc));
  }
}

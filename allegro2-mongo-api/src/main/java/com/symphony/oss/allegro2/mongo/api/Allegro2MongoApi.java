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

import com.symphony.oss.allegro2.api.Allegro2Api;
import com.symphony.oss.models.core.canon.facade.IApplicationRecord;
import com.symphony.oss.models.core.canon.facade.IEncryptedApplicationRecord;

public class Allegro2MongoApi extends Allegro2Api implements IAllegro2MongoApi
{
  protected Allegro2MongoApi(AbstractBuilder<?, ?> builder)
  {
    super(builder);
  }
  
  /**
   * The builder implementation.
   * 
   * This is implemented as an abstract class to allow for sub-classing in future.
   * 
   * Any sub-class of AllegroApi would need to implement its own Abstract sub-class of this class
   * and then a concrete Builder class which is itself a sub-class of that.
   * 
   * @author Bruce Skingle
   *
   * @param <T> The type of the concrete Builder
   * @param <B> The type of the built class, some subclass of AllegroApi
   */
  static abstract class AbstractBuilder<T extends AbstractBuilder<T,B>, B extends IAllegro2MongoApi>
  extends Allegro2Api.AbstractBuilder<T, B>
  {
    AbstractBuilder(Class<T> type)
    {
      super(type);
    }
  }
  
  /**
   * Builder.
   * 
   * @author Bruce Skingle
   *
   */
  public static class Builder extends AbstractBuilder<Builder, IAllegro2MongoApi>
  {
    /**
     * Constructor.
     */
    public Builder()
    {
      super(Builder.class);
    }

    @Override
    protected IAllegro2MongoApi construct()
    {
      return new Allegro2MongoApi(this);
    }
  }
  
  @Override
  public EncryptedDocumentBuilder newEncryptedDocumentBuilder()
  {
    return new EncryptedDocumentBuilder(newApplicationRecordBuilder());
  }
  
  IEncryptedApplicationRecord parse(Document doc)
  {
    return parse(doc.toJson());
  }

  @Override
  public IApplicationRecord decrypt(Document doc)
  {
    return decryptObject(parse(doc));
  }

  @Override
  public AllegroMongoConsumerManager.AbstractBuilder<?,?> newConsumerManagerBuilder()
  {
    return new AllegroMongoConsumerManager.Builder(this, getModelRegistry());
  }
}

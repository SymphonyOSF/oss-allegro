/*
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

package com.symphony.oss.allegro2.api;

import org.symphonyoss.symphony.messageml.util.IUserPresentation;

class UserPresentation implements IUserPresentation
{
  private final long   userId_;
  private final String screenName_;
  private final String prettyName_;
  private final String email_;

  UserPresentation(long userId, String screenName, String prettyName, String email)
  {
    userId_ = userId;
    screenName_ = screenName;
    prettyName_ = prettyName;
    email_ = email;
  }

  @Override
  public long getId()
  {
    return userId_;
  }

  @Override
  public String getScreenName()
  {
    return screenName_;
  }

  @Override
  public String getPrettyName()
  {
    return prettyName_;
  }

  @Override
  public String getEmail()
  {
    return email_;
  }

}

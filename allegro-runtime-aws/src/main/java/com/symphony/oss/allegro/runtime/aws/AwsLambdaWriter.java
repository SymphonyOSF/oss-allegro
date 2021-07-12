/*
 * Copyright 2020 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package com.symphony.oss.allegro.runtime.aws;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class AwsLambdaWriter extends Writer
{
  private static final String RESPONSE_STATUS = "statusCode";
  private static final String RESPONSE_BODY = "body";
  
  private boolean first = true;
  private String head = "{\"" + RESPONSE_STATUS + "\":200,\"" + RESPONSE_BODY + "\":\"";
  private String tail = "\"}";
  
  private OutputStreamWriter os_;

  public AwsLambdaWriter(OutputStream os)
  {
    os_ = new OutputStreamWriter(os);
  }

  @Override
  public void write(String ss) throws IOException
  {
    if (first)
    {
      os_.write(head);
      first = false;
    }
    
    os_.write(fmtBody(ss));
    os_.flush();
  }

  @Override
  public void close() throws IOException
  {
    os_.write(tail);
    os_.close();
  }
  
  private String fmtBody(String ss)
  {
    return ss.replaceAll("\\\"", "\\\\\"").replaceAll("\n", "\\\\n");
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException
  {
    os_.write(cbuf, off, len);
  }

  @Override
  public void flush() throws IOException
  {
    os_.flush();   
  }


}

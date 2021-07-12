/*
 * Copyright 2018-2020 Symphony Communication Services, LLC.
 *
 * All Rights Reserved
 */

package com.symphony.oss.allegro.runtime.aws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.symphony.oss.canon.runtime.IEntityHandler;
import com.symphony.oss.canon.runtime.IHandlerContainer;
import com.symphony.oss.canon.runtime.IModelRegistry;
import com.symphony.oss.canon.runtime.exception.CanonException;
import com.symphony.oss.canon.runtime.http.HttpMethod;
import com.symphony.oss.canon.runtime.http.ICorsHandler;
import com.symphony.oss.fugue.aws.lambda.AwsLambdaRequest;
import com.symphony.oss.fugue.trace.ITraceContext;
import com.symphony.oss.fugue.trace.ITraceContextTransaction;
import com.symphony.oss.fugue.trace.ITraceContextTransactionFactory;

public class AwsLambdaHandlerContainer implements IHandlerContainer
{
  private static final Logger log_ = LoggerFactory.getLogger(AwsLambdaHandlerContainer.class);
  
  private final ITraceContextTransactionFactory traceFactory_;
  private final IModelRegistry                  modelRegistry_;
  private final TreeMap<Integer, List<IEntityHandler>> handlerMap_   = new TreeMap<>(new Comparator<Integer>()
  {
    /*
     * We want the map in descending order.
     */
    @Override
    public int compare(Integer a, Integer b)
    {
      if(a>b)
        return -1;
      
      if(a<b)
        return 1;
      
      return 0;
    }});

  private ICorsHandler corsHandler_;

  public AwsLambdaHandlerContainer(ITraceContextTransactionFactory traceFactory, IModelRegistry modelRegistry)
  {
    traceFactory_ = traceFactory;
    modelRegistry_ = modelRegistry;
    log_.info("Cors enabled AwsLambdaHandlerContainer");
  }
  
  @Override
  public AwsLambdaHandlerContainer withHandler(IEntityHandler handler)
  {
    List<IEntityHandler> list = handlerMap_.get(handler.getPartsLength());
    
    if(list == null)
    {
      list = new ArrayList<>();
      handlerMap_.put(handler.getPartsLength(), list);
    }
    
    list.add(handler);
    
    return this;
  }
  
  @Override
  public AwsLambdaHandlerContainer withCorsHandler(ICorsHandler corsHandler)
  {
    corsHandler_ = corsHandler;
    log_.info("withCorsHandler  CorsHandler=" + corsHandler);
    
    return this;
  }

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context lambdaContext)
  {
    AwsLambdaRequest request = new AwsLambdaRequest(inputStream);
    
    try(ITraceContextTransaction traceTransaction = traceFactory_.createTransaction("HTTP " + request.getHttpMethod(), request.getAwsRequestId()))
    {
      ITraceContext trace = traceTransaction.open();
      AwsLambdaRequestContext context = new AwsLambdaRequestContext(trace, modelRegistry_, request, outputStream);
      
      if(corsHandler_ != null)
      {
        if(corsHandler_.handle(context) && context.getMethod() == HttpMethod.Options)
        {
          log_.info("CORS handled OPTIONS request");
          context.setStatus(HttpServletResponse.SC_OK);
          context.sendResponse(outputStream);
          traceTransaction.finished();
          return;
        }
      }
      
      for(List<IEntityHandler> list : handlerMap_.values())
      {
        for(IEntityHandler handler : list)
        {
          if(handle(handler, context))
          {
            trace.trace("SEND_RESPONSE");
            context.sendResponse(outputStream);
            traceTransaction.finished();
            return;
          }
        }
      }
      
      context.error("No handler found for " + context.getPathInfo());
      context.sendErrorResponse(HttpServletResponse.SC_NOT_FOUND);
      context.sendResponse(outputStream);
      traceTransaction.aborted();
    }
  }

  

  private boolean handle(IEntityHandler handler, AwsLambdaRequestContext context)
  {
    try
    {
      return handler.handle(context);
    }
    catch(CanonException e)
    {
      log_.error("Failed to handle request", e);
      
      context.sendErrorResponse(e.getHttpStatusCode());
      
      return true;
    }
    catch(RuntimeException | IOException e)
    {
      log_.error("Failed to handle request", e);
      
      context.sendErrorResponse(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      
      return true;
    }
  }
}

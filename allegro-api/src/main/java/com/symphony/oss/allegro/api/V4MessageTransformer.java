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

package com.symphony.oss.allegro.api;

import org.symphonyoss.symphony.messageml.MessageMLContext;
import org.symphonyoss.symphony.messageml.elements.MessageML;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

class V4MessageTransformer
{

  private static final ObjectMapper MAPPER = new ObjectMapper();
//  private static final JsonFactory FACTORY = MAPPER.getFactory();

  private static final String TEXT = "text";
//  private static final String AGENT = "Agent";
  private static final String ENTITIES = "entities";
  private static final String ATTACHMENTS = "attachments";
  private static final String CHIME = "isChime";
  private static final String VERSION = "version";
  private static final String FORMAT = "format";
  private static final String SENDING_APP = "sendingApp";
  private static final String LC = "lc";
  private static final String THREAD_ID = "threadId";
  private static final String CLIENT_VERSION_INFO = "clientVersionInfo";
  private static final String MESSAGE_FORMAT = "com.symphony.messageml.v2";
  private static final String VERSION_SOCIALMESSAGE = "SOCIALMESSAGE";
  private static final String PRESENTATIONML = "presentationML";
  private static final String ENTITY_JSON = "entityJSON";
//  private static final String FROM = "from";
//  private static final String ID = "id";
//  private static final String ORIGINATING_SYSTEM_ID = "originatingSystemId";
//  private static final String INGESTION_DATE = "ingestionDate";
//  private static final String IS_IMPORTED = "isImported";
//  private static final String ORIGINAL_MESSAGE_ID = "originalMessageId";
  private static final String DLP_ENFORCE_EXPRESSION_FILTERING = "enforceExpressionFiltering";
  
  private final String clientType_;

  public V4MessageTransformer(String clientType)
  {
    clientType_ = clientType;
  }


  /**
   * Create a SocialMessage payload to be sent to the back end. Populates the JSON fields for PresentationML, EntityJSON,
   * Markdown, legacy entities and message metadata.
   * @param context message context used to generate the respective representations of the message
   * @param threadId the ID of the thread to which the message belongs
   * @return JSON payload representing the Social Message
   * @throws JsonProcessingException thrown on errors processing the JSON contents of the message
   */
  JsonNode createSocialMessage(MessageMLContext context, String threadId, boolean dlpEnforceExpressionFiltering) throws JsonProcessingException {
    ObjectNode result = JsonNodeFactory.instance.objectNode();
    result.put(VERSION, VERSION_SOCIALMESSAGE);
    result.put(SENDING_APP, LC);
    result.put(THREAD_ID, threadId);
    result.put(CLIENT_VERSION_INFO, clientType_);
    result.set(ATTACHMENTS, JsonNodeFactory.instance.arrayNode());
    result.put(FORMAT, MESSAGE_FORMAT);
    result.put(TEXT, context.getMarkdown());
    result.set(ENTITIES, context.getEntities());
    result.put(PRESENTATIONML, context.getPresentationML());
    result.put(DLP_ENFORCE_EXPRESSION_FILTERING, dlpEnforceExpressionFiltering);

    JsonNode entityJson = context.getEntityJson();
    MessageML messageML = context.getMessageML();

    if (entityJson != null && entityJson.size() > 0) {
      result.put(ENTITY_JSON, MAPPER.writeValueAsString(entityJson));
    }

    if (messageML.isChime()) {
      result.put(CHIME, true);
    }

    return result;
  }
  

//
//  /**
//   * Convert an API object representing an imported message to a SocialMessage payload sent to the back end.
//   * Populates the JSON fields for PresentationML, EntityJSON, Markdown, legacy entities and message metadata.
//   * @param message the API object representing an imported message
//   * @param context message context used to generate the respective representations of the message
//   * @param threadId the ID of the thread to which the message belongs
//   * @return JSON payload representing the Social Message
//   * @throws JsonProcessingException thrown on errors processing the JSON contents of the message
//   */
//  public JsonNode createImportedSocialMessage(V4ImportedMessage message, MessageMLContext context, String threadId)
//      throws JsonProcessingException {
//    ObjectNode result = (ObjectNode) createSocialMessage(context, threadId);
//
//    result.put(ORIGINATING_SYSTEM_ID, message.getOriginatingSystemId());
//    result.put(INGESTION_DATE, message.getIntendedMessageTimestamp());
//    result.put(IS_IMPORTED, true);
//
//    ObjectNode intendedFromUserId = new ObjectNode(JsonNodeFactory.instance);
//    intendedFromUserId.put(ID, message.getIntendedMessageFromUserId());
//    result.set(FROM, intendedFromUserId);
//
//    if (StringUtils.isNotBlank(message.getOriginalMessageId())) {
//      result.put(ORIGINAL_MESSAGE_ID, message.getOriginalMessageId());
//    }
//
//    return result;
//  }
//
//  private V4EventParser determineParser(V4EventType eventType) {
//    if (eventType != null) {
//      return eventParsers.get(eventType);
//    } else {
//      return new V4EventParserUnsupported(null);
//    }
//  }
//
//  public V4EventType getEventTypeForMessage(JsonNode message) {
//    if (jsonMessageIsSocialMessage(message)) {
//      return determineEventForSocialMessage(message);
//    } else if (jsonMessageIsMaestroMessage(message)) {
//      return determineEventForMaestroMessage(message);
//    } else {
//      return null;
//    }
//  }
//
//  private V4EventType determineEventForMaestroMessage(JsonNode message) {
//    String maestroEventString = message.get("event").asText();
//    V4KnownEventType.V4Events maestroEventType = getV4Events(maestroEventString);
//    if (maestroEventType != null) {
//      return convertMaestroEvent(maestroEventType, message);
//    } else {
//      return null;
//    }
//  }
//
//  private V4KnownEventType.V4Events getV4Events(String maestroEventString) {
//    V4KnownEventType.V4Events maestroEventType;
//    try {
//      maestroEventType = V4KnownEventType.V4Events.valueOf(maestroEventString);
//    } catch (NullPointerException | IllegalArgumentException e) {
//      LOG.debug("Invalid event received: " + maestroEventString, e);
//      maestroEventType = null;
//    }
//    return maestroEventType;
//  }
//
//  private V4EventType convertMaestroEvent(V4KnownEventType.V4Events maestroEventType, JsonNode message) {
//    JsonNode payload = message.path("payload");
//    switch (maestroEventType) {
//      case CREATE_ROOM:
//        return V4EventType.ROOMCREATED;
//      case DEACTIVATE_ROOM:
//        return V4EventType.ROOMDEACTIVATED;
//      case REACTIVATE_ROOM:
//        return V4EventType.ROOMREACTIVATED;
//      case UPDATE_ROOM:
//        return V4EventType.ROOMUPDATED;
//      case LEAVE_ROOM:
//        return V4EventType.USERLEFTROOM;
//      case JOIN_ROOM:
//        return V4EventType.USERJOINEDROOM;
//      case MEMBER_MODIFIED:
//        return determineMemberModifiedSubEvent(payload);
//      case CONNECTION_REQUEST_ALERT:
//        return determineConnectionEvent(payload);
//      case CREATE_IM:
//        return V4EventType.INSTANTMESSAGECREATED;
//      case MESSAGE_SUPPRESSION:
//        return V4EventType.MESSAGESUPPRESSED;
//      default:
//        LOG.warn("Unexpected event type received: ", maestroEventType);
//        return null;
//    }
//  }
//
//  private V4EventType determineConnectionEvent(JsonNode payload) {
//    JsonNode cargoNode = payload.path("cargo");
//    String status = cargoNode.path("status").asText("");
//    if (status.equalsIgnoreCase(ConnectionStatus.PENDING_INCOMING.getDescription())) {
//      return V4EventType.CONNECTIONREQUESTED;
//    } else if (status.equalsIgnoreCase(ConnectionStatus.ACCEPTED.getDescription())) {
//      return V4EventType.CONNECTIONACCEPTED;
//    } else {
//      return null;
//    }
//  }
//
//  private V4EventType determineMemberModifiedSubEvent(JsonNode payload) {
//    JsonNode cargo = payload.path("cargo");
//    Boolean isOwner = jsonMessageFieldParser.parseIsOwner(cargo);
//    return isOwner ? V4EventType.ROOMMEMBERPROMOTEDTOOWNER : V4EventType.ROOMMEMBERDEMOTEDFROMOWNER;
//  }
//
//  private V4EventType determineEventForSocialMessage(JsonNode message) {
//    if (message.has("share")) {
//      return V4EventType.SHAREDPOST;
//    } else {
//      return V4EventType.MESSAGESENT;
//    }
//  }
//
//  /**
//   * Converts an exchange envelope to a JsonNode
//   * @param exchangeEnvelope {@link ExchangeEnvelope} which contains the firehose event
//   * @return a JsonNode which
//   */
//  private JsonNode toJsonNode(ExchangeEnvelope exchangeEnvelope) {
//    try {
//      JsonParser parser = FACTORY.createParser(exchangeEnvelope.toJson());
//      Optional<JsonNode> node = Optional.ofNullable(MAPPER.<JsonNode>readTree(parser));
//
//      return node.map(n -> n.get("message")).orElse(null);
//    } catch (IOException e) {
//      LOG.error("Unparseable message {}", exchangeEnvelope.toString());
//      return null;
//    }
//  }
//
//  /**
//   * Extract {@link V4Event}
//   *
//   * TODO: Add error handling if there is an error while getting the event
//   *
//   * @param session The session which can be used for decrypting the message
//   * @param exchangeEnvelope The exchange envelope which contains the message
//   * @return {@link V4Event}
//   */
//  public V4Event extractV4Event(SbeSession session, ExchangeEnvelope exchangeEnvelope){
//    JsonNode actualObj = toJsonNode(exchangeEnvelope);
//    return actualObj != null ? getEventFromEncryptedMessage(session, actualObj) : null;
//  }
}

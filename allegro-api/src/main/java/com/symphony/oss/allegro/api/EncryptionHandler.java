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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.symphonyoss.s2.common.fault.CodingFault;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.symphony.oss.models.core.canon.facade.ThreadId;

/**
 * This class provides helper methods for encrypting binary data and JSON payloads of Symphony messages.
 */
class EncryptionHandler {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static final int ENCRYPTION_ORDINAL = 0;
  private static final int RICHTEXT_ORDINAL = 1;
  private static final int MEDIA_ENCRYPTION_ORDINAL = 2;

  private static final String ENCRYPTED_ENTITIES_NODE = "encryptedEntities";
  private static final String ENTITIES_NODE = "entities";
  private static final String HASHTAGS_NODE = "hashtags";
  private static final String USER_MENTIONS_NODE = "userMentions";
  private static final String TOKEN_IDS_FLD = "tokenIds";
  private static final String ENCRYPTED_MEDIA_NODE = "encryptedMedia";
  private static final String CUSTOM_ENTITIES_NODE = "customEntities";
  private static final String MESSAGEML_NODE = "messageML";
  private static final String PRESENTATIONML_NODE = "presentationML";
  private static final String ENTITYJSON_NODE = "entityJSON";

  private static final String MSG_FEATURES_FLD = "msgFeatures";
  private static final String INDEX_START_FLD = "indexStart";
  private static final String INDEX_END_FLD = "indexEnd";
  private static final String URLS_FLD = "urls";
  private static final String USER_TYPE_FLD = "userType";
  private static final String TEXT_FLD = "text";
  private static final String PRETTY_NAME_FLD = "prettyName";
  private static final String SCREEN_NAME_FLD = "screenName";
  private static final String ID_FLD = "id";
  private static final String TYPE_FLD = "type";

  private final AllegroCryptoClient cryptoClient_;

  public EncryptionHandler(AllegroCryptoClient cryptoClient)
  {
    cryptoClient_ = cryptoClient;
  }

  //  /**
//   * Encrypt binary data.
//   */
//  public byte[] handleEncrypt(byte[] decrypted, int podId, String streamKey, int msgVersion, long rotationId)
//      throws SymphonyEncryptionException {
//    IDecryptionHelper crypto = new CDecryptionHelper(streamKey);
//    byte[] encrypted = crypto.encrypt(decrypted, msgVersion, podId, rotationId);
//    return encrypted;
//  }
//
//  /**
//   * Encrypt a string.
//   */
//  public String handleEncrypt(String decrypted, int podId, String streamKey, int msgVersion, long rotationId)
//      throws SymphonyEncryptionException {
//    IDecryptionHelper crypto = new CDecryptionHelper(streamKey);
//    String encrypted = crypto.encrypt(decrypted, msgVersion, podId, rotationId);
//    return encrypted;
//  }
//
//  /**
//   * Encrypt an entity with the entity key (perform a lowercase and strip spaces before encryption)
//   * @param decrypted The clear entity (must start with #, $ or ?)
//   * @param entityKey The base64 entity key
//   * @return The encrypted entity
//   */
//  public String handleEncryptWithEntityKey(String decrypted, String entityKey)
//      throws NoSuchAlgorithmException, SymphonyInputException, SymphonyEncryptionException {
//    return EntityCryptoHandler.encrypt(decrypted, Base64.decodeBase64(entityKey));
//  }
//
  /**
   * Encrypt a JSON message payload.
   */
  JsonNode handleEncrypt(ThreadId threadId, JsonNode socialMessage, String plaintextMessage
//      , int podId, String streamKey,
//      String publicKey, String entityKey, long rotationId
      )
  {

    // Ensure immutability of input
    socialMessage = socialMessage.deepCopy();

    // Get text.
    JsonNode textJson = socialMessage.get(TEXT_FLD);
    String text = (textJson != null && !textJson.isNull()) ? textJson.asText() : "";
    // Use both Markdown and PresentationML for search; the tokenizer creates a Set anyway
    String searchTokens = text + " " + ((StringUtils.isNotBlank(plaintextMessage)) ? plaintextMessage : "");
    Set<String> entityTokens = new HashSet<>();

    encryptTextAndMedia(threadId, socialMessage, text);
    encryptNode(threadId, socialMessage, CUSTOM_ENTITIES_NODE);
    addEncryptedEntitiesNode(threadId, socialMessage);
    encryptHashtags(socialMessage, entityTokens);
    encryptMentions(socialMessage);
    encryptUrls(socialMessage);
    addMsgFeaturesNode(socialMessage);
    encryptTokens(threadId, socialMessage, searchTokens, entityTokens);
    encryptNode(threadId, socialMessage, MESSAGEML_NODE);
    encryptTextNode(threadId, socialMessage, PRESENTATIONML_NODE);
    encryptTextNode(threadId, socialMessage, ENTITYJSON_NODE);

    return socialMessage;
  }
//
//  /**
//   * Encrypt a signal using entity key.
//   *
//   * Note: entity key doesn't have rotationId currently
//   * @param clearSignal the JSON payload containing the decrypted signal
//   * @param entityKey the entity key to use for encryption
//   * @return encrypted signal
//   */
//  public JsonNode handleSignalEncrypt(JsonNode clearSignal, String entityKey)
//      throws NoSuchAlgorithmException, SymphonyInputException, SymphonyEncryptionException {
//    ObjectNode encryptedSignal = new ObjectNode(JsonNodeFactory.instance);
//
//    for (Iterator<Map.Entry<String, JsonNode>> it = clearSignal.fields(); it.hasNext(); ) {
//      Map.Entry<String, JsonNode> field = it.next();
//      String nodeName = field.getKey();
//      JsonNode node = field.getValue();
//
//      switch (nodeName) {
//        case "ruleGroup":
//          JsonNode encryptedRules = encryptSignalRules(node, entityKey);
//          encryptedSignal.set(nodeName, encryptedRules);
//          break;
//        default:
//          encryptedSignal.set(nodeName, node);
//      }
//
//    }
//
//    return encryptedSignal;
//  }
//
//  /**
//   * Encrypt signal rules using entity key.
//   *
//   * Note: entity key doesn't have rotationId currently
//   * @param clearRuleGroup the JSON payload containing the decrypted rules
//   * @param entityKey the entity key to use for encryption
//   * @return encrypted signal rules
//   */
//  private JsonNode encryptSignalRules(JsonNode clearRuleGroup, String entityKey)
//      throws NoSuchAlgorithmException, SymphonyInputException, SymphonyEncryptionException {
//
//    ObjectNode encryptedRuleGroup = new ObjectNode(JsonNodeFactory.instance);
//
//    encryptedRuleGroup.set("operator", clearRuleGroup.get("operator"));
//    for (JsonNode clearRule : clearRuleGroup.get("rules")) {
//      ObjectNode encryptedRule = new ObjectNode(JsonNodeFactory.instance);
//      String ruleType = clearRule.get("definitionType").asText();
//
//      for (Iterator<Map.Entry<String, JsonNode>> it = clearRule.fields(); it.hasNext(); ) {
//        Map.Entry<String, JsonNode> field = it.next();
//        String nodeName = field.getKey();
//        JsonNode node = field.getValue();
//
//        switch (nodeName) {
//          case "text":
//          case "id":
//            if ("KEYWORD".equalsIgnoreCase(ruleType)) {
//              String clearKeyword = node.asText();
//              String encryptedKeyword = EntityCryptoHandler.encrypt(clearKeyword, Base64.decodeBase64(entityKey));
//              encryptedRule.put(nodeName, encryptedKeyword);
//            } else if ("USER_FOLLOW".equalsIgnoreCase(ruleType)) {
//              encryptedRule.put(nodeName, node.asText());
//            }
//
//            break;
//
//          default:
//            encryptedRule.set(nodeName, node);
//        }
//      }
//
//      encryptedRuleGroup.withArray("rules").add(encryptedRule);
//    }
//
//    return encryptedRuleGroup;
//  }
//
  /**
   * Encrypt text and media content. Modifies the input parameter "msg".
   */
  private void encryptTextAndMedia(ThreadId threadId, JsonNode msg, String clearText)
  {
    String encryptedText = cryptoClient_.encrypt(threadId, clearText);
    ((ObjectNode) msg).put("text", encryptedText);

    JsonNode mediaNode = msg.get("media");
    String media = null;
    if (mediaNode != null && !mediaNode.isNull()) { media = mediaNode.toString(); }
    if (media != null && !media.isEmpty()) {
      String encryptedMedia = cryptoClient_.encrypt(threadId, media);
      ((ObjectNode) msg).put("encryptedMedia", encryptedMedia);
      ((ObjectNode) msg).remove("media");
    }
  }

  /**
   * Encrypt the JSON node "node". Modifies the input parameter "msg".
   */
  private void encryptNode(ThreadId threadId, JsonNode msg, String node)
  {
    JsonNode nodeJson = msg.get(node);

    if (nodeJson != null && !nodeJson.isNull())
    {
      try
      {
        String nodeText = MAPPER.writeValueAsString(nodeJson);
        
        String encryptedNode = cryptoClient_.encrypt(threadId, nodeText);
        ((ObjectNode) msg).put(node, encryptedNode);
      }
      catch(JsonProcessingException e)
      {
        throw new CodingFault(e);
      }
    }
  }

  /**
   * Encrypt the JSON text node "node". Modifies the input parameter "msg".
   */
  private void encryptTextNode(ThreadId threadId, JsonNode msg, String node)
  {
    JsonNode nodeJson = msg.get(node);

    if (nodeJson != null && !nodeJson.isNull()) {
      String nodeText = nodeJson.textValue();
      
      String encryptedNode = cryptoClient_.encrypt(threadId, nodeText);
      ((ObjectNode) msg).put(node, encryptedNode);
    }
  }

  /**
   * Add a node for encrypted entities. Modifies the input parameter "msg".
   */
  private void addEncryptedEntitiesNode(ThreadId threadId, JsonNode msg)
  {
    JsonNode entities = msg.get(ENTITIES_NODE);

    if (entities != null && !entities.isNull())
    {
      try
      {
        String entitiesStr = MAPPER.writeValueAsString(entities);
        
        String encryptedEntitiesStr = cryptoClient_.encrypt(threadId, entitiesStr);
  
        ((ObjectNode) msg).put(ENCRYPTED_ENTITIES_NODE, encryptedEntitiesStr);
      }
      catch(JsonProcessingException e)
      {
        throw new CodingFault(e);
      }
    }
  }

  /**
   * Encrypt hashtags with the entity key. Modifies the input parameters "msg" and "clearTokens".
   */
  private void encryptHashtags(JsonNode msg, Set<String> clearTokens)
  {
    JsonNode containerNode = msg.path(ENTITIES_NODE).path(HASHTAGS_NODE);

    if (containerNode.isArray()) {
      List<ObjectNode> typeaheads = new ArrayList<>();

      for (Iterator<JsonNode> iterator = containerNode.elements(); iterator.hasNext();) {
        JsonNode hashtagNode = iterator.next();

        if (hashtagNode.isObject()) {
          ObjectNode hashtag = (ObjectNode) hashtagNode;

          if ("KEYWORD".equalsIgnoreCase(hashtag.get(TYPE_FLD).asText())) {

            clearTokens.add(hashtag.path(TEXT_FLD).asText());

            ObjectNode typeaheadNode = new ObjectNode(JsonNodeFactory.instance);
            typeaheadNode.put(INDEX_START_FLD, 0);
            typeaheadNode.put(INDEX_END_FLD, 0);
            typeaheadNode.put(TYPE_FLD, "KEYWORD");

            for (String field : new String[] {ID_FLD, TEXT_FLD}) {
              String plaintext = hashtag.path(field).asText();
              if (!plaintext.startsWith("?")) {
                

                hashtag.put(field, cryptoClient_.encryptTagV1(plaintext));
                typeaheadNode.put(field, cryptoClient_.encryptTagV2(plaintext));
              }
            }

            typeaheads.add(typeaheadNode);
          }

          // Strip to prevent information leakage
          hashtag.remove(INDEX_START_FLD);
          hashtag.remove(INDEX_END_FLD);
        }
      }

      ((ArrayNode) containerNode).addAll(typeaheads);
    }
  }

  /**
   * Strip sensitive information from user mention entities. Modifies the input parameters "msg".
   */
  private void encryptMentions(JsonNode msg)
  {
    for (Iterator<JsonNode> iterator = msg.path(ENTITIES_NODE).path(USER_MENTIONS_NODE).elements(); iterator.hasNext(); ) {
      JsonNode mentionNode = iterator.next();
      if (mentionNode.isObject()) {
        ObjectNode mention = (ObjectNode) mentionNode;

        // Strip to prevent information leakage
        mention.remove(SCREEN_NAME_FLD);
        mention.remove(PRETTY_NAME_FLD);
        mention.remove(TEXT_FLD);
        mention.remove(USER_TYPE_FLD);
        mention.remove(INDEX_START_FLD);
        mention.remove(INDEX_END_FLD);
      }
    }
  }

  /**
   * Strip sensitive information from url entities. Modifies the input parameters "msg".
   */
  private void encryptUrls(JsonNode msg) {
    // Strip to prevent information leakage
    JsonNode entities = msg.path(ENTITIES_NODE).path(ENTITIES_NODE);
    if (entities.isObject()) {
      ((ObjectNode) entities).remove(URLS_FLD);
    }
  }

  /**
   * Encrypt search tokens. Modifies the input parameter "msg".
   */
  private void encryptTokens(ThreadId threadId, JsonNode msg, String clear, Set<String> clearTokens)
  {
    List<String> tokenList = cryptoClient_.tokenize(threadId, clear, clearTokens); 
   
    ArrayNode tokenArray = new ArrayNode(JsonNodeFactory.instance);
    for (String t : tokenList)
    {
      tokenArray.add(t); 
    }

    ((ObjectNode) msg).set(TOKEN_IDS_FLD, tokenArray);
  }

  /**
   * Add a node for message features. Modifies the input parameter "msg".
   */
  private void addMsgFeaturesNode(JsonNode msg) {
    int msgFeatures = 0;
    int newMsgFeatures = encryptionOn(msgFeatures);
    newMsgFeatures = richtextOn(newMsgFeatures);
    if (msg.get(ENCRYPTED_MEDIA_NODE) != null) {
      newMsgFeatures = mediaEncryptionOn(newMsgFeatures);
    }
    ((ObjectNode) msg).put(MSG_FEATURES_FLD, newMsgFeatures);
  }

  private int encryptionOn(int msgFeatures) {
    int featureMask = 1 << ENCRYPTION_ORDINAL;
    return msgFeatures | featureMask;
  }

  private int richtextOn(int msgFeatures) {
    int featureMask = 1 << RICHTEXT_ORDINAL;
    return msgFeatures | featureMask;
  }

  private int mediaEncryptionOn(int msgFeatures) {
    int featureMask = 1 << MEDIA_ENCRYPTION_ORDINAL;
    return msgFeatures | featureMask;
  }
}

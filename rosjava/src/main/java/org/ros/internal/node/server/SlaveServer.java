/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.server;

import com.google.common.collect.Lists;

import org.ros.address.AdvertiseAddress;
import org.ros.address.BindAddress;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.parameter.ParameterManager;
import org.ros.internal.node.service.DefaultServiceServer;
import org.ros.internal.node.service.ServiceManager;
import org.ros.internal.node.topic.DefaultPublisher;
import org.ros.internal.node.topic.DefaultSubscriber;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.internal.node.topic.TopicDefinition;
import org.ros.internal.node.topic.TopicManager;
import org.ros.internal.node.xmlrpc.SlaveXmlRpcEndpointImpl;
import org.ros.internal.system.Process;
import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.ProtocolNames;
import org.ros.internal.transport.tcp.TcpRosProtocolDescription;
import org.ros.internal.transport.tcp.TcpRosServer;
import org.ros.namespace.GraphName;

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class SlaveServer extends XmlRpcServer {

  private final GraphName nodeName;
  private final MasterClient masterClient;
  private final TopicManager topicManager;
  private final ServiceManager serviceManager;
  private final ParameterManager parameterManager;
  private final TcpRosServer tcpRosServer;

  public SlaveServer(GraphName nodeName, BindAddress tcpRosBindAddress,
      AdvertiseAddress tcpRosAdvertiseAddress, BindAddress xmlRpcBindAddress,
      AdvertiseAddress xmlRpcAdvertiseAddress, MasterClient master, TopicManager topicManager,
      ServiceManager serviceManager, ParameterManager parameterManager,
      ScheduledExecutorService executorService) {
    super(xmlRpcBindAddress, xmlRpcAdvertiseAddress);
    this.nodeName = nodeName;
    this.masterClient = master;
    this.topicManager = topicManager;
    this.serviceManager = serviceManager;
    this.parameterManager = parameterManager;
    this.tcpRosServer =
        new TcpRosServer(tcpRosBindAddress, tcpRosAdvertiseAddress, topicManager, serviceManager,
            executorService);
  }

  public AdvertiseAddress getTcpRosAdvertiseAddress() {
    return tcpRosServer.getAdvertiseAddress();
  }

  /**
   * Start the XML-RPC server. This start() routine requires that the
   * {@link TcpRosServer} is initialized first so that the slave server returns
   * correct information when topics are requested.
   */
  public void start() {
    super.start(org.ros.internal.node.xmlrpc.SlaveXmlRpcEndpointImpl.class,
        new SlaveXmlRpcEndpointImpl(this));
    tcpRosServer.start();
  }

  // TODO(damonkohler): This should also shut down the Node.
  @Override
  public void shutdown() {
    super.shutdown();
    tcpRosServer.shutdown();
  }

  public void addService(DefaultServiceServer<?, ?> server) {
    serviceManager.putServer(server);
  }

  public List<Object> getBusStats(String callerId) {
    throw new UnsupportedOperationException();
  }

  public List<Object> getBusInfo(String callerId) {
    // For each publication and subscription (alive and dead):
    // ((connection_id, destination_caller_id, direction, transport, topic_name,
    // connected)*)
    // TODO(kwc): returning empty list right now to keep debugging tools happy
    return Lists.newArrayList();
  }

  public URI getMasterUri() {
    return masterClient.getRemoteUri();
  }

  /**
   * @return PID of this process if available, throws
   *         {@link UnsupportedOperationException} otherwise.
   */
  @Override
  public int getPid() {
    return Process.getPid();
  }

  public List<DefaultSubscriber<?>> getSubscriptions() {
    return topicManager.getSubscribers();
  }

  public List<DefaultPublisher<?>> getPublications() {
    return topicManager.getPublishers();
  }

  /**
   * @param parameterName
   * @param parameterValue
   * @return the number of parameter subscribers that received the update
   */
  public int paramUpdate(GraphName parameterName, Object parameterValue) {
    return parameterManager.updateParameter(parameterName, parameterValue);
  }

  public void publisherUpdate(String callerId, String topicName, Collection<URI> publisherUris) {
    if (topicManager.hasSubscriber(topicName)) {
      DefaultSubscriber<?> subscriber = topicManager.getSubscriber(topicName);
      TopicDefinition topicDefinition = subscriber.getTopicDefinition();
      Collection<PublisherIdentifier> identifiers =
          PublisherIdentifier.newCollectionFromUris(publisherUris, topicDefinition);
      subscriber.updatePublishers(identifiers);
    }
  }

  public ProtocolDescription requestTopic(String topicName, Collection<String> protocols)
      throws ServerException {
    // Canonicalize topic name.
    topicName = new GraphName(topicName).toGlobal().toString();
    if (!topicManager.hasPublisher(topicName)) {
      throw new ServerException("No publishers for topic: " + topicName);
    }
    for (String protocol : protocols) {
      if (protocol.equals(ProtocolNames.TCPROS)) {
        try {
          return new TcpRosProtocolDescription(tcpRosServer.getAdvertiseAddress());
        } catch (Exception e) {
          throw new ServerException(e);
        }
      }
    }
    throw new ServerException("No supported protocols specified.");
  }

  /**
   * @return a {@link NodeIdentifier} for this {@link SlaveServer}
   */
  public NodeIdentifier toSlaveIdentifier() {
    return new NodeIdentifier(nodeName, getUri());
  }
}

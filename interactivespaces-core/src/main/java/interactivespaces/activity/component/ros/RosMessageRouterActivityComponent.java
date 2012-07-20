/*
 * Copyright (C) 2012 Google Inc.
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

package interactivespaces.activity.component.ros;

import interactivespaces.InteractiveSpacesException;
import interactivespaces.SimpleInteractiveSpacesException;
import interactivespaces.activity.Activity;
import interactivespaces.activity.component.ActivityComponent;
import interactivespaces.activity.component.ActivityComponentContext;
import interactivespaces.activity.component.BaseActivityComponent;
import interactivespaces.activity.ros.RosActivity;
import interactivespaces.configuration.Configuration;
import interactivespaces.util.ros.RosPublishers;
import interactivespaces.util.ros.RosSubscribers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ros.message.Message;
import org.ros.message.MessageListener;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * An {@link ActivityComponent} instance which supports multiple named message
 * topics which can send and receive a given ROS message.
 * 
 * @author Keith M. Hughes
 */
public class RosMessageRouterActivityComponent<T extends Message> extends
		BaseActivityComponent {

	/**
	 * Name of the component.
	 */
	public static final String COMPONENT_NAME = "comm.router.ros";

	/**
	 * Dependencies for the component.
	 */
	public static final List<String> COMPONENT_DEPENDENCIES = Collections
			.unmodifiableList(Lists
					.newArrayList(RosActivityComponent.COMPONENT_NAME));

	/**
	 * Separator between names or topics.
	 */
	public static final String SEPARATOR = ":";

	public static final String CONFIGURATION_ROUTES_INPUTS = "space.activity.routes.inputs";

	public static final String CONFIGURATION_ROUTE_INPUT_TOPIC_PREFIX = "space.activity.route.input.";

	public static final String CONFIGURATION_ROUTES_OUTPUTS = "space.activity.routes.outputs";

	public static final String CONFIGURATION_ROUTE_OUTPUT_TOPIC_PREFIX = "space.activity.route.output.";

	/**
	 * The ROS activity this component is part of.
	 */
	private RosActivity rosActivity;

	/**
	 * {@code true} if the component is "running", false otherwise.
	 */
	private volatile boolean running;

	/**
	 * The listener for input messages.
	 */
	private final RoutableInputMessageListener<T> messageListener;

	/**
	 * The name of the ROS message type.
	 */
	private final String rosMessageType;

	/**
	 * All topic inputs
	 */
	private Map<String, RosSubscribers<T>> inputs = Maps.newConcurrentMap();

	/**
	 * All topic outputs
	 */
	private Map<String, RosPublishers<T>> outputs = Maps.newConcurrentMap();

	public RosMessageRouterActivityComponent(String rosMessageType,
			RoutableInputMessageListener<T> messageListener) {
		this.rosMessageType = rosMessageType;
		this.messageListener = messageListener;
	}

	@Override
	public String getName() {
		return COMPONENT_NAME;
	}

	@Override
	public List<String> getDependencies() {
		return COMPONENT_DEPENDENCIES;
	}

	@Override
	public void configureComponent(Configuration configuration,
			ActivityComponentContext componentContext) {
		Activity activity = componentContext.getActivity();
		if (!RosActivity.class.isAssignableFrom(activity.getClass())) {
			throw new InteractiveSpacesException(String.format(
					"Cannot convert an activity of type %s to %s", activity
							.getClass().getName(), RosActivity.class.getName()));
		}

		rosActivity = (RosActivity) activity;

		super.configureComponent(configuration, componentContext);

		boolean hasRouteErrors = false;

		String inputNames = configuration
				.getPropertyString(CONFIGURATION_ROUTES_INPUTS);
		if (inputNames != null) {
			inputNames = inputNames.trim();
			for (String inputName : inputNames.split(SEPARATOR)) {
				String propertyName = CONFIGURATION_ROUTE_INPUT_TOPIC_PREFIX
						+ inputName;
				if (configuration.getPropertyString(propertyName) == null) {
					activity.getLog()
							.error(String
									.format("Input route %s missing topic configuration %s",
											inputName, propertyName));
					hasRouteErrors = true;
				}
			}
		}

		String outputNames = configuration
				.getPropertyString(CONFIGURATION_ROUTES_OUTPUTS);
		if (outputNames != null) {
			outputNames = outputNames.trim();
			for (String outputName : outputNames.split(SEPARATOR)) {
				String propertyName = CONFIGURATION_ROUTE_OUTPUT_TOPIC_PREFIX
						+ outputName;
				if (configuration.getPropertyString(propertyName) == null) {
					activity.getLog()
							.error(String
									.format("Output route %s missing topic configuration %s",
											outputName, propertyName));
					hasRouteErrors = true;
				}
			}
		}

		if (hasRouteErrors) {
			throw new InteractiveSpacesException("Router has missing routes");
		}

		if ((inputNames == null || inputNames.isEmpty())
				&& (outputNames == null || outputNames.isEmpty())) {
			throw new SimpleInteractiveSpacesException(
					String.format(
							"Router has no routes. Define either %s or %s in your configuration",
							CONFIGURATION_ROUTES_INPUTS,
							CONFIGURATION_ROUTES_OUTPUTS));
		}
	}

	@Override
	public void startupComponent() {
		Configuration configuration = rosActivity.getConfiguration();

		String inputNames = configuration
				.getPropertyString(CONFIGURATION_ROUTES_INPUTS);
		if (inputNames != null) {
			for (String inputName : inputNames.split(SEPARATOR)) {
				inputName = inputName.trim();
				if (!inputName.isEmpty()) {
					String inputTopicNames = configuration
							.getRequiredPropertyString(CONFIGURATION_ROUTE_INPUT_TOPIC_PREFIX
									+ inputName);
					final String channelName = inputName;
					RosSubscribers<T> subscribers = new RosSubscribers<T>(
							getComponentContext().getActivity().getLog());
					inputs.put(inputName, subscribers);

					subscribers.addSubscribers(rosActivity.getMainNode(),
							rosMessageType, inputTopicNames,
							new MessageListener<T>() {
								@Override
								public void onNewMessage(T message) {
									handleNewMessage(channelName, message);
								}
							});
				}
			}
		}

		String outputNames = configuration
				.getPropertyString(CONFIGURATION_ROUTES_OUTPUTS);
		if (outputNames != null) {
			for (String outputName : outputNames.split(SEPARATOR)) {
				outputName.trim();
				if (!outputName.isEmpty()) {
					String outputTopicNames = configuration
							.getRequiredPropertyString(CONFIGURATION_ROUTE_OUTPUT_TOPIC_PREFIX
									+ outputName);

					boolean latch = false;
					int semiPos = outputTopicNames.indexOf(';');
					if (semiPos != -1) {
						String extra = outputTopicNames.substring(0, semiPos);
						outputTopicNames = outputTopicNames
								.substring(semiPos + 1);

						String[] pair = extra.split("=");
						if (pair.length > 1) {
							if ("latch".equals(pair[0].trim())) {
								latch = "true".equals(pair[1].trim());
							}
						}
					}

					RosPublishers<T> publishers = new RosPublishers<T>(
							getComponentContext().getActivity().getLog());
					outputs.put(outputName, publishers);

					publishers.addPublishers(rosActivity.getMainNode(),
							rosMessageType, outputTopicNames, latch);
				}
			}
		}

		running = true;
	}

	@Override
	public void shutdownComponent() {
		running = false;

		for (RosPublishers<T> output : outputs.values()) {
			output.shutdown();
		}
		outputs.clear();

		for (RosSubscribers<T> input : inputs.values()) {
			input.shutdown();
		}
		inputs.clear();
	}

	@Override
	public boolean isComponentRunning() {
		return running;
	}

	/**
	 * Handle a new route message.
	 * 
	 * @param channelName
	 *            name of the channel the message came in on
	 * @param message
	 *            the message that came in
	 */
	void handleNewMessage(String channelName, T message) {
		try {
			if (getComponentContext().lockReadRunningRead()) {
				// Send the message out to the
				// listener.
				messageListener.onNewRoutableInputMessage(channelName, message);
			}
		} catch (Exception e) {
			getComponentContext()
					.getActivity()
					.getLog()
					.error(String.format(
							"Error after receiving routing message for channel %s",
							channelName), e);
		} finally {
			getComponentContext().unlockReadRunningRead();
		}
	}

	/**
	 * Send out a message on one of the output channels.
	 * 
	 * <p>
	 * The message is dropped if there is no such channel, though it will be
	 * logged
	 * 
	 * @param outputChannelName
	 *            name of the output channel
	 * @param message
	 *            message to send
	 */
	public void writeOutputMessage(String outputChannelName, T message) {
		try {
			if (getComponentContext().lockReadRunningRead()) {
				if (outputChannelName != null) {
					RosPublishers<T> output = outputs.get(outputChannelName);
					if (output != null) {
						output.publishMessage(message);
					} else {
						getComponentContext()
								.getActivity()
								.getLog()
								.error(String
										.format("Unknown route output channel %s. Message dropped.",
												outputChannelName));
					}
				} else {
					getComponentContext()
							.getActivity()
							.getLog()
							.error("Route output channel has no name. Message dropped.");
				}
			}
		} catch (Exception e) {
			getComponentContext()
					.getActivity()
					.getLog()
					.error(String.format("Error writing message on channel %s",
							outputChannelName), e);
		} finally {
			getComponentContext().unlockReadRunningRead();
		}
	}
}
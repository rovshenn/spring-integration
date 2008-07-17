/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.gateway;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.easymock.IAnswer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessageMapper;

/**
 * @author Iwein Fuld
 */
@SuppressWarnings("unchecked")
public class SimpleMessagingGatewayTests {

	private SimpleMessagingGateway simpleMessagingGateway;

	private MessageChannel requestChannel = createMock(MessageChannel.class);

	private MessageChannel replyChannel = createMock(MessageChannel.class);

	private Message<?> messageMock = createMock(Message.class);

	private MessageMapper messageMapperMock = createMock(MessageMapper.class);

	private MessageBus messageBusMock = createMock(MessageBus.class);

	private Object[] allmocks = new Object[] {
			requestChannel, replyChannel, messageMock, messageMapperMock };


	@Before
	public void initializeSample() {
		this.simpleMessagingGateway = new SimpleMessagingGateway(requestChannel);
		this.simpleMessagingGateway.setReplyChannel(replyChannel);
		this.simpleMessagingGateway.setMessageMapper(messageMapperMock);
		this.simpleMessagingGateway.setMessageBus(messageBusMock);
		reset(allmocks);
	}


	/* send tests */

	@Test
	public void sendMessage() {
		expect(requestChannel.send(messageMock)).andReturn(true);
		replay(allmocks);
		this.simpleMessagingGateway.send(messageMock);
		verify(allmocks);
	}

	@Test(expected=MessageDeliveryException.class)
	public void sendMessage_failure() {
		expect(requestChannel.send(messageMock)).andReturn(false);
		replay(allmocks);
		this.simpleMessagingGateway.send(messageMock);
		verify(allmocks);
	}

	@Test
	public void sendObject() {
		expect(requestChannel.send(isA(Message.class))).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				assertEquals("test", ((Message) getCurrentArguments()[0]).getPayload());
				return true;
			}
		});
		replay(allmocks);
		this.simpleMessagingGateway.send("test");
		verify(allmocks);
	}

	@Test(expected=MessageDeliveryException.class)
	public void sendObject_failure() {
		expect(requestChannel.send(isA(Message.class))).andAnswer(new IAnswer<Boolean>() {
			public Boolean answer() throws Throwable {
				assertEquals("test", ((Message) getCurrentArguments()[0]).getPayload());
				return false;
			}
		});
		replay(allmocks);
		this.simpleMessagingGateway.send("test");
		verify(allmocks);
	}

	@Test
	public void sendMessage_null() {
		replay(allmocks);
		this.simpleMessagingGateway.send(null);
		verify(allmocks);
	}

	/* receive tests */

	@Test
	public void receiveMessage() {
		expect(replyChannel.receive()).andReturn(messageMock);
		expect(messageMapperMock.mapMessage(messageMock)).andReturn(messageMock);
		replay(allmocks);
		assertEquals(this.simpleMessagingGateway.receive(), messageMock);
		verify(allmocks);
	}

	@Test
	public void receiveMessage_null() {
		expect(replyChannel.receive()).andReturn(null);
		replay(allmocks);
		assertEquals(this.simpleMessagingGateway.receive(), null);
		verify(allmocks);
	}

	/* send and receive tests */

	@Test
	public void sendObjectAndReceiveObject() {
		expect(replyChannel.getName()).andReturn("replyChannel").anyTimes();
		expect(requestChannel.send(isA(Message.class))).andReturn(true);
		replay(allmocks);
		this.simpleMessagingGateway.sendAndReceive("test");
		verify(allmocks);
	}

	@Test
	@Ignore
	public void sendMessageAndReceiveObject() {
		// setup local mocks
		MessageHeaders messageHeadersMock = createMock(MessageHeaders.class);	
		//set expectations
		//messageHeaderMock.setReturnAddress(replyChannel);
		expect(replyChannel.getName()).andReturn("replyChannel").anyTimes();
		expect(messageMock.getHeaders()).andReturn(messageHeadersMock);
		expect(requestChannel.send(messageMock)).andReturn(true);
		expect(messageMock.getId()).andReturn(1);

		//play scenario
		replay(allmocks);
		replay(messageHeadersMock);
		this.simpleMessagingGateway.sendAndReceive(messageMock);
		verify(allmocks);
		verify(messageHeadersMock);
	}

	@Test
	public void sendNullAndReceiveObject() {
		expect(replyChannel.getName()).andReturn("replyChannel").anyTimes();
		replay(allmocks);
		this.simpleMessagingGateway.sendAndReceive(null);
		verify(allmocks);
	}

	@Test
	public void sendObjectAndReceiveMessage() {
		expect(replyChannel.getName()).andReturn("replyChannel").anyTimes();
		expect(requestChannel.send(isA(Message.class))).andReturn(true);
		replay(allmocks);
		this.simpleMessagingGateway.sendAndReceiveMessage("test");
		verify(allmocks);
	}

	@Test
	@Ignore
	public void sendMessageAndReceiveMessage() {
		// setup local mocks
		MessageHeaders messageHeadersMock = createMock(MessageHeaders.class);	
		//set expectations
		expect(replyChannel.getName()).andReturn("replyChannel").anyTimes();
		expect(messageMock.getHeaders()).andReturn(messageHeadersMock);
		expect(messageHeadersMock.getReturnAddress()).andReturn(replyChannel);
		expect(requestChannel.send(messageMock)).andReturn(true);
		expect(messageMock.getId()).andReturn(1);

		replay(allmocks);
		this.simpleMessagingGateway.sendAndReceiveMessage(messageMock);
		verify(allmocks);
	}

	@Test
	public void sendNullAndReceiveMessage() {
		expect(replyChannel.getName()).andReturn("replyChannel").anyTimes();
		replay(allmocks);
		this.simpleMessagingGateway.sendAndReceiveMessage(null);
		verify(allmocks);
	}

}

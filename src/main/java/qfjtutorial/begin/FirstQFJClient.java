package qfjtutorial.begin;

import java.util.concurrent.TimeUnit;

import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.Session;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

//This is a simplified version.   
//For production code, please read common.DefaultQFJSingleSessionInitiator.
public class FirstQFJClient {

	public static void main(String[] args) throws Exception {

		String configurationFileInClasspath = "qfjtutorial/begin/FirstQFJClient.qfj.config.txt";

		Application application = new FirstMessageCallback();

		SessionSettings settings = new SessionSettings(configurationFileInClasspath);
		MessageStoreFactory storeFactory = new FileStoreFactory(settings);
		LogFactory logFactory = new FileLogFactory(settings);
		MessageFactory messageFactory = new DefaultMessageFactory();

		SocketInitiator initiator = new SocketInitiator(application, storeFactory, settings, logFactory,
				messageFactory);

		initiator.start();

		// after start, you have to wait several seconds before sending
		// messages.
		// in production code, you should check the response Logon message.
		// Refer: DefaultQFJSingSessionInitiator.java
		TimeUnit.SECONDS.sleep(3);

		Session.sendToTarget(buildHarcodedNewOrderSingleForTest());
		
		TimeUnit.SECONDS.sleep(1);
		Session.sendToTarget(buildHarcodedNewOrderSingleForTest());

		TimeUnit.SECONDS.sleep(3);
		initiator.stop();
	}

	private static Message buildHarcodedNewOrderSingleForTest() {
		/**
		 * <message name="NewOrderSingle" msgtype="D" msgcat="app">
		 * <field name="ClOrdID" required="Y"/>
		 * <component name="Instrument" required="Y"/>
		 * <field name="Side" required="Y"/>
		 * <field name="TransactTime" required="Y"/>
		 * <component name="OrderQtyData" required="Y"/>
		 * <field name="OrdType" required="Y"/> </message>
		 */
		// NewOrderSingle
		Message newOrderSingle = new Message();
		// It is not required to set 8,49,56 if you know SessionID. See
		// DefaultSQFSingleSessionInitiator.java
		newOrderSingle.getHeader().setString(8, "FIXT.1.1");
		newOrderSingle.getHeader().setString(49, "abcClientCompID");
		newOrderSingle.getHeader().setString(56, "xyzServerCompID");

		newOrderSingle.getHeader().setString(35, "D");
		newOrderSingle.setString(11, "ClOrdID_" + System.currentTimeMillis());
		newOrderSingle.setString(55, "USDJPY"); // non-repeating group
												// instrument->Symbol 55
		newOrderSingle.setString(54, "1");// Side 54 - 1:buy, 2:sell
		newOrderSingle.setUtcTimeStamp(60, new java.util.Date(), true); // TransactTime
		newOrderSingle.setString(38, "2000000"); // non-repeating group
													// OrderQtyData->OrderQty
													// 38
		newOrderSingle.setString(40, "1"); // OrdType 1:Market

		return newOrderSingle;

	}

}

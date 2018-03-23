package qfjtutorial.begin;

import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

//This is a simplified version.   
//For production code, please read common.DefaultQFJSingleSessionInitiator.
public class FirstQFJClient {

	public static void main(String[] args) throws Exception {

		String configurationFileInClasspath = "src/main/java/qfjtutorial/begin/FirstQFJClient.qfj.config.txt";

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
		TimeUnit.SECONDS.sleep(5);
		
		final SessionID sessionID = new SessionID("FIXT.1.1", "abcClientCompID", "xyzServerCompID", "");	
		
		long period =10;
		TimeUnit unit = TimeUnit.SECONDS;
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable command = new Runnable() {
            @Override
            public void run(){
                try {
                	Session.sendToTarget(buildHarcodedNewOrderSingleForTest(),sessionID);  
                }catch (Exception e){
                    //TODO log exception
                    e.printStackTrace();
                }

            }
        };
        long initialDelay = 0;
        executor.scheduleAtFixedRate( command,  initialDelay,   period,   unit);

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
		newOrderSingle.getHeader().setString(49, "LTC$$_FIX_001");
		newOrderSingle.getHeader().setString(56, "BaoyingMatchingCompID");

		newOrderSingle.getHeader().setString(35, "D");
		newOrderSingle.setString(11, "ClOrdID_" + System.currentTimeMillis());
		newOrderSingle.setString(55, "USDJPY"); // non-repeating group
												// instrument->Symbol 55
		newOrderSingle.setString(54, "1");// Side 54 - 1:buy, 2:sell
		newOrderSingle.setUtcTimeStamp(60, new java.util.Date(), true); // TransactTime
		newOrderSingle.setString(38, "200"); // non-repeating group
													// OrderQtyData->OrderQty
													// 38
		newOrderSingle.setString(40, "1"); // OrdType 1:Market

		return newOrderSingle;

	}

}

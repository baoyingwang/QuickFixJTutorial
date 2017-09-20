package qfjtutorial.begin;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qfjtutorial.common.DefaultMessageCallback;
import qfjtutorial.common.DefaultSessionStateListener;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

//the session has been setup at both client and server side, in related configuration files
//You could setup your own log4j2 file, e.g.-Dlog4j.configurationFile=qfjtutorial/common/ConsoleOnly.log4j2.xml
public class FirstQFJClient {

	protected final static Logger log = LoggerFactory.getLogger(FirstQFJClient.class);

	private final String _appConfigInClasspath;
	private final SessionSettings _settings;
	private final MessageStoreFactory _storeFactory;
	private final LogFactory _logFactory;
	private final Application _msgCallback;

	// Each configuration file could include multi session. In this example,
	// only single session.
	private final SessionID _sessionID;

	private final SocketInitiator _initiator;

	private final CountDownLatch _latchForLogonResponse;

	public FirstQFJClient(String appConfigInClasspath, Application msgCallback) throws Exception {

		_appConfigInClasspath = appConfigInClasspath;
		log.info("qfj client begin initializing, with app configuration file in classpath:{}", appConfigInClasspath);

		_msgCallback = msgCallback;

		_settings = new SessionSettings(appConfigInClasspath);
		log.info("_settings.size(): {}", _settings.size());
		if (_settings.size() != 1) {
			String errorInfo = "For this simple app, expect 1 session definition in settings, but found "
					+ _settings.size();
			log.error(errorInfo);
			throw new RuntimeException(errorInfo);
		}
		SessionID sessionID = null;
		for (final Iterator<SessionID> i = _settings.sectionIterator(); i.hasNext();) {
			sessionID = i.next();
			log.info("session in the configuration : " + sessionID.toString());
		}
		_sessionID = sessionID;

		// Other store factory, e.g. quickfix.JdbcStoreFactory,
		// quickfix.MemoryStoreFactory, quickfix.SleepycatStoreFactory. See QFJ
		// Advanced.
		_storeFactory = new FileStoreFactory(_settings);

		// Other log factory are supported, too , e.g.quickfix.ScreenLogFactory,
		// quickfix.JdbcLogFactory, quickfix.CompositeLogFactory,
		// quickfix.SLF4JLogFactory. See QFJ-Advanced.
		// quickfix.FileLogFactory
		_logFactory = new SLF4JLogFactory(_settings);

		// This is single thread. For multi-thread, see
		// quickfix.ThreadedSocketInitiator, and QFJ-Advanced.
		_initiator = new SocketInitiator(_msgCallback, _storeFactory, _settings, _logFactory,
				new DefaultMessageFactory());

		_latchForLogonResponse = new CountDownLatch(1);

		log.info("qfj client initialized, with app configuration file in classpath:{}", appConfigInClasspath);

	}

	// start is NOT put in constructor deliberately, to let it pair with
	// shutdown
	public void start() throws Exception {

		log.info("qfj client start, {}", _appConfigInClasspath);

		_initiator.start();
		
		//session is created during Connector.start();
		Session session = Session.lookupSession(_sessionID);
		session.addStateListener(new DefaultSessionStateListener(_sessionID){
			@Override
			public void onLogon() {
				log.debug("InternalSessionStateListener onLogon session:{}", _sessionID.toString());
				_latchForLogonResponse.countDown();
			}
		});
		_latchForLogonResponse.await();

		log.info("qfj client started, {}", _appConfigInClasspath);
	}

	public void stop() throws Exception {

		log.info("qfj client stop, {}", _appConfigInClasspath);

		_initiator.stop();

		log.info("qfj client stopped, {}", _appConfigInClasspath);
	}

	// the 49 and 56 value of the message will be used to identify the FIX
	// session
	// if you don't know what's the tag 49 and 56, please read FIX session
	// specification.
	public void send(Message message) throws SessionNotFound {
		quickfix.Session.sendToTarget(message, _sessionID);
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

	public static void main(String[] args) throws Exception {

		Application msgCallback = new DefaultMessageCallback();
		String configurationFileInClasspath = "qfjtutorial/begin/FirstQFJClient.qfj.config.txt";
		FirstQFJClient client = new FirstQFJClient(configurationFileInClasspath, msgCallback);
		client.start();

		TimeUnit.SECONDS.sleep(3);

		for (int i = 0; i < 3; i++) {

			Message newOrderSingle = buildHarcodedNewOrderSingleForTest();
			client.send(newOrderSingle);

			//TimeUnit.SECONDS.sleep(1);
			TimeUnit.MILLISECONDS.sleep(10);
		}
	}
}

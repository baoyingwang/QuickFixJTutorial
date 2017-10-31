package qfjtutorial.common;

import java.util.Iterator;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import quickfix.SessionStateListener;
import quickfix.SocketInitiator;

//the session has been setup at both client and server side, in related configuration files
public class DefaultQFJSingleSessionInitiator {

	protected final static Logger log = LoggerFactory.getLogger(DefaultQFJSingleSessionInitiator.class);

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

	private final SessionStateListener _clientSessionStateListener;
	public DefaultQFJSingleSessionInitiator(String appConfigInClasspath, Application msgCallback) throws Exception {

		this(appConfigInClasspath,  msgCallback, null);

	}
	
	public DefaultQFJSingleSessionInitiator(String appConfigInClasspath, Application msgCallback, SessionStateListener sessionStateListener) throws Exception {

		_appConfigInClasspath = appConfigInClasspath;
		log.info("qfj client begin initializing, with app configuration file in classpath:{}", appConfigInClasspath);

		_msgCallback = msgCallback;
		
		_clientSessionStateListener = sessionStateListener;

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

		// It also supports other store factory, e.g. JDBC, memory. Maybe you
		// could use them in some advanced cases.
		_storeFactory = new FileStoreFactory(_settings);

		// It also supports other log factory, e.g. JDBC. But I think SL4J is
		// good enough.
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

		// Add state listner callback to a session, after initiator start.
		// Because session is created during initiator(Connector).start();
		Session session = Session.lookupSession(_sessionID);
		
		//TODO the below session state listen maybe has thread conflict with the one from client.
		session.addStateListener(new DefaultSessionStateListener(_sessionID) {
			@Override
			public void onLogon() {
				log.debug("InternalSessionStateListener onLogon session:{}", _sessionID.toString());
				_latchForLogonResponse.countDown();
			}
		});
		
		if(_clientSessionStateListener != null){
			session.addStateListener(_clientSessionStateListener);
		}
		_latchForLogonResponse.await();

		log.info("qfj client started, {}", _appConfigInClasspath);
	}

	public void stop() throws Exception {

		log.info("qfj client stop, {}", _appConfigInClasspath);

		_initiator.stop();

		log.info("qfj client stopped, {}", _appConfigInClasspath);
	}

	// You could ignore 8/49/56, because the initiator knows the values form sessionID
	// if you don't know what's the tag 49 and 56, please read FIX session
	// specification.
	public void send(Message message) throws SessionNotFound {
		quickfix.Session.sendToTarget(message, _sessionID);
	}

	public static void main(String... args) throws Exception{

		String appConfigInClasspath = "qfjtutorial/begin/FirstQFJClient.qfj.config.txt";
		Application msgCallback = new DefaultMessageCallback();
		DefaultQFJSingleSessionInitiator initiator = new DefaultQFJSingleSessionInitiator(appConfigInClasspath, msgCallback);
		initiator.start();
	}
	
}

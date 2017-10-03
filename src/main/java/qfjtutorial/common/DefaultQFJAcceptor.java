package qfjtutorial.common;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

//client sessions are hardcoded in configuration file
public class DefaultQFJAcceptor {

	public SessionSettings get_settings() {
		return _settings;
	}

	public Application get_msgCallback() {
		return _msgCallback;
	}

	public SocketAcceptor get_acceptor() {
		return _acceptor;
	}

	public MessageStoreFactory get_storeFactory() {
		return _storeFactory;
	}

	public LogFactory get_logFactory() {
		return _logFactory;
	}

	protected final static Logger log = LoggerFactory.getLogger(DefaultQFJAcceptor.class);

	private final String _appConfigInClasspath;
	private final SessionSettings _settings;
	private final MessageStoreFactory _storeFactory;
	private final LogFactory _logFactory;
	private final Application _msgCallback;

	private SocketAcceptor _acceptor = null;

	public DefaultQFJAcceptor(String appConfigInClasspath, Application msgCallback) throws Exception {

		_appConfigInClasspath = appConfigInClasspath;
		log.info("qfj server begin initializing, with app configuration file in classpath:{}", appConfigInClasspath);

		_msgCallback = msgCallback;

		_settings = new SessionSettings(appConfigInClasspath);
		for (final Iterator<SessionID> i = _settings.sectionIterator(); i.hasNext();) {
			final SessionID sessionID = i.next();
			log.info("session in the configuration : " + sessionID.toString());
		}

		// It also supports other store factory, e.g. JDBC, memory. Maybe you
		// could use them in some advanced cases.
		_storeFactory = new FileStoreFactory(_settings);

		// It also supports other log factory, e.g. JDBC. But I think SL4J is
		// good enough.
		_logFactory = new SLF4JLogFactory(_settings);

		// This is single thread. For multi-thread, see
		// quickfix.ThreadedSocketInitiator, and QFJ Advanced.
		_acceptor = new SocketAcceptor(_msgCallback, _storeFactory, _settings, _logFactory,
				new DefaultMessageFactory());

		log.info("qfj server initialized, with app configuration file in classpath:{}", appConfigInClasspath);

	}

	// start is NOT put in constructor deliberately, to let it pair with
	// shutdown
	public void start() throws Exception {

		log.info("qfj server start, {}", _appConfigInClasspath);

		_acceptor.start();

		log.info("qfj server started, {}", _appConfigInClasspath);
	}

	public void stop() throws Exception {

		log.info("qfj server stop, {}", _appConfigInClasspath);

		_acceptor.stop();

		log.info("qfj server stopped, {}", _appConfigInClasspath);
	}

	public static void main(String... args) throws Exception{

		String appConfigInClasspath = "qfjtutorial/begin/FirstQFJServer.qfj.config.txt";
		Application msgCallback = new DefaultMessageCallback();
		DefaultQFJAcceptor acceptor = new DefaultQFJAcceptor(appConfigInClasspath, msgCallback);
		acceptor.start();
	}
}

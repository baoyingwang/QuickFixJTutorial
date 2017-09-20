package qfjtutorial.begin;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qfjtutorial.common.DefaultMessageCallback;
import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FieldNotFound;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.UnsupportedMessageType;

//client sessions are hardcoded in configuration file
//You could setup your own log4j2 file, e.g.-Dlog4j.configurationFile=qfjtutorial/common/ConsoleOnly.log4j2.xml
public class FirstQFJServer {

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

	protected final static Logger log = LoggerFactory.getLogger(FirstQFJServer.class);

	private final String _appConfigInClasspath;
	private final SessionSettings _settings;
	private final MessageStoreFactory _storeFactory;
	private final LogFactory _logFactory;
	private final Application _msgCallback;

	private SocketAcceptor _acceptor = null;

	public FirstQFJServer(String appConfigInClasspath, Application msgCallback) throws Exception {

		_appConfigInClasspath = appConfigInClasspath;
		log.info("qfj server begin initializing, with app configuration file in classpath:{}", appConfigInClasspath);

		_msgCallback = msgCallback;

		_settings = new SessionSettings(appConfigInClasspath);
		for (final Iterator<SessionID> i = _settings.sectionIterator(); i.hasNext();) {
			final SessionID sessionID = i.next();
			log.info("session in the configuration : " + sessionID.toString());
		}

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

	static class AcceptorMessageCalback extends DefaultMessageCallback {
		@Override
		public void fromApp(Message paramMessage, SessionID paramSessionID)
				throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

			log.debug("fromApp session:{}, received : {} ", paramSessionID.toString(), paramMessage.toString());
			
			try {
				
				Message executionReport = buildHardcodedExecutionReportForTest(paramMessage);
				quickfix.Session.sendToTarget(executionReport, paramSessionID);
				
			} catch (Exception e) {
				log.error("problem while processing:" + paramMessage.toString(), e);
			}

		}
		private Message buildHardcodedExecutionReportForTest(Message newOrderSingle) throws Exception{
			/**
			 * <message name="ExecutionReport" msgtype="8" msgcat="app">
			 * <field name="OrderID" required="Y"/>
			 * <field name="ExecID" required="Y"/>
			 * <field name="ExecType" required="Y"/>
			 * <field name="OrdStatus" required="Y"/>
			 * <component name="Instrument" required="Y"/>
			 * <field name="Side" required="Y"/>
			 * <field name="LeavesQty" required="Y"/>
			 * <field name="CumQty" required="Y"/> </message>
			 */
			
			Message executionReport = new Message();
			executionReport.getHeader().setString(35, "8"); // ExecutionReport
			executionReport.setString(37, "OrderID_" + System.currentTimeMillis());
			executionReport.setString(17, "ExecID_" + System.currentTimeMillis());
			executionReport.setString(150, "0"); // NEW
			executionReport.setString(39, "0"); // NEW //OrderStatus
			executionReport.setString(55, "USDJPY"); // non-repeating group
			// instrument->Symbol 55
			executionReport.setString(54, "1");// Side 54 - 1:buy, 2:sell
			executionReport.setString(151, newOrderSingle.getString(38));// LeavesQty
																		// 151. Not
																		// yet
																		// executed.
																		// So, all
																		// of the
																		// OrderQty(38)
																		// is
																		// leavesQty
			executionReport.setString(14, "0");// CumQty 14. 0, since not yet
												// executed.

			executionReport.setString(11, newOrderSingle.getString(11)); // client use
																		// this id
																		// to link
																		// the
																		// ExecutionReport
																		// with the
																		// NewOrderSingle
			return executionReport;
		}		
	}	
	
	public static void main(String[] args) throws Exception {

		Application msgCallback = new AcceptorMessageCalback();
		String configurationFileInClasspath = "qfjtutorial/begin/FirstQFJServer.qfj.config.txt";
		FirstQFJServer server = new FirstQFJServer(configurationFileInClasspath, msgCallback);
		server.start();
	}	
}

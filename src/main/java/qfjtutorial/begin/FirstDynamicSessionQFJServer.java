package qfjtutorial.begin;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.mina.acceptor.AcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;

//Dynamic means, client configuration is NOT required to be hardcoded in server configuration file.
//http://www.quickfixj.org/quickfixj/usermanual/1.5.3/usage/acceptor_dynamic.html
//The only difference with FirstQFJServer is run another setup:setupDynamicSessionProvider
//You could setup your own log4j2 file, e.g.-Dlog4j.configurationFile=qfjtutorial/common/ConsoleOnly.log4j2.xml
public class FirstDynamicSessionQFJServer {

	protected final static Logger log = LoggerFactory.getLogger(FirstDynamicSessionQFJServer.class);

	private final FirstQFJServer _qfjServer;
	private final SessionSettings _settings;

	public FirstDynamicSessionQFJServer(FirstQFJServer qfjServer) throws Exception {

		_qfjServer = qfjServer;
		_settings = qfjServer.get_settings();
		setupDynamicSessionProvider(qfjServer.get_msgCallback(), qfjServer.get_acceptor());


	}

	private void setupDynamicSessionProvider(Application application, SocketAcceptor connectorAsAcc)
			throws ConfigError, FieldConvertError {
		for (final Iterator<SessionID> i = _settings.sectionIterator(); i.hasNext();) {
			final SessionID sessionID = i.next();

			boolean isAcceptorTemplateSet = _settings.isSetting(sessionID, "AcceptorTemplate");
			if (isAcceptorTemplateSet && _settings.getBool(sessionID, "AcceptorTemplate")) {

				log.info("dynamic acceptor is configured on {}", sessionID);
				AcceptorSessionProvider provider = new DynamicAcceptorSessionProvider(_settings, sessionID, application,
						_qfjServer.get_storeFactory(), _qfjServer.get_logFactory(), new DefaultMessageFactory());
				// SocketAcceptAddress
				// SocketAcceptPort
				SocketAddress address = new InetSocketAddress(_settings.getString(sessionID, "SocketAcceptAddress"),
						(int) (_settings.getLong(sessionID, "SocketAcceptPort")));
				connectorAsAcc.setSessionProvider(address, provider);

				// we have to skip setup SessionStateListener,
				// since the concrete session is not identified yet for
				// dynamic acceptor.
				// TODO try to figure out how to setup
				// SessionStateListener
				// when the concrete session is created.
			}
		}
	}
	
	public void start() throws Exception{
		_qfjServer.start();
	}

	public void stop() throws Exception{
		_qfjServer.stop();
	}
	public static void main(String[] args) throws Exception {

		String dynamicAcceptorConfigurationFileInClasspath = "qfjtutorial/begin/FirstDynamicSessionQFJServer.qfj.config.txt";
		Application msgCallback = new FirstQFJServer.AcceptorMessageCalback();
		
		FirstQFJServer qfjServer = new FirstQFJServer(dynamicAcceptorConfigurationFileInClasspath, msgCallback);

		FirstDynamicSessionQFJServer server = new FirstDynamicSessionQFJServer(qfjServer);
		server.start();
	}	
}

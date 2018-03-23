package qfjtutorial.other;

import quickfix.Application;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.Group;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

//This is simplified version. 
//For production, please see common.DefaultQFJAcceptor and common.DefaultDynamicSessionQFJServer
public class CrackerQFJServer {

//	public static void main(String[] args) throws Exception {
//
//		String configurationFileInClasspath = "qfjtutorial/begin/FirstQFJServer.qfj.config.txt";
//		Application application = new MyApplication();
//
//		SessionSettings settings = new SessionSettings(configurationFileInClasspath);
//		MessageStoreFactory storeFactory = new FileStoreFactory(settings);
//		LogFactory logFactory = new FileLogFactory(settings);
//		MessageFactory message50SP1Factory = new quickfix.fix50sp1.MessageFactory();
//		MessageFactory messageFXT11Factory = new quickfix.fixt11.MessageFactory();
//
//		MessageFactory messageFactory = new CompositeMessageFactory(message50SP1Factory, messageFXT11Factory);
//
//		SocketAcceptor accptor = new SocketAcceptor(application, storeFactory, settings, logFactory, messageFactory);
//
//		accptor.start();
//	}
//
//	static class CompositeMessageFactory implements MessageFactory {
//
//		MessageFactory _f1;
//		MessageFactory _f2;
//
//		CompositeMessageFactory(MessageFactory f1, MessageFactory f2) {
//			_f1 = f1;
//			_f2 = f2;
//		}
//
//		@Override
//		public Message create(String beginString, String msgType) {
//
//			Message m = _f1.create(beginString, msgType);
//
//			if (m == null || !m.getHeader().isSetField(35)) {
//				m = _f2.create(beginString, msgType);
//			}
//
//			return m;
//		}
//
//		@Override
//		public Group create(String beginString, String msgType, int correspondingFieldID) {
//			Group m = _f1.create(beginString, msgType, correspondingFieldID);
//
//			if (m == null) {
//				m = _f2.create(beginString, msgType, correspondingFieldID);
//			}
//
//			return m;
//		}
//
//	}

}

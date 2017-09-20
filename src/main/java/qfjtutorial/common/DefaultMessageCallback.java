package qfjtutorial.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

/**
 * 
		// "from" means from remote endpoint
		// "to" means to remote endpoint *
 */
public class DefaultMessageCallback implements Application {
	
	protected final static Logger log = LoggerFactory.getLogger(DefaultMessageCallback.class);

	@Override
	public void fromAdmin(Message paramMessage, SessionID paramSessionID)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		log.debug("fromAdmin session:{}, received : {} ", paramSessionID.toString(), paramMessage.toString());

	}

	@Override
	public void fromApp(Message paramMessage, SessionID paramSessionID)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
		log.debug("fromApp session:{}, received : {} ", paramSessionID.toString(), paramMessage.toString());

	}

	@Override
	public void onCreate(SessionID paramSessionID) {
		log.debug("onCreate session:{}", paramSessionID.toString());
	}

	@Override
	public void onLogon(SessionID paramSessionID) {
		log.debug("onLogon session:{}", paramSessionID.toString());

	}

	@Override
	public void onLogout(SessionID paramSessionID) {
		log.debug("onLogout session:{}", paramSessionID.toString());

	}

	@Override
	public void toAdmin(Message paramMessage, SessionID paramSessionID) {
		log.debug("toAdmin session:{}, send : {} ", paramSessionID.toString(), paramMessage.toString());

	}

	@Override
	public void toApp(Message paramMessage, SessionID paramSessionID) throws DoNotSend {
		log.debug("toApp session:{}, send : {} ", paramSessionID.toString(), paramMessage.toString());

	}

}

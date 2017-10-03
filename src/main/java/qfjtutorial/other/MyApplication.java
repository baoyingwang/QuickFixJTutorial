package qfjtutorial.other;

import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

public class MyApplication extends MessageCracker implements quickfix.Application
{
	@Override
    public void fromApp(Message message, SessionID sessionID)
            throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
        crack(message, sessionID);
    }

    // Using annotation
    @Handler
    public void myEmailHandler(quickfix.fix50sp1.NewOrderSingle newOrderSingle, SessionID sessionID) {
        // handler implementation
    	
    	System.out.println("new order single");
    }



	@Override
	public void onCreate(SessionID sessionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLogon(SessionID sessionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLogout(SessionID sessionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fromAdmin(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void toApp(Message message, SessionID sessionId) throws DoNotSend {
		// TODO Auto-generated method stub
		
	}
}
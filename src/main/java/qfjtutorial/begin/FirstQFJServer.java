package qfjtutorial.begin;

import quickfix.Application;
import quickfix.DefaultMessageFactory;
import quickfix.FieldNotFound;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.LogFactory;
import quickfix.Message;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;
import quickfix.UnsupportedMessageType;

//This is simplified version. 
//For production, please see common.DefaultQFJAcceptor and common.DefaultDynamicSessionQFJServer
public class FirstQFJServer {
	
	public static void main(String[] args) throws Exception {

		String configurationFileInClasspath = "qfjtutorial/begin/FirstQFJServer.qfj.config.txt";
		Application application = new AcceptorMessageCalback();

		SessionSettings settings = new SessionSettings(configurationFileInClasspath);
		MessageStoreFactory storeFactory = new FileStoreFactory(settings);
		LogFactory logFactory = new FileLogFactory(settings);
		MessageFactory messageFactory = new DefaultMessageFactory();

		SocketAcceptor accptor = new SocketAcceptor(application, storeFactory, settings, logFactory,
				messageFactory);
		
		accptor.start();
	}
	
	static class AcceptorMessageCalback extends FirstMessageCallback {
		@Override
		public void fromApp(Message paramMessage, SessionID paramSessionID)
				throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {

			System.out.println("fromApp session:, received  " + paramMessage.toString());
			
			try {
				
				Message executionReport = buildHardcodedExecutionReportForTest(paramMessage);
				quickfix.Session.sendToTarget(executionReport, paramSessionID);
				
			} catch (Exception e) {
				System.out.println("problem while processing:" + paramMessage.toString() + e.toString());
				e.printStackTrace(System.err);
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
	
}

package qfjtutorial.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.SessionID;
import quickfix.SessionStateListener;

public class DefaultSessionStateListener implements SessionStateListener{

	protected final static Logger log = LoggerFactory.getLogger(DefaultSessionStateListener.class);
	
	private final SessionID _sessionID;
	public DefaultSessionStateListener(SessionID sessionID){
		_sessionID = sessionID;
	}
	
	@Override
	public void onConnect() {
		log.info("DefaultSessionStateListener onConnect, on session : {}", _sessionID.toString());
		
	}

	@Override
	public void onDisconnect() {
		log.info("DefaultSessionStateListener onDisconnect, on session : {}", _sessionID.toString());
		
	}

	@Override
	public void onHeartBeatTimeout() {
		log.info("DefaultSessionStateListener onHeartBeatTimeout, on session : {}", _sessionID.toString());		
	}

	@Override
	public void onLogon() {
		log.info("DefaultSessionStateListener onLogon, on session : {}", _sessionID.toString());		
	}

	@Override
	public void onLogout() {
		log.info("DefaultSessionStateListener onLogout, on session : {}", _sessionID.toString());		
	}

	@Override
	public void onMissedHeartBeat() {
		log.info("DefaultSessionStateListener onMissedHeartBeat, on session : {}", _sessionID.toString());		
	}

	@Override
	public void onRefresh() {
		log.info("DefaultSessionStateListener onRefresh, on session : {}", _sessionID.toString());		
	}

	@Override
	public void onReset() {
		log.info("DefaultSessionStateListener onReset, on session : {}", _sessionID.toString());		
	}
	
}
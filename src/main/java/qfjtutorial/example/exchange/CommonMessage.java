package qfjtutorial.example.exchange;

public interface CommonMessage {

	// buy or sell the base ccy
	static enum Side {
		BID, OFFER;
	}
	
	static enum OrderType {
		MARKET, LIMIT;
	}
	

}

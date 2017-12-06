package qfjtutorial.example.exchange;

import qfjtutorial.example.exchange.CommonMessage.OrderType;
import qfjtutorial.example.exchange.MatchingEngine.MatchingEngineInputMessageFlag;
import qfjtutorial.example.exchange.MatchingEngine.MatchingEnginOutputMessageFlag;
import qfjtutorial.example.exchange.CommonMessage.Side;

public class TradeMessage {
	static class OriginalOrder implements MatchingEngineInputMessageFlag {

		public OriginalOrder(String symbol, Side side, double price, int qty, long enteringSystemTime, String orderID,
		        String clientOrdID, String clientEntityID) {

			_symbol = symbol;
			_side = side;
			_price = price;
			_qty = qty;
			_enteringSystemTime = enteringSystemTime;
			_orderID = orderID;
			_clientOrdID = clientOrdID;
			_clientEntityID = clientEntityID;
		}

		String _symbol;

		Side _side;
		OrderType _type;
		double _price; //required for LIMIT order
		int _qty;
		long _enteringSystemTime;
		String _orderID;
		String _clientOrdID;
		String _clientEntityID; // to avoid execution with himself
	}

	enum SingleSideExecutionType{

		CANCELLED, REJECTED;
	}
	// maker: who sit in the book
	static class SingleSideExecutionReport implements MatchingEnginOutputMessageFlag{
		
		final SingleSideExecutionType _type;
		final OriginalOrder _originOrder;
		final String _description;
		
		SingleSideExecutionReport(OriginalOrder originOrder, SingleSideExecutionType type, String description){
			_originOrder = originOrder;
			_type = type;
			_description = description;
		}
	}
	// maker: who sit in the book
	static class MatchedExecutionReport implements MatchingEnginOutputMessageFlag{

		final double _lastPrice;
		final int _lastQty;

		final OriginalOrder _makerOriginOrder;
		final OriginalOrder _takerOriginOrder;

		int _makerLeavesQty;
		int _takerLeavesQty;

		public MatchedExecutionReport(double lastPrice, int lastQty, OriginalOrder makerOriginOrder, int makerLeavesQty,
		        OriginalOrder takerOriginOrder, int takerLeavesQty) {

			_lastPrice = lastPrice;
			_lastQty = lastQty;

			_makerOriginOrder = makerOriginOrder;
			_takerOriginOrder = takerOriginOrder;
			_makerLeavesQty = makerLeavesQty;
			_takerLeavesQty = takerLeavesQty;
		}

	}

}

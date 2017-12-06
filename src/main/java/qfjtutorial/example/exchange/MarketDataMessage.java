package qfjtutorial.example.exchange;

import java.util.TreeMap;

import qfjtutorial.example.exchange.CommonMessage.Side;
import qfjtutorial.example.exchange.MatchingEngine.MatchingEnginOutputMessageFlag;
import qfjtutorial.example.exchange.MatchingEngine.MatchingEngineInputMessageFlag;

public class MarketDataMessage {
	
	static class OrderBookDelta implements MatchingEnginOutputMessageFlag {

		final String _symbol;
		final Side _side;
		final double _px;
		final int _deltaQty_couldNegative;

		OrderBookDelta(String symbol, Side side, double px, int deltaQty_couldNegative) {
			_symbol = symbol;
			_side = side;
			_px = px;
			_deltaQty_couldNegative = deltaQty_couldNegative;
		}

	}

	static class AggregatedOrderBook implements MatchingEnginOutputMessageFlag{

		final String _symbol;
		final int _depth;
		TreeMap<Double, Integer> _bidBookMap;
		TreeMap<Double, Integer> _offerBookMap;

		AggregatedOrderBook(String symbol, int depth, TreeMap<Double, Integer> bidBookMap, TreeMap<Double, Integer> offerBookMap) {
			_symbol = symbol;
			_depth = depth;
			_bidBookMap = bidBookMap;
			_offerBookMap= offerBookMap;
		}

	}	
	 
	static class AggregatedOrderBookRequest implements MatchingEngineInputMessageFlag{
		int _depth;
		AggregatedOrderBookRequest(int depth){
			_depth = depth;
		}
	}
}

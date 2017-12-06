package qfjtutorial.example.exchange;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qfjtutorial.example.exchange.MarketDataMessage.AggregatedOrderBook;
import qfjtutorial.example.exchange.MarketDataMessage.OrderBookDelta;
import qfjtutorial.example.exchange.MatchingEngine.MatchingEnginOutputMessageFlag;

/*-
 * partially impelmented
 * 
 * Market Data processing engine
 * - input : output of multi Matching engines 
 * - output : for now, only support JMX query full orderbook
 * 
 * note:
 *  - depth is hardcoded to 10
 *  - only query by JMS for now
 * 
 */
public class MarketDataEngine {

	protected final static Logger log = LoggerFactory.getLogger(MarketDataEngine.class);

	private BlockingQueue<MatchingEnginOutputMessageFlag> _inputAggOrdBookOrDelta; // TODO

	//key : symbol
	private Map<String, AggregatedOrderBook> _lastUpdatedAggregatedOrderBook;

	final int _depth = 10; //hardcode to 10 for for now
	public MarketDataEngine() {
		_lastUpdatedAggregatedOrderBook = new ConcurrentHashMap<String, AggregatedOrderBook>();
	}

	class MarketDataEngineThread extends Thread {

		private volatile boolean _stopFlag = false;

		MarketDataEngineThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted() && !_stopFlag) {
				try {

					MatchingEnginOutputMessageFlag _originalOrderORAggBookRequest = _inputAggOrdBookOrDelta.poll(5,
					        TimeUnit.SECONDS);
					if (_originalOrderORAggBookRequest == null) {
						continue;
					}

					if (_originalOrderORAggBookRequest instanceof AggregatedOrderBook) {

						AggregatedOrderBook aggOrderBook = (AggregatedOrderBook) _originalOrderORAggBookRequest;
						if(aggOrderBook._depth != _depth){
							log.error("diff depth");
							continue;
						}
						
						_lastUpdatedAggregatedOrderBook.put(aggOrderBook._symbol, aggOrderBook);
						
						//TODO generate delta orderbook
						

					} else if (_originalOrderORAggBookRequest instanceof OrderBookDelta) {
						// TODO

					} else {
						log.error("received unknown type : {}",
						        _originalOrderORAggBookRequest.getClass().toGenericString());
					}

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					log.info("matching thread " + Thread.currentThread().getName() + " is interruped", e);
				}
			}

			_stopFlag = true;

		}

		public void stopIt() {
			this._stopFlag = true;
		}
	}
}

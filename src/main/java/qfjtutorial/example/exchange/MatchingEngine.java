package qfjtutorial.example.exchange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qfjtutorial.example.exchange.CommonMessage.Side;
import qfjtutorial.example.exchange.MarketDataMessage.AggregatedOrderBook;
import qfjtutorial.example.exchange.MarketDataMessage.AggregatedOrderBookRequest;
import qfjtutorial.example.exchange.MarketDataMessage.OrderBookDelta;
import qfjtutorial.example.exchange.TradeMessage.MatchedExecutionReport;
import qfjtutorial.example.exchange.TradeMessage.OriginalOrder;
import qfjtutorial.example.exchange.TradeMessage.SingleSideExecutionReport;

/*-
 * 
 * A matching engine for a specific symbol.
 * 
 * note: the /*- prevent the eclipse to format the below TODO list with '-' as prefix
	- //TODO support market order
	- //TODO avoid execution with himself
	- //TODO consider use other data structure(instead of simply priority queue) for book, to 1) simplify dump order book. 2) for FX, support match 2nd best price, if no relationship with 1st one(should this be supported?).
	- //TODO consider to apply DISCRUPTOR to improve the performance, if required.
	- //note: for FX swap, we could use same logic, symbol like USDJPY_1W. But only support Spot+Fwd as standard way.
*/
public class MatchingEngine {

	// e.g. USDJPY for FX, or BABA for security exchange
	private final String _symbol;

	protected final static Logger log = LoggerFactory.getLogger(MatchingEngine.class);
	public static double MIN_DIFF_FOR_PRICE = 0.00000001;

	private BlockingQueue<MatchingEngineInputMessageFlag> _inputOriginalOrderORAggOrdBookRequests; 
	private Queue<MatchingEnginOutputMessageFlag> _outputQueueForExecRpt;
	//the book depth is determined by the request
	//the delta is for ALL prices in the full engine orderbook. 
	private Queue<MatchingEnginOutputMessageFlag> _outputQueueForAggBookAndBookDelta;

	// for FX, bid|ask the base ccy,
	// e.g. for USDJPY, USD is always the base ccy, and JPY is the terms ccy.
	// not required to be thread safe, since it will be operated by single
	// thread
	private PriorityQueue<ExecutingOrder> _bidBook;// higher price is on the top
	private PriorityQueue<ExecutingOrder> _offerBook;// lower price is on the
	                                                 // top

	private final MatchingThread _matchingThread;

	public MatchingEngine(String symbol) {
		_symbol = symbol;

		_bidBook = createBidBook();
		_offerBook = createAskBook();

		_inputOriginalOrderORAggOrdBookRequests = new LinkedBlockingQueue<MatchingEngineInputMessageFlag>();
		_outputQueueForExecRpt = new LinkedBlockingQueue<MatchingEnginOutputMessageFlag>();
		_outputQueueForAggBookAndBookDelta = new LinkedBlockingQueue<MatchingEnginOutputMessageFlag>();

		_matchingThread = new MatchingThread("matcing-thread-" + _symbol);
	}

	public void start() {
		_matchingThread.start();
		log.info("starting matching thread" + _matchingThread.getName());
	}

	public void stop() {

		_matchingThread.stopIt();
		_matchingThread.interrupt();
		log.info("stop matching thread" + _matchingThread.getState());
	}

	// check matching result(ExecutionReport) from _processResult
	public void addOrder(OriginalOrder order) {

		if (!_symbol.equals(order._symbol)) {
			// it should never reach here
			// TODO log error, and throw exception
			return;
		}

		_inputOriginalOrderORAggOrdBookRequests.add(order);
	}

	class MatchingThread extends Thread {

		private volatile boolean _stopFlag = false;

		MatchingThread(String name) {
			super(name);
		}

		@Override
		public void run() {
			while (!Thread.currentThread().isInterrupted() && !_stopFlag) {
				try {

					MatchingEngineInputMessageFlag _originalOrderORAggBookRequest = _inputOriginalOrderORAggOrdBookRequests
					        .poll(5, TimeUnit.SECONDS);
					if (_originalOrderORAggBookRequest == null) {
						continue;
					}

					if (_originalOrderORAggBookRequest instanceof OriginalOrder) {

						OriginalOrder originalOrder = (OriginalOrder) _originalOrderORAggBookRequest;
						processInputOrder(originalOrder);

					} else if (_originalOrderORAggBookRequest instanceof AggregatedOrderBookRequest) {

						AggregatedOrderBookRequest aggOrdBookRequest = (AggregatedOrderBookRequest) _originalOrderORAggBookRequest;
						AggregatedOrderBook aggOrderBook = buildAggregatedOrderBook(aggOrdBookRequest._depth);
						_outputQueueForAggBookAndBookDelta.add(aggOrderBook);

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

	void processInputOrder(OriginalOrder order) {

		ExecutingOrder executingOrder = new ExecutingOrder(order);

		final PriorityQueue<ExecutingOrder> contraSideBook;
		final PriorityQueue<ExecutingOrder> sameSideBook;
		{
			switch (executingOrder._origOrder._side) {
			case BID:
				sameSideBook = _bidBook;
				contraSideBook = _offerBook;
				break;
			case OFFER:
				sameSideBook = _offerBook;
				contraSideBook = _bidBook;
				break;
			default:
				throw new RuntimeException("unknown side : " + executingOrder._origOrder._side);
			}
		}

		List<MatchingEnginOutputMessageFlag> executionReports = new ArrayList<MatchingEnginOutputMessageFlag>();
		List<OrderBookDelta> orderbookDeltas = new ArrayList<OrderBookDelta>();

		match(executingOrder, contraSideBook, sameSideBook, executionReports, orderbookDeltas);

		_outputQueueForExecRpt.addAll(executionReports);
		_outputQueueForAggBookAndBookDelta.addAll(orderbookDeltas);
	}

	/*-
	 * - executingOrder, contraSideBook, and sameSideBook will be updated accordingly 
	 * - TODO all clients could trade with each other. There is NO relationship/credit check. 
	 * - not private, because of UT
	 */
	void match(ExecutingOrder executingOrder, PriorityQueue<ExecutingOrder> contraSideBook,
	        PriorityQueue<ExecutingOrder> sameSideBook, List<MatchingEnginOutputMessageFlag> execReportsAsResult,
	        List<OrderBookDelta> orderbookDeltasAsResult) {

		boolean enterBook = true;
		while (true) {

			ExecutingOrder peekedContraBestPriceOrder = contraSideBook.peek();
			if (peekedContraBestPriceOrder == null) {
				break;
			}

			final boolean isExecutablePrice;
			switch (executingOrder._origOrder._side) {
			case BID:
				isExecutablePrice = executingOrder._origOrder._price
				        + MIN_DIFF_FOR_PRICE > peekedContraBestPriceOrder._origOrder._price;
				break;
			case OFFER:
				isExecutablePrice = executingOrder._origOrder._price
				        - MIN_DIFF_FOR_PRICE > peekedContraBestPriceOrder._origOrder._price;
				break;
			default:
				throw new RuntimeException("unknown side : " + executingOrder._origOrder._side);
			}
			if (!isExecutablePrice) {
				break;
			}

			if (peekedContraBestPriceOrder._origOrder._clientEntityID
			        .equals(executingOrder._origOrder._clientEntityID)) {

				execReportsAsResult.add(new SingleSideExecutionReport(peekedContraBestPriceOrder._origOrder,
				        TradeMessage.SingleSideExecutionType.REJECTED, "Cannot trade with yourself"));
				enterBook = false;
				break;
			}

			// pick the price on top of book, for both BID and ASK order
			final double lastPrice = peekedContraBestPriceOrder._origOrder._price;
			final int lastQty = peekedContraBestPriceOrder._leavesQty < executingOrder._leavesQty
			        ? peekedContraBestPriceOrder._leavesQty : executingOrder._leavesQty;

			peekedContraBestPriceOrder._leavesQty = peekedContraBestPriceOrder._leavesQty - lastQty;
			executingOrder._leavesQty = executingOrder._leavesQty - lastQty;

			execReportsAsResult.add(new MatchedExecutionReport(lastPrice, lastQty,
			        peekedContraBestPriceOrder._origOrder, peekedContraBestPriceOrder._leavesQty,
			        executingOrder._origOrder, executingOrder._leavesQty));
			orderbookDeltasAsResult.add(
			        new OrderBookDelta(_symbol, peekedContraBestPriceOrder._origOrder._side, lastPrice, 0 - lastQty));

			if (peekedContraBestPriceOrder._leavesQty <= 0) {
				// remove it, since already all filled
				contraSideBook.poll();
			}

			if (executingOrder._leavesQty <= 0) {
				break;
			}
		}

		if (enterBook && executingOrder._leavesQty > 0) {
			sameSideBook.add(executingOrder);
			orderbookDeltasAsResult.add(new OrderBookDelta(_symbol, executingOrder._origOrder._side,
			        executingOrder._origOrder._price, executingOrder._leavesQty));
		}

	}

	AggregatedOrderBook buildAggregatedOrderBook(int depth) {

		TreeMap<Double, Integer> bidBookMap = buildOneSideAggOrdBook(depth, Side.BID, _bidBook);
		TreeMap<Double, Integer> offerBookMap = buildOneSideAggOrdBook(depth, Side.OFFER, _offerBook);

		return new AggregatedOrderBook(_symbol, depth, bidBookMap, offerBookMap);
	}

	/*-
	 * - the map key is sorted. bid - reverse, offer - natural ordering
	 * - the map value's order is same with the one of matching order book
	 */
	TreeMap<Double, Integer> buildOneSideAggOrdBook(int depth, Side side, PriorityQueue<ExecutingOrder> sameSideBook) {

		final PriorityQueue<ExecutingOrder> shadowCopyOfSameSideBook;
		final TreeMap<Double, Integer> bookMap;
		switch (side) {
		case BID:
			shadowCopyOfSameSideBook = createBidBook();
			shadowCopyOfSameSideBook.addAll(sameSideBook); // TODO possible
			                                               // performance issue
			                                               // on
			// huge book. Not found better way, yet.
			bookMap = new TreeMap<Double, Integer>(new Comparator<Double>() {

				@Override
				public int compare(Double o1, Double o2) {

					// TODO i assume there is no 1.300000000000000000001 and
			        // 1.29999999999999 case. why? I expect the incoming FIX
			        // message will be checked on the decimal scale of price.
					double r = o1 - o2;
					if (r > 0) {
						return -1;
					} else if (r < 0) {
						return 1;
					} else {
						return 0;
					}
				}
			});
			break;
		case OFFER:
			shadowCopyOfSameSideBook = createAskBook();
			shadowCopyOfSameSideBook.addAll(sameSideBook);// TODO possible
			                                              // performance issue
			                                              // on huge
			// book
			bookMap = new TreeMap<Double, Integer>();
			break;
		default:
			// TODO log error, and don't throw exception to avoid the engine
			// shutdown
			throw new RuntimeException("");
		}

		while (!shadowCopyOfSameSideBook.isEmpty()) {
			ExecutingOrder o = shadowCopyOfSameSideBook.poll();
			double price = o._origOrder._price;
			int leavesQty = o._leavesQty;

			int aggregatedLeavesQty = bookMap.get(price) == null ? 0 : bookMap.get(price);
			bookMap.put(price, aggregatedLeavesQty + leavesQty);

			if (bookMap.size() == depth + 1) {
				bookMap.remove(price);
				break;
			}
		}

		return bookMap;
	}

	static interface MatchingEngineInputMessageFlag {
	}

	static interface MatchingEnginOutputMessageFlag {
	}

	static class ExecutingOrder {

		// this value will change on each matching
		int _leavesQty;

		final OriginalOrder _origOrder;

		ExecutingOrder(OriginalOrder originalOrder) {
			_origOrder = originalOrder;
			_leavesQty = originalOrder._qty;
		}

	}

	PriorityQueue<ExecutingOrder> createBidBook() {
		return new PriorityQueue<ExecutingOrder>(new Comparator<ExecutingOrder>() {

			@Override
			public int compare(ExecutingOrder o1, ExecutingOrder o2) {

				// TODO it should also be considered equal price, if the diff is
		        // very minor.
				if (o1._origOrder._price == o2._origOrder._price) {
					return (int) (o1._origOrder._enteringSystemTime - o2._origOrder._enteringSystemTime);
				}

				if (o1._origOrder._price > o2._origOrder._price) {
					return -1;
				} else {
					return 1;
				}

			}
		});
	}

	PriorityQueue<ExecutingOrder> createAskBook() {
		return new PriorityQueue<ExecutingOrder>(new Comparator<ExecutingOrder>() {

			@Override
			public int compare(ExecutingOrder o1, ExecutingOrder o2) {

				// TODO it should also be considered equal price, if the diff is
		        // very minor.
				if (o1._origOrder._price == o2._origOrder._price) {
					return (int) (o1._origOrder._enteringSystemTime - o2._origOrder._enteringSystemTime);
				}

				if (o1._origOrder._price > o2._origOrder._price) {
					return 1;
				} else {
					return -1;
				}

			}
		});

	}

}

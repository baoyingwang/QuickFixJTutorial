package qfjtutorial.example.fxexchange;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

//TODO support market order
//TODO avoid execution with himself
//TODO consider use other data structure(instead of simply priority queue) for book, to simplify dump order book.
//TODO consider to apply DISCRUPTOR to improve the performance, if required.
//TODO extend to support swap, we could use same logic, symbol like USDJPY_1W. But only support Spot+Fwd as standard way.
public class FXSpotExchange {

	protected final static Logger log = LoggerFactory.getLogger(FXSpotExchange.class);

	public static double min_dff_for_price = 0.00000001;

	// e.g. USDJPY
	private final String _symbol;

	private BlockingQueue<Object> _inputOriginalOrderORSnapshotRequests; //TODO refactor this bad name
	private Queue<ExecutionReport> _outputExecRptQueue;
	private Queue<Object> _outputBookSnapshotOrBookDeltaQueue;
	
	// bid|ask the base ccy,
	// e.g. for USDJPY, USD is always the base ccy, and JPY is the terms ccy.
	// not required to be thread safe, since it will be operated by single
	// thread
	private PriorityQueue<ExecutingOrder> _bidBook;// higher price is on the top
	private PriorityQueue<ExecutingOrder> _offerBook;// lower price is on the top

	

	private final MatchingThread _matchingThread;

	public FXSpotExchange(String symbol) {		
		_symbol = symbol;
		
		_bidBook = createBidBook();
		_offerBook = createAskBook();

		_inputOriginalOrderORSnapshotRequests = new LinkedBlockingQueue<Object>();
		_outputExecRptQueue = new LinkedBlockingQueue<ExecutionReport>();
		_outputBookSnapshotOrBookDeltaQueue = new LinkedBlockingQueue<Object>();

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
			//TODO log error, and throw exception
			return;
		}

		_inputOriginalOrderORSnapshotRequests.add(order);
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

					Object _originalOrderORSnapshotRequests = _inputOriginalOrderORSnapshotRequests.poll(5, TimeUnit.SECONDS);
					if (_originalOrderORSnapshotRequests == null) {
						continue;
					}
					
					if(_originalOrderORSnapshotRequests instanceof OriginalOrder){
					
						OriginalOrder originalOrder = (OriginalOrder)_originalOrderORSnapshotRequests;
						processInputOrder(originalOrder);
						
					}else if(_originalOrderORSnapshotRequests instanceof OrderbookSnapshotRequest){
						
						OrderbookSnapshotRequest snapshotRequest = (OrderbookSnapshotRequest)_originalOrderORSnapshotRequests;
						OrderBookSnapshot orderBookSnapshot = buildOrderBookSnapshot(snapshotRequest._depth, _bidBook, _offerBook);
						_outputBookSnapshotOrBookDeltaQueue.add(orderBookSnapshot);
						
					}else{
						log.error("received unknown type : {}" , _originalOrderORSnapshotRequests.getClass().toGenericString());
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

	void processInputOrder(OriginalOrder order){
		
		ExecutingOrder executingOrder = new ExecutingOrder(order);
		final PriorityQueue<ExecutingOrder> contraSideBook;
		final PriorityQueue<ExecutingOrder> sameSideBook;
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

		List<ExecutionReport> execReports = new ArrayList<ExecutionReport>();
		List<OrderBookDelta> orderbookDeltas = new ArrayList<OrderBookDelta>();
		match(executingOrder, contraSideBook, sameSideBook, execReports, orderbookDeltas);
		_outputExecRptQueue.addAll(execReports);
		_outputBookSnapshotOrBookDeltaQueue.addAll(orderbookDeltas);
	}
	
	/**
	 * 1) executingOrder, contraSideBook, and sameSideBook will be updated
	 * accordingly 2) TODO all clients could trade with each other. There is NO
	 * relationship/credit check. 3) not private, because of UT
	 */
	void match(ExecutingOrder executingOrder, PriorityQueue<ExecutingOrder> contraSideBook,
	        PriorityQueue<ExecutingOrder> sameSideBook, List<ExecutionReport> execReportsAsResult, List<OrderBookDelta> orderbookDeltasAsResult) {

		//List<ExecutionReport> execReports = new ArrayList<ExecutionReport>();
		//List<OrderBookDelta> orderbookDeltas = new ArrayList<OrderBookDelta>();
		while (true) {

			ExecutingOrder peekedContraBestPriceOrder = contraSideBook.peek();
			if (peekedContraBestPriceOrder == null) {
				break;
			}

			final boolean isExecutable;
			switch (executingOrder._origOrder._side) {
			case BID:
				isExecutable = executingOrder._origOrder._price
				        + min_dff_for_price > peekedContraBestPriceOrder._origOrder._price;
				break;
			case OFFER:
				isExecutable = executingOrder._origOrder._price
				        - min_dff_for_price > peekedContraBestPriceOrder._origOrder._price;
				break;
			default:
				throw new RuntimeException("unknown side : " + executingOrder._origOrder._side);
			}
			if (!isExecutable) {
				break;
			}

			// pick the price on top of book, for both BID and ASK order
			final double lastPrice = peekedContraBestPriceOrder._origOrder._price;
			final int lastQty = peekedContraBestPriceOrder._leavesQty < executingOrder._leavesQty
			        ? peekedContraBestPriceOrder._leavesQty : executingOrder._leavesQty;

			peekedContraBestPriceOrder._leavesQty = peekedContraBestPriceOrder._leavesQty - lastQty;
			executingOrder._leavesQty = executingOrder._leavesQty - lastQty;

			execReportsAsResult.add(new ExecutionReport(lastPrice, lastQty, peekedContraBestPriceOrder._origOrder,
			        peekedContraBestPriceOrder._leavesQty, executingOrder._origOrder, executingOrder._leavesQty));
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

		if (executingOrder._leavesQty > 0) {
			sameSideBook.add(executingOrder);
			orderbookDeltasAsResult.add(new OrderBookDelta(_symbol, executingOrder._origOrder._side,
			        executingOrder._origOrder._price, executingOrder._leavesQty));
		}

	}
	
	OrderBookSnapshot buildOrderBookSnapshot(int depth, PriorityQueue<ExecutingOrder> bidBook, PriorityQueue<ExecutingOrder> offerBook){
	
		Multimap<Double, Integer> bidBookMap =  buildOneSideBookSnapshot(depth, Side.BID, bidBook);
		Multimap<Double, Integer> offerBookMap =  buildOneSideBookSnapshot(depth, Side.OFFER, offerBook);
		
		return new OrderBookSnapshot(_symbol,depth, bidBookMap, offerBookMap);
	}
	
	Multimap<Double, Integer> buildOneSideBookSnapshot(int depth, Side side, PriorityQueue<ExecutingOrder> sameSideBook){
		
		PriorityQueue<ExecutingOrder> book;
		switch (side) {
		case BID:			
			book = createBidBook();
			book.addAll(sameSideBook); //TODO possible performance issue on huge book
			break;
		case OFFER:
			book = createAskBook();
			book.addAll(sameSideBook);//TODO possible performance issue on huge book
			break;
		default:
			//TODO log error, and dont exception
			throw new RuntimeException("");
		}		
		
		Multimap<Double, Integer> bookMap =  ArrayListMultimap.create();
		while( !book.isEmpty() ){
			ExecutingOrder o = book.poll();
			double price = o._origOrder._price;
			int leavesQty = o._leavesQty;
			bookMap.put(price, leavesQty);
			
			if(bookMap.size() == depth+1){
				bookMap.removeAll(price);
				break;
			}
		}
		
		return bookMap;
	}
	
	

	// buy or sell the base ccy
	static enum Side {
		BID, OFFER;
	}

	static class OrderBookDelta {

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

	static class OrderBookSnapshot {

		final String _symbol;
		final int _depth;
		Multimap<Double, Integer> _bidBookMap;
		Multimap<Double, Integer> _offerBookMap;

		OrderBookSnapshot(String symbol, int depth, Multimap<Double, Integer> bidBookMap, Multimap<Double, Integer> offerBookMap) {
			_symbol = symbol;
			_depth = depth;
			_bidBookMap = bidBookMap;
			_offerBookMap= offerBookMap;
		}

	}	
	 
	static class OrderbookSnapshotRequest{
		int _depth;
		OrderbookSnapshotRequest(int depth){
			_depth = depth;
		}
	}
	static class OriginalOrder {

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
		double _price;
		int _qty;
		long _enteringSystemTime;
		String _orderID;
		String _clientOrdID;
		String _clientEntityID; // to avoid execution with himself
	}

	static class ExecutingOrder {

		// this value will change
		int _leavesQty;

		final OriginalOrder _origOrder;

		public ExecutingOrder(OriginalOrder originalOrder) {
			_origOrder = originalOrder;
			_leavesQty = originalOrder._qty;
		}

	}

	// maker: who sit in the book
	static class ExecutionReport {

		final double _lastPrice;
		final int _lastQty;

		final OriginalOrder _makerOriginOrder;
		final OriginalOrder _takerOriginOrder;

		int _makerLeavesQty;
		int _takerLeavesQty;

		public ExecutionReport(double lastPrice, int lastQty, OriginalOrder makerOriginOrder, int makerLeavesQty,
		        OriginalOrder takerOriginOrder, int takerLeavesQty) {

			_lastPrice = lastPrice;
			_lastQty = lastQty;

			_makerOriginOrder = makerOriginOrder;
			_takerOriginOrder = takerOriginOrder;
			_makerLeavesQty = makerLeavesQty;
			_takerLeavesQty = takerLeavesQty;
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

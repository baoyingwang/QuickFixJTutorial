package qfjtutorial.example.fxexchange;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import org.junit.Test;

import com.google.common.collect.Multimap;

import qfjtutorial.example.fxexchange.FXSpotExchange.ExecutingOrder;
import qfjtutorial.example.fxexchange.FXSpotExchange.ExecutionReport;
import qfjtutorial.example.fxexchange.FXSpotExchange.OrderBookDelta;
import qfjtutorial.example.fxexchange.FXSpotExchange.OriginalOrder;
import qfjtutorial.example.fxexchange.FXSpotExchange.Side;

public class FXSpotExchangeTest {

	FXSpotExchange _exchange = new FXSpotExchange("USDJPY");
	
	
	@Test
	public void testCreateBidBook(){
		
		String symbol = "USDJPY";
		FXSpotExchange.Side side = FXSpotExchange.Side.BID;
		
		PriorityQueue<ExecutingOrder> book = _exchange.createBidBook();
		
		OriginalOrder o_100_1mio = new OriginalOrder(symbol, side, 100.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
		OriginalOrder o_120_1mio = new OriginalOrder(symbol, side, 120.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
		OriginalOrder o_130_1mio_sysT1 = new OriginalOrder(symbol, side, 130.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
		OriginalOrder o_130_1mio_sysT2 = new OriginalOrder(symbol, side, 130.1, 1000_000, 2, "orderID", "clientOrdID", "clientEntityID");
		
		book.add(new ExecutingOrder(o_100_1mio));
		book.add(new ExecutingOrder(o_130_1mio_sysT1));
		book.add(new ExecutingOrder(o_120_1mio));
		book.add(new ExecutingOrder(o_130_1mio_sysT2));
		
		//TODO add assert, rather than view by eyes!
//		for(ExecutingOrder o : bidBook){
//			System.out.println(o._origOrder._price + " " + o._origOrder._enteringSystemTime);
//		}
		while( !book.isEmpty() ){
			ExecutingOrder o = book.poll();
			System.out.println(o._origOrder._price + " " + o._origOrder._enteringSystemTime);
		}		
	}
	
	@Test
	public void testCreateAskBook(){

		String symbol = "USDJPY";
		FXSpotExchange.Side side = FXSpotExchange.Side.OFFER;
		
		
		OriginalOrder o_140_1mio = new OriginalOrder(symbol, side, 140.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
		OriginalOrder o_150_1mio = new OriginalOrder(symbol, side, 150.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
		OriginalOrder o_160_1mio_sysT1 = new OriginalOrder(symbol, side, 160.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
		OriginalOrder o_160_1mio_sysT2 = new OriginalOrder(symbol, side, 160.1, 1000_000, 2, "orderID", "clientOrdID", "clientEntityID");
		
		PriorityQueue<ExecutingOrder> book = _exchange.createAskBook();
		book.add(new ExecutingOrder(o_140_1mio));
		book.add(new ExecutingOrder(o_160_1mio_sysT1));
		book.add(new ExecutingOrder(o_150_1mio));
		book.add(new ExecutingOrder(o_160_1mio_sysT2));
		
		
		while( !book.isEmpty() ){
			ExecutingOrder o = book.poll();
			System.out.println(o._origOrder._price + " " + o._origOrder._enteringSystemTime);
		}
		
		//TODO add assert, rather than view by eyes!
//		for(ExecutingOrder o : book){
//			System.out.println(o._origOrder._price + " " + o._origOrder._enteringSystemTime);
//		}
	}
	
	
	@Test
	public void testMatch(){
		String symbol = "USDJPY";
		
		
		PriorityQueue<ExecutingOrder> bidBook = _exchange.createBidBook();
		{
			FXSpotExchange.Side side = FXSpotExchange.Side.BID;
			OriginalOrder o_100_1mio = new OriginalOrder(symbol, side, 100.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_120_1mio = new OriginalOrder(symbol, side, 120.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_130_1mio_sysT1 = new OriginalOrder(symbol, side, 130.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_130_1mio_sysT2 = new OriginalOrder(symbol, side, 130.1, 1000_000, 2, "orderID", "clientOrdID", "clientEntityID");
			
			bidBook.add(new ExecutingOrder(o_100_1mio));
			bidBook.add(new ExecutingOrder(o_130_1mio_sysT1));
			bidBook.add(new ExecutingOrder(o_120_1mio));
			bidBook.add(new ExecutingOrder(o_130_1mio_sysT2));
		}
		
		PriorityQueue<ExecutingOrder> askBook = _exchange.createAskBook();
		{
			FXSpotExchange.Side side = FXSpotExchange.Side.OFFER;
			OriginalOrder o_140_1mio = new OriginalOrder(symbol, side, 140.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_150_1mio = new OriginalOrder(symbol, side, 150.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_160_1mio_sysT1 = new OriginalOrder(symbol, side, 160.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_160_1mio_sysT2 = new OriginalOrder(symbol, side, 160.1, 1000_000, 2, "orderID", "clientOrdID", "clientEntityID");
			
			
			askBook.add(new ExecutingOrder(o_140_1mio));
			askBook.add(new ExecutingOrder(o_160_1mio_sysT1));
			askBook.add(new ExecutingOrder(o_150_1mio));
			askBook.add(new ExecutingOrder(o_160_1mio_sysT2));
		}
		
		OriginalOrder bid_145_1point5Mio = new OriginalOrder(symbol, FXSpotExchange.Side.BID, 155, 1500_000, 1, "orderID", "clientOrdID", "clientEntityID");
		List<ExecutionReport> reports = new ArrayList<ExecutionReport>();
		List<OrderBookDelta> orderbookDeltas = new ArrayList<OrderBookDelta>();
		this._exchange.match(new ExecutingOrder(bid_145_1point5Mio), askBook, bidBook, reports,orderbookDeltas);
		for(ExecutionReport er : reports){
			System.out.println("last px:"+er._lastPrice +" last qty:"+ er._lastQty);
		}

		System.out.println("bid book");
		while( !bidBook.isEmpty() ){
			ExecutingOrder o = bidBook.poll();
			System.out.println(o._origOrder._price + " " + o._leavesQty);
		}
		System.out.println("ask book");
		while( !askBook.isEmpty() ){
			ExecutingOrder o = askBook.poll();
			System.out.println(o._origOrder._price + " " + o._leavesQty);
		}
	}
	
	@Test
	public void testBuildOrderbookSnapshot(){
		
		String symbol = "USDJPY";
		
		PriorityQueue<ExecutingOrder> bidBook = _exchange.createBidBook();
		{
			FXSpotExchange.Side side = FXSpotExchange.Side.BID;
			OriginalOrder o_100_1mio = new OriginalOrder(symbol, side, 100.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_120_1mio = new OriginalOrder(symbol, side, 120.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_130_1mio_sysT1 = new OriginalOrder(symbol, side, 130.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_130_1mio_sysT2 = new OriginalOrder(symbol, side, 130.1, 1000_000, 2, "orderID", "clientOrdID", "clientEntityID");
			
			bidBook.add(new ExecutingOrder(o_100_1mio));
			bidBook.add(new ExecutingOrder(o_130_1mio_sysT1));
			bidBook.add(new ExecutingOrder(o_120_1mio));
			bidBook.add(new ExecutingOrder(o_130_1mio_sysT2));
		}
		
		PriorityQueue<ExecutingOrder> askBook = _exchange.createAskBook();
		{
			FXSpotExchange.Side side = FXSpotExchange.Side.OFFER;
			OriginalOrder o_140_1mio = new OriginalOrder(symbol, side, 140.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_150_1mio = new OriginalOrder(symbol, side, 150.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_160_1mio_sysT1 = new OriginalOrder(symbol, side, 160.1, 1000_000, 1, "orderID", "clientOrdID", "clientEntityID");
			OriginalOrder o_160_1mio_sysT2 = new OriginalOrder(symbol, side, 160.1, 1000_000, 2, "orderID", "clientOrdID", "clientEntityID");
			
			
			askBook.add(new ExecutingOrder(o_140_1mio));
			askBook.add(new ExecutingOrder(o_160_1mio_sysT1));
			askBook.add(new ExecutingOrder(o_150_1mio));
			askBook.add(new ExecutingOrder(o_160_1mio_sysT2));
		}
		
		Multimap<Double, Integer> bidBookSnapshot = _exchange.buildOneSideBookSnapshot(3, Side.BID, bidBook);
		
		for(Double price : bidBookSnapshot.keySet() ){
			
			System.out.println("price : " + price);
			Collection<Integer> qtyList = bidBookSnapshot.get(price);
			for(Integer qty : qtyList){
				System.out.println("          :" + qty);	
			}
		}
		
	}
}

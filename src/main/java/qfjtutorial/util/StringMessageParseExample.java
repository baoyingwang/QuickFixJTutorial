package qfjtutorial.util;

import quickfix.DataDictionary;
import quickfix.DefaultMessageFactory;
import quickfix.Message;
import quickfix.MessageFactory;

public class StringMessageParseExample {

    public static void main(String[] args) throws Exception{
        MessageFactory messageFactory = new DefaultMessageFactory();
        //DataDictionary dataDictionary = new DataDictionary("dictionary/RTNS-FIX50SP1.xml");
        DataDictionary dataDictionary = new DataDictionary("dictionary/v7.7.2_FIX50SP1_TRTN.xml");
        dataDictionary.setCheckUnorderedGroupFields(false);
        dataDictionary.setCheckFieldsOutOfOrder(false);


        String messageString = "8=FIXT.1.1%9=1308%35=AE%34=112%1128=8%49=RTNSFIXUAT%56=ITAURECUAT%52=20171003-15:05:38%571=17001004%150=F%570=N%460=4%167=OPT%60=20171003-10:34:00%75=20171003%55=USD/BRL%552=1%54=1%1057=Y%37=20171003-100%453=8%448=IBBS%452=3%447=D%802=4%523=5H5XJI2U5XWL4UIKDF77%803=4010%523=ESMA%803=4024%523=DoddFrankAct%803=4024%523=Itau Bank Test%803=5%448=BSMX%452=1%447=D%802=5%523=HKLN%803=4012%523=MP6I5ZYZBEU3UXPYFY54%803=4010%523=ESMA%803=4024%523=DoddFrankAct%803=4024%523=test Santander Counterparty%803=5%448=admin%452=44%447=D%448=LMEB%452=16%447=D%802=1%523=test system%803=5%448=USA%452=11%447=D%448=DTCC%452=72%447=D%802=4%523=GFIBLTL2668381%803=4014%11059=0%523=GFIBLTH419350%803=4015%11059=0%523=2887408%803=4017%523=1010000274%803=4013%448=SEF%452=73%447=D%802=1%523=DoddFrankAct%803=4023%448=MTF%452=73%447=D%802=1%523=ESMA%803=4023%555=1%624=1%1418=30000000%11012=2%9075=37%1379=11.00%942=USD%600=USD/BRL%1358=0%9126=1%588=20171010%687=108750%566=0.3625%11013=1%11019=4%11015=20171010%11016=USD%612=3.15%611=20171005%11014=BRSAO%1212=13:15:00%1420=0%675=USD%1445=1%1446=Other%11033=20171005-00:00:00%11034=BRSAO%670=1%671=UNSPECIFIED%673=30000000%11254=94500000%32=0.0%31=0.0%38=0.0%9016=2%11020=1%11021=20171010%11022=1%11023=12300000%11025=USD%11024=3.1645%675=USD%1445=1%1446=Other%11033=20171005-13:15:00%11034=BRSAO%10=230%";
        messageString = messageString.replace('%', (char)0x01);
        Message qfjMsg = quickfix.MessageUtils.parse(messageFactory,dataDictionary,messageString );

        dataDictionary.validate(qfjMsg);

        System.out.println(qfjMsg.toString());
    }
}

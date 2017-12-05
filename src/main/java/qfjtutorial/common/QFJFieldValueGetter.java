package qfjtutorial.common;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.FieldMap;
import quickfix.FieldNotFound;
/**
 * 
 * if you use the generated messages, this method is not required for you.
 * looks like it is more prefered to use the generated messages for dev, but maybe QA need the raw usage.
 *
 */
public class QFJFieldValueGetter {

	protected final static Logger log = LoggerFactory.getLogger(QFJFieldValueGetter.class);
	
    public static Optional<String> getString(FieldMap msgOrGroup, int tag){
    	
    	String value = null;
        if(msgOrGroup.isSetField(tag)){
            try {
            	value = msgOrGroup.getString(tag);
			} catch (FieldNotFound e) {
				// it should NOT reach here, since we have checked whether it exists
				log.error("fail to get tag :" + tag +" from "+ msgOrGroup.toString(), e);
			}
        }
        
        if(value == null){
        	return Optional.empty();
        }else{
        	return Optional.of(value);
        }
    }
    
    public static Optional<Integer> getInt(FieldMap msgOrGroup, int tag){
    	
    	Integer value = null;
        if(msgOrGroup.isSetField(tag)){
            try {
            	value = msgOrGroup.getInt(tag);
			} catch (FieldNotFound e) {
				log.error("fail to get tag :" + tag +" from "+ msgOrGroup.toString(), e);
			}
        }
        
        if(value == null){
        	return Optional.empty();
        }else{
        	return Optional.of(value);
        }
    }
    
}

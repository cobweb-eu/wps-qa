package example.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.FeatureCollection;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;

public class ReturnSOSData extends AbstractAlgorithm {

	private String input = "input";
	private String output = "output";




@Override
public Map<String, IData> run(Map<String, List<IData>> inputData)
		throws ExceptionReport {
	
	List inputList = inputData.get("input");
	
	FeatureCollection fc = ((GTVectorDataBinding)inputList.get(0)).getPayload();
	
	
	Map<String,IData>result = new HashMap<String, IData>();
	
	result.put("output", new GTVectorDataBinding (fc));	
	
	return result;
}

@Override
public List<String> getErrors() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public Class<?> getInputDataType(String identifier) {
	if(identifier.equalsIgnoreCase("input")){
		return GTVectorDataBinding.class;
	}
	return null;
}

@Override
public Class<?> getOutputDataType(String identifier) {
	if(identifier.equalsIgnoreCase( "output")){
		return GTVectorDataBinding.class;
	}
	return null;
}
}

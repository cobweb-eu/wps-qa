package storage.geoserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class StoreWFSOutput extends AbstractAlgorithm {

	private final String inputObservations = "inputObservations";
	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		return null;
	}

	@Override
	public Class<?> getOutputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("qual_result")){
			return GTVectorDataBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {
		List<IData> obsList = inputData.get(inputObservations);
		FeatureCollection obsFc = ((GTVectorDataBinding)obsList.get(0)).getPayload();
		
		SimpleFeatureType typeF = null;
		
		ArrayList<SimpleFeature> result = new ArrayList<SimpleFeature>();
		
		SimpleFeatureIterator obsIt =  (SimpleFeatureIterator) obsFc.features();
		
		while (obsIt.hasNext()){
			SimpleFeature tempFeature = obsIt.next();
			typeF = tempFeature.getFeatureType();
			result.add(tempFeature);
			
		}
		ListFeatureCollection resultList = new ListFeatureCollection(typeF, result);
		Map <String, IData> results = new HashMap<String, IData>();
		results.put("qual_result",new GTVectorDataBinding (resultList));
		
		return results;
	}
	
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}


}

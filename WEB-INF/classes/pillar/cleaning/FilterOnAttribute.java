package pillar.cleaning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralBooleanBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class FilterOnAttribute extends AbstractAlgorithm{
	Logger LOG = Logger.getLogger(FilterOnAttribute.class);
	private final String inputObservations = "inputObservations";
	private final String fieldName = "fieldName";
	private final String featureName = "featureName";
	private final String include = "include";
	
	@Override
	public Class<?> getInputDataType(String identifier) {
		if (identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("fieldName")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("featureName")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("include")){
			return LiteralBooleanBinding.class;
		}
		return null;
	}

	@Override
	public Class<?> getOutputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("result")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("qual_result")){
			return GTVectorDataBinding.class;
		}
		
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {
		
		List obsList = inputData.get("inputObservations");
		List fieldList = inputData.get("fieldName");
		List featureList = inputData.get("featureName");
		List incList = inputData.get("include");
		
		FeatureCollection obsFc = ((GTVectorDataBinding)obsList.get(0)).getPayload();
		String fieldN = ((LiteralStringBinding) fieldList.get(0)).getPayload();
		String featureN = ((LiteralStringBinding) featureList.get(0)).getPayload();
		boolean includeB = ((LiteralBooleanBinding) incList.get(0)).getPayload();
		LOG.warn("inlcudeB " + includeB);
		
		ArrayList<SimpleFeature> resultList = new ArrayList<SimpleFeature>(); 
		
		ArrayList<SimpleFeature> qual_resultList = new ArrayList<SimpleFeature>();
		
		
		SimpleFeatureIterator sfi = (SimpleFeatureIterator) obsFc.features();
		SimpleFeature tempPropFeature = sfi.next();
		
		CoordinateReferenceSystem inputObsCrs = obsFc.getSchema().getCoordinateReferenceSystem();
		
		Collection<Property> obsProp = tempPropFeature.getProperties();
		
		
		//SimpleFeatureType typeF = tempPropFeature.getType();
		
		SimpleFeatureTypeBuilder resultTypeBuilder = new SimpleFeatureTypeBuilder();
		resultTypeBuilder.setName("typeBuilder");
		resultTypeBuilder.setCRS(inputObsCrs);
		sfi.close();
		
		Iterator<Property> pItObs = obsProp.iterator();

		while (pItObs.hasNext()==true){
			
			try{
				
			Property tempProp = pItObs.next();
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
			
			resultTypeBuilder.add(name, valueClass);
			

			LOG.warn ("Obs property " + name + " " + valueClass);
			}
			catch (Exception e){
				LOG.error("property error " + e);
			}
		}
		
		resultTypeBuilder.add("DQ_ThematicAccuracy", Double.class);
		
		SimpleFeatureType typeF = resultTypeBuilder.buildFeatureType();
		SimpleFeatureBuilder resultFeatureBuilder = new SimpleFeatureBuilder(typeF);
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) obsFc.features();
		int within = 0;
		
		while (obsIt.hasNext()){
			
			within = 0;
			
			SimpleFeature tempFeature = obsIt.next();
			
			String tempProp = tempFeature.getProperty(fieldN).getValue().toString();
			
			LOG.warn("tempProp " + tempProp);
			
			for (Property obsProperty : tempFeature.getProperties()){

				
				String name = obsProperty.getName().toString();
				Object value = obsProperty.getValue();
				
				resultFeatureBuilder.set(name, value);
				
			}
			
			if(includeB == true){
			
			LOG.warn("Here Within " + within);
				if (tempProp.equalsIgnoreCase(featureN)){
					within = 1;
					
					
					
					resultFeatureBuilder.set("DQ_ThematicAccuracy", 1);
				
				}
				else{
					resultFeatureBuilder.set("DQ_ThematicAccuracy", 0);
					
				}
			}
			
			if(includeB == false){
				
				
				if (!tempProp.equalsIgnoreCase(featureN)){
					within = 1;
					resultFeatureBuilder.set("DQ_ThematicAccuracy", 1);
				}
					else{
						resultFeatureBuilder.set("DQ_ThematicAccuracy", 0);
						
					}
				
				
				
			}
			Geometry geom = (Geometry) tempFeature.getDefaultGeometry();
			
		
			SimpleFeature tempResult = resultFeatureBuilder.buildFeature(tempFeature.getID());
			tempResult.setDefaultGeometry(geom);
			LOG.warn("temp Result " + tempResult.getType().toString());
			resultList.add(tempResult);
			if(within == 1){
				qual_resultList.add(tempResult);
			}
			
			
			
			
		}
		
		
		obsIt.close();
		ListFeatureCollection qual_resultFc = new ListFeatureCollection(typeF, qual_resultList);
		ListFeatureCollection resultFc = new ListFeatureCollection(typeF, resultList);
		
		Map<String, IData> results = new HashMap<String, IData>();
		
		results.put("result", new GTVectorDataBinding (resultFc));
		results.put("qual_result", new GTVectorDataBinding (qual_resultFc));
		
		
		
		
		
		
		
		return results;
	}
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}

}

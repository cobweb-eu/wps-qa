package pillar.authoritativedata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralIntBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

//counts number of similar features within a radius
//returns similar features within buffer using attribute as similarity
//includes maximum and minimum threshold
//metadata ThematicAccuracy

public class NumberOfNeighbourSimilarFeatures extends AbstractAlgorithm{
	
	Logger LOG = Logger.getLogger(NumberOfNeighbourSimilarFeatures.class);
	
	private final String inputObservations = "inputObservations";
	private final String inputAuthoritativeData = "inputAuthoritativeData";
	private final String inputDistance = "inputDistance";
	private final String minNumber = "minNumber";
	private final String fieldName = "fieldName";
	
	

	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("inputAuthoritativeData")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("inputDistance")){
			return LiteralDoubleBinding.class;
		}
		if(identifier.equalsIgnoreCase("minNumber")){
			return LiteralIntBinding.class;
		}
		if(identifier.equalsIgnoreCase("fieldName")){
			return LiteralStringBinding.class;
		}
		
		return null;
	}

	@Override
	public Class<?> getOutputDataType(String identifier) {
		if (identifier.equalsIgnoreCase("result")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("qual_result")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("metadata")){
			return GenericFileDataBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {
		
		List obsList = inputData.get("inputObservations");
		List authList = inputData.get("inputAuthoritativeData");
		List distList = inputData.get("inputDistance");
		List minList = inputData.get("minNumber");
		List fieldList = inputData.get("fieldName");
		
		
		
		FeatureCollection obsFc = ((GTVectorDataBinding) obsList.get(0)).getPayload();
		FeatureCollection authFc = ((GTVectorDataBinding) authList.get(0)).getPayload();
		double dist = ((LiteralDoubleBinding)distList.get(0)).getPayload();
		int minNum = ((LiteralIntBinding)minList.get(0)).getPayload();
		String fieldName = ((LiteralStringBinding)fieldList.get(0)).getPayload();
		
		
		ArrayList<SimpleFeature> resultFeatures = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> returnFeatures = new ArrayList<SimpleFeature>();
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) obsFc.features();
		SimpleFeatureIterator authIt = (SimpleFeatureIterator) authFc.features();
		
		SimpleFeatureIterator sfi = (SimpleFeatureIterator) obsFc.features();
		SimpleFeatureType fType = null;
		SimpleFeature tempPropFeature = sfi.next();
		CoordinateReferenceSystem inputObsCrs = obsFc.getSchema().getCoordinateReferenceSystem();
		
		Collection<Property> obsProp = tempPropFeature.getProperties();
		
		SimpleFeatureTypeBuilder resultTypeBuilder = new SimpleFeatureTypeBuilder();
		resultTypeBuilder.setName("typeBuilder");
		resultTypeBuilder.setCRS(inputObsCrs);
		
		Iterator<Property> pItObs = obsProp.iterator();
		
		sfi.close();
		while (pItObs.hasNext()==true){
			
			try{
				
			Property tempProp = pItObs.next();
			PropertyType type = tempProp.getDescriptor().getType();
			String name = type.getName().getLocalPart();
			Class<String> valueClass = (Class<String>)tempProp.getType().getBinding();
			
			resultTypeBuilder.add(name, valueClass);
			
			//LOG.warn ("Obs property " + name + " " + valueClass + " " +type.toString());
			}
			catch (Exception e){
				LOG.error("property error " + e);
			}
		}
		
		//add DQ_Field
		
		resultTypeBuilder.add("DQ_SA_SimilarF", Double.class);
		SimpleFeatureType typeF = resultTypeBuilder.buildFeatureType();
		//LOG.warn("Get Spatial Accuracy Feature Type " + typeF.toString());
		
		SimpleFeatureBuilder resultFeatureBuilder = new SimpleFeatureBuilder(typeF);
		
		obsIt.close();
		
		SimpleFeatureIterator obsIt2 = (SimpleFeatureIterator) obsFc.features();
		
		while (obsIt2.hasNext()){
			SimpleFeature tempFeature = obsIt2.next();
			
			fType = tempFeature.getType();
			
			//LOG.warn("fieldName " + fieldName + " featureName " + featureN + " tempFeature Type " + fType);
			//LOG.warn("TableFeatureName " + tempFeature.getProperty(fieldName).getValue());
			
			String tempFeatureName = (String) tempFeature.getProperty(fieldName).getValue();
			
			Geometry geom = (Geometry) tempFeature.getDefaultGeometry();
			for (Property obsProperty : tempFeature.getProperties()){

				
				String name = obsProperty.getName().toString();
				Object value = obsProperty.getValue();
				
				resultFeatureBuilder.set(name, value);
				
			}
			
			Geometry bufferGeom = geom.buffer(dist);
			
			SimpleFeatureIterator authIt2 = (SimpleFeatureIterator) authFc.features();
		
			int count = 0;
			int within = 0;
			
			while (authIt2.hasNext() && count <= minNum){
				SimpleFeature tempAuth = authIt2.next();
				String featureN = (String) tempAuth.getProperty(fieldName).getValue();
				
				Geometry tempGeom = (Geometry) tempAuth.getDefaultGeometry();
				
				String authProperty = (String) tempAuth.getProperty(fieldName).getValue();
				
				
				if (tempGeom.within(bufferGeom) && authProperty.equalsIgnoreCase(featureN)){
					
					count++;
					
					if(count >= minNum){
						within = 1;
		
					}
					
				}
				
			}
			LOG.warn("HERE " + within);
			resultFeatureBuilder.set("DQ_SA_SimilarF", within);
			
			SimpleFeature result = resultFeatureBuilder.buildFeature(tempFeature.getID());
			result.setDefaultGeometry(geom);
			LOG.warn("HERE " + result);
			returnFeatures.add(result);
			
			if(within == 1){
				resultFeatures.add(result);
			}
			
			
			authIt2.close();
			
		}
		obsIt2.close();	
		
		ListFeatureCollection qualResult = new ListFeatureCollection(fType, resultFeatures);
		ListFeatureCollection returns = new ListFeatureCollection(fType, returnFeatures);
		
		LOG.warn("HERE 2 " + qualResult.size() + " " + returns.size());
		
		Map <String, IData> results = new HashMap<String, IData>();
		results.put("qual_result", new GTVectorDataBinding(qualResult));
		results.put("result", new GTVectorDataBinding(returns));
		
		
		
		return results;
	}
	
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}
}

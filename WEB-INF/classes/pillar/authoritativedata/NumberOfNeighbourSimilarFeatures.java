package pillar.authoritativedata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
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
		
		
		FeatureCollection obsFcW = ((GTVectorDataBinding) obsList.get(0)).getPayload();
		FeatureCollection authFcW = ((GTVectorDataBinding) authList.get(0)).getPayload();
		double dist = ((LiteralDoubleBinding)distList.get(0)).getPayload();
		int minNum = ((LiteralIntBinding)minList.get(0)).getPayload();
		String fieldName = ((LiteralStringBinding)fieldList.get(0)).getPayload();
		
		
		ArrayList<SimpleFeature> resultFeatures = new ArrayList<SimpleFeature>();
		
		CoordinateReferenceSystem sourceCRS = null;
		
		CoordinateReferenceSystem projectCRS = null;
		
		CRSAuthorityFactory   factory = CRS.getAuthorityFactory(true);
		try {
			sourceCRS = factory.createCoordinateReferenceSystem("EPSG:4326");
			projectCRS = factory.createCoordinateReferenceSystem("EPSG:27700");
		} catch (NoSuchAuthorityCodeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (FactoryException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		FeatureCollection obsFc = new ReprojectingFeatureCollection(obsFcW, projectCRS);
		FeatureCollection authFc = new ReprojectingFeatureCollection(authFcW, projectCRS);
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) obsFc.features();
		SimpleFeatureIterator authIt = (SimpleFeatureIterator) authFc.features();
		
		SimpleFeatureType fType = null;
		
		while (obsIt.hasNext()){
			SimpleFeature tempFeature = obsIt.next();
			
			fType = tempFeature.getType();
		
			String tempFeatureName = (String) tempFeature.getProperty(fieldName).getValue();
			
			
			
			Geometry geom = (Geometry) tempFeature.getDefaultGeometry();
			
			Geometry bufferGeom = geom.buffer(dist);
			
			int count = 0;
			
			while (authIt.hasNext() && count <= minNum){
				SimpleFeature tempAuth = authIt.next();
				Geometry tempGeom = (Geometry) tempAuth.getDefaultGeometry();
				
				String authProperty = (String) tempAuth.getProperty(fieldName).getValue();
				
				
				if (tempGeom.within(bufferGeom) && authProperty.equalsIgnoreCase(tempFeatureName)){
					
					count++;
					
					if(count >= minNum){
						resultFeatures.add(tempFeature);
					}
					
				}
			}
			
			
			
		}
		
		
		FeatureCollection obsFcR = new ReprojectingFeatureCollection(obsFc, sourceCRS);
		
		ListFeatureCollection qualResult = new ListFeatureCollection(fType, resultFeatures);
		
		FeatureCollection resultF = new ReprojectingFeatureCollection(qualResult, sourceCRS);
		
		Map <String, IData> results = new HashMap<String, IData>();
		results.put("qual_result", new GTVectorDataBinding(resultF));
		results.put("result", new GTVectorDataBinding(obsFcR));
		results.put("metadata", new GenericFileDataBinding(null));
		
		
		return results;
	}
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}
}

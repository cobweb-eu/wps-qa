package pillar.lbs;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlOptions;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geoviqua.gmd19157.DQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDataQualityType;
import org.geoviqua.qualityInformationModel.x40.GVQDiscoveredIssueType;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataDocument;
import org.geoviqua.qualityInformationModel.x40.GVQMetadataType;
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
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class GetSpatialAccuracy extends AbstractAlgorithm {
	Logger LOG = Logger.getLogger(GetSpatialAccuracy.class);
	private final String inputObservations = "inputObservations";
	
	private final String inputSatelliteNumberField = "inputSatelliteNumberField";
	private final String inputAccuracyField = "inputAccuracyField";
	private final String minSatNum = "minSatNum";
	private final String minAcc = "minAcc";
	
	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		
		if(identifier.equalsIgnoreCase("inputSatelliteNumberField")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("inputAccuracyField")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("minSatNum")){
			return LiteralIntBinding.class;
		}
		if(identifier.equalsIgnoreCase("minAcc")){
			return LiteralDoubleBinding.class;
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
		
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {

		List inputObsList = inputData.get("inputObservations");
		List satFieldList = inputData.get("inputSatelliteNumberField");
		List accFieldList = inputData.get("inputAccuracyField");
		List minSatList= inputData.get("minSatNum");
		List minAccList = inputData.get("minAcc");
		
		
		
		FeatureCollection inputObs = ((GTVectorDataBinding) inputObsList.get(0)).getPayload();
		String inputSatField = ((LiteralStringBinding) satFieldList.get(0)).getPayload();
		String accField = ((LiteralStringBinding) accFieldList.get(0)).getPayload();
		
		int minSatNum = ((LiteralIntBinding) minSatList.get(0)).getPayload();
		double minAcc = ((LiteralDoubleBinding) minAccList.get(0)).getPayload();
		
		SimpleFeatureIterator sfi = (SimpleFeatureIterator) inputObs.features();
		SimpleFeature tempPropFeature = sfi.next();
		CoordinateReferenceSystem inputObsCrs = inputObs.getSchema().getCoordinateReferenceSystem();
		
		Collection<Property> obsProp = tempPropFeature.getProperties();
		
		
		//SimpleFeatureType typeF = tempPropFeature.getType();
		
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
			

			LOG.warn ("Obs property " + name + " " + valueClass + " " + type.getBinding());
			}
			catch (Exception e){
				LOG.error("property error " + e);
			}
		}
		
		//add DQ_Field
		
		resultTypeBuilder.add("DQ_SpatialAccuracy", Double.class);
		resultTypeBuilder.add("DQ_AbstractAccuracy", Double.class);
		SimpleFeatureType typeF = resultTypeBuilder.buildFeatureType();
		LOG.warn("Get Spatial Accuracy Feature Type " + typeF.toString());
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) inputObs.features();
		
		SimpleFeatureBuilder resultFeatureBuilder = new SimpleFeatureBuilder(typeF);
		
		ArrayList<SimpleFeature> qualResultList = new ArrayList<SimpleFeature>();
		ArrayList<SimpleFeature> resultList = new ArrayList<SimpleFeature>();

		while (obsIt.hasNext() == true){
			
			SimpleFeature tempFeature = obsIt.next();
					
			Property accProperty = (Property) tempFeature.getProperty(inputSatField);
			Property satNumProperty = (Property) tempFeature.getProperty(accField);
			
			double numberSat = Double.parseDouble(accProperty.getValue().toString());
			double acc = Double.parseDouble(satNumProperty.getValue().toString());
			
			LOG.warn("properties " + numberSat + " " + acc);
			
			//String UUID = UUIDProperty.getValue().toString();
			
			for (Property obsProperty : tempFeature.getProperties()){

				
				String name = obsProperty.getName().toString();
				Object value = obsProperty.getValue();
				
				resultFeatureBuilder.set(name, value);
				
			}
			
			
			
			if(numberSat >= minSatNum){
				
				LOG.warn("test " + 1);
				resultFeatureBuilder.set("DQ_SpatialAccuracy", 1);
			}
			if(numberSat < minSatNum){
				resultFeatureBuilder.set("DQ_SpatialAccuracy", 0);
				LOG.warn("test " + 2);
			}
			
			if(acc >= minAcc){
				resultFeatureBuilder.set("DQ_AbstractAccuracy", 1);
				LOG.warn("test " + 3);
			}
			if(acc < minAcc){
				resultFeatureBuilder.set("DQ_AbstractAccuracy", 0);
				LOG.warn("test " + 4);
			}
			
			Geometry obsGeom = (Geometry) tempFeature.getDefaultGeometry();
			SimpleFeature resultFeature = resultFeatureBuilder.buildFeature(tempFeature.getName().toString());
			resultFeature.setDefaultGeometry(obsGeom);
			
			resultList.add(resultFeature);
			
			
			if(acc >= minAcc){
				if(numberSat >= minSatNum){
				
				qualResultList.add(resultFeature);
				}
						
			}
		
		
		}
		
		obsIt.close();
		ListFeatureCollection result = new ListFeatureCollection(typeF, resultList);
		ListFeatureCollection qualResult = new ListFeatureCollection(typeF, qualResultList);
		
		Map <String, IData> results = new HashMap<String, IData>();
		
		results.put("result", new GTVectorDataBinding(result));
		results.put("qual_result", new GTVectorDataBinding (qualResult));
		
		
		return results;
		
	}

	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
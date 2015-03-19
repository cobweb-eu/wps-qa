package pillar.automaticvalidation;

import java.io.File;
import java.io.IOException;
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
import org.n52.wps.io.data.GenericFileData;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

// same as attribute range check but with metadata

// currently copied from attribute range check
public class ValueRange extends AbstractAlgorithm{
	
	Logger LOG = Logger.getLogger(ValueRange.class);


	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("attributeName")){
			return LiteralStringBinding.class;
		}
		if(identifier.equalsIgnoreCase("maxRange")){
			return LiteralDoubleBinding.class;
		}
		if(identifier.equalsIgnoreCase("minRange")){
			return LiteralDoubleBinding.class;
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

		List<IData> obsList = inputData.get("inputObservations");
		List<IData> maxList = inputData.get("maxRange");
		List<IData> minList = inputData.get("minRange");
		
		List <IData> attributeNameList = inputData.get("attributeName");
				
		IData attributeNameIData = attributeNameList.get(0);
		
		FeatureCollection obsFc = ((GTVectorDataBinding)obsList.get(0)).getPayload();
		double maxRange = ((LiteralDoubleBinding)maxList.get(0)).getPayload();
		double minRange = ((LiteralDoubleBinding)minList.get(0)).getPayload();
		String nameString = ((LiteralStringBinding)attributeNameList.get(0)).getPayload();
		
		SimpleFeatureIterator sfi = (SimpleFeatureIterator) obsFc.features();
		SimpleFeature tempPropFeature = sfi.next();
		CoordinateReferenceSystem inputObsCrs = obsFc.getSchema().getCoordinateReferenceSystem();
		
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
		
			PropertyType type = tempProp.getType();
			String name = type.getName().getLocalPart();
			Class<?> valueClass = (Class<?>)tempProp.getType().getBinding();
			
			resultTypeBuilder.add(name, valueClass);
			

			LOG.warn ("Obs property " + " name " +  name + " class<?> " + valueClass +
					" type " + type + " tempProp.getValue() " + tempProp.getValue() );
			}
			catch (Exception e){
				LOG.error("property error " + e);
			}
		}
		
		//add DQ_Field
		
		resultTypeBuilder.add("DQ_ValueRange", Double.class);
		
		SimpleFeatureType typeF = resultTypeBuilder.buildFeatureType();
		LOG.warn("Get Spatial Accuracy Feature Type " + typeF.toString());
		
		LOG.warn("++++++++++++++ HERE +++++++++++++");
		LOG.warn("obsFc " + obsFc.size());
		LOG.warn("maxRange " + maxRange);
		LOG.warn("minRange " + minRange);
		LOG.warn("nameString " + nameString);
		SimpleFeatureBuilder resultFeatureBuilder = new SimpleFeatureBuilder(typeF);
		
				
		ArrayList<SimpleFeature> resultArrayList = new ArrayList<SimpleFeature>(); 
		ArrayList<SimpleFeature> qual_resultArrayList = new ArrayList<SimpleFeature>();
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) obsFc.features();
		
		LOG.warn("Attribute Range Feature Type " + typeF.toString() );
		obsIt.close();
		SimpleFeatureIterator obsIt2 = (SimpleFeatureIterator) obsFc.features();
		
		int within = 0;
		while (obsIt2.hasNext()==true){
			
			SimpleFeature tempSf = obsIt2.next();	
			within = 0;
			for (Property obsProperty : tempSf.getProperties()){

				
				String name = obsProperty.getName().toString();
				Object value = obsProperty.getValue();
				
				resultFeatureBuilder.set(name, value);
				
			}
			//Property tempAttribute =  tempSf.getProperty(nameString);
			
		//	LOG.warn("tempFeature " + tempSf.toString() );
			
			//LOG.warn("doubleValue " + tempAttribute.getValue().toString());
			
			double tempAttributeValue = Double.parseDouble(tempSf.getProperty(nameString).getValue().toString());
			resultFeatureBuilder.set("DQ_ValueRange", 0);
			if (tempAttributeValue>= minRange){
				
				if(tempAttributeValue<=maxRange){
					within = 1;
					resultFeatureBuilder.set("DQ_ValueRange", 1);
				}
				
		
			}
			SimpleFeature tempResult = resultFeatureBuilder.buildFeature(tempSf.getID());
			Geometry tempGeom = (Geometry) tempSf.getDefaultGeometry();
			tempResult.setDefaultGeometry(tempGeom);
			resultArrayList.add(tempResult);
			if(within == 1){
				qual_resultArrayList.add(tempResult);
			}
			
		}
		obsIt2.close();
		FeatureCollection resultFeatureCollection = new ListFeatureCollection(typeF, resultArrayList);
		FeatureCollection qual_resultFeatureCollection = new ListFeatureCollection(typeF, qual_resultArrayList);

		
		HashMap<String, IData> results = new HashMap<String, IData>();
		results.put("result", new GTVectorDataBinding((FeatureCollection)resultFeatureCollection));
		results.put("qual_result", new GTVectorDataBinding((FeatureCollection)qual_resultFeatureCollection));
		
		
		
		return results;
	}
	
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}

	

}



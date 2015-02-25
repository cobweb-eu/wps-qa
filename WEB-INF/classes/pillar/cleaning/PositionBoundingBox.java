package pillar.cleaning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralDoubleBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;


//check if within a bounding box
public class PositionBoundingBox extends AbstractAlgorithm {
	
	private final String inputObservations = "inputObservations";
	private final String minX = "minX";
	private final String minY = "minY";
	private final String maxX = "maxX";
	private final String maxY = "maxY";
	
	
	@Override
	public Class<?> getInputDataType(String identifier) {
		if(identifier.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if(identifier.equalsIgnoreCase("minX")){
			return LiteralDoubleBinding.class;
		}
		if(identifier.equalsIgnoreCase("minY")){
			return LiteralDoubleBinding.class;
		}
		if(identifier.equalsIgnoreCase("maxX")){
			return LiteralDoubleBinding.class;
		}
		if(identifier.equalsIgnoreCase("maxY")){
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
		if(identifier.equalsIgnoreCase("metadata")){
			return GenericFileDataBinding.class;
		}
		return null;
	}

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {
		List obsList = inputData.get("inputObservations");
		List minXList = inputData.get("minX");
		List minYList = inputData.get("minY");
		List maxXList = inputData.get("maxX");
		List maxYList = inputData.get("maxY");
		
		FeatureCollection obsFc = ((GTVectorDataBinding) obsList.get(0)).getPayload();
		
		double minXD = ((LiteralDoubleBinding) minXList.get(0)).getPayload();
		double minYD = ((LiteralDoubleBinding) minYList.get(0)).getPayload();
		double maxXD = ((LiteralDoubleBinding) maxXList.get(0)).getPayload();
		double maxYD = ((LiteralDoubleBinding) maxYList.get(0)).getPayload();
		
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
		
		ReferencedEnvelope bbox = new ReferencedEnvelope(minXD, minYD, maxXD, maxYD, sourceCRS);
		
		
		ArrayList <SimpleFeature> resultList = new ArrayList<SimpleFeature>();
		
		SimpleFeatureIterator obsIt = (SimpleFeatureIterator) obsFc.features();
		
		SimpleFeatureType typeF = null;
		
		while (obsIt.hasNext()){
			
			SimpleFeature tempFeature = obsIt.next();
			
			Geometry geom = (Geometry) tempFeature.getDefaultGeometry();
			
			if (bbox.contains(geom.getCoordinate())){
				resultList.add(tempFeature);
				
			}
			
		}
		 
		Map<String, IData> results = new HashMap<String, IData>();
		
		ListFeatureCollection listResult = new ListFeatureCollection(typeF, resultList);
		
		results.put("result", new GTVectorDataBinding(obsFc));
		results.put("qual_result", new GTVectorDataBinding(listResult));
		results.put("metadata", new GenericFileDataBinding(null));
				 
		
		
		
		return results;
	}
	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}

}

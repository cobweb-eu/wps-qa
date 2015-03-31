package storage.geoserver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opengis.feature.type.PropertyType;
import org.apache.log4j.Logger;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.GeometryAttributeImpl;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.identity.GmlObjectIdImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.n52.wps.io.data.GenericFileDataWithGT;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataWithGTBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.data.binding.literal.LiteralStringBinding;
import org.n52.wps.server.AbstractAlgorithm;
import org.n52.wps.server.ExceptionReport;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.filter.identity.Identifier;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

public class StoreWFSNamedOutput extends AbstractAlgorithm{
	private static String inputObservations;
	private static String fileName;
	private static String result;
	Logger LOG = Logger.getLogger(StoreWFSNamedOutput.class);

	@Override
	public Map<String, IData> run(Map<String, List<IData>> inputData)
			throws ExceptionReport {

		List inputList = inputData.get("inputObservations");
		List inputName = inputData.get("fileName");
		
		String fileName = ((LiteralStringBinding)inputName.get(0)).getPayload();
		FeatureCollection inputObs = ((GTVectorDataBinding) inputList.get(0)).getPayload();
		
		LOG.warn(inputObs.size() + " " + fileName);
		HashMap<String, IData> result = new HashMap<String, IData>(); 

		try {
			result.put("result", new GenericFileDataWithGTBinding(new GenericFileDataWithGT(getShpFile(inputObs, fileName + ".shp"), "application/x-zipped-shp")));
		} catch (IllegalAttributeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		LOG.warn("output " + result.size());
		
		return result; 
		
		
	}

	@Override
	public List<String> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<?> getInputDataType(String id) {
		if (id.equalsIgnoreCase("inputObservations")){
			return GTVectorDataBinding.class;
		}
		if(id.equalsIgnoreCase("fileName")){
			return LiteralStringBinding.class;
		}
		return null;
	}

	@Override
	public Class<?> getOutputDataType(String id) {
		if (id.equalsIgnoreCase("result")){
			return GenericFileDataWithGTBinding.class;
		}
		return null;
	}
	
	private static File getShpFile(FeatureCollection<?, ?> collection, String filename) 
            throws IOException, IllegalAttributeException {
		
		
    SimpleFeatureType type = null; 
    SimpleFeatureBuilder build = null; 
    FeatureIterator<?> iterator = collection.features(); 
    List<SimpleFeature> featureList = new ArrayList<SimpleFeature>(); 
    ListFeatureCollection modifiedFeatureCollection = null; 
    Transaction transaction = new DefaultTransaction("create"); 
    FeatureStore<SimpleFeatureType, SimpleFeature> store = null; 
    File shp = new File(System.getProperty("java.io.tmpdir") + File.separator + filename); 
    while (iterator.hasNext()) { 
            SimpleFeature sf = (SimpleFeature) iterator.next(); 
            // create SimpleFeatureType 
            if (type == null) { 
                    SimpleFeatureType inType = (SimpleFeatureType) collection 
                                    .getSchema(); 
                    SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder(); 
                    builder.setName(inType.getName()); 
                    builder.setNamespaceURI(inType.getName().getNamespaceURI()); 

                    if (collection.getSchema().getCoordinateReferenceSystem() == null) { 
                            builder.setCRS(DefaultGeographicCRS.WGS84); 
                    } else { 
                            builder.setCRS(collection.getSchema() 
                                            .getCoordinateReferenceSystem()); 
                    } 

                    builder.setDefaultGeometry(sf.getDefaultGeometryProperty() 
                                    .getName().getLocalPart()); 

                    /* 
                     * seems like the geometries must always be the first property.. 
                     * @see also ShapeFileDataStore.java getSchema() method 
                     */ 
                    Property geomProperty = sf.getDefaultGeometryProperty(); 

                    if(geomProperty.getType().getBinding().getSimpleName().equals("Geometry")){ 
                    Geometry g = (Geometry)geomProperty.getValue(); 
                    if(g!=null){ 
                            GeometryAttribute geo = null; 
                            if(g instanceof MultiPolygon){ 

                            GeometryAttribute oldGeometryDescriptor = sf.getDefaultGeometryProperty(); 
                            GeometryType type1 = new GeometryTypeImpl(geomProperty.getName(),MultiPolygon.class, oldGeometryDescriptor.getType().getCoordinateReferenceSystem(),oldGeometryDescriptor.getType().isIdentified(),oldGeometryDescriptor.getType().isAbstract(),oldGeometryDescriptor.getType().getRestrictions(),oldGeometryDescriptor.getType().getSuper(),oldGeometryDescriptor.getType().getDescription());

                            GeometryDescriptor newGeometryDescriptor = new GeometryDescriptorImpl(type1,geomProperty.getName(),0,1,true,null); 
                            Identifier identifier = new GmlObjectIdImpl(sf.getID()); 
                            geo = new GeometryAttributeImpl((Object)g,newGeometryDescriptor, identifier); 
                            sf.setDefaultGeometryProperty(geo); 
                            sf.setDefaultGeometry(g); 
                            } 
                            if(geo != null){ 
                            builder.add(geo.getName().getLocalPart(), geo 
                                            .getType().getBinding()); 
                            } 
                    } 
                    }else if (isSupportedShapefileType(geomProperty.getType()) 
                                    && (geomProperty.getValue() != null)) { 
                            builder.add(geomProperty.getName().getLocalPart(), geomProperty 
                                            .getType().getBinding()); 
                    } 

                    for (Property prop : sf.getProperties()) { 

                            if (prop.getType() instanceof GeometryType) { 
                                    /* 
                                     * skip, was handled before 
                                     */ 
                            }else if (isSupportedShapefileType(prop.getType()) 
                                            && (prop.getValue() != null)) { 
                                    builder.add(prop.getName().getLocalPart(), prop 
                                                    .getType().getBinding()); 
                            } 
                    } 

                    type = builder.buildFeatureType(); 

                    ShapefileDataStore dataStore = new ShapefileDataStore(shp 
                                    .toURI().toURL()); 
                    dataStore.createSchema(type); 
                    dataStore.forceSchemaCRS(type.getCoordinateReferenceSystem()); 

                    String typeName = dataStore.getTypeNames()[0]; 
                    store = (FeatureStore<SimpleFeatureType, SimpleFeature>) dataStore 
                                    .getFeatureSource(typeName); 

                    store.setTransaction(transaction); 

                    build = new SimpleFeatureBuilder(type); 
                    modifiedFeatureCollection = new ListFeatureCollection(type); 
            } 
            for (AttributeType attributeType : type.getTypes()) { 
                    build.add(sf.getProperty(attributeType.getName()).getValue()); 
            } 

            SimpleFeature newSf = build.buildFeature(sf.getIdentifier() 
                            .getID()); 

            featureList.add(newSf); 
    } 

    try { 
            modifiedFeatureCollection.addAll(featureList); 
            store.addFeatures(modifiedFeatureCollection); 
            transaction.commit(); 
            return shp; 
    } catch (Exception e1) { 
            e1.printStackTrace(); 
            transaction.rollback(); 
            throw new IOException(e1.getMessage()); 
    } finally { 
            transaction.close(); 
    } 
} 

private static boolean isSupportedShapefileType(PropertyType type) { 
    String supported[] = { "String", "Integer", "Double", "Boolean", 
                    "Date", "LineString", "MultiLineString", "Polygon", 
                    "MultiPolygon", "Point", "MultiPoint", "Long"}; 
    for (String iter : supported) { 
            if (type.getBinding().getSimpleName().equalsIgnoreCase(iter)) { 
                    return true; 
            } 
    } 
    return false; 
}

}

/**
 * 
 */
package br.com.geocab.domain.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.io.FileTransfer;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.files.ShpFileType;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.util.Assert;

import br.com.geocab.application.aspect.GeodesicCoordinatesAcceptedException;
import br.com.geocab.domain.entity.layer.Attribute;
import br.com.geocab.domain.entity.layer.AttributeType;
import br.com.geocab.domain.entity.layer.Layer;
import br.com.geocab.domain.entity.marker.Marker;
import br.com.geocab.domain.entity.marker.MarkerAttribute;
import br.com.geocab.domain.entity.shapefile.Shapefile;
import br.com.geocab.domain.repository.attribute.IAttributeRepository;
import br.com.geocab.domain.repository.marker.IMarkerRepository;

/**
 * @author emanuelvictor
 *
 */
@Component
//@Service //n�o l� os properties
@RemoteProxy(name = "shapefileService")
@SuppressWarnings("unchecked")
public class ShapefileService
{
	/*-------------------------------------------------------------------
	 * 		 					ATTRIBUTES
	 *-------------------------------------------------------------------*/
	
	/**
	 * export shapefile path
	 */
	@Value("${path.shapefiles:/tmp/geocab/files/shapefile/}" + "export/")
	private String pathShapefilesExport;
	/**
	 * import shapefile path
	 */
	@Value("${path.shapefiles:/tmp/geocab/files/shapefile/}" + "import/")
	private String pathShapefilesImport;
	/**
	 * Log
	 */
	private static final Logger LOG = Logger.getLogger(DataSourceService.class.getName());
	/**
	 * 
	 */
	@Autowired
	private IMarkerRepository markerRepository;
	/**
	 * 
	 */
	@Autowired
	private IAttributeRepository attributeRepository;
	 /**
	 * I18n
	 */
	 @Autowired
	 private MessageSource messages;
	/*-------------------------------------------------------------------
	 *				 		    CONSTRUCTORS
	 *-------------------------------------------------------------------*/
	
	/*-------------------------------------------------------------------
	 *				 		    BEHAVIORS
	 *-------------------------------------------------------------------*/
	 	
	
	/**
	 * Logo ap�s o component ser instanciado, 
	 * o mesmo verifica se existe a pasta shapefile e caso a mesma n�o exista ser� criada
	 */
	@PostConstruct
	public void createPaths()
	{
		try
		{
			new File(pathShapefilesExport).mkdirs();
			new File(pathShapefilesImport).mkdirs();
		}
		catch (final RuntimeException e)
		{
			e.printStackTrace();
			LOG.info(e.getMessage());
		}
	}
	
	/**
	 * TODO alterar shapefile para shapefile
	 * 
	 * Servi�o de importa��o de shapefile
	 * 
	 * @param shapefile
	 * @return
	 */
	public final List<Marker> importShapefile(final List<Shapefile> shapefiles)
	{
		try
		{
			final String pathFile = pathShapefilesImport + String.valueOf("geocab_" + Calendar.getInstance().getTimeInMillis());
			// L� os arquivos
			final List<File> files = new ArrayList<File>();
			
			for (final Shapefile shapefile : shapefiles)
			{
				files.add(readFile(shapefile, pathFile));
			}
			
			List<Marker> markers = new ArrayList<>();
			
			for (final Shapefile shapefile : shapefiles)
			{
				if (shapefile.getType() == ShpFileType.SHP)
				{
					markers = importt(readFile(shapefile, pathFile), this.messages);
				}
			}
			
			// Deleta o arquivo
			delete(new File(pathShapefilesImport));
			
			return markers;
		}
		catch (final GeodesicCoordinatesAcceptedException e)
		{
			e.printStackTrace();
			LOG.info(e.getMessage());
			throw new RuntimeException(messages.getMessage("map.Tips-coordinate", null, null));
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			LOG.info(e.getMessage());
			throw new RuntimeException(messages.getMessage("admin.shape.error.import", null, null));
		}
	}
	
	/**
	 * 
	 * @param wktPoint
	 * @return
	 */
	private static Geometry wktToGeometry(String wktPoint)
	{
		WKTReader fromText = new WKTReader();
		Geometry geom = null;
		try
		{
			geom = fromText.read(wktPoint);
		}
		catch (com.vividsolutions.jts.io.ParseException e)
		{
			e.printStackTrace();
			throw new RuntimeException("Not a WKT string:" + wktPoint);
		}
		return geom;
	}
	
	/**
	 * Importa a lista de postagem dos shapefiles j� gravados no sistema de arquivos
	 * @param file
	 * @param messages2 
	 * @return
	 * @throws GeodesicCoordinatesAcceptedException 
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws TransformException 
	 * @throws MismatchedDimensionException 
	 */
	private static final List<Marker> importt(final File file, final MessageSource messages ) throws GeodesicCoordinatesAcceptedException 
	{
		try
		{
			final Map<String, Object> map = new HashMap<String, Object>();
		    map.put("url", file.toURI().toURL());

		    final DataStore dataStore = DataStoreFinder.getDataStore(map);
		    
		    final String typeName = dataStore.getTypeNames()[0];

		    final FeatureSource<SimpleFeatureType, SimpleFeature> source = dataStore.getFeatureSource(typeName);
		    
		    final FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
		    
		    final List<Marker> markers = new ArrayList<>();
		    
		    final FeatureIterator<SimpleFeature> features = collection.features(); 
		    
	    	while (features.hasNext()) 
	        {
	    		final SimpleFeature feature = features.next();

//	    		double coordinates[] = MercatorTransform.forward(getX(feature.getDefaultGeometryProperty().getValue().toString()), getY(feature.getDefaultGeometryProperty().getValue().toString()));
	    		
//    			final Coordinate coordinate = new Coordinate(-getX(feature.getDefaultGeometryProperty().getValue().toString()), -getY(feature.getDefaultGeometryProperty().getValue().toString()));
//    			JTS.transform(source, dest, transform) 
	    		
	    		final CoordinateReferenceSystem entryCRS = source.getSchema().getCoordinateReferenceSystem(); // CRS.decode(source.getSchema().getCoordinateReferenceSystem().getCoordinateSystem());

	    		if (!entryCRS.getCoordinateSystem().getName().getCode().contains("GCS_WGS_1984") && !entryCRS.getCoordinateSystem().getName().getCode().contains("SIRGAS_2000"))
				{
	    			throw new GeodesicCoordinatesAcceptedException();
				}
	    		
	    		final CoordinateReferenceSystem exitCRS = CRS.decode("EPSG:3857");
    			final MathTransform transform = CRS.findMathTransform(entryCRS, exitCRS, true);
	    		
    			final Geometry targetGeometry = JTS.transform( wktToGeometry(  feature.getDefaultGeometryProperty().getValue().toString()).getEnvelope(), transform);
	    		
    			final Marker marker = new Marker(new Point(/*coordinate*/));
//    			CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326", true);
//    			Geometry targetGeometry = JTS.toGeographic(wktToGeometry(feature.getDefaultGeometryProperty().getValue().toString()), entryCRS /*DefaultGeographicCRS.WGS84*/);
    			marker.setLocation((Point) targetGeometry);
    			
	            final List<MarkerAttribute> markersAttributes = new ArrayList<>();
				for (final Property property : feature.getProperties())
				{
					// Deve ignorar a propriedade "the_geom"
					if (property.getDescriptor().getName().toString() != "the_geom")
					{
						try
						{
							final Attribute attribute = new Attribute(null, property.getDescriptor().getName().toString(), property.getDescriptor().getType().getBinding().toString(), null);
							
							//Extrai os atributos da feature
							final MarkerAttribute markerAttribute = extractAttributes(feature, attribute, property, marker);
							
							//Valida o atributo
//							validateMarkerAttribute(markerAttribute);
							markersAttributes.add(markerAttribute);
							
							marker.setMarkerAttribute(markersAttributes);
							
						}
						//Ignora atributos inv�lidos, separar em uma fun��o
						catch (Exception e)
						{
							e.printStackTrace();
							continue; 
						}
					}
				}
				markers.add(marker);
	        }
		    	
		    return markers;
		}
		catch ( final IOException | FactoryException | MismatchedDimensionException | TransformException e )
		{	
			e.printStackTrace();
			LOG.info(e.getMessage());
			throw new RuntimeException("admin.shape.error.import");
		}
	}
	
	/**
	 * Insere no sistema de arquivos o shapefile, provisoriamente
	 * 
	 * @param shapefile
	 * @return
	 */
	private static final File readFile(final Shapefile shapefile, String pathFile)
	{
		pathFile += "." + shapefile.getType().toString().toLowerCase();//".shp";

		final Base64 decoder = new Base64();
		final byte[] shpBytes = decoder.decode(shapefile.getSource());
		try
		{
			final FileOutputStream osf = new FileOutputStream(new File(pathFile));
			osf.write(shpBytes);
			osf.flush();
			osf.close();
			return new File(pathFile);
		}
		catch (final IOException e)
		{
			LOG.info(e.getMessage());
			throw new RuntimeException("admin.shape.error.import");
		}
	}
	
	/**
	 * Agrupa as postagens pelas camadas
	 * @param markers
	 * @return
	 */
	private static final List<Layer> groupByLayers(final List<Marker> markers)
	{		
		final Set<Layer> layers = new HashSet<>();
		
		for (final Marker marker : markers)
		{
			layers.add(marker.getLayer());
		}
		
		for (final Layer layer : layers)
		{	
			// Inicializa os markers da layer
			layer.setMarkers(new ArrayList<Marker>());
			for (final Marker marker : markers)
			{	
				if (marker.getLayer().getId().equals(layer.getId()))
				{
					layer.getMarkers().add(marker);
				}
			}
		}
		return new ArrayList<Layer>(layers);
	}	
	
	/**
	 * Servi�o de exporta��o para shapefile (Obs:. FileTransfer do DWR n�o pode ser final)
	 * 
	 * @param markers
	 * @return
	 */
	public FileTransfer exportShapefile(final List<Marker> markers)
	{
		
		final List<Layer> layers = groupByLayers(markers);
		
		final String fileExport = String.valueOf("geocab_" + Calendar.getInstance().getTimeInMillis() );
		 
		for (final Layer layer : layers)
		{
			try
			{				
				layer.setAttributes(this.attributeRepository.listAttributeByLayer(layer.getId()));
				
				layer.setName(layer.getName().replaceAll(" ", "_"));
				
				SimpleFeatureType TYPE = null; 
	
				final DefaultFeatureCollection collection = new DefaultFeatureCollection();
				
				for (int i = 0; i < layer.getMarkers().size(); i++)
	            {	
					final Marker marker = markerRepository.findOne(layer.getMarkers().get(i).getId());
	            	
					marker.formattedNameAttributes();
					
	            	if ((i != 0 && marker.getLayer().getId() != layer.getMarkers().get(i - 1).getId()) || TYPE == null)
	            		TYPE = DataUtilities.createType(layer.getName(), "the_geom:Point,"+marker.formattedAttributes());
					
	            	final GeometryFactory factory = JTSFactoryFinder.getGeometryFactory(null);
	            	
	            	final double longitude = marker.getLocation().getX();
	                final double latitude = marker.getLocation().getY();
	
	                final Point point = factory.createPoint(new Coordinate(longitude, latitude));
	                
	                // O ponto tamb�m � um atributo "new Object[]{point}"
	                SimpleFeature feature = SimpleFeatureBuilder.build(TYPE, new Object[]{point}, null);
	                
	                // Extrai os atributos da feature 
	                feature = extractFeatures(feature, marker);
	                
	                collection.add(feature);	
	            }
	            
		        final DataStoreFactorySpi dataStoreFactorySpi = new ShapefileDataStoreFactory();
	
		        final File newFile = new File(pathShapefilesExport + layer.getName() + ".shp");
		        final Map<String, Serializable> create = new HashMap<String, Serializable>();
		        create.put("url", newFile.toURI().toURL());
		        create.put("create spatial index", Boolean.TRUE);
	
		        final ShapefileDataStore newDataStore = (ShapefileDataStore) dataStoreFactorySpi.createNewDataStore(create);
		        newDataStore.createSchema(TYPE);
		        
		        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857");
		        newDataStore.forceSchemaCRS( crs );
		        
		        final Transaction transaction = new DefaultTransaction("create");
		        final String typeName = newDataStore.getTypeNames()[0];
		        
				final FeatureStore<SimpleFeatureType, SimpleFeature> featureStore = (FeatureStore<SimpleFeatureType, SimpleFeature>) newDataStore.getFeatureSource(typeName);
	
		        featureStore.setTransaction(transaction);
		        try 
		        {
		            featureStore.addFeatures(collection);
		            transaction.commit();
		        } 
		        catch (final Exception e) 
		        {
		            e.printStackTrace();
		            transaction.rollback();
		        } 
		        finally 
		        {
		            transaction.close();
		        }
			}
			catch (final Exception e)
			{
				e.printStackTrace();
				LOG.info(e.getMessage());
				throw new RuntimeException("admin.shape.error.export");
			}
	    }
		
		//Compcta os arquivos de exporta��o e tr�s para mem�ria
		final FileTransfer fileTransfer = new FileTransfer(fileExport + ".zip", "application/zip", compactFilesToZip(pathShapefilesExport, fileExport + ".zip"));

		//Deleta os arquivos de exporta��o
		delete(new File(pathShapefilesExport));

		return fileTransfer;
	}
	
	/**
	 * 
	 * @param feature
	 * @param marker
	 * @return
	 */
	private static final SimpleFeature extractFeatures(SimpleFeature feature, final Marker marker)
	{
		for (final MarkerAttribute markerAttribute : marker.getMarkerAttribute())
		{
			try
			{
				validateMarkerAttribute(markerAttribute);
				
				feature = extractFeatures(feature, markerAttribute);
			}
			catch (RuntimeException e)
			{
				// Se o attributo n�o � inv�lido parte para o pr�ximo
				LOG.info(e.getMessage());
				continue;
			}
		}
		return feature;
	}
	
	
	/**
	 * Encapsula comportamento de extra��o dos atributos da feature
	 * @param feature
	 * @param attribute
	 * @param property
	 * @param marker
	 * @return
	 */
	private static final MarkerAttribute extractAttributes(final SimpleFeature feature, final Attribute attribute, final Property property, final Marker marker)
	{		
		final String propertyDescriptor = property.getDescriptor().getName().getLocalPart();
		final Object featureAttribute = feature.getAttribute(propertyDescriptor);
		if (featureAttribute == null)
		{
			return new MarkerAttribute(null, "", marker, attribute);
		}
		// Se a feature for no formato tipo data, deve formatar a mesma
		if (attribute.getType() == AttributeType.DATE)
		{
			Date date = (Date) featureAttribute;
			return new MarkerAttribute(null, new SimpleDateFormat("dd/MM/yyyy").format(date), marker, attribute); 
		}
		else if (attribute.getType() == AttributeType.BOOLEAN)
		{
			if ((boolean) featureAttribute)
			{
				return new MarkerAttribute(null, "Yes", marker, attribute);
			}
			else
			{
				return new MarkerAttribute(null, "No", marker, attribute);
			}
		}
		else
		{
			return new MarkerAttribute(null, featureAttribute.toString(), marker, attribute);
		}
	}
	
	
	
	/**
	 * Valida o markerAttribute (sobrecarga de m�todo)
	 * @param feature
	 * @param markerAttribute
	 * @return
	 */
	private static final SimpleFeature extractFeatures(final SimpleFeature feature, final MarkerAttribute markerAttribute)
	{		
		final String attribute = markerAttribute.getAttribute().getName().replaceAll(" ", "_");
		
		if (markerAttribute.getValue().toLowerCase().trim().equals("yes") || markerAttribute.getValue().toLowerCase().trim().equals("sim") && markerAttribute.getAttribute().getType() == AttributeType.BOOLEAN)
		{
			feature.setAttribute(attribute, new Boolean(true));
		}
		else if (markerAttribute.getValue().toLowerCase().trim().equals("no") || markerAttribute.getValue().toLowerCase().trim().equals("nao") && markerAttribute.getAttribute().getType() == AttributeType.BOOLEAN)
		{
			feature.setAttribute(attribute, new Boolean(false));
		}
		else if (markerAttribute.getAttribute().getType() == AttributeType.DATE)
		{
			try
			{
				Date date = null;
				if (markerAttribute.getValue() != null && !markerAttribute.getValue().equals(""))
				{
					date = new SimpleDateFormat("dd/MM/yyyy").parse(markerAttribute.getValue());
				}
				feature.setAttribute(attribute, date);
			}
			catch (ParseException e)
			{
				e.printStackTrace();
				LOG.info(e.getMessage());
				throw new RuntimeException(e);
			}
		}
		else
		{
			feature.setAttribute(attribute, markerAttribute.getValue());
		}
		
		return feature;
	}
	
	/**
	 * Valida o attributo para exporta��o
	 * @param markerAttribute
	 */
	private static final void validateMarkerAttribute(final MarkerAttribute markerAttribute)
	{
		Assert.isTrue(markerAttribute.getAttribute().getType()  != null, "admin.shape.error.type-attribute-can-not-be-null");
		Assert.isTrue(markerAttribute.getAttribute().getType() != AttributeType.PHOTO_ALBUM, "admin.shape.error.type-attribute-can-not-be-a-photoalbum");
		Assert.isTrue(markerAttribute.getAttribute().getName() != null, "admin.shape.error.name-attribute-can-not-be-null");
		Assert.isTrue(markerAttribute.getValue() != null , "admin.shape.error.value-attribute-can-not-be-null");
	}
	
	/**
	 * Compacta os arquivos .shp, .dbf e shx para o formato zip e devolve um fileTransfer
	 * @param pathExport
	 * @return
	 */
	public static final FileInputStream compactFilesToZip(final String pathExport, final String fileExport)
	{
		try
		{
			final File file = new File(pathExport);

			byte[] buffer = new byte[10240];
			
			final FileOutputStream fileOutputStream = new FileOutputStream(pathExport + fileExport);

			final ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

			final List<ZipEntry> entries = new ArrayList<ZipEntry>();
			
			final List<FileInputStream> filesInputStream = new ArrayList<FileInputStream>();
			
			for (int i = 0; i < file.list().length; i++)
			{
				if (!file.list()[i].contains(".zip"))
				{
					entries.add(new ZipEntry(file.list()[i]));
					filesInputStream.add(new FileInputStream(pathExport +file.list()[i]));
				}
			}

			for (int i = 0; i < entries.size(); i++)
			{
				zipOutputStream.putNextEntry(entries.get(i));

				int len;
				while ((len = filesInputStream.get(i).read(buffer)) > 0)
				{
					zipOutputStream.write(buffer, 0, len);
				}

				filesInputStream.get(i).close();
			}

			zipOutputStream.closeEntry();

			zipOutputStream.close();
			
			return new FileInputStream(pathExport + fileExport);

		}
		catch (final IOException e)
		{
			LOG.info(e.getMessage());
			throw new RuntimeException("admin.shape.error.export");
		}
	}
	
	/**
	 * Deleta uma pasta com todos os arquivos dentro
	 * @param file
	 */
	public static final void delete(final File file)
	{
		if (file.isDirectory())
		{
			for (final String temp : file.list())
			{
				delete(new File(file, temp));
			}
		}
		else
		{
			file.delete();
		}
	}
	
//	/**
//	 * Pega a string com as coordenadas e retorna a coordenada 'X'
//	 * @param coordinateString
//	 * @return
//	 */
//	private static final double getX(String coordinateString)
//	{
//		coordinateString = coordinateString.replace("POINT (", "");
//		coordinateString = coordinateString.replace(")", "");
//		coordinateString = coordinateString.substring(0, coordinateString.indexOf(" "));
//		double doubleCoordinate = Double.parseDouble(coordinateString);
//		return doubleCoordinate;
//	}
//	
//	/**
//	 * Pega a string com as coordenadas e retorna a coordenada 'Y'
//	 * @param coordinateString
//	 * @return
//	 */
//	private static final double getY(String coordinateString)
//	{
//		coordinateString = coordinateString.replace("POINT (", "");
//		coordinateString = coordinateString.replace(")", "");
//		coordinateString = coordinateString.substring(coordinateString.indexOf(" "), coordinateString.length());
//		double doubleCoordinate = Double.parseDouble(coordinateString);
//		return doubleCoordinate;
//	}
	
	
	
}
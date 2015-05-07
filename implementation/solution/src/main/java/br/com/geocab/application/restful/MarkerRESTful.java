package br.com.geocab.application.restful;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.RepositoryException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.vividsolutions.jts.io.WKTWriter;

import br.com.geocab.domain.entity.layer.Layer;
import br.com.geocab.domain.entity.marker.Marker;
import br.com.geocab.domain.entity.marker.MarkerAttribute;
import br.com.geocab.domain.service.MarkerService;


/**
 * @author Vinicius Ramos Kawamoto
 * @since 15/10/2014
 * @version 1.0
 * @category Controller
 */

@Controller
@RequestMapping("marker")
public class MarkerRESTful
{
	
	/*-------------------------------------------------------------------
	 * 		 					ATTRIBUTES
	 *-------------------------------------------------------------------*/
	/**
	 * 
	 */
	@Autowired
	private MarkerService markerService;
	
	/*-------------------------------------------------------------------
	 * 		 					BEHAVIORS
	 *-------------------------------------------------------------------*/

	/**
	 * 
	 * @param layerId
	 * @return
	 */
	@RequestMapping(value="/{layerId}/markers", method = RequestMethod.GET)
	public @ResponseBody List<Marker> listMarkerByLayer(@PathVariable long layerId)
	{
		List<Marker> markers = this.markerService.listMarkerByLayer(layerId);
		final WKTWriter writer = new WKTWriter();
		
		// Pega a localização do objeto e converte para o formato WKT
		for (Marker marker : markers)
		{
		    marker.setWktCoordenate(writer.write(marker.getLocation()));
		}
		
		return markers;
	}
	
	/**
	 * 
	 * @param layerId
	 * @return
	 */
	@RequestMapping(value="/{markerId}/markerattributes", method = RequestMethod.GET)
	public @ResponseBody List<MarkerAttribute> listAttributeByMarker(@PathVariable long markerId)
	{
		return this.markerService.listAttributeByMarker(markerId);
	}
	
	/**
	 * 
	 * @param marker
	 * @return
	 * @throws RepositoryException 
	 * @throws IOException 
	 */
	@RequestMapping(value="/", method = RequestMethod.POST)
	public @ResponseBody Marker insertMarker(@RequestBody Marker marker) throws IOException, RepositoryException
	{
		return this.markerService.insertMarker(marker);
	}
	
	/**
	 * 
	 * @param markerId
	 * @return
	 * @throws RepositoryException 
	 * @throws IOException 
	 */
	@RequestMapping(value="/{markerId}", method = RequestMethod.DELETE)
	public @ResponseBody void removeMarker(@PathVariable long markerId)
	{
		this.markerService.removeMarker(markerId);
	}	
	
	/**
	 * 
	 * @param marker
	 * @return
	 * @throws RepositoryException 
	 * @throws IOException 
	 */
	@RequestMapping(value="/{marker}", method = RequestMethod.PUT)
	public @ResponseBody Marker updateMarker(@PathVariable Marker marker) throws IOException, RepositoryException
	{
		return this.markerService.updateMarker(marker);
	}	
	
}
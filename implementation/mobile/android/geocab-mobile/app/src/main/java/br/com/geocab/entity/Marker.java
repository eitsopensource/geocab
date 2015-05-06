/**
 * 
 */
package br.com.geocab.entity;

import android.graphics.Bitmap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 
 *  {@link br.com.geocab.entity.Marker}
 * 
 * @author Thiago Rossetto Afonso
 * @since 03/10/2014
 * @version 1.0
 * @category Entity
 */
public class Marker implements Serializable
{


    /**
     * Id {@link Marker}
     */
    private Long id;

	private String wktCoordenate;

    private Bitmap image;
	
	private MarkerStatus status;

    private User user;

    private Layer layer;

    private List<MarkerAttribute> markerAttributes = new ArrayList<MarkerAttribute>();

    private List<MarkerModeration> markerModeration = new ArrayList<MarkerModeration>();

    private Calendar created;

    private String markerCreatedFormated;

	/*-------------------------------------------------------------------
	 * 		 					CONSTRUCTORS
	 *-------------------------------------------------------------------*/
	/**
	 * 
	 */
    public Marker()
    {
    }


	public Marker(Long id, String markerCreatedFormated, User user)
	{
        this.id = id;
        this.markerCreatedFormated = markerCreatedFormated;
		this.user = user;
	}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWktCoordenate() {
        return wktCoordenate;
    }

    public void setWktCoordenate(String wktCoordenate) {
        this.wktCoordenate = wktCoordenate;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public MarkerStatus getStatus() {
        return status;
    }

    public void setStatus(MarkerStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Layer getLayer() {
        return layer;
    }

    public void setLayer(Layer layer) {
        this.layer = layer;
    }

    public List<MarkerAttribute> getMarkerAttributes() {
        return markerAttributes;
    }

    public void setMarkerAttributes(List<MarkerAttribute> markerAttributes) {
        this.markerAttributes = markerAttributes;
    }

    public Calendar getCreated() {
        return created;
    }

    public void setCreated(Calendar created) {
        this.created = created;
    }

    public String getMarkerCreatedFormated() {
        return markerCreatedFormated;
    }

    public void setMarkerCreatedFormated(String markerCreatedFormated) {
        this.markerCreatedFormated = markerCreatedFormated;
    }
}

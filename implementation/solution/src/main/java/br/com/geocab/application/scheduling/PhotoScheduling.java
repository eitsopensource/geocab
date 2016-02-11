/**
 * 
 */
package br.com.geocab.application.scheduling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.measure.unit.SystemOfUnits;
import javax.transaction.Transactional;

import org.directwebremoting.io.FileTransfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import br.com.geocab.domain.entity.MetaFile;
import br.com.geocab.domain.entity.layer.AttributeType;
import br.com.geocab.domain.entity.marker.Marker;
import br.com.geocab.domain.entity.marker.MarkerAttribute;
import br.com.geocab.domain.entity.marker.photo.Photo;
import br.com.geocab.domain.entity.marker.photo.PhotoAlbum;
import br.com.geocab.domain.repository.marker.IMarkerAttributeRepository;
import br.com.geocab.domain.repository.marker.IMarkerRepository;
import br.com.geocab.domain.repository.marker.photo.IPhotoAlbumRepository;
import br.com.geocab.domain.repository.marker.photo.IPhotoRepository;
import br.com.geocab.domain.service.DataSourceService;
import br.com.geocab.infrastructure.jcr.MetaFileRepository;

/**
 * @author emanuelvictor
 *
 */
@Component
@Scope(proxyMode=ScopedProxyMode.TARGET_CLASS)
@Transactional
public class PhotoScheduling
{
	
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
	private IMarkerAttributeRepository markerAttributeRepository;
		
	/**
	 * 
	 */
	@Autowired
	private IPhotoAlbumRepository photoAlbumRepository;
	
	/**
	 * 
	 */
	@Autowired
	private MetaFileRepository metaFileRepository;
	
	/**
	 * 
	 */
	@Autowired
	private IPhotoRepository photoRepository;
	
	/**
	 * 
	 */
	@PostConstruct
	public void postConstruct()
	{
		for (Marker marker : markerRepository.listAll())
		{
			System.out.println("HEEEEEEEEEEEEEEEEEEEEEREEEEEEEEEEEEE");
			try
			{
				// Verifica se o marker tem fotos relacionadas diretamente a ele
				final FileTransfer fileTransfer = this.verifyMarker(marker);
											
				marker.setMarkerAttribute(markerAttributeRepository.listAttributeByMarker(marker.getId()));
				
				// Cria o novo marker_attribute (que ser� o photo_album) a ser inserido
				MarkerAttribute markerAttribute = new MarkerAttribute();
				markerAttribute.setMarker(marker);
				markerAttribute.setValue("Default photo album");
				
				//Salva o marker_attribute
				markerAttribute = markerAttributeRepository.save(markerAttribute);
				
				// Cria o photo_album com o marker_attribute rec�m salvo
				PhotoAlbum photoAlbum = new PhotoAlbum();
				photoAlbum.setMarkerAttribute(markerAttribute);
				
				// Salva o photo_album 
				photoAlbum = photoAlbumRepository.save(photoAlbum);
				photoAlbum.getIdentifier(); // J� tem o id ent�o pode criar o identificador (GJ)
				photoAlbum = photoAlbumRepository.save(photoAlbum);
				
				// Cria o photo com o photo_album rec�m salvo
				Photo photo = new Photo();
				photo.setDescription("Default description");
				photo.setPhotoAlbum(photoAlbum);
				
				//Salva a foto
				photo = photoRepository.save(photo);
				photo.getIdentifier(); // J� tem o id ent�o pode criar o identificador  (GJ)
				photo = photoRepository.save(photo);
				
				// Seta o metafile no objeto photo
				photo.setImage(fileTransfer);
								
				// Realiza o upload da foto
				photo = this.uploadImg(photo);
				
			}
			catch (RepositoryException | RuntimeException e)
			{
				e.printStackTrace();
				continue;
			}
		}	
	}
	
	
	/**
	 * Salva uma foto e devolve o objeto foto
	 * @param photo
	 * @return
	 */
	private Photo uploadImg(Photo photo)
	{
		try
		{
			
			final String mimeType = photo.getMimeType();
	
			final List<String> validMimeTypes = new ArrayList<String>();
			validMimeTypes.add("image/gif");
			validMimeTypes.add("image/jpeg");
			validMimeTypes.add("image/bmp");
			validMimeTypes.add("image/png");
	
			if (mimeType == null || !validMimeTypes.contains(mimeType))
			{
				throw new IllegalArgumentException("Formato inv�lido!");
			}
	
			MetaFile metaFile = new MetaFile();
			
			metaFile.setId(String.valueOf(photo.getId()));
			metaFile.setContentType(photo.getMimeType());
			metaFile.setContentLength(photo.getContentLength());
			metaFile.setFolder(photo.getPhotoAlbum().getIdentifier());
			metaFile.setInputStream(photo.getImage().getInputStream());
			metaFile.setName(photo.getName());
	
			this.metaFileRepository.insert(metaFile);
		
		}
		catch (IOException | RepositoryException e)
		{
			e.printStackTrace();
			LOG.info(e.getMessage());
		}

		return photo;
	}
	
	/**
	 * Verifica se a postagem tem foto e se j� tem o atributo album de fotos padr�o
	 * @throws RepositoryException 
	 * @throws Exception 
	 */
	private FileTransfer verifyMarker(Marker marker) throws RuntimeException, RepositoryException
	{
		// Se j� tiver photo_album n�o pode migrar		
		for (MarkerAttribute markerAttribute : marker.getMarkerAttribute())
		{
			if (markerAttribute.getAttribute().getType() == AttributeType.PHOTO_ALBUM)
			{
				throw new RuntimeException("J� tem album de photos");
			}
		}
		
		// Verifica se o marker tem foto, se n�o tiver estoura exce��o
		final MetaFile metaFile = this.metaFileRepository.findByPath("/marker/" + marker.getId() + "/" + marker.getId(), true);
		
		Assert.isTrue(metaFile != null, "A postagem com id " +marker.getId()+ " n�o tem foto");
		
		return new FileTransfer(metaFile.getName(), metaFile.getContentType(), metaFile.getInputStream());
	}	
}

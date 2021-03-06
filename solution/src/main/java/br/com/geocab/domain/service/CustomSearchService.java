/**
 * 
 */
package br.com.geocab.domain.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.directwebremoting.annotations.RemoteProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.geocab.application.security.ContextHolder;
import br.com.geocab.domain.entity.accessgroup.AccessGroup;
import br.com.geocab.domain.entity.accessgroup.AccessGroupCustomSearch;
import br.com.geocab.domain.entity.configuration.account.User;
import br.com.geocab.domain.entity.configuration.account.UserRole;
import br.com.geocab.domain.entity.layer.CustomSearch;
import br.com.geocab.domain.entity.layer.Layer;
import br.com.geocab.domain.entity.layer.LayerField;
import br.com.geocab.domain.entity.marker.Marker;
import br.com.geocab.domain.entity.marker.MarkerAttribute;
import br.com.geocab.domain.repository.ILayerFieldRepository;
import br.com.geocab.domain.repository.accessgroup.IAccessGroupCustomSearchRepository;
import br.com.geocab.domain.repository.accessgroup.IAccessGroupRepository;
import br.com.geocab.domain.repository.attribute.IAttributeRepository;
import br.com.geocab.domain.repository.customsearch.ICustomSearchRepository;
import br.com.geocab.domain.repository.layergroup.ILayerRepository;
import br.com.geocab.domain.repository.marker.IMarkerAttributeRepository;
import br.com.geocab.domain.repository.marker.IMarkerRepository;

/**
 * Relevant class to control the actions of {@link CustomSearch}}
 * 
 * @author Thiago Rossetto Afonso
 * @since 25/06/2014
 * @version 1.0
 * @category Service
 *
 */

@Service
@Transactional
@RemoteProxy(name = "customSearchService")
public class CustomSearchService
{
	/*-------------------------------------------------------------------
	 * 		 					ATTRIBUTES
	 *-------------------------------------------------------------------*/
	/**
	 * 
	 */
	// private static final Logger LOG = Logger.getLogger(
	// DataSourceService.class.getName() );
	/**
	 * Repository of an {@link CustomRepository}
	 */
	@Autowired
	private ICustomSearchRepository customSearchRepository;

	/**
	 * 
	 */
	@Autowired
	private ILayerFieldRepository layerFieldRepository;

	/**
	 * 
	 */
	@Autowired
	private IAccessGroupCustomSearchRepository accessGroupCustomSearchRepository;

	/**
	 * 
	 */
	@Autowired
	private IAccessGroupRepository accessGroupRepository;

	/**
	 * 
	 */
	@Autowired
	private IMarkerAttributeRepository markerAttributeRepository;

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
	 * 
	 */
	@Autowired
	IAccessGroupCustomSearchRepository accessGroupCustomSearch;
	/**
	 * 
	 */
	@Autowired
	ILayerRepository layerRepository;

	/*-------------------------------------------------------------------
	 *				 		    BEHAVIORS
	 *-------------------------------------------------------------------*/
	/**
	 * Remove os atributos do tipo foto da pesquisa personalizada, para que a
	 * mesma possa ser salva com sucesso
	 * 
	 * @param customSearch
	 * @return
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	private static CustomSearch removeAttributesPhotoAlbum(
			CustomSearch customSearch)
	{
		for (Iterator<LayerField> layerFields = customSearch.getLayerFields()
				.iterator(); layerFields.hasNext();)
		{
			LayerField l = layerFields.next();
			if (l.getType() == null)
			{
				layerFields.remove();
			}
		}
		return customSearch;
	}

	/**
	 * Method to insert an {@link CustomSearch}
	 * 
	 * @param MarkerModeration
	 * @return CustomSearch
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	public Long insertCustomSearch(CustomSearch customSearch)
	{
		customSearch = this.customSearchRepository
				.save(removeAttributesPhotoAlbum(customSearch));
		for (AccessGroupCustomSearch accessGroupCustomSearch : customSearch
				.getAccessGroupCustomSearch())
		{
			accessGroupCustomSearch.setCustomSearch(customSearch);
			this.accessGroupCustomSearchRepository
					.save(accessGroupCustomSearch);
		}

		return customSearch.getId();
	}

	/**
	 * Method to update an {@link CustomSearch}
	 * 
	 * @param customSearch
	 * @return
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	public CustomSearch updateCustomSearch(CustomSearch customSearch)
	{
		customSearch.setLayer(this.customSearchRepository
				.getFindLayerById(customSearch.getLayer().getId()));
		customSearch.setAccessGroupCustomSearch(null);
		customSearch = this.customSearchRepository
				.save(removeAttributesPhotoAlbum(customSearch));

		return customSearch;
	}

	/**
	 * Method to remove an {@link customSearch}
	 * 
	 * @param id
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	public void removeCustomSearch(Long id)
	{
		List<AccessGroupCustomSearch> accessGroupCustomSearchs = this.accessGroupCustomSearchRepository
				.listByCustomSearchId(id);
		for (AccessGroupCustomSearch accessGroupCustomSearch : accessGroupCustomSearchs)
		{
			this.accessGroupCustomSearchRepository
					.delete(accessGroupCustomSearch);
		}

		this.customSearchRepository.delete(id);
	}

	/**
	 * Method to find an {@link CustomSearch} by id
	 * 
	 * @param id
	 * @return customSearch
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	@Transactional(readOnly = true)
	public CustomSearch findCustomSearchById(Long id)
	{
		CustomSearch customSearch = this.customSearchRepository.findById(id);

		customSearch.setLayerFields(
				new HashSet<>(layerFieldRepository.findByCustomSearchId(id)));
		customSearch.setAccessGroupCustomSearch(new HashSet<>(
				accessGroupCustomSearchRepository.listByCustomSearchId(id)));

		// Get the legend of GeoServer's layer
		if (customSearch.getLayer().getDataSource().getUrl() != null)
		{
			int position = customSearch.getLayer().getDataSource().getUrl()
					.lastIndexOf("ows?");
			String urlGeoserver = customSearch.getLayer().getDataSource()
					.getUrl().substring(0, position);
			String urlLegend = urlGeoserver + Layer.LEGEND_GRAPHIC_URL
					+ customSearch.getLayer().getName()
					+ Layer.LEGEND_GRAPHIC_FORMAT;
			customSearch.getLayer().setLegend(urlLegend);
		}

		return customSearch;
	}

	/**
	 * Method to list custom search pageable with filter option
	 *
	 * @param filter
	 * @param pageable
	 * @return customSearch
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	@Transactional(readOnly = true)
	public Page<CustomSearch> listCustomSearchByFilters(String filter,
			PageRequest pageable)
	{
		Page<CustomSearch> customsSearch = this.customSearchRepository
				.listByFilters(filter, pageable);

		for (CustomSearch customSearch : customsSearch.getContent())
		{
			if (customSearch.getLayer().getDataSource().getUrl() != null)
			{
				int position = customSearch.getLayer().getDataSource().getUrl()
						.lastIndexOf("ows?");
				String urlGeoserver = customSearch.getLayer().getDataSource()
						.getUrl().substring(0, position);
				String urlLegend = urlGeoserver + Layer.LEGEND_GRAPHIC_URL
						+ customSearch.getLayer().getName()
						+ Layer.LEGEND_GRAPHIC_FORMAT;
				customSearch.getLayer().setLegend(urlLegend);
			}
		}

		return customsSearch;
	}

	/**
	 * 
	 * @param id
	 * @return
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	@Transactional(readOnly = true)
	public ArrayList<AccessGroup> listAccessGroupBySearchId(Long id)
	{
		ArrayList<AccessGroup> accessGroups = new ArrayList<AccessGroup>();

		List<AccessGroupCustomSearch> accessGroupCustomSearchs = this.accessGroupCustomSearchRepository
				.listByCustomSearchId(id);

		for (AccessGroupCustomSearch accessGroupCustomSearch : accessGroupCustomSearchs)
		{
			accessGroups.add(accessGroupCustomSearch.getAccessGroup());
		}

		return accessGroups;
	}

	
	/**
	 * Method that return an list of custom searchs according the access group
	 * of user
	 */
	@PreAuthorize("true")
	public List<CustomSearch> listCustomSearchsByUser()
	{
		
		if(ContextHolder.getAuthenticatedUser().getRole().equals(UserRole.ADMINISTRATOR))
		{
			return this.customSearchRepository.findAll();
		}
		
		List<CustomSearch> customsSearchUser = new ArrayList<CustomSearch>();

		List<Layer> layers = layerRepository.listAllInternalLayerGroupsAndByAnonymousUser();
		
		List<AccessGroup> accessGroupUser = new ArrayList<>(Arrays.asList(this.accessGroupRepository.findOne(AccessGroup.PUBLIC_GROUP_ID)));
		
		// List of all access groups of user
		if (ContextHolder.getAuthenticatedUser() != null && !ContextHolder.getAuthenticatedUser().getRole().equals(UserRole.ANONYMOUS))
		{
			layers = layerRepository.listAllInternalLayerGroupsAndByUser(ContextHolder.getAuthenticatedUser().getId());
			accessGroupUser = this.accessGroupRepository.listByUser(ContextHolder.getAuthenticatedUser().getEmail());
		}
		
		for (AccessGroup accessGroup : accessGroupUser)
		{
			accessGroup = this.accessGroupRepository.findById(accessGroup.getId());

			for (AccessGroupCustomSearch accessGroupCustomSearch : this.accessGroupCustomSearch.listByAccessGroupId(accessGroup.getId(), null))
			{
				CustomSearch customSearch = customSearchRepository.findById(accessGroupCustomSearch.getCustomSearch().getId());
				
				for (Layer layer : layers) 
				{
					if(layer.getId().equals(customSearch.getLayer().getId()))
					{
						accessGroupCustomSearch.setCustomSearch(customSearch);

						accessGroupCustomSearch.getCustomSearch().setLayerFields(new HashSet<>(layerFieldRepository.findByCustomSearchId(accessGroupCustomSearch.getCustomSearch().getId())));
						
						customsSearchUser.add(accessGroupCustomSearch.getCustomSearch());	
					}
				}
			}
		}
		return customsSearchUser;
	}

	/**
	 * 
	 * @param customSearch
	 *            (Survey List customized to be associated with the access
	 *            group).
	 * @param AccessGroup
	 *            (Access group to be associated).
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	public void linkAccessGroup(List<AccessGroup> accessGroups,
			Long customSearchId)
	{
		CustomSearch customSearch = new CustomSearch(customSearchId);
		for (AccessGroup accessGroup : accessGroups)
		{
			AccessGroupCustomSearch accessGroupCustomSearch = new AccessGroupCustomSearch();
			accessGroupCustomSearch.setAccessGroup(accessGroup);
			accessGroupCustomSearch.setCustomSearch(customSearch);

			this.accessGroupCustomSearchRepository
					.save(accessGroupCustomSearch);
		}
	}

	/**
	 * 
	 * @param accessGroup
	 * @param customSearch
	 */
	@PreAuthorize("hasRole('" + UserRole.ADMINISTRATOR_VALUE + "')")
	public void unlinkAccessGroup(List<AccessGroup> accessGroups,
			Long customSearchId)
	{
		for (AccessGroup accessGroup : accessGroups)
		{
			List<AccessGroupCustomSearch> accessGroupCustomSearchs = this.accessGroupCustomSearchRepository
					.listByAccessGroupCustomSearchId(accessGroup.getId(),
							customSearchId);
			for (AccessGroupCustomSearch accessGroupCustomSearch : accessGroupCustomSearchs)
			{
				this.accessGroupCustomSearchRepository
						.delete(accessGroupCustomSearch);
			}
		}
	}

	/**
	 * M�todo utiliza para execu��o da pesquisa personalizada
	 * 
	 * @param layerId
	 * @param layerFields
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<Marker> listMarkerByLayerFilters(Long layerId,
			List<LayerField> layerFields)
	{

		final User user = ContextHolder.getAuthenticatedUser();

		List<Marker> listMarker = null;

		if (!user.equals(User.ANONYMOUS))
		{

			if (user.getRole().name().equals(UserRole.ADMINISTRATOR_VALUE)
					|| user.getRole().name().equals(UserRole.MODERATOR_VALUE))
			{
				listMarker = this.markerRepository
						.listMarkerByLayerAll(layerId);
			}
			else
			{
				listMarker = this.markerRepository.listMarkerByLayer(layerId,
						user.getId());
			}

		}
		else
		{
			listMarker = this.markerRepository.listMarkerByLayerPublic(layerId);
		}
		// Pega os markerAttributes do banco (Isso foi feito para n�o deixar a
		// requisi��o pesada)
		List<MarkerAttribute> markersAttribute = new ArrayList<>();
		// Vari�vel auxiliar para verificar se h� algum valor nas pesquisas
		boolean hasSearch = false;
		// Verifica se h� algum valor na pesquisa
		for (LayerField layerField : layerFields)
		{
			if (layerField.getValue() != null
					&& layerField.getValue().length() > 0)
			{
				hasSearch = true;
				break;
			}
		}

		if (hasSearch)
		{
			for (LayerField layerField : layerFields)
			{
				if (layerField.getValue() != null
						&& layerField.getValue().length() > 0)
				{
					markersAttribute.addAll(this.markerAttributeRepository
							.listMarkerAttributeByAttributeIdAndFilters(
									layerField.getAttributeId(),
									layerField.getName(), layerField.getValue(),
									this.attributeRepository
											.findById(
													layerField.getAttributeId())
											.getType()));
				}
			}
		}
		else
		{
			return listMarker;
		}

		for (MarkerAttribute markerAttribute : markersAttribute)
		{
			for (Marker marker : listMarker)
			{
				if (markerAttribute.getMarker().getId() == marker.getId())
				{
					markerAttribute.setMarker(marker);
				}
			}
		}

		// Popula a lista de markers com os markers attributos (Isso foi feito
		// para n�o deixar a requisi��o pesada)
		List<Marker> markersReturn = new ArrayList<>();
		for (MarkerAttribute markerAttribute : markersAttribute)
		{
			Marker marker = markerAttribute.getMarker();
			// Instancia lista de markerAttributes dentro do marker
			marker.setMarkerAttribute(new ArrayList<MarkerAttribute>());
			// Adiciona o markerAttribute
			marker.getMarkerAttribute().add(markerAttribute);
			// Adiciona o o marker na lista de retorno
			markersReturn.add(marker);
		}

		return markersReturn;
	}
}

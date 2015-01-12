'use strict';

/**
 *
 * @param $scope
 * @param $log
 * @param $location
 */
function MarkerModerationController($scope, $injector, $log, $state, $timeout, $modal, $location, $importService, $translate) {

	/**
     * Inject the methods, attributes and its states inherited from AbstractCRUDController.
     * @see AbstractCRUDController
     */
    $injector.invoke(AbstractCRUDController, this, {$scope: $scope});   


    /*-------------------------------------------------------------------
     * 		 				 	ATTRIBUTES
     *-------------------------------------------------------------------*/
    //STATES
    /**
     * Static variable that represents
     * the State records list.
     */
    $scope.LIST_STATE = "marker-moderation.list";
    /**
     * Static variable that represents
     * detail of a State record.
     */
    $scope.DETAIL_STATE = "marker-moderation.detail";
    /**
     * Static variable that represents
     * the State for the creation of records.
     */
    $scope.INSERT_STATE = "marker-moderation.create";
    /**
     * Static variable that represents
     * the rule for editing records.
     */
    $scope.UPDATE_STATE = "marker-moderation.update";
    /**
     * Variable that stores the current state of the screen.
     * This variable shall ALWAYS conform to the URL
     * that is in the browser.
     */
    $scope.currentState;
    
    $scope.hiding = true;
    
  //DATA GRID
    /**
     * Static variable coms stock grid buttons
     * The Edit button navigates via URL (sref) why editing is done in another page,
     * Since the delete button calls a method directly via ng-click why does not have a specific screen state.
     */
    var GRID_ACTION_BUTTONS = '<div class="cell-centered button-action">' +
        '<a ui-sref="layer-config.update({id:row.entity.id})"  " title="'+ $translate("admin.layer-config.Update") +'" class="btn btn-mini"><i class="itaipu-icon-edit"></i></a>' +
        '<a ng-click="changeToRemove(row.entity)" title="'+ $translate("admin.layer-config.Delete") +'" class="btn btn-mini"><i class="itaipu-icon-delete"></i></a>' +
        '</div>';
    
    var LAYER_TYPE_NAME = '<div class="ngCellText ng-scope col4 colt4">' +
    '<span ng-if="!row.entity.dataSource.url" ng-cell-text="" class="ng-binding">Camada interna</span>' +
    '<span ng-if="row.entity.dataSource.url" ng-cell-text="" class="ng-binding">{{ row.entity.name }}</span>' +
    '</div>';

    
    var MARKER_BUTTONS = '<div  class="cell-centered">' +
    '<a ng-if="!row.entity.dataSource.url && row.entity.enabled == false" class="btn btn-mini"><i style="font-size: 16px; color: red" class="glyphicon glyphicon-ban-circle"></i></a>'+
    '<a ng-if="!row.entity.dataSource.url && row.entity.enabled == true" class="btn btn-mini"><i style="font-size: 16px; color: green" class="glyphicon glyphicon-ok"></i></a>'+
    '<a ng-if="row.entity.dataSource.url" class="btn btn-mini"><i style="font-size: 16px; color: blue" class="glyphicon glyphicon glyphicon-minus"></i></a>'+
    '</div>';
    
    var IMAGE_LEGEND = '<div class="ngCellText" ng-cell-text ng-class="col.colIndex()">' +
	'<img ng-if="row.entity.dataSource.url" style="width: 20px; height: 20px; border: solid 1px #c9c9c9;" ng-src="{{row.entity.legend}}"/>' +
	'<img ng-if="!row.entity.dataSource.url" style="width: 20px; height: 20px; border: solid 1px #c9c9c9;" ng-src="{{row.entity.icon}}"/>' +
	'</div>';
    
    $scope.gridOptions = {
			data: 'currentPage.content',
			multiSelect: false,
			useExternalSorting: true,
            headerRowHeight: 45,

            rowHeight: 45,
			beforeSelectionChange: function (row, event) {
				//evita chamar a selecao, quando clicado em um action button.
				if ( $(event.target).is("a") || $(event.target).is("i") ) return false;
				$state.go($scope.DETAIL_STATE, {id:row.entity.id});
			},
			columnDefs: [
			             {displayName: $translate('admin.marker-moderation.Name') , field:'name', sortable:false, width: '120px', cellTemplate: IMAGE_LEGEND},
			             {displayName: $translate('admin.marker-moderation.Layer'), field:'layer'}, 
			             {displayName: $translate('admin.marker-moderation.Situation'), field:'situation'},
			             {displayName: $translate('admin.marker-moderation.Review'), field:'review'},			             
			             ]
	};
    
    /**
     * The configuration view of the map
     */
    $scope.view = new ol.View({
        center: ol.proj.transform([-54.1394, -24.7568], 'EPSG:4326', 'EPSG:3857'),
        zoom: 9,
        minZoom: 3
    })
    
    /*-------------------------------------------------------------------
     * 		 				 	  NAVIGATIONS
     *-------------------------------------------------------------------*/
    /**
     * Main method that makes the role of front-controller of the screen.
	 * He is invoked whenever there is a change of URL (@see $stateChangeSuccess),
	 * When this occurs, gets the State via the $state and calls the initial method of that State.
	 * Ex.: /list -> changeToList()
	 *      /criar -> changeToInsert()
	 *
	 * If the State is not found, he directs to the listing,
	 * Although the front controller of angle won't let enter an invalid URL.
     */
    $scope.initialize = function (toState, toParams, fromState, fromParams) {
                        
    	var state = $state.current.name;
    	
        /**
         * It is necessary to remove the sortInfo attribute because the return of an edition was doubling the value of the same with the Sort attribute
         * preventing the ordinations in the columns of the grid.
         */
//        if ($scope.gridOptions.sortInfo) {
//            delete $scope.gridOptions.sortInfo;
//        }
    	
        $log.info("Starting the front controller.");

        /*switch (state) {
            case $scope.LIST_STATE:
            {
                $scope.changeToList();
            }
                break;
            case $scope.DETAIL_STATE:
            {
                $scope.changeToDetail($state.params.id);
            }
                break;
            case $scope.INSERT_STATE:
            {
                $scope.changeToInsert();
            }
                break;
            case $scope.UPDATE_STATE:
            {
                $scope.changeToUpdate($state.params.id);
            }
                break;
            default :
            {
                $state.go($scope.LIST_STATE);
            }
        }*/
    
        /**
         * Openlayers map configuration
         */
        $scope.olMapDiv = document.getElementById('olmap');
        $scope.map = new ol.Map({

            controls: [
                $scope.mousePositionControl
            ],
            layers: [
                 new ol.layer.Tile({
                   source: new ol.source.OSM()
                 })
               ],
            target: $scope.olMapDiv,
            view: $scope.view
        });
    
    };
    
    /**
     * Performs initial procedures (prepares the State)
     * for the query screen and after that, change the State to list.
     * @see LIST_STATE
     * @see $stateChangeSuccess
     *
     * To change to this State, one must first load the data from the query.
     */
    $scope.changeToList = function () {
        $log.info("changeToList");
        
        $scope.currentState = $scope.LIST_STATE;

        var pageRequest = new PageRequest();
        pageRequest.size = 10;
        $scope.pageRequest = pageRequest;

        //$scope.listPostsByFilters(null, pageRequest);
    };

    /**
     * Performs initial procedures (prepares the State)
     * for screen and after that, change the State to insert.
     * @see INSERT_STATE
     * @see $stateChangeSuccess
     *
     * To change to this State, you must first instantiate a new currentEntity,
     * to clear fields and configure defaults values.
     */
    $scope.changeToInsert = function () {
        $log.info("changeToInsert");

               

    };

    /**
     * Performs initial procedures (prepares the State)
     * to the Edit screen and after that, change the State to update.
     * @see UPDATE_STATE
     * @see $stateChangeSuccess
     *
     * To change to this State, must first obtain via id
     * the query service record and only then change the State of the screen.
     */
    $scope.changeToUpdate = function (id) {

        $log.info("changeToUpdate", id);

       
    };

    /**
     * Performs initial procedures (prepares the State)
     * to the detail screen and after that, change the State to detail.
     * @see DETAIL_STATE
     * @see $stateChangeSuccess
     *
     * To change to this State, must first obtain via id
     * the updated record query service, and then change the State of the screen.
     * If the modifier is not valid, returns to the State of the listing.
     */
    $scope.changeToDetail = function (id) {
        $log.info("changeToDetail", id);

        if (id == null || id == "" || id == 0) {
            $scope.msg = {type: "error", text: $scope.INVALID_ID_MESSAGE, dismiss: true};
            $scope.currentState = $scope.LIST_STATE;
            $state.go($scope.LIST_STATE);
            return;
        }

        
    };

    /**
     * Performs initial procedures (prepares the State)
     * for the delete screen.
     *
     * Before deleting the user notified for confirmation
     * and just so the record is deleted.
     * After deleted, update the grid with filter State, paging and sorting.
     */
    $scope.changeToRemove = function (layer) {
        $log.info("changeToRemove");

        
    };    
    
    	
    $scope.showFields = function (showFields){
    	if (showFields) {
    		$scope.hiding = false;
    	} else
    		$scope.hiding = true;
    }
    
    
    
}
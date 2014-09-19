﻿'use strict';

/**
 * 
 * @param $scope
 * @param $log
 * @param $location
 */
function DataSourceController( $scope, $injector, $log, $state, $timeout, $modal, $location, $importService ) {
	
	/**
	 * Injeta os métodos, atributos e seus estados herdados de AbstractCRUDController.
	 * @see AbstractCRUDController
	 */
	$injector.invoke(AbstractCRUDController, this, {$scope: $scope});
	
	console.log($importService("dataSourceService"));

	/*-------------------------------------------------------------------
	 * 		 				 	EVENT HANDLERS
	 *-------------------------------------------------------------------*/

	/**
	 *  Handler que escuta toda vez que o usuário/programadamente faz o sorting na ng-grid.
	 *  Quando o evento é disparado, configuramos o pager do spring-data 
	 *  e chamamos novamente a consulta, considerando também o estado do filtro (@see $scope.data.filter)
	 */
	$scope.$on('ngGridEventSorted', function(event, sort) {

		// compara os objetos para garantir que o evento seja executado somente uma vez q não entre em loop
		if ( !angular.equals(sort, $scope.gridOptions.sortInfo) ) {
			$scope.gridOptions.sortInfo = angular.copy(sort);

			//Order do spring-data
			var order = new Order();
			order.direction = sort.directions[0].toUpperCase();
			order.property = sort.fields[0];

			//Sort do spring-data
			$scope.currentPage.pageable.sort = new Sort();
			$scope.currentPage.pageable.sort.orders = [ order ];

			$scope.listDataSourceByFilters( $scope.data.filter, $scope.currentPage.pageable );
		}
	});

	/*-------------------------------------------------------------------
	 * 		 				 	ATTRIBUTES
	 *-------------------------------------------------------------------*/
	//STATES
	/**
	 * Variável estática que representa 
	 * o estado de listagem de registros.
	 */
	$scope.LIST_STATE = "data-source.list";
	/**
	 * Variável estática que representa
	 * o estado de detalhe de um registro.
	 */
	$scope.DETAIL_STATE = "data-source.detail";
	/**
	 * Variável estática que representa
	 * o estado para a criação de registros.
	 */
	$scope.INSERT_STATE = "data-source.create";
	/**
	 * Variável estática que representa
	 * o estado para a edição de registros.
	 */
	$scope.UPDATE_STATE = "data-source.update";
	/**
	 * Variável que armazena o estado corrente da tela.
	 * Esta variável deve SEMPRE estar de acordo com a URL 
	 * que está no browser.
	 */
	$scope.currentState;

	//DATA GRID
	/**
	 * Variável estática coms os botões de ações da grid
	 * O botão de editar navega via URL (sref) por que a edição é feita em outra página,
	 * já o botão de excluir chama um método direto via ng-click por que não tem um estado da tela específico.
	 */
    var GRID_ACTION_BUTTONS = '<div class="cell-centered">' +
	'<a ui-sref="data-source.update({id:row.entity.id})" title="Update" class="btn btn-mini"><i class="itaipu-icon-edit"></i></a>'+
	'<a ng-click="changeToRemove(row.entity)" title="Remove" class="btn btn-mini"><i class="itaipu-icon-delete"></i></a>'+
	'</div>';

	/**
	 * Configurações gerais da ng-grid. 
	 * @see https://github.com/angular-ui/ng-grid/wiki/Configuration-Options
	 */
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
			             {displayName:'Name', field:'name'},
			             {displayName:'Address', field:'url',  width:'55%'},
			             {displayName:'Action', sortable:false, cellTemplate: GRID_ACTION_BUTTONS, width:'100px'}
			             ]
	};

	/**
	 * Variável que armazena o estado da paginação 
	 * para renderizar o pager e também para fazer as requisições das
	 * novas páginas, contendo o estado do Sort incluído.
	 * 
	 * @type PageRequest
	 */
	$scope.currentPage;

	//FORM
	/**
	 * Variável para armazenar atributos do formulário que
	 * não cabem em uma entidade. Ex.:
	 * @filter - Filtro da consulta
	 */
	$scope.data = { filter:null, showFields: true, tipoFonteDados: 'WMS' };
	/**
	 * Armazena a entitidade corrente para edição ou detalhe.
	 */
	$scope.currentEntity;
	
	/*-------------------------------------------------------------------
	 * 		 				 	  NAVIGATIONS
	 *-------------------------------------------------------------------*/
	/**
	 * Método principal que faz o papel de front-controller da tela.
	 * Ele é invocado toda vez que ocorre uma mudança de URL (@see $stateChangeSuccess),
	 * quando isso ocorre, obtém o estado através do $state e chama o método inicial daquele estado.
	 * Ex.: /list -> changeToList()
	 *      /criar -> changeToInsert()
	 *      
	 * Caso o estado não for encontrado, ele direciona para a listagem,
	 * apesar que o front-controller do angular não deixa digitar uma URL inválida.
	 */
	$scope.initialize = function( toState, toParams, fromState, fromParams ) {
		var state = $state.current.name;
	
		/**
		 * É necessario remover o atributo sortInfo pois o retorno de uma edição estava duplicando o valor do mesmo com o atributo Sort
		 * impossibilitando as ordenações nas colunas da grid.
		 */
		if( $scope.gridOptions.sortInfo ){
			delete $scope.gridOptions.sortInfo;
		}

		$log.info("Starting the front controller.");

		switch (state) {
			case $scope.LIST_STATE: {
				$scope.changeToList();
			}
			break;
			case $scope.DETAIL_STATE: {
				$scope.changeToDetail( $state.params.id );
			}
			break;
			case $scope.INSERT_STATE: {
				$scope.changeToInsert();
			}
			break;
			case $scope.UPDATE_STATE: {
				$scope.changeToUpdate( $state.params.id );
			}
			break;
			default : {
				$state.go( $scope.LIST_STATE );
			}
		}
	};

	/**
	 * Realiza os procedimentos iniciais (prepara o estado) 
	 * para a tela de consulta e após isso, muda o estado para list. 
	 * @see LIST_STATE
	 * @see $stateChangeSuccess
	 * 
	 * Para mudar para este estado, deve-se primeiro carregar os dados da consulta.
	 */
	$scope.changeToList = function() {
		$log.info("changeToList");
		
		var pageRequest = new PageRequest();
		pageRequest.size = 6;
		$scope.pageRequest = pageRequest;

		$scope.listDataSourceByFilters( null, pageRequest );
		
		$scope.currentState = $scope.LIST_STATE;
	};

	/**
	 * Realiza os procedimentos iniciais (prepara o estado) 
	 * para a tela de inserção e após isso, muda o estado para insert. 
	 * @see INSERT_STATE
	 * @see $stateChangeSuccess
	 * 
	 * Para mudar para este estado, deve-se primeiro instanciar um novo currentEntity,
	 * para limpar os campos e configurar valores defaults.
	 */
	$scope.changeToInsert = function() {
		$log.info("changeToInsert");

		$scope.currentEntity = new DataSource();
		$scope.currentEntity.typeDataSource = "WMS";
		/*
		$scope.currentEntity = new FonteDados();
		
		//Para funcionar com o ng-model no radio button
		$scope.currentEntity.tipoFonteDados = 'WMS';*/

		$scope.currentState = $scope.INSERT_STATE;

	};

	/**
	 * Realiza os procedimentos iniciais (prepara o estado) 
	 * para a tela de edição e após isso, muda o estado para update. 
	 * @see UPDATE_STATE
	 * @see $stateChangeSuccess
	 * 
	 * Para mudar para este estado, deve-se primeiro obter via id
	 * o registro pelo serviço de consulta e só então mudar o estado da tela.
	 */
	$scope.changeToUpdate = function( id ) {
		$log.info("changeToUpdate", id);

		dataSourceService.findDataSourceById( $state.params.id, {
			callback : function(result) {
				$scope.currentEntity = result;
				$scope.currentState = $scope.UPDATE_STATE;
				$state.go($scope.UPDATE_STATE);
				$scope.$apply();
			},
			errorHandler : function(message, exception) {
				$scope.msg = {type:"danger", text: message, dismiss:true};
				$scope.$apply();
			}
		}); 
	};

	/**
	 * Realiza os procedimentos iniciais (prepara o estado) 
	 * para a tela de detalhe e após isso, muda o estado para detail. 
	 * @see DETAIL_STATE
	 * @see $stateChangeSuccess
	 * 
	 * Para mudar para este estado, deve-se primeiro obter via id
	 * o registro atualizado pelo serviço de consulta e só então mudar o estado da tela.
	 * Caso o indentificador esteja inválido, retorna para o estado de listagem.
	 */
	$scope.changeToDetail = function( id ) {
		$log.info("changeToDetail", id);

		if ( id == null || id == "" || id == 0 ) {
			$scope.msg = {type:"error", text: $scope.INVALID_ID_MESSAGE, dismiss:true};
			$scope.currentState = $scope.LIST_STATE;
			$state.go($scope.LIST_STATE);
			return;
		}

		dataSourceService.findDataSourceById( id, {
			callback : function(result) {
				$scope.currentEntity = result;
				$scope.currentState = $scope.DETAIL_STATE;
				$state.go($scope.DETAIL_STATE, {id:id});
				$scope.$apply();
			},
			errorHandler : function(message, exception) {
				$scope.msg = {type:"danger", text: message, dismiss:true};
				$scope.$apply();
			}
		});
	};

	/**
	 * Realiza os procedimentos iniciais (prepara o estado) 
	 * para a tela de exclusão. 
	 * 
	 * Antes de excluir, o usuário notificado para confirmação 
	 * e só então o registro é excluido.
	 * Após excluído, atualizamos a grid com estado de filtro, paginação e sorting. 
	 */
	$scope.changeToRemove = function( dataSource ) {
		$log.info("changeToRemove", dataSource);

		var dialog = $modal.open( {
			templateUrl: "static/libs/eits-directives/dialog/dialog-template.html",
			controller: DialogController,
			windowClass: 'dialog-delete',
			resolve: {
				title: function(){return "Exclusão de fonte de dados";},
				message: function(){return 'Tem certeza que deseja excluir a fonte de dados "'+dataSource.name+'"? <br/>Esta operação não poderá mais ser desfeita.';},
				buttons: function(){return [ {label:'Excluir', css:'btn btn-danger'}, {label:'Cancelar', css:'btn btn-default', dismiss:true} ];}
			}
		});

        dialog.result.then( function(result) {

			dataSourceService.removeDataSource( dataSource.id, {
				callback : function(result) {
					//caso o currentPage esteja null, configura o pager default
					if ( $scope.currentPage == null ) {
						$scope.changeToList();
						//caso não, usa o mesmo estado para carregar a listagem
					} else {
						$scope.listDataSourceByFilters($scope.data.filter, $scope.currentPage.pageable);
					}

					$scope.msg = {type: "success", text: 'O registro "'+dataSource.name+'" foi excluído com sucesso.', dismiss:true};
				},
				errorHandler : function(message, exception) {
					$scope.msg = {type:"danger", text: message, dismiss:true};
					$scope.$apply();
				}
			});
		});
	};

	/**
	 * Configura o pageRequest conforme o componente visual pager
	 * e chama o serviçoe de listagem, considerando o filtro corrente na tela.
	 * 
	 * @see currentPage
	 * @see data.filter 
	 */
	$scope.changeToPage = function( filter, pageNumber ) {
		$scope.currentPage.pageable.page = pageNumber-1;
		$scope.listFontesDadosByFilters( filter, $scope.currentPage.pageable );
	};

	/*-------------------------------------------------------------------
	 * 		 				 	  BEHAVIORS
	 *-------------------------------------------------------------------*/

	/**
	 * Realiza a consulta de registros, consirando filtro, paginação e sorting.
	 * Quando ok, muda o estado da tela para list.
	 * 
	 * @see data.filter
	 * @see currentPage
	 */
	$scope.listDataSourceByFilters = function( filter, pageRequest ) {

		dataSourceService.listDataSourceByFilters( filter, pageRequest, {
			callback : function(result) {
				$scope.currentPage = result;
				$scope.currentPage.pageable.pageNumber++;//Para fazer o bind com o pagination
				$scope.currentState = $scope.LIST_STATE;
				$state.go( $scope.LIST_STATE );
				$scope.$apply();
			},
			errorHandler : function(message, exception) {
				$scope.msg = {type:"danger", text: message, dismiss:true};
				$scope.$apply();
			}
		});
	};

	/**
	 * Insert a new Data Source
	 * If success, change to detail state.
	 */
	$scope.insertDataSource = function() {

		if ( !$scope.form().$valid ) {
			$scope.msg = {type:"danger", text: $scope.INVALID_FORM_MESSAGE, dismiss:true};
			return;
		}

		dataSourceService.insertDataSource( $scope.currentEntity, {
			callback : function() {
				$scope.currentState = $scope.LIST_STATE;
				$state.go($scope.LIST_STATE);
				$scope.msg = {type:"success", text: "Fonte de dados geográficos inserida com sucesso!", dismiss:true};
				$scope.$apply();
			},
			errorHandler : function(message, exception) {
				$scope.msg = {type:"danger", text: message, dismiss:true};
				$scope.$apply();
			}
		});
	};

	/**
	 * Realiza a atualiza de um registro
	 * e no sucesso modifica o estado da tela para o estado de detalhe
	 */
	$scope.updateDataSource = function() {

		if ( !$scope.form().$valid ) {
			$scope.msg = {type:"danger", text: $scope.INVALID_FORM_MESSAGE, dismiss:true};
			return;
		}

		dataSourceService.updateDataSource( $scope.currentEntity , {
			callback : function() {
				$scope.currentState = $scope.LIST_STATE;
				$state.go($scope.LIST_STATE);
				$scope.msg = {type:"success", text: "Fonte de dados geográficos atualizada com sucesso!", dismiss:true};
				$scope.$apply();
			},
			errorHandler : function(message, exception) {
				if (exception.message.indexOf("ConstraintViolationException") > -1){
					message = "O campo Nome ou Endereço informado já existe, altere e tente novamente.";
				}
				$scope.msg = {type:"danger", text: message, dismiss:true};
				$scope.$apply();
			}
		});
	};
	
	/**
	 * Testa conexão se é WMS ou WFS
	 */
	$scope.testaConexaoFonteDados = function( fonteDados ) {

		if ( !$scope.form().endereco.$valid ) {
			$scope.msg = {type:"danger", text: $scope.INVALID_FORM_MESSAGE, dismiss:true};
			return;
		}

		fonteDadosService.testaConexao( fonteDados.endereco, fonteDados.tipoFonteDados, {
			callback : function(result) {
				if(result){
					$scope.msg = {type:"success", text: "Conexão estabelecida com êxito.", dismiss:true};					
				}
				else{
					$scope.msg = {type:"danger", text: "Não foi possível estabelecer conexão com a fonte de dados geográficos.", dismiss:true};
				}
				$scope.$apply();
			},
			errorHandler : function(message, exception) {
				$scope.msg = {type:"danger", text: message, dismiss:true};
				$scope.$apply();
			}
		});
	};
	
	/**
	 * Limpa os campos
	 */
	$scope.clearFields = function(){
		if(!$scope.data.showFields){
			$scope.currentEntity.nomeUsuario = "";
			$scope.currentEntity.senha = "";
		}
	};

};


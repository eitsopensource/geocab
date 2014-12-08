<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>

<div class="modal-content">

	<div class="modal-header">
		<button type="button" class="close" ng-click="close(true)"><span aria-hidden="true">&times;</span><span class="sr-only"><spring:message code="admin.custom-search.Close"/></span></button>
	    <h3 class="modal-title"><spring:message code="admin.custom-search.Link-layer"/></h3>
	</div>

	<div class="modal-body" ng-init="initialize();" style="overflow: visible">
	
		<!--Messages-->
		<div class="msg" ng-include="'static/libs/eits-directives/alert/alert.html'"></div>
		
        <form novalidate name="form" default-button="{{buttonInsert}}">
            <input type="text" class="form-control" autofocus ng-model="data.filter" placeholder="<spring:message code="admin.custom-search.Search-by-name-or-title"/>" style="width: 300px; float: left; margin-bottom: 10px;"/>
            <input type="submit" class="btn btn-default" style="margin-left: 5px" value="<spring:message code="admin.custom-search.Search"/>" ng-disabled="currentPage == null" ng-click="listByFilters(data.filter, data.fonteDados.id, currentPage.pageable )"/>
        </form>

		<br style="clear: both"/>

		<div ng-show="showLoading" class="grid-loading"></div>
		<div ng-grid="gridOptions" style="height: 350px; border: 1px solid rgb(212,212,212);"></div>
	       
		<div class="gridFooterDiv">
			<pagination style="text-align: center;"
				total-items="currentPage.total" rotate="false"
				items-per-page="currentPage.size"
				max-size="currentPage.totalPages"
				ng-change="changeToPage(data.filter, currentPage.pageable.pageNumber)"
				ng-model="currentPage.pageable.pageNumber" boundary-links="true"
				previous-text="‹" next-text="›" first-text="«" last-text="»">
			</pagination>
		</div>
		
		<div class="grid-elements-count" ng-show="currentPage.totalElements > 0">
			{{currentPage.numberOfElements}} <spring:message code="admin.layer-config.of"/> {{currentPage.totalElements}} items
		</div>
        
	</div>
	<div class="modal-footer">
		<button id="buttonClose" ng-disabled="selectedEntity == null && gridOptions.selectedItems.length == 0" class="btn btn-primary" ng-click="close(false)">Selecionar</button>
        <button class="btn btn-default" ng-click="close(true)">Fechar</button>
	</div>

</div>

</html>
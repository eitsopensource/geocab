(function (window, angular, undefined) {
  'use strict';

  //Start the AngularJS
  var module = angular.module('application', ['ngMessages', 'ionic', 'eits-ng']);

  /**
   *
   */
  module.constant('$API_ENDPOINT', 'http://geocab.sbox.me');

  /**
   *
   */
  module.config(function ($stateProvider, $urlRouterProvider, $importServiceProvider, $sceDelegateProvider, $API_ENDPOINT) {
    //-------
    //Broker configuration
    //-------
    $importServiceProvider.setBrokerURL($API_ENDPOINT + "/broker/interface");

    //-------
    //Strict Contextual Escaping
    //-------
    $sceDelegateProvider.resourceUrlWhitelist([
      // Allow same origin resource loads.
      'self',
      // Allow loading from our assets domain. Notice the difference between * and **.
      $API_ENDPOINT + '/**'
    ]);


    //-------
    //URL Router
    //-------
    $urlRouterProvider.otherwise("/authentication/login");

    //AUTHENTICATION
    $stateProvider.state('authentication', {
      abstract: true,
      url: "/authentication",
      template: '<ion-nav-view></ion-nav-view>',
      controller: 'AuthenticationController'
    }).state('authentication.signin', {
      url: "/signin",
      templateUrl: './views/authentication/authentication-index.html',
    });

    //HOME
    $stateProvider.state('home', {
      url: "/home",
      controller: 'HomeController',
      templateUrl: './views/home/home-index.html',
    });
  });

  /**
   *
   */
  module.run(function ($rootScope, $ionicPlatform, $state, $stateParams, $API_ENDPOINT) {

    $rootScope.$state = $state;
    $rootScope.$stateParams = $stateParams;
    $rootScope.$API_ENDPOINT = $API_ENDPOINT;

    $ionicPlatform.ready(function () {
      // Hide the accessory bar by default (remove this to show the accessory bar above the keyboard for form inputs
      if (window.cordova && window.cordova.plugins.Keyboard) {
        cordova.plugins.Keyboard.hideKeyboardAccessoryBar(true);
        cordova.plugins.Keyboard.disableScroll(true);
      }

      if (window.StatusBar) {
        // org.apache.cordova.statusbar required
        StatusBar.styleDefault();
      }
    });
  });

  /**
   *
   */
  angular.element(document).ready(function () {
    angular.bootstrap(document, ['application']);
  });

})(window, window.angular);
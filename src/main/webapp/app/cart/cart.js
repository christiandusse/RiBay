/**
 * Created by Christian on 11.01.2016.
 */

'use strict';

angular.module('myApp.cart', [])

    .config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/cart', {
            templateUrl: 'cart/cart.html',
            controller: 'cartCtrl'
        });
    }])

    .service('cartService', ['$http', 'imageService', function ($http, imageService) {

        this.getCart = function (callback) {
            $http.get('/cart').success(function (response) {

                response.articles.forEach(function (article) {
                    // resolve images
                    article.image = imageService.createImageURLFromId(article.image);
                });

                callback(response);
            });
        };

        this.deleteItem = function (item, callback) {
            $http.delete('/cart/remove/' + item.id).success(function (response) {

                response.articles.forEach(function (article) {
                    // resolve images
                    article.image = imageService.createImageURLFromId(article.image);
                });

                callback(response);
            });
        };

        this.setQuantity = function (item, oldQuantity, newQuantity, callback) {
            var delta = newQuantity - oldQuantity;
            var url;
            if (delta > 0) {
                url = '/cart/add/' + item.id + '/' + delta;
            }
            else if (delta < 0) {
                url = '/cart/remove/' + item.id + '/' + Math.abs(delta);
            }

            if (url) {
                $http.put(url).success(callback);
            }
        };

        this.checkout = function () {
            // TODO proceed to checkout
            alert("Checkout");
        };

    }])

    .controller('cartCtrl', ['$scope', 'waitingService', 'cartService', function ($scope, waitingService, cartService) {

        waitingService.startWaiting();

        $scope.cart = undefined;
        $scope.subtotal = undefined;
        $scope.isEmpty = true;

        var update = function (data) {
            $scope.cart = data.articles;
            $scope.isEmpty = (data.articles.length == 0);

            var total_amount = 0;
            angular.forEach($scope.cart, function (item) {
                total_amount += item.quantity;
            });

            var total_price = 0;
            angular.forEach($scope.cart, function (item) {
                total_price += (item.quantity * item.price);
            });

            $scope.subtotal = {
                amount: total_amount,
                price: total_price
            };

            waitingService.endWaiting();
        }

        cartService.getCart(update); // init cart


        $scope.deleteItem = function (item) {
            waitingService.startWaiting();

            cartService.deleteItem(item, function (data) {
                // update with new cart from callback
                update(data);
            })
        };

        $scope.setQuantity = function (item, quantity) {
            waitingService.startWaiting();

            cartService.setQuantity(item, item.quantity, quantity, function () {
                // TODO update without need for additional call for getting updated cart?
                cartService.getCart(update); // update cart
            });
        };

        $scope.checkout = cartService.checkout;

    }]);

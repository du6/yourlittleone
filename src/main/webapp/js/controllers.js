'use strict';

/**
 * The root activityApp module.
 *
 * @type {activityApp|*|{}}
 */
var activityApp = activityApp || {};

/**
 * @ngdoc module
 * @name activityControllers
 *
 * @description
 * Angular module for controllers.
 *
 */
activityApp.controllers = angular.module('activityControllers', ['ui.bootstrap']);

/**
 * @ngdoc controller
 * @name MyProfileCtrl
 *
 * @description
 * A controller used for the My Profile page.
 */
activityApp.controllers.controller('MyProfileCtrl',
    function ($scope, $log, oauth2Provider, HTTP_ERRORS) {
        $scope.submitted = false;
        $scope.loading = false;

        /**
         * The initial profile retrieved from the server to know the dirty state.
         * @type {{}}
         */
        $scope.initialProfile = {};

        /**
         * Candidates for the gender select box.
         * @type {string[]}
         */
        $scope.genderOptions = [
            {value: 'Male', label: 'Male'},
            {value: 'Female', label: 'Female'},
            {value: 'Third', label: 'Third'},
            {value: 'You_Guess', label: 'You Guess'}
        ];

        /**
         * Initializes the My profile page.
         * Update the profile if the user's profile has been stored.
         */
        $scope.init = function () {
            var retrieveProfileCallback = function () {
                $scope.profile = {};
                $scope.loading = true;
                gapi.client.activity.getProfile().
                    execute(function (resp) {
                        $scope.$apply(function () {
                            $scope.loading = false;
                            if (resp.error) {
                                // Failed to get a user profile.
                            } else {
                                // Succeeded to get the user profile.
                                $scope.profile.displayName = resp.result.displayName;
                                $scope.profile.gender = resp.result.gender;
                                $scope.initialProfile = resp.result;
                            }
                        });
                    }
                );
            };
            if (!oauth2Provider.signedIn) {
                var modalInstance = oauth2Provider.showLoginModal();
                modalInstance.result.then(retrieveProfileCallback);
            } else {
                retrieveProfileCallback();
            }
        };

        /**
         * Invokes the activity.saveProfile API.
         *
         */
        $scope.saveProfile = function () {
            $scope.submitted = true;
            $scope.loading = true;
            gapi.client.activity.saveProfile($scope.profile).
                execute(function (resp) {
                    $scope.$apply(function () {
                        $scope.loading = false;
                        if (resp.error) {
                            // The request has failed.
                            var errorMessage = resp.error.message || '';
                            $scope.messages = 'Failed to update a profile : ' + errorMessage;
                            $scope.alertStatus = 'warning';
                            $log.error($scope.messages + 'Profile : ' + JSON.stringify($scope.profile));

                            if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                                oauth2Provider.showLoginModal();
                                return;
                            }
                        } else {
                            // The request has succeeded.
                            $scope.messages = 'The profile has been updated';
                            $scope.alertStatus = 'success';
                            $scope.submitted = false;
                            $scope.initialProfile = {
                                displayName: $scope.profile.displayName,
                                gender: $scope.profile.gender
                            };

                            $log.info($scope.messages + JSON.stringify(resp.result));
                        }
                    });
                });
        };
    })
;

/**
 * @ngdoc controller
 * @name CreateActivityCtrl
 *
 * @description
 * A controller used for the Create activities page.
 */
activityApp.controllers.controller('CreateActivityCtrl',
    function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

        /**
         * The activity object being edited in the page.
         * @type {{}|*}
         */
        $scope.activity = $scope.activity || {};

        /**
         * Holds the default values for the input candidates for topics select.
         * @type {string[]}
         */
        $scope.topics = [
            'General Social',
            'Eating',
            'Gamming',
            'Chatting',
            'Indoor',
            'Outdoor'
        ];

        /**
         * Tests if the argument is an integer and not negative.
         * @returns {boolean} true if the argument is an integer, false otherwise.
         */
        $scope.isValidMaxAttendees = function () {
            if (!$scope.activity.maxAttendees || $scope.activity.maxAttendees.length == 0) {
                return true;
            }
            return /^[\d]+$/.test($scope.activity.maxAttendees) && $scope.activity.maxAttendees >= 0;
        }

        /**
         * Tests if the activity.startDate and activity.endDate are valid.
         * @returns {boolean} true if the dates are valid, false otherwise.
         */
        $scope.isValidDates = function () {
            if (!$scope.activity.startDate && !$scope.activity.endDate) {
                return true;
            }
            if ($scope.activity.startDate && !$scope.activity.endDate) {
                return true;
            }
            return $scope.activity.startDate <= $scope.activity.endDate;
        }

        /**
         * Tests if $scope.activity is valid.
         * @param activityForm the form object from the create_activities.html page.
         * @returns {boolean|*} true if valid, false otherwise.
         */
        $scope.isValidActivity = function (activityForm) {
            return !activityForm.$invalid &&
                $scope.isValidMaxAttendees() &&
                $scope.isValidDates();
        }

        /**
         * Invokes the activity.createActivity API.
         *
         * @param activityForm the form object.
         */
        $scope.createActivity = function (activityForm) {
            if (!$scope.isValidActivity(activityForm)) {
                return;
            }

            $scope.loading = true;
            gapi.client.activity.createActivity($scope.activity).
                execute(function (resp) {
                    $scope.$apply(function () {
                        $scope.loading = false;
                        if (resp.error) {
                            // The request has failed.
                            var errorMessage = resp.error.message || '';
                            $scope.messages = 'Failed to create a activity : ' + errorMessage;
                            $scope.alertStatus = 'warning';
                            $log.error($scope.messages + ' Activity : ' + JSON.stringify($scope.activity));

                            if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                                oauth2Provider.showLoginModal();
                                return;
                            }
                        } else {
                            // The request has succeeded.
                            $scope.messages = 'The activity has been created : ' + resp.result.name;
                            $scope.alertStatus = 'success';
                            $scope.submitted = false;
                            $scope.activity = {};
                            $log.info($scope.messages + ' : ' + JSON.stringify(resp.result));
                        }
                    });
                });
        };
    });

/**
 * @ngdoc controller
 * @name ShowActivityCtrl
 *
 * @description
 * A controller used for the Show activities page.
 */
activityApp.controllers.controller('ShowActivityCtrl', function ($scope, $log, oauth2Provider, HTTP_ERRORS) {

    /**
     * Holds the status if the query is being executed.
     * @type {boolean}
     */
    $scope.submitted = false;

    $scope.selectedTab = 'ALL';

    /**
     * Holds the filters that will be applied when queryActivitiesAll is invoked.
     * @type {Array}
     */
    $scope.filters = [
    ];

    $scope.filtereableFields = [
        {enumValue: 'LOCATION', displayName: 'Location'},
        {enumValue: 'TOPIC', displayName: 'Topic'},
        {enumValue: 'MONTH', displayName: 'Start month'},
        {enumValue: 'MAX_ATTENDEES', displayName: 'Max Attendees'}
    ]

    /**
     * Possible operators.
     *
     * @type {{displayName: string, enumValue: string}[]}
     */
    $scope.operators = [
        {displayName: '=', enumValue: 'EQ'},
        {displayName: '>', enumValue: 'GT'},
        {displayName: '>=', enumValue: 'GTEQ'},
        {displayName: '<', enumValue: 'LT'},
        {displayName: '<=', enumValue: 'LTEQ'},
        {displayName: '!=', enumValue: 'NE'}
    ];

    /**
     * Holds the activities currently displayed in the page.
     * @type {Array}
     */
    $scope.activities = [];

    /**
     * Holds the state if offcanvas is enabled.
     *
     * @type {boolean}
     */
    $scope.isOffcanvasEnabled = false;

    /**
     * Sets the selected tab to 'ALL'
     */
    $scope.tabAllSelected = function () {
        $scope.selectedTab = 'ALL';
        $scope.queryActivities();
    };

    /**
     * Sets the selected tab to 'YOU_HAVE_CREATED'
     */
    $scope.tabYouHaveCreatedSelected = function () {
        $scope.selectedTab = 'YOU_HAVE_CREATED';
        if (!oauth2Provider.signedIn) {
            oauth2Provider.showLoginModal();
            return;
        }
        $scope.queryActivities();
    };

    /**
     * Sets the selected tab to 'YOU_WILL_ATTEND'
     */
    $scope.tabYouWillAttendSelected = function () {
        $scope.selectedTab = 'YOU_WILL_ATTEND';
        if (!oauth2Provider.signedIn) {
            oauth2Provider.showLoginModal();
            return;
        }
        $scope.queryActivities();
    };

    /**
     * Toggles the status of the offcanvas.
     */
    $scope.toggleOffcanvas = function () {
        $scope.isOffcanvasEnabled = !$scope.isOffcanvasEnabled;
    };

    /**
     * Namespace for the pagination.
     * @type {{}|*}
     */
    $scope.pagination = $scope.pagination || {};
    $scope.pagination.currentPage = 0;
    $scope.pagination.pageSize = 20;
    /**
     * Returns the number of the pages in the pagination.
     *
     * @returns {number}
     */
    $scope.pagination.numberOfPages = function () {
        return Math.ceil($scope.activities.length / $scope.pagination.pageSize);
    };

    /**
     * Returns an array including the numbers from 1 to the number of the pages.
     *
     * @returns {Array}
     */
    $scope.pagination.pageArray = function () {
        var pages = [];
        var numberOfPages = $scope.pagination.numberOfPages();
        for (var i = 0; i < numberOfPages; i++) {
            pages.push(i);
        }
        return pages;
    };

    /**
     * Checks if the target element that invokes the click event has the "disabled" class.
     *
     * @param event the click event
     * @returns {boolean} if the target element that has been clicked has the "disabled" class.
     */
    $scope.pagination.isDisabled = function (event) {
        return angular.element(event.target).hasClass('disabled');
    }

    /**
     * Adds a filter and set the default value.
     */
    $scope.addFilter = function () {
        $scope.filters.push({
            field: $scope.filtereableFields[0],
            operator: $scope.operators[0],
            value: ''
        })
    };

    /**
     * Clears all filters.
     */
    $scope.clearFilters = function () {
        $scope.filters = [];
    };

    /**
     * Removes the filter specified by the index from $scope.filters.
     *
     * @param index
     */
    $scope.removeFilter = function (index) {
        if ($scope.filters[index]) {
            $scope.filters.splice(index, 1);
        }
    };

    /**
     * Query the activities depending on the tab currently selected.
     *
     */
    $scope.queryActivities = function () {
        $scope.submitted = false;
        if ($scope.selectedTab == 'ALL') {
            $scope.queryActivitiesAll();
        } else if ($scope.selectedTab == 'YOU_HAVE_CREATED') {
            $scope.getActivitiesCreated();
        } else if ($scope.selectedTab == 'YOU_WILL_ATTEND') {
            $scope.getActivitiesAttend();
        }
    };

    /**
     * Invokes the activity.queryActivities API.
     */
    $scope.queryActivitiesAll = function () {
        var sendFilters = {
            filters: []
        }
        for (var i = 0; i < $scope.filters.length; i++) {
            var filter = $scope.filters[i];
            if (filter.field && filter.operator && filter.value) {
                sendFilters.filters.push({
                    field: filter.field.enumValue,
                    operator: filter.operator.enumValue,
                    value: filter.value
                });
            }
        }
        $scope.loading = true;
        gapi.client.activity.queryActivities(sendFilters).
            execute(function (resp) {
                $scope.$apply(function () {
                    $scope.loading = false;
                    if (resp.error) {
                        // The request has failed.
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to query activities : ' + errorMessage;
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages + ' filters : ' + JSON.stringify(sendFilters));
                    } else {
                        // The request has succeeded.
                        $scope.submitted = false;
                        $scope.messages = 'Query succeeded : ' + JSON.stringify(sendFilters);
                        $scope.alertStatus = 'success';
                        $log.info($scope.messages);

                        $scope.activities = [];
                        angular.forEach(resp.items, function (activity) {
                            $scope.activities.push(activity);
                        });
                    }
                    $scope.submitted = true;
                });
            });
    }

    /**
     * Invokes the activity.getActivitiesCreated method.
     */
    $scope.getActivitiesCreated = function () {
        $scope.loading = true;
        gapi.client.activity.getActivitiesCreated().
            execute(function (resp) {
                $scope.$apply(function () {
                    $scope.loading = false;
                    if (resp.error) {
                        // The request has failed.
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to query the activities created : ' + errorMessage;
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages);

                        if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                            oauth2Provider.showLoginModal();
                            return;
                        }
                    } else {
                        // The request has succeeded.
                        $scope.submitted = false;
                        $scope.messages = 'Query succeeded : activities you have created';
                        $scope.alertStatus = 'success';
                        $log.info($scope.messages);

                        $scope.activities = [];
                        angular.forEach(resp.items, function (activity) {
                            $scope.activities.push(activity);
                        });
                    }
                    $scope.submitted = true;
                });
            });
    };

    /**
     * Retrieves the activities to attend by calling the activity.getProfile method and
     * invokes the activity.getActivity method n times where n == the number of the activities to attend.
     */
    $scope.getActivitiesAttend = function () {
        $scope.loading = true;
        gapi.client.activity.getActivitiesToAttend().
            execute(function (resp) {
                $scope.$apply(function () {
                    if (resp.error) {
                        // The request has failed.
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to query the activities to attend : ' + errorMessage;
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages);

                        if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                            oauth2Provider.showLoginModal();
                            return;
                        }
                    } else {
                        // The request has succeeded.
                        $scope.activities = resp.result.items;
                        $scope.loading = false;
                        $scope.messages = 'Query succeeded : activities you will attend (or you have attended)';
                        $scope.alertStatus = 'success';
                        $log.info($scope.messages);
                    }
                    $scope.submitted = true;
                });
            });
    };
});


/**
 * @ngdoc controller
 * @name ActivityDetailCtrl
 *
 * @description
 * A controller used for the activity detail page.
 */
activityApp.controllers.controller('ActivityDetailCtrl', function ($scope, $log, $routeParams, HTTP_ERRORS) {
    $scope.activity = {};

    $scope.isUserAttending = false;

    /**
     * Initializes the activity detail page.
     * Invokes the activity.getActivity method and sets the returned activity in the $scope.
     *
     */
    $scope.init = function () {
        $scope.loading = true;
        gapi.client.activity.getActivity({
            websafeActivityKey: $routeParams.websafeActivityKey
        }).execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // The request has failed.
                    var errorMessage = resp.error.message || '';
                    $scope.messages = 'Failed to get the activity : ' + $routeParams.websafeKey
                        + ' ' + errorMessage;
                    $scope.alertStatus = 'warning';
                    $log.error($scope.messages);
                } else {
                    // The request has succeeded.
                    $scope.alertStatus = 'success';
                    $scope.activity = resp.result;
                }
            });
        });

        $scope.loading = true;
        // If the user is attending the activity, updates the status message and available function.
        gapi.client.activity.getProfile().execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // Failed to get a user profile.
                } else {
                    var profile = resp.result;
                    for (var i = 0; i < profile.activityKeysToAttend.length; i++) {
                        if ($routeParams.websafeActivityKey == profile.activityKeysToAttend[i]) {
                            // The user is attending the activity.
                            $scope.alertStatus = 'info';
                            $scope.messages = 'You are attending this activity';
                            $scope.isUserAttending = true;
                        }
                    }
                }
            });
        });
    };


    /**
     * Invokes the activity.registerForActivity method.
     */
    $scope.registerForActivity = function () {
        $scope.loading = true;
        gapi.client.activity.registerForActivity({
            websafeActivityKey: $routeParams.websafeActivityKey
        }).execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // The request has failed.
                    var errorMessage = resp.error.message || '';
                    $scope.messages = 'Failed to register for the activity : ' + errorMessage;
                    $scope.alertStatus = 'warning';
                    $log.error($scope.messages);

                    if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                        oauth2Provider.showLoginModal();
                        return;
                    }
                } else {
                    if (resp.result) {
                        // Register succeeded.
                        $scope.messages = 'Registered for the activity';
                        $scope.alertStatus = 'success';
                        $scope.isUserAttending = true;
                        $scope.activity.seatsAvailable = $scope.activity.seatsAvailable - 1;
                    } else {
                        $scope.messages = 'Failed to register for the activity';
                        $scope.alertStatus = 'warning';
                    }
                }
            });
        });
    };

    /**
     * Invokes the activity.unregisterForActivity method.
     */
    $scope.unregisterFromActivity = function () {
        $scope.loading = true;
        gapi.client.activity.unregisterFromActivity({
            websafeActivityKey: $routeParams.websafeActivityKey
        }).execute(function (resp) {
            $scope.$apply(function () {
                $scope.loading = false;
                if (resp.error) {
                    // The request has failed.
                    var errorMessage = resp.error.message || '';
                    $scope.messages = 'Failed to unregister from the activity : ' + errorMessage;
                    $scope.alertStatus = 'warning';
                    $log.error($scope.messages);
                    if (resp.code && resp.code == HTTP_ERRORS.UNAUTHORIZED) {
                        oauth2Provider.showLoginModal();
                        return;
                    }
                } else {
                    if (resp.result) {
                        // Unregister succeeded.
                        $scope.messages = 'Unregistered from the activity';
                        $scope.alertStatus = 'success';
                        $scope.activity.seatsAvailable = $scope.activity.seatsAvailable + 1;
                        $scope.isUserAttending = false;
                        $log.info($scope.messages);
                    } else {
                        var errorMessage = resp.error.message || '';
                        $scope.messages = 'Failed to unregister from the activity : ' + $routeParams.websafeKey +
                            ' : ' + errorMessage;
                        $scope.messages = 'Failed to unregister from the activity';
                        $scope.alertStatus = 'warning';
                        $log.error($scope.messages);
                    }
                }
            });
        });
    };
});


/**
 * @ngdoc controller
 * @name RootCtrl
 *
 * @description
 * The root controller having a scope of the body element and methods used in the application wide
 * such as user authentications.
 *
 */
activityApp.controllers.controller('RootCtrl', function ($scope, $location, oauth2Provider) {

    /**
     * Returns if the viewLocation is the currently viewed page.
     *
     * @param viewLocation
     * @returns {boolean} true if viewLocation is the currently viewed page. Returns false otherwise.
     */
    $scope.isActive = function (viewLocation) {
        return viewLocation === $location.path();
    };

    /**
     * Returns the OAuth2 signedIn state.
     *
     * @returns {oauth2Provider.signedIn|*} true if siendIn, false otherwise.
     */
    $scope.getSignedInState = function () {
        return oauth2Provider.signedIn;
    };

    /**
     * Calls the OAuth2 authentication method.
     */
    $scope.signIn = function () {
        oauth2Provider.signIn(function () {
            gapi.client.oauth2.userinfo.get().execute(function (resp) {
                $scope.$apply(function () {
                    if (resp.email) {
                        oauth2Provider.signedIn = true;
                        $scope.alertStatus = 'success';
                        $scope.rootMessages = 'Logged in with ' + resp.email;
                    }
                });
            });
        });
    };

    /**
     * Render the signInButton and restore the credential if it's stored in the cookie.
     * (Just calling this to restore the credential from the stored cookie. So hiding the signInButton immediately
     *  after the rendering)
     */
    $scope.initSignInButton = function () {
        gapi.signin.render('signInButton', {
            'callback': function () {
                jQuery('#signInButton button').attr('disabled', 'true').css('cursor', 'default');
                if (gapi.auth.getToken() && gapi.auth.getToken().access_token) {
                    $scope.$apply(function () {
                        oauth2Provider.signedIn = true;
                    });
                }
            },
            'clientid': oauth2Provider.CLIENT_ID,
            'cookiepolicy': 'single_host_origin',
            'scope': oauth2Provider.SCOPES
        });
    };

    /**
     * Logs out the user.
     */
    $scope.signOut = function () {
        oauth2Provider.signOut();
        $scope.alertStatus = 'success';
        $scope.rootMessages = 'Logged out';
    };

    /**
     * Collapses the navbar on mobile devices.
     */
    $scope.collapseNavbar = function () {
        angular.element(document.querySelector('.navbar-collapse')).removeClass('in');
    };

});


/**
 * @ngdoc controller
 * @name OAuth2LoginModalCtrl
 *
 * @description
 * The controller for the modal dialog that is shown when an user needs to login to achive some functions.
 *
 */
activityApp.controllers.controller('OAuth2LoginModalCtrl',
    function ($scope, $modalInstance, $rootScope, oauth2Provider) {
        $scope.singInViaModal = function () {
            oauth2Provider.signIn(function () {
                gapi.client.oauth2.userinfo.get().execute(function (resp) {
                    $scope.$root.$apply(function () {
                        oauth2Provider.signedIn = true;
                        $scope.$root.alertStatus = 'success';
                        $scope.$root.rootMessages = 'Logged in with ' + resp.email;
                    });

                    $modalInstance.close();
                });
            });
        };
    });

/**
 * @ngdoc controller
 * @name DatepickerCtrl
 *
 * @description
 * A controller that holds properties for a datepicker.
 */
activityApp.controllers.controller('DatepickerCtrl', function ($scope) {
    $scope.today = function () {
        $scope.dt = new Date();
    };
    $scope.today();

    $scope.clear = function () {
        $scope.dt = null;
    };

    // Disable weekend selection
    $scope.disabled = function (date, mode) {
        return ( mode === 'day' && ( date.getDay() === 0 || date.getDay() === 6 ) );
    };

    $scope.toggleMin = function () {
        $scope.minDate = ( $scope.minDate ) ? null : new Date();
    };
    $scope.toggleMin();

    $scope.open = function ($event) {
        $event.preventDefault();
        $event.stopPropagation();
        $scope.opened = true;
    };

    $scope.dateOptions = {
        'year-format': "'yy'",
        'starting-day': 1
    };

    $scope.formats = ['dd-MMMM-yyyy', 'yyyy/MM/dd', 'shortDate'];
    $scope.format = $scope.formats[0];
});

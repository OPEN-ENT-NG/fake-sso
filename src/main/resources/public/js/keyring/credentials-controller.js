function CredentialController($scope, $rootScope, model, template, route, date, lang) {

	model.credentials.sync(function() {
    	if (model.credentials.all.length > 0) {
    		$scope.credential = model.credentials.all[0];
    	} else {
    		$scope.credential = new Credential();
    	}
	});

	$scope.saveCredential = function(credential) {
		credential.save(function(data) {
			if (data && data.id) {
				credential['id'] = data.id;
				$scope.credential = credential;
				$scope.access();
			}
		});
	};

	$scope.cancelEdit = function() {
		window.location = "/welcome";
	};

	$scope.delete = function(credential) {
		credential.delete(function() {
			$scope.credential = new Credential();
			$scope.$apply();
		});
	};

	$scope.access = function() {
		window.location = "/sso/keyring/access/" + serviceId;
	};

}

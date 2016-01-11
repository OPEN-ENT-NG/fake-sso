
function KeyringAdminController($scope, $rootScope, model, template, route, date, lang) {

	$scope.keyrings = model.keyRings;

	formatViewObject = function(keyring) {
		if (keyring.form_schema) {
			keyring['schema'] = [];
			for (var key in keyring.form_schema) {
				keyring.schema.push(keyring.form_schema[key]);
			}
		}
		keyring['old'] = JSON.parse(JSON.stringify(keyring));
		return keyring;
	}

	$scope.viewKeyring = function(keyring) {
		$scope.keyring = formatViewObject(keyring);
	};

	$scope.saveKeyring = function(keyring) {
		if (keyring && keyring.schema) {
            keyring['form_schema'] = {};
            for (var i = 0; i < keyring.schema.length; ++i) {
            	keyring.form_schema[keyring.schema[i].name] = keyring.schema[i];
			}
		}
		if (keyring.id && keyring.service_id && !$scope.schemaEqual(keyring)) {
			var action = function() {
				$scope.persist(keyring);
			};
			$scope.notifyTop(lang.translate('keyring.confirm.edit.schema'), action);
		} else {
			$scope.persist(keyring);
		}
	};

	$scope.persist = function(keyring) {
		keyring.save(function(data) {
			if (data) {
				keyring.id = data.id;
				keyring.service_id = data.service_id;
			}
			delete keyring.old;
			if (!$scope.keyrings.findWhere({ name: keyring.name })) {
				$scope.keyrings.push(keyring);
			}
			$scope.viewKeyring(keyring);
		});
	};

	$scope.deleteKeyring = function(keyring) {
		var action = function() {
			keyring.delete(function() {
				$scope.keyrings.sync(function() { $scope.$apply });
				//$scope.keyrings = model.keyRings;
//        		_.reject($scope.keyrings, function(el) { return el.id === keyring.id; });
        		$scope.keyring = $scope.newKeyRing();
        	});
		};
		$scope.notifyTop(lang.translate('keyring.confirm.deletion'), action);
	};

	$scope.newKeyRing = function() {
		$scope.keyring = new KeyRing();
		$scope.keyring['schema'] = [];
	};

	$scope.addLine = function(keyring) {
		var idx = keyring.schema.length;
		keyring.schema.push({"name" : "", "display" : "", "type" : ""});
	};

	$scope.removeLine = function(keyring, idx) {
		keyring.schema.splice(idx, 1);
	};

	$scope.schemaEqual = function(keyring) {
		var k = JSON.parse(JSON.stringify(keyring));
		if (k.form_schema && k.old) {
			for (var attr in k.form_schema) {
				delete k.form_schema[attr].$$hashKey;
			}
			if (k.old.form_schema && _.isEqual(k.form_schema, k.old.form_schema)) {
				return true;
			}
		}
		return false;
	};

	/////// TOP NOTIFICATIONS ///////
	$scope.topNotification = {
		show: false,
		message: "",
		confirm: null
	}
	$scope.notifyTop = function(text, action){
		$scope.topNotification.message = "<p>"+text+"</p>"
		$scope.topNotification.confirm = action
		$scope.topNotification.show = true
	}
	$scope.colourText = function(text){
		return '<span class="colored">'+text+'</span>'
	}

}

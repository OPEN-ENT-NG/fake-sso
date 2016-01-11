function KeyRing() {}

/**
 * Allows to save the keyring. If the keyring is new and does not have any id set,
 * this method calls the create method otherwise it calls the update method.
 * @param callback a function to call after saving.
 */
KeyRing.prototype.save = function(callback) {
	if (this.service_id) {
		this.update(callback);
	} else {
		this.create(callback);
	}
};


/**
 * Allows to create a new keyring. This method calls the REST web service to
 * persist data.
 * @param callback a function to call after create.
 */
KeyRing.prototype.create = function(callback) {
    http().postJson('/sso/keyring', this.cleanRequestObject()).done(function(data) {
        if (data) {
            this.id = data.id;
            this.service_id = data.service_id;
        }
        if(typeof callback === 'function') {
            callback(data);
        }
    });
};

/**
 * Allows to update the keyring. This method calls the REST web service to persist
 * data.
 * @param callback a function to call after create.
 */
KeyRing.prototype.update = function(callback) {
    var o = this.cleanRequestObject();
    if (_.size(o) > 0) {
        http().putJson('/sso/keyring/' + this.service_id, o).done(function() {
            if(typeof callback === 'function') {
                callback();
            }
        });
    }
};

/**
 * Allows to delete the keyring. This method calls the REST web service to delete
 * data.
 * @param callback a function to call after delete.
 */
KeyRing.prototype.delete = function(callback) {
    http().delete('/sso/keyring/' + this.service_id).done(function() {
        model.keyRings.remove(this);
        if(typeof callback === 'function'){
            callback();
        }
    });
};

KeyRing.prototype.cleanRequestObject = function() {
    var k = JSON.parse(JSON.stringify(this));
    if (this.service_id && k.old) {
        if (k.form_schema) {
            for (var attr in k.form_schema) {
                delete k.form_schema[attr].$$hashKey;
            }
            if (k.old.form_schema && _.isEqual(k.form_schema, k.old.form_schema)) {
                delete k.form_schema;
            }
        }
        delete k.old.form_schema;
        delete k.old.schema
        for (var key in k.old) {
            if (k[key] === k.old[key]) {
                delete k[key];
            }
        }
    }
    delete k.old;
    delete k.id;
    delete k.service_id;
    delete k.schema;
    return k;
};

model.build = function() {
	this.makeModel(KeyRing);

    this.collection(KeyRing, {
        sync: function(callback){
            http().get('/sso/keyring').done(function(keyrings) {
                this.load(keyrings);
                if(typeof callback === 'function') {
                    callback();
                }
            }.bind(this));
        }
    });

}

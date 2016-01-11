var p = window.location.pathname.split('/'),
serviceId = p[p.length - 1];

function Credential() {}

/**
 * Allows to save the keyring. If the credential is new and does not have any id set,
 * this method calls the create method otherwise it calls the update method.
 * @param callback a function to call after saving.
 */
Credential.prototype.save = function(callback) {
	if (this.id) {
		this.update(callback);
	} else {
		this.create(callback);
	}
};


/**
 * Allows to create a new credential. This method calls the REST web service to
 * persist data.
 * @param callback a function to call after create.
 */
Credential.prototype.create = function(callback) {
    http().postJson('/sso/keyring/' + serviceId, this).done(function(data) {
        if(typeof callback === 'function'){
            callback(data);
        }
    });
};

/**
 * Allows to update credential. This method calls the REST web service to persist
 * data.
 * @param callback a function to call after create.
 */
Credential.prototype.update = function(callback) {
    http().putJson('/sso/keyring/' + serviceId + '/' + this.id, this).done(function() {
        if(typeof callback === 'function'){
            callback();
        }
    });
};

/**
 * Allows to delete credential. This method calls the REST web service to delete
 * data.
 * @param callback a function to call after delete.
 */
Credential.prototype.delete = function(callback) {
    http().delete('/sso/keyring/' + serviceId + '/' + this.id).done(function() {
        model.credentials.remove(this);
        if(typeof callback === 'function'){
            callback();
        }
    }.bind(this));
};

/**
 * Allows to create a model and load the list of credentials from the backend.
 */
 model.build = function(){
 	this.makeModel(Credential);

     this.collection(Credential, {
         sync: function(callback){
             http().get('/sso/keyring/credentials/' + serviceId).done(function(credentials){
                 this.load(credentials);
                 if(typeof callback === 'function'){
                     callback();
                 }
             }.bind(this));
         }
     });

 }

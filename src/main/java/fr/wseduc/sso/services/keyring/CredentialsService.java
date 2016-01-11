package fr.wseduc.sso.services.keyring;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface CredentialsService {

	void insert(String serviceId, JsonObject data, Handler<Either<String, JsonObject>> handler);

	void getCredentials(String serviceId, String userId, Handler<Either<String, JsonArray>> handler);

	void update(String serviceId, String id, JsonObject data, Handler<Either<String, JsonObject>> handler);

	void delete(String serviceId, String id, Handler<Either<String, JsonObject>> handler);

}

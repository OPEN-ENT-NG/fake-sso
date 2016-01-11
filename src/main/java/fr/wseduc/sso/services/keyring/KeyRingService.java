package fr.wseduc.sso.services.keyring;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

public interface KeyRingService {

	void create(JsonObject data, Handler<Either<String, JsonObject>> handler);

	void alter(String serviceId, JsonObject data, Handler<Either<String, JsonObject>> handler);

	void drop(String serviceId, Handler<Either<String, JsonObject>> handler);

	void description(String serviceId, Handler<Either<String, JsonObject>> handler);

	void listServices(Handler<Either<String, JsonArray>> handler);

}

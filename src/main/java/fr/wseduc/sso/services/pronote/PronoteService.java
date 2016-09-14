package fr.wseduc.sso.services.pronote;

import fr.wseduc.webutils.Either;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.List;

public interface PronoteService {
	void getPronoteApps(List<String> groupIds,
						final Handler<Either<String, JsonArray>> handler);

	void generateTicketByApp(JsonArray appArray, String userId, String pronoteContext, String collection, final Handler<Either<String, JsonArray>> handler);

	void getFromCache(String userId, final Handler<JsonObject> handler);

	void storeInCache(String userId, JsonArray ja, final Handler<Boolean> handler);
}

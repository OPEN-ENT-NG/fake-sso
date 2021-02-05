package fr.wseduc.sso.services.pronote;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface PronoteService {
	void getPronoteApps(List<String> groupIds,
						final Handler<Either<String, JsonArray>> handler);

	void generateTicketByApp(JsonArray appArray, UserInfos user, String pronoteContext, String collection, final Handler<Either<String, JsonArray>> handler);

	void getFromCache(String userId, final Handler<JsonObject> handler);

	void storeInCache(String userId, JsonArray ja, final Handler<Boolean> handler);
}
